package com.veeva.vault.toolbox.intellij.language.vql;

import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.project.Project;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.util.ProcessingContext;
import com.veeva.vault.toolbox.intellij.metadata.MetadataIndex;
import com.veeva.vault.toolbox.intellij.metadata.MetadataService;
import com.veeva.vault.toolbox.intellij.metadata.model.FieldMeta;
import icons.ToolboxIcons;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Provides VQL completions in the console editor: object names after {@code FROM},
 * field names of the queried object elsewhere, and the VQL keyword set. Object and
 * field names are sourced from the shared {@link MetadataService} cache (EDT-safe,
 * no network on the completion path); fields are lazily fetched on first use.
 */
public class VqlCompletionContributor extends CompletionContributor {

    private static final Pattern FROM_OBJECT =
            Pattern.compile("(?i)\\bfrom\\s+([A-Za-z_][A-Za-z0-9_]*)");

    public VqlCompletionContributor() {
        extend(CompletionType.BASIC, PlatformPatterns.psiElement(), new CompletionProvider<>() {
            @Override
            protected void addCompletions(@NotNull CompletionParameters parameters,
                                          @NotNull ProcessingContext context,
                                          @NotNull CompletionResultSet result) {
                addVqlCompletions(parameters, result);
            }
        });
    }

    private static void addVqlCompletions(CompletionParameters parameters, CompletionResultSet result) {
        Project project = parameters.getPosition().getProject();
        String text = parameters.getEditor().getDocument().getImmutableCharSequence().toString();
        int offset = Math.min(parameters.getOffset(), text.length());
        String before = text.substring(0, offset);

        String prefix = wordPrefix(before);
        CompletionResultSet rs = result.withPrefixMatcher(prefix);

        MetadataService service = MetadataService.getInstance(project);
        MetadataIndex index = service.getIndex();

        if ("from".equalsIgnoreCase(previousWord(before, prefix.length()))) {
            addObjects(rs, index);
            return;
        }

        String object = objectInFrom(text);
        if (object != null) {
            service.ensureObjectFieldsLoaded(object);
            for (FieldMeta field : index.fieldsFor(object)) {
                String type = field.getType() != null ? field.getType() : "";
                rs.addElement(LookupElementBuilder.create(field.getName())
                        .withIcon(ToolboxIcons.Code)
                        .withTypeText(type));
            }
        }
        addKeywords(rs);
        addObjects(rs, index);
    }

    private static void addObjects(CompletionResultSet rs, MetadataIndex index) {
        for (String object : index.objectNames()) {
            rs.addElement(LookupElementBuilder.create(object).withIcon(ToolboxIcons.Component));
        }
    }

    private static void addKeywords(CompletionResultSet rs) {
        for (String keyword : VqlLexer.KEYWORDS) {
            rs.addElement(LookupElementBuilder.create(keyword.toUpperCase()).bold());
        }
    }

    /** @return the object name referenced by the first top-level {@code FROM}, or {@code null}. */
    private static String objectInFrom(String text) {
        Matcher matcher = FROM_OBJECT.matcher(text);
        return matcher.find() ? matcher.group(1) : null;
    }

    /** The identifier characters immediately preceding the caret. */
    private static String wordPrefix(String before) {
        int i = before.length();
        while (i > 0 && isWordPart(before.charAt(i - 1))) {
            i--;
        }
        return before.substring(i);
    }

    /** The whole word that appears just before the prefix currently being typed. */
    private static String previousWord(String before, int prefixLength) {
        int end = before.length() - prefixLength;
        while (end > 0 && Character.isWhitespace(before.charAt(end - 1))) {
            end--;
        }
        int start = end;
        while (start > 0 && isWordPart(before.charAt(start - 1))) {
            start--;
        }
        return before.substring(start, end);
    }

    private static boolean isWordPart(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }
}
