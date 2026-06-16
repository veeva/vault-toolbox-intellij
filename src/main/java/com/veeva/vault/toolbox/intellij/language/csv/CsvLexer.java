package com.veeva.vault.toolbox.intellij.language.csv;

import com.intellij.lexer.LexerBase;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Lexer for parsing CSV files.
 */
public class CsvLexer extends LexerBase {
    
    /** Element type representing a comma. */
    public static final IElementType COMMA = new IElementType("COMMA", CsvLanguage.INSTANCE);
    
    /** Element type representing a quote. */
    public static final IElementType QUOTE = new IElementType("QUOTE", CsvLanguage.INSTANCE);
    
    /** Element type representing text. */
    public static final IElementType TEXT = new IElementType("TEXT", CsvLanguage.INSTANCE);
    
    /** Element type representing a newline. */
    public static final IElementType NEWLINE = new IElementType("NEWLINE", CsvLanguage.INSTANCE);

    private CharSequence myBuffer;
    private int myStartOffset;
    private int myEndOffset;
    private int myState;

    private int myCurrentPosition;
    private int myTokenStart;
    private int myTokenEnd;
    private IElementType myTokenType;

    /**
     * Starts the lexer.
     *
     * @param buffer      the buffer to lex
     * @param startOffset the starting offset
     * @param endOffset   the ending offset
     * @param initialCond the initial state
     */
    @Override
    public void start(@NotNull CharSequence buffer, int startOffset, int endOffset, int initialCond) {
        myBuffer = buffer;
        myStartOffset = startOffset;
        myEndOffset = endOffset;
        myState = initialCond;
        myCurrentPosition = startOffset;
        advance();
    }

    /**
     * Gets the current state.
     *
     * @return the current state
     */
    @Override
    public int getState() {
        return myState;
    }

    /**
     * Gets the current token type.
     *
     * @return the token type
     */
    @Override
    @Nullable
    public IElementType getTokenType() {
        return myTokenType;
    }

    /**
     * Gets the start offset of the current token.
     *
     * @return the token start offset
     */
    @Override
    public int getTokenStart() {
        return myTokenStart;
    }

    /**
     * Gets the end offset of the current token.
     *
     * @return the token end offset
     */
    @Override
    public int getTokenEnd() {
        return myTokenEnd;
    }

    /**
     * Advances the lexer to the next token.
     */
    @Override
    public void advance() {
        if (myCurrentPosition >= myEndOffset) {
            myTokenType = null;
            return;
        }

        myTokenStart = myCurrentPosition;
        char c = myBuffer.charAt(myCurrentPosition);

        if (c == ',') {
            myTokenType = COMMA;
            myCurrentPosition++;
            myTokenEnd = myCurrentPosition;
            return;
        }

        if (c == '\n' || c == '\r') {
            myTokenType = NEWLINE;
            myCurrentPosition++;
            if (c == '\r' && myCurrentPosition < myEndOffset && myBuffer.charAt(myCurrentPosition) == '\n') {
                myCurrentPosition++;
            }
            myTokenEnd = myCurrentPosition;
            return;
        }

        if (c == '"') {
            myTokenType = QUOTE;
            myCurrentPosition++;
            myTokenEnd = myCurrentPosition;
            return;
        }

        myTokenType = TEXT;
        while (myCurrentPosition < myEndOffset) {
            char nextChar = myBuffer.charAt(myCurrentPosition);
            if (nextChar == ',' || nextChar == '"' || nextChar == '\n' || nextChar == '\r') {
                break;
            }
            myCurrentPosition++;
        }
        myTokenEnd = myCurrentPosition;
    }

    /**
     * Gets the buffer sequence.
     *
     * @return the buffer sequence
     */
    @NotNull
    @Override
    public CharSequence getBufferSequence() {
        return myBuffer;
    }

    /**
     * Gets the buffer end offset.
     *
     * @return the buffer end offset
     */
    @Override
    public int getBufferEnd() {
        return myEndOffset;
    }
}
