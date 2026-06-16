package com.veeva.vault.toolbox.intellij.inspections;

import com.intellij.codeInspection.AbstractBaseJavaLocalInspectionTool;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;

/**
 * Inspection that flags static mutable fields and {@code volatile} fields
 * in Vault Java SDK projects.
 * <p>
 * Static mutable fields can retain state across invocations in unpredictable ways
 * inside the Vault sandbox. {@code volatile} fields imply multi-threaded access,
 * which is not permitted.
 */
public class VaultSdkStaticStateInspection extends AbstractBaseJavaLocalInspectionTool {

    @Override
    public @NotNull PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new JavaElementVisitor() {
            @Override
            public void visitField(@NotNull PsiField field) {
                if (!VaultSdkUtils.isVaultSdkProject(field)) return;
                PsiModifierList modifiers = field.getModifierList();
                if (modifiers == null) return;
                if (modifiers.hasModifierProperty(PsiModifier.STATIC)
                        && !modifiers.hasModifierProperty(PsiModifier.FINAL)) {
                    holder.registerProblem(
                            field.getNameIdentifier(),
                            "Vault Java SDK: Static mutable fields are not permitted; use static final constants instead",
                            new AddFinalModifierFix()
                    );
                }
                if (modifiers.hasModifierProperty(PsiModifier.VOLATILE)) {
                    for (PsiElement child : modifiers.getChildren()) {
                        if (child instanceof PsiKeyword && PsiKeyword.VOLATILE.equals(child.getText())) {
                            holder.registerProblem(child,
                                    "Vault Java SDK: volatile fields are not permitted (implies multi-threading)");
                            return;
                        }
                    }
                }
            }
        };
    }

    /**
     * Quick-fix that adds the {@code final} modifier to a static field,
     * converting it from a mutable variable to a constant.
     */
    private static class AddFinalModifierFix implements LocalQuickFix {

        @Override
        public @NotNull String getName() {
            return "Add 'final' modifier";
        }

        @Override
        public @NotNull String getFamilyName() {
            return "Vault Java SDK";
        }

        /**
         * Adds {@code final} to the modifier list of the field identified by the descriptor.
         *
         * @param project    the current project
         * @param descriptor the problem descriptor identifying the flagged field name identifier
         */
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            PsiElement element = descriptor.getPsiElement();
            if (!(element.getParent() instanceof PsiField field)) return;
            PsiModifierList modifiers = field.getModifierList();
            if (modifiers != null) {
                modifiers.setModifierProperty(PsiModifier.FINAL, true);
            }
        }
    }
}
