package com.veeva.vault.toolbox.intellij.language.navigation;

import com.intellij.openapi.vfs.VirtualFile;
import com.veeva.vault.toolbox.intellij.metadata.mdl.MdlReferenceKindRegistry.RefKind;
import org.jetbrains.annotations.Nullable;

/**
 * Locates a component's extracted {@code .mdl} definition file relative to the source file's
 * vault directory, using the deterministic extraction layout
 * {@code <vaultRoot>/<ComponentType>/<ComponentType>.<name>.mdl}. Shared by Ctrl-click navigation
 * and the editor right-click "Go to Definition" action so both behave identically and offline.
 */
public final class MdlDefinitionNavigator {

    private MdlDefinitionNavigator() {
    }

    /**
     * @param sourceFile the file the reference appears in (used to locate the vault directory)
     * @param kind       the reference kind (only OBJECT/PICKLIST have their own files)
     * @param name       the referenced component name
     * @return the definition file, or {@code null} if it cannot be located
     */
    @Nullable
    public static VirtualFile findDefinition(@Nullable VirtualFile sourceFile, RefKind kind, @Nullable String name) {
        String folder = folderFor(kind);
        if (folder == null || sourceFile == null || name == null || name.isEmpty()) {
            return null;
        }
        VirtualFile typeDir = sourceFile.getParent();
        VirtualFile vaultRoot = typeDir != null ? typeDir.getParent() : null;
        if (vaultRoot == null) {
            return null;
        }
        VirtualFile target = vaultRoot.findFileByRelativePath(folder + "/" + folder + "." + name + ".mdl");
        return target != null && target.exists() ? target : null;
    }

    @Nullable
    public static String folderFor(RefKind kind) {
        switch (kind) {
            case OBJECT:
                return "Object";
            case PICKLIST:
                return "Picklist";
            default:
                return null;
        }
    }
}
