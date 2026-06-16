package com.veeva.vault.toolbox.intellij.inspections;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.Key;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import org.jetbrains.annotations.NotNull;

/**
 * Shared utility methods used by all Vault Java SDK inspections.
 */
public final class VaultSdkUtils {

    private static final Key<CachedValue<Boolean>> SDK_PRESENT_KEY = Key.create("vault.sdk.present");
    private static final String SDK_SENTINEL = "com.veeva.vault.sdk.api.core.ServiceLocator";

    private VaultSdkUtils() {}

    /**
     * Returns {@code true} when the Vault Java SDK JAR is on the project's classpath.
     * The result is cached per project and invalidated when module roots change.
     *
     * @param project the project to check
     * @return {@code true} if the Vault Java SDK is present on the classpath
     */
    public static boolean isVaultSdkProject(@NotNull Project project) {
        return CachedValuesManager.getManager(project).getCachedValue(
                project,
                SDK_PRESENT_KEY,
                () -> {
                    boolean present = JavaPsiFacade.getInstance(project)
                            .findClass(SDK_SENTINEL, GlobalSearchScope.allScope(project)) != null;
                    return CachedValueProvider.Result.create(present, ProjectRootManager.getInstance(project));
                },
                false
        );
    }

    /**
     * Convenience overload that resolves the project from the given PSI element.
     *
     * @param context any PSI element within the file being inspected
     * @return {@code true} if the Vault Java SDK is present on the classpath
     */
    public static boolean isVaultSdkProject(@NotNull PsiElement context) {
        return isVaultSdkProject(context.getProject());
    }
}
