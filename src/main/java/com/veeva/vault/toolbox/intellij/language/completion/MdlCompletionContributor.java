package com.veeva.vault.toolbox.intellij.language.completion;

import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.patterns.PlatformPatterns;
import com.veeva.vault.toolbox.intellij.language.psi.MdlTypes;

public class MdlCompletionContributor extends CompletionContributor {
    MdlCompletionContributor() {
        extend(null, PlatformPatterns.psiElement(MdlTypes.COMMAND), new CommandCompletionProvider());
        extend(null, PlatformPatterns.psiElement(MdlTypes.OPERATOR),new OperatorCompletionProvider());
        extend(null, PlatformPatterns.psiElement(MdlTypes.TO), new ToCompletionProvider());
        extend(null, PlatformPatterns.psiElement(MdlTypes.SUBCOMMAND), new SubCommandCompletionProvider());
        extend(null, PlatformPatterns.psiElement(MdlTypes.COMPONENT_TYPE), new ComponentTypeCompletionProvider());
        extend(null, PlatformPatterns.psiElement(MdlTypes.ATTRIBUTE_COMMAND), new AttributeCommandCompletionProvider());
        extend(null, PlatformPatterns.psiElement(MdlTypes.POST_ATTRIBUTE_COMMAND), new PostAttributeCommandCompletionProvider());
        extend(null, PlatformPatterns.psiElement(MdlTypes.ATTRIBUTE), new AttributeCompletionProvider());
        //TODO can consider having an ATTRIBUTE_OR_SUBCOMMAND or an ATTRIBUTE_OR_COMPONENT_TYPE MdlType if we want to provide suggestions for both types. Current Precedence is set to the attribute
    }
}
