package com.veeva.vault.toolbox.intellij.inspections;

import com.intellij.codeInspection.AbstractBaseJavaLocalInspectionTool;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * Inspection that flags forbidden file I/O, NIO operations, console output,
 * system calls, and environment access in Vault Java SDK projects.
 * <p>
 * Catches {@code java.io} file class imports and instantiation, {@code java.nio.file.*}
 * imports and static method calls, {@code System.out}/{@code System.err} field access,
 * {@code System.getenv()}/{@code System.getProperty()}/{@code System.getProperties()},
 * {@code System.exit()}, and {@code Runtime.exec()}/{@code Runtime.halt()}.
 */
public class VaultSdkFileIoInspection extends AbstractBaseJavaLocalInspectionTool {

    private static final Set<String> FORBIDDEN_IO_CLASSES = Set.of(
            "java.io.File",
            "java.io.FileInputStream",
            "java.io.FileOutputStream",
            "java.io.FileReader",
            "java.io.FileWriter",
            "java.io.RandomAccessFile",
            "java.io.FileDescriptor"
    );

    private static final Set<String> FORBIDDEN_NIO_CLASSES = Set.of(
            "java.nio.file.Files",
            "java.nio.file.Paths",
            "java.nio.file.Path",
            "java.nio.file.FileChannel",
            "java.nio.file.FileSystems"
    );

    @Override
    public @NotNull PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new JavaElementVisitor() {

            @Override
            public void visitImportStatement(@NotNull PsiImportStatement statement) {
                if (!VaultSdkUtils.isVaultSdkProject(statement)) return;
                String name = statement.getQualifiedName();
                if (name == null) return;
                for (String forbidden : FORBIDDEN_IO_CLASSES) {
                    if (name.equals(forbidden)) {
                        holder.registerProblem(statement,
                                "Vault Java SDK: Direct file I/O (" + forbidden + ") is not permitted");
                        return;
                    }
                }
                if (name.startsWith("java.nio.file")) {
                    holder.registerProblem(statement,
                            "Vault Java SDK: NIO file I/O (java.nio.file) is not permitted");
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
                    if (qName != null && FORBIDDEN_IO_CLASSES.contains(qName)) {
                        holder.registerProblem(expression,
                                "Vault Java SDK: Direct file I/O (" + psiClass.getName() + ") is not permitted");
                    }
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
                if (containingClass == null) return;
                String className = containingClass.getQualifiedName();

                if ("java.lang.System".equals(className) && methodName.equals("exit")) {
                    holder.registerProblem(expression,
                            "Vault Java SDK: System.exit() is not permitted",
                            new RemoveStatementFix("System.exit()"));
                } else if ("java.lang.System".equals(className)
                        && (methodName.equals("getenv") || methodName.equals("getProperty")
                                || methodName.equals("getProperties"))) {
                    holder.registerProblem(expression,
                            "Vault Java SDK: System." + methodName + "() is not permitted;"
                                    + " environment variables and system properties are unavailable in the Vault sandbox");
                } else if ("java.lang.Runtime".equals(className)
                        && (methodName.equals("exec") || methodName.equals("halt"))) {
                    holder.registerProblem(expression,
                            "Vault Java SDK: Runtime." + methodName + "() is not permitted",
                            new RemoveStatementFix("Runtime." + methodName + "()"));
                } else if (FORBIDDEN_NIO_CLASSES.contains(className)) {
                    holder.registerProblem(expression,
                            "Vault Java SDK: NIO file I/O (" + containingClass.getName()
                                    + "." + methodName + "()) is not permitted");
                }
            }

            @Override
            public void visitReferenceExpression(@NotNull PsiReferenceExpression expression) {
                if (!VaultSdkUtils.isVaultSdkProject(expression)) return;
                String name = expression.getReferenceName();
                if (!"out".equals(name) && !"err".equals(name)) return;
                PsiElement resolved = expression.resolve();
                if (resolved instanceof PsiField field) {
                    PsiClass containingClass = field.getContainingClass();
                    if (containingClass != null
                            && "java.lang.System".equals(containingClass.getQualifiedName())) {
                        holder.registerProblem(expression,
                                "Vault Java SDK: System." + name + " is not permitted; use LogService instead");
                    }
                }
            }
        };
    }

    /**
     * Quick-fix that removes the entire expression statement containing the flagged call.
     */
    private static class RemoveStatementFix implements LocalQuickFix {

        private final String callName;

        /**
         * @param callName the display name of the call used in the quick-fix label
         */
        RemoveStatementFix(String callName) {
            this.callName = callName;
        }

        @Override
        public @NotNull String getName() {
            return "Remove " + callName + " call";
        }

        @Override
        public @NotNull String getFamilyName() {
            return "Vault Java SDK";
        }

        /**
         * Deletes the parent {@link PsiExpressionStatement} of the flagged element.
         *
         * @param project    the current project
         * @param descriptor the problem descriptor identifying the flagged element
         */
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            PsiElement element = descriptor.getPsiElement();
            PsiElement parent = element.getParent();
            if (parent instanceof PsiExpressionStatement) {
                parent.delete();
            }
        }
    }
}
