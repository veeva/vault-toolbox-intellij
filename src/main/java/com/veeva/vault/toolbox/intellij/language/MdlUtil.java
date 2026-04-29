// Copyright 2000-2022 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.veeva.vault.toolbox.intellij.language;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.FileTypeIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import com.veeva.vault.toolbox.intellij.language.psi.MdlCommandList;

import java.util.*;

public class MdlUtil {

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
