package com.veeva.vault.toolbox.intellij.language.annotator;

import com.intellij.lang.ASTNode;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.ExternalAnnotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IElementType;
import com.veeva.vault.toolbox.intellij.language.MdlPsiContext;
import com.veeva.vault.toolbox.intellij.language.psi.MdlTypes;
import com.veeva.vault.toolbox.intellij.metadata.MetadataIndex;
import com.veeva.vault.toolbox.intellij.metadata.MetadataIndex.Existence;
import com.veeva.vault.toolbox.intellij.metadata.MetadataService;
import com.veeva.vault.toolbox.intellij.metadata.mdl.MdlReferenceKindRegistry;
import com.veeva.vault.toolbox.intellij.metadata.mdl.MdlReferenceKindRegistry.RefKind;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Validates the names an MDL file references against the connected vault's cached schema and
 * squiggles the ones that do not exist (e.g. a typo'd object name in {@code object('prdct__v')}
 * or an unknown component type). Only name-level references are checked in this milestone.
 *
 * <p>Implemented as an {@link ExternalAnnotator} so the schema lookups run off the EDT: PSI is
 * traversed in {@link #collectInformation(PsiFile)} (read action, EDT), existence is resolved in
 * {@link #doAnnotate(CollectedInfo)} (background thread, no PSI access), and annotations are
 * created in {@link #apply}. Only {@link Existence#MISSING} is ever flagged — {@code UNKNOWN}
 * (not connected, snapshot absent, or a slice not yet loaded) is treated as "do not flag" to
 * avoid false positives.</p>
 */
public class MdlSchemaAnnotator extends ExternalAnnotator<MdlSchemaAnnotator.CollectedInfo, List<MdlSchemaAnnotator.Problem>> {

    /** Maximum number of "Change to 'X'" suggestions offered per problem. */
    private static final int MAX_SUGGESTIONS = 3;

    /** Top-level command element types whose COMPONENT_TYPE names exist in the metadata index. */
    private static final Set<IElementType> TOP_LEVEL_COMMANDS = Set.of(
            MdlTypes.RECORD_CHANGE_COMMAND,
            MdlTypes.COMPONENT_CHANGE_COMMAND,
            MdlTypes.RECORD_DROP_COMMAND,
            MdlTypes.COMPONENT_DROP_COMMAND,
            MdlTypes.RECORD_RENAME_COMMAND,
            MdlTypes.COMPONENT_RENAME_COMMAND);

    /** Nested subcomponent command element types whose COMPONENT_TYPE names are not top-level. */
    private static final Set<IElementType> SUBCOMPONENT_COMMANDS = Set.of(
            MdlTypes.SUBCOMPONENT_CHANGE_COMMAND,
            MdlTypes.SUBCOMPONENT_DROP_COMMAND,
            MdlTypes.SUBCOMPONENT_RENAME_COMMAND);

    @Nullable
    @Override
    public CollectedInfo collectInformation(@NotNull PsiFile file) {
        List<Candidate> candidates = new ArrayList<>();
        collect(file, candidates);
        if (candidates.isEmpty()) {
            return null;
        }
        return new CollectedInfo(file.getProject(), candidates);
    }

    private void collect(PsiElement element, List<Candidate> candidates) {
        for (PsiElement child = element.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child.getNode() == null) {
                continue;
            }
            if (child.getNode().getElementType() == MdlTypes.COMPONENT_TYPE) {
                // Only validate top-level component types; subcomponent types (e.g. Picklistentry,
                // Field) are nested and are not returned by the component metadata API, so they must
                // not be flagged.
                if (isTopLevelComponentType(child)) {
                    candidates.add(new Candidate(CheckKind.COMPONENT_TYPE, child.getText(), child.getTextRange()));
                }
            } else if (child.getNode().getElementType() == MdlTypes.STRING_VALUE) {
                addStringCandidate(child, candidates);
            }
            collect(child, candidates);
        }
    }

    /**
     * @return whether the {@code COMPONENT_TYPE} token sits in a top-level command (e.g.
     * {@code RECREATE Picklist x}) rather than a nested subcomponent command (e.g.
     * {@code Picklistentry y}). Decided by the nearest enclosing command in the AST.
     */
    private boolean isTopLevelComponentType(PsiElement componentTypeLeaf) {
        ASTNode node = componentTypeLeaf.getNode().getTreeParent();
        while (node != null) {
            IElementType type = node.getElementType();
            if (SUBCOMPONENT_COMMANDS.contains(type)) {
                return false;
            }
            if (TOP_LEVEL_COMMANDS.contains(type)) {
                return true;
            }
            node = node.getTreeParent();
        }
        return false;
    }

    private void addStringCandidate(PsiElement stringValue, List<Candidate> candidates) {
        String attributeName = MdlPsiContext.attributeNameOf(stringValue);
        if (attributeName == null) {
            return;
        }
        RefKind kind = MdlReferenceKindRegistry.kindFor(MdlPsiContext.componentTypeOf(stringValue), attributeName);
        CheckKind checkKind = toCheckKind(kind);
        if (checkKind == null) {
            return;
        }
        MdlPsiContext.UnquotedValue value = MdlPsiContext.unquote(stringValue);
        if (value == null || value.getText().isEmpty()) {
            return;
        }
        // MDL references may be qualified (e.g. Picklist.default_status__v). Validate the bare name
        // against the index, and underline/replace only the name portion so any fix keeps the prefix.
        String raw = value.getText();
        String bareName = MdlPsiContext.bareName(raw);
        TextRange range = value.getRange();
        if (!bareName.equals(raw)) {
            range = new TextRange(range.getEndOffset() - bareName.length(), range.getEndOffset());
        }
        candidates.add(new Candidate(checkKind, bareName, range));
    }

    @Nullable
    @Override
    public List<Problem> doAnnotate(CollectedInfo info) {
        MetadataService service = MetadataService.getInstance(info.project);
        if (service == null) {
            return null;
        }
        MetadataIndex index = service.getIndex();
        if (!index.isReady()) {
            return null;
        }

        List<Problem> problems = new ArrayList<>();
        for (Candidate candidate : info.candidates) {
            Existence existence = existenceOf(candidate, index);
            if (existence != Existence.MISSING) {
                continue;
            }
            Collection<String> universe = universeOf(candidate.kind, index);
            problems.add(new Problem(candidate.range, candidate.describe(),
                    suggest(candidate.name, universe)));
        }
        return problems.isEmpty() ? null : problems;
    }

    @Override
    public void apply(@NotNull PsiFile file, List<Problem> problems, @NotNull AnnotationHolder holder) {
        for (Problem problem : problems) {
            var builder = holder.newAnnotation(HighlightSeverity.WARNING, problem.message)
                    .range(problem.range);
            for (String suggestion : problem.suggestions) {
                builder = builder.withFix(new ChangeReferenceFix(problem.range, suggestion));
            }
            builder.create();
        }
    }

    private Existence existenceOf(Candidate candidate, MetadataIndex index) {
        switch (candidate.kind) {
            case OBJECT:
                return index.objectExists(candidate.name);
            case PICKLIST:
                return index.picklistExists(candidate.name);
            case COMPONENT_TYPE:
                Existence existence = index.componentTypeExists(candidate.name);
                // Component type names are canonical-cased; do not flag a mere casing difference.
                if (existence == Existence.MISSING && containsIgnoreCase(index.componentTypeNames(), candidate.name)) {
                    return Existence.EXISTS;
                }
                return existence;
            default:
                return Existence.UNKNOWN;
        }
    }

    private Collection<String> universeOf(CheckKind kind, MetadataIndex index) {
        switch (kind) {
            case OBJECT:
                return index.objectNames();
            case PICKLIST:
                return index.picklistNames();
            case COMPONENT_TYPE:
                return index.componentTypeNames();
            default:
                return List.of();
        }
    }

    private static boolean containsIgnoreCase(Collection<String> names, String target) {
        for (String name : names) {
            if (name.equalsIgnoreCase(target)) {
                return true;
            }
        }
        return false;
    }

    /** Returns up to {@link #MAX_SUGGESTIONS} close names by edit distance, nearest first. */
    private static List<String> suggest(String name, Collection<String> universe) {
        int threshold = Math.max(2, name.length() / 3);
        List<String> ranked = new ArrayList<>();
        List<int[]> distances = new ArrayList<>();
        for (String candidate : universe) {
            int distance = levenshtein(name, candidate, threshold);
            if (distance >= 0 && distance <= threshold) {
                insertRanked(ranked, distances, candidate, distance);
            }
        }
        return ranked.size() > MAX_SUGGESTIONS ? ranked.subList(0, MAX_SUGGESTIONS) : ranked;
    }

    private static void insertRanked(List<String> ranked, List<int[]> distances, String candidate, int distance) {
        int i = 0;
        while (i < distances.size() && distances.get(i)[0] <= distance) {
            i++;
        }
        ranked.add(i, candidate);
        distances.add(i, new int[]{distance});
    }

    /** Bounded Levenshtein distance; returns -1 when it provably exceeds {@code max}. */
    private static int levenshtein(String a, String b, int max) {
        int n = a.length();
        int m = b.length();
        if (Math.abs(n - m) > max) {
            return -1;
        }
        int[] prev = new int[m + 1];
        int[] curr = new int[m + 1];
        for (int j = 0; j <= m; j++) {
            prev[j] = j;
        }
        for (int i = 1; i <= n; i++) {
            curr[0] = i;
            int rowMin = curr[0];
            for (int j = 1; j <= m; j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                curr[j] = Math.min(Math.min(curr[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
                rowMin = Math.min(rowMin, curr[j]);
            }
            if (rowMin > max) {
                return -1;
            }
            int[] tmp = prev;
            prev = curr;
            curr = tmp;
        }
        return prev[m];
    }

    @Nullable
    private static CheckKind toCheckKind(RefKind kind) {
        switch (kind) {
            case OBJECT:
                return CheckKind.OBJECT;
            case PICKLIST:
                return CheckKind.PICKLIST;
            default:
                return null;
        }
    }

    /** What a candidate should be validated against. */
    private enum CheckKind {
        OBJECT("object"),
        PICKLIST("picklist"),
        COMPONENT_TYPE("component type");

        private final String label;

        CheckKind(String label) {
            this.label = label;
        }
    }

    /** A reference found in the file to be validated. */
    private static final class Candidate {
        private final CheckKind kind;
        private final String name;
        private final TextRange range;

        Candidate(CheckKind kind, String name, TextRange range) {
            this.kind = kind;
            this.name = name;
            this.range = range;
        }

        String describe() {
            return "Unknown " + kind.label + " '" + name + "' — not found in the connected vault";
        }
    }

    /** Information gathered on the EDT and handed to the background {@link #doAnnotate}. */
    static final class CollectedInfo {
        private final Project project;
        private final List<Candidate> candidates;

        CollectedInfo(Project project, List<Candidate> candidates) {
            this.project = project;
            this.candidates = candidates;
        }
    }

    /** A resolved problem to be turned into an annotation on the EDT. */
    static final class Problem {
        private final TextRange range;
        private final String message;
        private final List<String> suggestions;

        Problem(TextRange range, String message, List<String> suggestions) {
            this.range = range;
            this.message = message;
            this.suggestions = suggestions;
        }
    }
}
