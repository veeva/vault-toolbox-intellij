package com.veeva.vault.toolbox.intellij.language;

import com.intellij.lang.Commenter;
import org.jetbrains.annotations.Nullable;

/**
 * Allows using comment hotkeys to comment/uncomment code
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
