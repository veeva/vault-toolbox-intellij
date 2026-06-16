package com.veeva.vault.toolbox.intellij.inspections;

import com.intellij.codeInspection.AbstractBaseJavaLocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * Inspection that flags use of the Java Reflection API in Vault Java SDK projects.
 * Reflection is not permitted in the Vault sandbox.
 * <p>
 * Catches explicit imports of {@code java.lang.reflect.*} and direct calls to
 * reflection entry points on {@code java.lang.Class} (e.g. {@code Class.forName()},
 * {@code getClass().getMethod()}) that do not require an explicit import.
 */
public class VaultSdkReflectionInspection extends AbstractBaseJavaLocalInspectionTool {

    private static final Set<String> REFLECT_CLASS_METHODS = Set.of(
            "forName",
            "getMethod", "getDeclaredMethod", "getMethods", "getDeclaredMethods",
            "getField", "getDeclaredField", "getFields", "getDeclaredFields",
            "getConstructor", "getDeclaredConstructor", "getConstructors", "getDeclaredConstructors",
            "newInstance",
            "getAnnotation", "getDeclaredAnnotation", "getAnnotations", "getDeclaredAnnotations",
            "getAnnotationsByType", "getDeclaredAnnotationsByType"
    );

    @Override
    public @NotNull PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new JavaElementVisitor() {

            @Override
            public void visitImportStatement(@NotNull PsiImportStatement statement) {
                if (!VaultSdkUtils.isVaultSdkProject(statement)) return;
                String name = statement.getQualifiedName();
                if (name != null && name.startsWith("java.lang.reflect")) {
                    holder.registerProblem(statement,
                            "Vault Java SDK: Java reflection (java.lang.reflect) is not permitted");
                }
            }

            @Override
            public void visitMethodCallExpression(@NotNull PsiMethodCallExpression expression) {
                if (!VaultSdkUtils.isVaultSdkProject(expression)) return;
                String methodName = expression.getMethodExpression().getReferenceName();
                if (methodName == null || !REFLECT_CLASS_METHODS.contains(methodName)) return;
                PsiMethod method = expression.resolveMethod();
                if (method == null) return;
                PsiClass containingClass = method.getContainingClass();
                if (containingClass != null && "java.lang.Class".equals(containingClass.getQualifiedName())) {
                    holder.registerProblem(expression,
                            "Vault Java SDK: Java reflection (" + methodName + "()) is not permitted");
                }
            }
        };
    }
}
