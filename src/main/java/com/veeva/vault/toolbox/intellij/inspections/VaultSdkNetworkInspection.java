package com.veeva.vault.toolbox.intellij.inspections;

import com.intellij.codeInspection.AbstractBaseJavaLocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * Inspection that flags direct network access and external process execution
 * in Vault Java SDK projects.
 * <p>
 * Catches imports and instantiation of socket, HTTP connection, and
 * {@code ProcessBuilder} classes. Use the SDK {@code ConnectionService} for
 * outbound HTTP communication.
 */
public class VaultSdkNetworkInspection extends AbstractBaseJavaLocalInspectionTool {

    private static final Set<String> FORBIDDEN_NET_CLASSES = Set.of(
            "java.net.Socket",
            "java.net.ServerSocket",
            "java.net.DatagramSocket",
            "java.net.MulticastSocket",
            "java.net.HttpURLConnection",
            "java.net.URLConnection",
            "java.lang.ProcessBuilder"
    );

    @Override
    public @NotNull PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new JavaElementVisitor() {

            @Override
            public void visitImportStatement(@NotNull PsiImportStatement statement) {
                if (!VaultSdkUtils.isVaultSdkProject(statement)) return;
                String name = statement.getQualifiedName();
                if (name == null) return;
                for (String forbidden : FORBIDDEN_NET_CLASSES) {
                    if (name.equals(forbidden)) {
                        holder.registerProblem(statement, message(forbidden));
                        return;
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
                    if (qName != null && FORBIDDEN_NET_CLASSES.contains(qName)) {
                        holder.registerProblem(expression, message(psiClass.getName()));
                    }
                }
            }

            /**
             * Returns the diagnostic message for the given class name,
             * using a dedicated message for {@code ProcessBuilder}.
             *
             * @param name the simple or qualified class name
             * @return the formatted problem message
             */
            private String message(String name) {
                if (name != null && name.contains("ProcessBuilder")) {
                    return "Vault Java SDK: External process execution (ProcessBuilder) is not permitted";
                }
                return "Vault Java SDK: Direct network access (" + name + ") is not permitted; use SDK ConnectionService instead";
            }
        };
    }
}
