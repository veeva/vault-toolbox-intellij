package com.veeva.vault.toolbox.intellij.inspections;

import com.intellij.codeInspection.AbstractBaseJavaLocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * Inspection that flags multi-threading constructs in Vault Java SDK projects.
 * The Vault sandbox executes user code in a single-threaded context; spawning
 * or synchronizing threads produces undefined behavior.
 * <p>
 * Catches imports and instantiation of {@code Thread}, {@code ThreadLocal},
 * {@code java.util.concurrent.*}, subclasses of {@code Thread}, {@code synchronized}
 * blocks and methods, and static {@code Thread} method calls such as
 * {@code Thread.sleep()} that do not require an explicit import.
 */
public class VaultSdkThreadingInspection extends AbstractBaseJavaLocalInspectionTool {

    private static final String MESSAGE = "Vault Java SDK: Multi-threading is not permitted";

    private static final Set<String> FORBIDDEN_THREAD_CLASSES = Set.of(
            "java.lang.Thread",
            "java.lang.ThreadLocal",
            "java.util.concurrent.ThreadPoolExecutor",
            "java.util.concurrent.ForkJoinPool",
            "java.util.concurrent.CompletableFuture",
            "java.util.concurrent.ScheduledThreadPoolExecutor"
    );

    private static final Set<String> FORBIDDEN_THREAD_METHODS = Set.of(
            "sleep", "yield", "interrupted", "currentThread"
    );

    @Override
    public @NotNull PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new JavaElementVisitor() {

            @Override
            public void visitImportStatement(@NotNull PsiImportStatement statement) {
                if (!VaultSdkUtils.isVaultSdkProject(statement)) return;
                String name = statement.getQualifiedName();
                if (name != null && (name.equals("java.lang.Thread")
                        || name.equals("java.lang.ThreadLocal")
                        || name.startsWith("java.util.concurrent"))) {
                    holder.registerProblem(statement, MESSAGE);
                }
            }

            @Override
            public void visitClass(@NotNull PsiClass aClass) {
                if (!VaultSdkUtils.isVaultSdkProject(aClass)) return;
                PsiReferenceList extendsList = aClass.getExtendsList();
                if (extendsList == null) return;
                for (PsiJavaCodeReferenceElement ref : extendsList.getReferenceElements()) {
                    PsiElement resolved = ref.resolve();
                    if (resolved instanceof PsiClass superClass
                            && "java.lang.Thread".equals(superClass.getQualifiedName())) {
                        holder.registerProblem(ref, MESSAGE + " (extending Thread)");
                    }
                }
            }

            @Override
            public void visitNewExpression(@NotNull PsiNewExpression expression) {
                if (!VaultSdkUtils.isVaultSdkProject(expression)) return;
                PsiJavaCodeReferenceElement classRef = expression.getClassOrAnonymousClassReference();
                if (classRef == null) return;
                PsiElement resolved = classRef.resolve();
                if (resolved instanceof PsiClass psiClass) {
                    String qName = psiClass.getQualifiedName();
                    if (qName != null && FORBIDDEN_THREAD_CLASSES.contains(qName)) {
                        holder.registerProblem(expression, MESSAGE + " (" + psiClass.getName() + ")");
                    }
                }
            }

            @Override
            public void visitMethodCallExpression(@NotNull PsiMethodCallExpression expression) {
                if (!VaultSdkUtils.isVaultSdkProject(expression)) return;
                String methodName = expression.getMethodExpression().getReferenceName();
                if (methodName == null || !FORBIDDEN_THREAD_METHODS.contains(methodName)) return;
                PsiMethod method = expression.resolveMethod();
                if (method == null) return;
                PsiClass containingClass = method.getContainingClass();
                if (containingClass != null && "java.lang.Thread".equals(containingClass.getQualifiedName())) {
                    holder.registerProblem(expression,
                            MESSAGE + " (Thread." + methodName + "() — use RuntimeService.sleep() instead)");
                }
            }

            @Override
            public void visitSynchronizedStatement(@NotNull PsiSynchronizedStatement statement) {
                if (!VaultSdkUtils.isVaultSdkProject(statement)) return;
                PsiElement keyword = statement.getFirstChild();
                holder.registerProblem(keyword != null ? keyword : statement, MESSAGE + " (synchronized block)");
            }

            @Override
            public void visitMethod(@NotNull PsiMethod method) {
                if (!VaultSdkUtils.isVaultSdkProject(method)) return;
                PsiModifierList modifiers = method.getModifierList();
                if (!modifiers.hasModifierProperty(PsiModifier.SYNCHRONIZED)) return;
                for (PsiElement child : modifiers.getChildren()) {
                    if (child instanceof PsiKeyword && PsiKeyword.SYNCHRONIZED.equals(child.getText())) {
                        holder.registerProblem(child, MESSAGE + " (synchronized method)");
                        return;
                    }
                }
            }
        };
    }
}
