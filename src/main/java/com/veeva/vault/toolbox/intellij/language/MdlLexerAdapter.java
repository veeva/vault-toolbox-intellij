package com.veeva.vault.toolbox.intellij.language;

import com.intellij.lexer.FlexAdapter;

/**
 * Adapts the JFlex-generated MDL lexer to the IntelliJ {@link com.intellij.lexer.Lexer}
 * interface.
 */
public class MdlLexerAdapter extends FlexAdapter {
    public MdlLexerAdapter() {
        super(new MdlLexer(null));
    }
}
