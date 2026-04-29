package com.veeva.vault.toolbox.intellij.language;

import com.intellij.lexer.FlexAdapter;

public class MdlLexerAdapter extends FlexAdapter {
    public MdlLexerAdapter() {
        super(new MdlLexer(null));
    }
}
