package com.veeva.vault.toolbox.intellij.language.completion;

import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.patterns.PlatformPatterns;
import com.veeva.vault.toolbox.intellij.language.psi.MdlTypes;

/**
 * Registers MDL completion providers against PSI element types so the IDE can
 * surface contextual code-completion suggestions (commands, sub-commands, operators,
 * attributes, etc.).
 *
 * <p>Note: when both an attribute and a sub-command or component type could legally
 * appear at a position, the attribute provider currently takes precedence.</p>
 */
public class MdlCompletionContributor extends CompletionContributor {
    /**
     * Constructs a new MdlCompletionContributor and registers completion providers for various MDL tokens.
     */
    MdlCompletionContributor() {
        extend(null, PlatformPatterns.psiElement(MdlTypes.COMMAND), new CommandCompletionProvider());
        extend(null, PlatformPatterns.psiElement(MdlTypes.OPERATOR), new OperatorCompletionProvider());
        extend(null, PlatformPatterns.psiElement(MdlTypes.TO), new ToCompletionProvider());
        extend(null, PlatformPatterns.psiElement(MdlTypes.SUBCOMMAND), new SubCommandCompletionProvider());
        extend(null, PlatformPatterns.psiElement(MdlTypes.COMPONENT_TYPE), new ComponentTypeCompletionProvider());
        extend(null, PlatformPatterns.psiElement(MdlTypes.ATTRIBUTE_COMMAND), new AttributeCommandCompletionProvider());
        extend(null, PlatformPatterns.psiElement(MdlTypes.POST_ATTRIBUTE_COMMAND), new PostAttributeCommandCompletionProvider());
        extend(null, PlatformPatterns.psiElement(MdlTypes.ATTRIBUTE), new AttributeCompletionProvider());
        extend(null, PlatformPatterns.psiElement(MdlTypes.VALUE), new ReferenceValueCompletionProvider());
    }
}
