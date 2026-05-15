package com.veeva.vault.toolbox.intellij.language.formatter;

import com.intellij.formatting.FormattingContext;
import com.intellij.formatting.FormattingModel;
import com.intellij.formatting.FormattingModelBuilder;
import com.intellij.formatting.FormattingModelProvider;
import com.intellij.formatting.Indent;
import com.intellij.formatting.SpacingBuilder;
import com.intellij.formatting.Wrap;
import com.intellij.formatting.WrapType;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.veeva.vault.toolbox.intellij.language.MdlLanguage;
import com.veeva.vault.toolbox.intellij.language.MdlTokenSets;
import com.veeva.vault.toolbox.intellij.language.psi.MdlTypes;
import org.jetbrains.annotations.NotNull;

/**
 * Builds the formatting model used by the IDE's reformat code action for MDL files.
 * The configured {@link SpacingBuilder} encodes the spacing, line-break, and
 * blank-line rules between MDL token types.
 */
public class MdlFormattingModelBuilder implements FormattingModelBuilder {

    private static SpacingBuilder createSpaceBuilder(CodeStyleSettings settings) {
        SpacingBuilder spacingBuilder = new SpacingBuilder(settings, MdlLanguage.INSTANCE);

        spacingBuilder
                .after(MdlTypes.COMMAND).spaces(1)
                .after(MdlTypes.SUBCOMMAND).spaces(1)
                .before(MdlTypes.COMMAND_BLOCK).spaces(1)
                .before(MdlTypes.ATTRIBUTE_CONTENT).spaces(1)
                .around(MdlTypes.OPERATOR).spaces(1)
                .around(MdlTypes.TO).spaces(1)
                .around(MdlTypes.RECORD_NAME).spaces(1)
                .around(MdlTypes.COMPONENT_TYPE).spaces(1);

        spacingBuilder
                .aroundInside(MdlTypes.COMPONENT_TYPE, MdlTypes.COMPONENT_CHANGE_COMMAND).spaces(1)
                .aroundInside(MdlTypes.START_PAREN, MdlTypes.ATTRIBUTE_CONTENT).spaces(0)
                .aroundInside(MdlTypes.END_PAREN, MdlTypes.ATTRIBUTE_CONTENT).spaces(0)
                .aroundInside(MdlTypes.COMMA, MdlTypes.ATTRIBUTE_CONTENT).spaces(0);

        spacingBuilder
                .afterInside(MdlTypes.COMMA, MdlTypes.COMMAND_BLOCK).lineBreakInCode()
                .before(MdlTypes.COMMAND_ITEM).lineBreakInCode()
                .after(MdlTypes.COMMAND_LIST).blankLines(1);

        spacingBuilder
                .around(MdlTypes.XML_TEXT).none()
                .after(MdlTokenSets.XML_TYPES).lineBreakInCode();

        return spacingBuilder;
    }

    @Override
    public @NotNull FormattingModel createModel(@NotNull FormattingContext formattingContext) {
        final CodeStyleSettings codeStyleSettings = formattingContext.getCodeStyleSettings();
        return FormattingModelProvider.createFormattingModelForPsiFile(
                formattingContext.getContainingFile(),
                new MdlBlock(
                        formattingContext.getNode(),
                        Wrap.createWrap(WrapType.NONE, false),
                        null,
                        createSpaceBuilder(codeStyleSettings),
                        Indent.getNoneIndent()) {
                },
                codeStyleSettings);
    }
}
