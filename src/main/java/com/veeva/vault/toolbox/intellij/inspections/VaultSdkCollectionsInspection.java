package com.veeva.vault.toolbox.intellij.inspections;

import com.intellij.codeInspection.AbstractBaseJavaLocalInspectionTool;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;

/**
 * Inspection that flags use of standard Java collection classes and
 * {@code java.util.stream.Collectors} in Vault Java SDK projects.
 * <p>
 * The Vault sandbox requires {@code VaultCollections} for collection instantiation
 * and {@code VaultCollectors} for stream terminal operations. Provides quick-fixes
 * for {@code Collectors.toList()} and {@code Collectors.toSet()}.
 */
public class VaultSdkCollectionsInspection extends AbstractBaseJavaLocalInspectionTool {

    private static final Set<String> FORBIDDEN_COLLECTION_CLASSES = Set.of(
            "java.util.ArrayList",
            "java.util.LinkedList",
            "java.util.HashMap",
            "java.util.LinkedHashMap",
            "java.util.TreeMap",
            "java.util.HashSet",
            "java.util.LinkedHashSet",
            "java.util.TreeSet",
            "java.util.ArrayDeque",
            "java.util.Vector",
            "java.util.Stack"
    );

    private static final Map<String, String> COLLECTION_REPLACEMENTS = Map.of(
            "java.util.ArrayList", "VaultCollections.newList()",
            "java.util.LinkedList", "VaultCollections.newList()",
            "java.util.HashMap", "VaultCollections.newMap()",
            "java.util.LinkedHashMap", "VaultCollections.newMap()",
            "java.util.HashSet", "VaultCollections.newSet()",
            "java.util.LinkedHashSet", "VaultCollections.newSet()"
    );

    private static final Map<String, String> COLLECTORS_REPLACEMENTS = Map.of(
            "toList", "VaultCollectors.toList()",
            "toSet", "VaultCollectors.toSet()"
    );

    @Override
    public @NotNull PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new JavaElementVisitor() {

            @Override
            public void visitImportStatement(@NotNull PsiImportStatement statement) {
                if (!VaultSdkUtils.isVaultSdkProject(statement)) return;
                if ("java.util.stream.Collectors".equals(statement.getQualifiedName())) {
                    holder.registerProblem(statement,
                            "Vault Java SDK: java.util.stream.Collectors is not permitted; use VaultCollectors instead");
                }
            }

            @Override
            public void visitMethodCallExpression(@NotNull PsiMethodCallExpression expression) {
                if (!VaultSdkUtils.isVaultSdkProject(expression)) return;
                String methodName = expression.getMethodExpression().getReferenceName();
                if (methodName == null) return;
                PsiMethod method = expression.resolveMethod();
                if (method == null) return;
                PsiClass containingClass = method.getContainingClass();
                if (containingClass == null
                        || !"java.util.stream.Collectors".equals(containingClass.getQualifiedName())) return;

                String replacement = COLLECTORS_REPLACEMENTS.get(methodName);
                if (replacement != null) {
                    holder.registerProblem(expression,
                            "Vault Java SDK: Collectors." + methodName + "() is not permitted; use " + replacement,
                            new ReplaceCollectorQualifierFix(replacement));
                } else {
                    holder.registerProblem(expression,
                            "Vault Java SDK: Collectors." + methodName + "() is not permitted; use VaultCollectors instead");
                }
            }

            @Override
            public void visitNewExpression(@NotNull PsiNewExpression expression) {
                if (!VaultSdkUtils.isVaultSdkProject(expression)) return;
                PsiJavaCodeReferenceElement classRef = expression.getClassOrAnonymousClassReference();
                if (classRef == null) return;
                PsiElement resolved = classRef.resolve();
                if (!(resolved instanceof PsiClass psiClass)) return;
                String qName = psiClass.getQualifiedName();
                if (qName == null || !FORBIDDEN_COLLECTION_CLASSES.contains(qName)) return;

                String replacement = COLLECTION_REPLACEMENTS.get(qName);
                String message = replacement != null
                        ? "Vault Java SDK: new " + psiClass.getName() + "() is not permitted; use " + replacement
                        : "Vault Java SDK: new " + psiClass.getName() + "() is not permitted; use VaultCollections instead";
                holder.registerProblem(expression, message);
            }
        };
    }

    /**
     * Quick-fix that replaces the {@code Collectors} qualifier with {@code VaultCollectors},
     * e.g. {@code Collectors.toList()} becomes {@code VaultCollectors.toList()}.
     */
    private static class ReplaceCollectorQualifierFix implements LocalQuickFix {

        private final String replacement;

        /**
         * @param replacement the {@code VaultCollectors} expression used in the quick-fix label
         */
        ReplaceCollectorQualifierFix(String replacement) {
            this.replacement = replacement;
        }

        @Override
        public @NotNull String getName() {
            return "Replace with " + replacement;
        }

        @Override
        public @NotNull String getFamilyName() {
            return "Vault Java SDK";
        }

        /**
         * Replaces the qualifier expression of the flagged method call with
         * a {@code VaultCollectors} reference.
         *
         * @param project    the current project
         * @param descriptor the problem descriptor identifying the flagged call expression
         */
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            PsiElement element = descriptor.getPsiElement();
            if (!(element instanceof PsiMethodCallExpression callExpr)) return;
            @Nullable PsiExpression qualifier = callExpr.getMethodExpression().getQualifierExpression();
            if (qualifier == null) return;
            qualifier.replace(JavaPsiFacade.getElementFactory(project)
                    .createExpressionFromText("VaultCollectors", element));
        }
    }
}
