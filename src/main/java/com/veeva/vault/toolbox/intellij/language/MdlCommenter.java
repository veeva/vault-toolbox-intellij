package com.veeva.vault.toolbox.intellij.language;

import com.intellij.lang.Commenter;
import org.jetbrains.annotations.Nullable;

/**
 * Defines comment markers for MDL so the IDE's Comment with Line Comment action
 * (Cmd/Ctrl+/) toggles a leading {@code #} on the current line. Block comments
 * are not supported by the language.
 */
public class MdlCommenter implements Commenter {
    @Override
    public String getLineCommentPrefix() {
        return "#";
    }

    @Override
    public String getBlockCommentPrefix() {
        return "";
    }

    @Nullable
    @Override
    public String getBlockCommentSuffix() {
        return null;
    }

    @Nullable
    @Override
    public String getCommentedBlockCommentPrefix() {
        return null;
    }

    @Nullable
    @Override
    public String getCommentedBlockCommentSuffix() {
        return null;
    }
}
