package com.veeva.vault.toolbox.intellij.language;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.FileTypeIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import com.veeva.vault.toolbox.intellij.language.psi.MdlCommandList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * PSI traversal helpers for locating MDL command lists either within a single file
 * or across every MDL file in a project.
 */
public class MdlUtil {

    private MdlUtil() {
    }

    /**
     * Collects every {@link MdlCommandList} declared at the top level of the supplied
     * MDL file.
     *
     * @param project     the project that owns {@code virtualFile}
     * @param virtualFile the MDL file to inspect
     * @return the discovered command lists, or an empty list if the file is not an
     * MDL file or has no command lists
     */
    public static List<MdlCommandList> findProperties(Project project, VirtualFile virtualFile) {
        List<MdlCommandList> result = new ArrayList<>();
        MdlFile mdlFile = (MdlFile) PsiManager.getInstance(project).findFile(virtualFile);
        if (mdlFile != null) {
            MdlCommandList[] properties = PsiTreeUtil.getChildrenOfType(mdlFile, MdlCommandList.class);
            if (properties != null) {
                Collections.addAll(result, properties);
            }
        }
        return result;
    }

    /**
     * Collects every top-level {@link MdlCommandList} from every MDL file in the
     * project.
     *
     * @param project the project to scan
     * @return the discovered command lists across all MDL files
     */
    public static List<MdlCommandList> findProperties(Project project) {
        List<MdlCommandList> result = new ArrayList<>();
        Collection<VirtualFile> virtualFiles =
                FileTypeIndex.getFiles(MdlFileType.INSTANCE, GlobalSearchScope.allScope(project));
        for (VirtualFile virtualFile : virtualFiles) {
            MdlFile mdlFile = (MdlFile) PsiManager.getInstance(project).findFile(virtualFile);
            if (mdlFile != null) {
                MdlCommandList[] properties = PsiTreeUtil.getChildrenOfType(mdlFile, MdlCommandList.class);
                if (properties != null) {
                    Collections.addAll(result, properties);
                }
            }
        }
        return result;
    }
}
