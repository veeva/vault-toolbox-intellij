// This is a generated file. Not intended for manual editing.
package com.veeva.vault.toolbox.intellij.language.psi;

import com.intellij.psi.tree.IElementType;
import com.intellij.psi.PsiElement;
import com.intellij.lang.ASTNode;
import com.veeva.vault.toolbox.intellij.language.impl.*;

public interface MdlTypes {

  IElementType ATTRIBUTE_CONTENT = new MdlElementType("ATTRIBUTE_CONTENT");
  IElementType ATTRIBUTE_PAIR = new MdlElementType("ATTRIBUTE_PAIR");
  IElementType ATTRIBUTE_VALUE = new MdlElementType("ATTRIBUTE_VALUE");
  IElementType CHANGE_COMMAND = new MdlElementType("CHANGE_COMMAND");
  IElementType COMMAND_BLOCK = new MdlElementType("COMMAND_BLOCK");
  IElementType COMMAND_ITEM = new MdlElementType("COMMAND_ITEM");
  IElementType COMMAND_LIST = new MdlElementType("COMMAND_LIST");
  IElementType COMPONENT_CHANGE_COMMAND = new MdlElementType("COMPONENT_CHANGE_COMMAND");
  IElementType COMPONENT_DROP_COMMAND = new MdlElementType("COMPONENT_DROP_COMMAND");
  IElementType COMPONENT_RENAME_COMMAND = new MdlElementType("COMPONENT_RENAME_COMMAND");
  IElementType DROP_COMMAND = new MdlElementType("DROP_COMMAND");
  IElementType EXPRESSION_VALUE = new MdlElementType("EXPRESSION_VALUE");
  IElementType NEW_ATTRIBUTE = new MdlElementType("NEW_ATTRIBUTE");
  IElementType RECORD_CHANGE_COMMAND = new MdlElementType("RECORD_CHANGE_COMMAND");
  IElementType RECORD_DROP_COMMAND = new MdlElementType("RECORD_DROP_COMMAND");
  IElementType RECORD_RENAME_COMMAND = new MdlElementType("RECORD_RENAME_COMMAND");
  IElementType RENAME_COMMAND = new MdlElementType("RENAME_COMMAND");
  IElementType STRING_VALUE = new MdlElementType("STRING_VALUE");
  IElementType SUBCOMPONENT_CHANGE_COMMAND = new MdlElementType("SUBCOMPONENT_CHANGE_COMMAND");
  IElementType SUBCOMPONENT_COMMAND_LIST = new MdlElementType("SUBCOMPONENT_COMMAND_LIST");
  IElementType SUBCOMPONENT_DROP_COMMAND = new MdlElementType("SUBCOMPONENT_DROP_COMMAND");
  IElementType SUBCOMPONENT_RENAME_COMMAND = new MdlElementType("SUBCOMPONENT_RENAME_COMMAND");
  IElementType XML_ASSIGNMENT = new MdlElementType("XML_ASSIGNMENT");
  IElementType XML_ATTRIBUTE_VALUE = new MdlElementType("XML_ATTRIBUTE_VALUE");
  IElementType XML_BLOCK = new MdlElementType("XML_BLOCK");
  IElementType XML_CLOSED_TAG = new MdlElementType("XML_CLOSED_TAG");
  IElementType XML_INFO_TAG = new MdlElementType("XML_INFO_TAG");
  IElementType XML_OPEN_TAG = new MdlElementType("XML_OPEN_TAG");
  IElementType XML_SELF_CLOSING_TAG = new MdlElementType("XML_SELF_CLOSING_TAG");
  IElementType XML_TEXT = new MdlElementType("XML_TEXT");

  IElementType ATTRIBUTE = new MdlTokenType("ATTRIBUTE");
  IElementType ATTRIBUTE_COMMAND = new MdlTokenType("ATTRIBUTE_COMMAND");
  IElementType ATTRIBUTE_LITERAL = new MdlTokenType("ATTRIBUTE_LITERAL");
  IElementType CLOSED_ANGLE_BRACKET = new MdlTokenType("CLOSED_ANGLE_BRACKET");
  IElementType COMMA = new MdlTokenType("COMMA");
  IElementType COMMAND = new MdlTokenType("COMMAND");
  IElementType COMMENT = new MdlTokenType("COMMENT");
  IElementType COMPONENT_TYPE = new MdlTokenType("COMPONENT_TYPE");
  IElementType COMPONENT_TYPE_LITERAL = new MdlTokenType("COMPONENT_TYPE_LITERAL");
  IElementType CRLF = new MdlTokenType("CRLF");
  IElementType DROP = new MdlTokenType("DROP");
  IElementType END_BRACE = new MdlTokenType("END_BRACE");
  IElementType END_BRACKET = new MdlTokenType("END_BRACKET");
  IElementType END_PAREN = new MdlTokenType("END_PAREN");
  IElementType END_QUOTE = new MdlTokenType("END_QUOTE");
  IElementType EQUALS = new MdlTokenType("EQUALS");
  IElementType OPEN_ANGLE_BRACKET = new MdlTokenType("OPEN_ANGLE_BRACKET");
  IElementType OPERATOR = new MdlTokenType("OPERATOR");
  IElementType POST_ATTRIBUTE_COMMAND = new MdlTokenType("POST_ATTRIBUTE_COMMAND");
  IElementType QUESTION = new MdlTokenType("QUESTION");
  IElementType RECORD_NAME = new MdlTokenType("RECORD_NAME");
  IElementType RENAME = new MdlTokenType("RENAME");
  IElementType SEMICOLON = new MdlTokenType("SEMICOLON");
  IElementType START_BRACE = new MdlTokenType("START_BRACE");
  IElementType START_BRACKET = new MdlTokenType("START_BRACKET");
  IElementType START_PAREN = new MdlTokenType("START_PAREN");
  IElementType START_QUOTE = new MdlTokenType("START_QUOTE");
  IElementType SUBCOMMAND = new MdlTokenType("SUBCOMMAND");
  IElementType TO = new MdlTokenType("TO");
  IElementType VALUE = new MdlTokenType("VALUE");
  IElementType XML_ATTRIBUTE = new MdlTokenType("XML_ATTRIBUTE");
  IElementType XML_IDENTIFIER = new MdlTokenType("XML_IDENTIFIER");
  IElementType XML_SLASH = new MdlTokenType("XML_SLASH");
  IElementType XML_TAG_CONTENT = new MdlTokenType("XML_TAG_CONTENT");
  IElementType XML_VALUE = new MdlTokenType("XML_VALUE");

  class Factory {
    public static PsiElement createElement(ASTNode node) {
      IElementType type = node.getElementType();
      if (type == ATTRIBUTE_CONTENT) {
        return new MdlAttributeContentImpl(node);
      }
      else if (type == ATTRIBUTE_PAIR) {
        return new MdlAttributePairImpl(node);
      }
      else if (type == ATTRIBUTE_VALUE) {
        return new MdlAttributeValueImpl(node);
      }
      else if (type == CHANGE_COMMAND) {
        return new MdlChangeCommandImpl(node);
      }
      else if (type == COMMAND_BLOCK) {
        return new MdlCommandBlockImpl(node);
      }
      else if (type == COMMAND_ITEM) {
        return new MdlCommandItemImpl(node);
      }
      else if (type == COMMAND_LIST) {
        return new MdlCommandListImpl(node);
      }
      else if (type == COMPONENT_CHANGE_COMMAND) {
        return new MdlComponentChangeCommandImpl(node);
      }
      else if (type == COMPONENT_DROP_COMMAND) {
        return new MdlComponentDropCommandImpl(node);
      }
      else if (type == COMPONENT_RENAME_COMMAND) {
        return new MdlComponentRenameCommandImpl(node);
      }
      else if (type == DROP_COMMAND) {
        return new MdlDropCommandImpl(node);
      }
      else if (type == EXPRESSION_VALUE) {
        return new MdlExpressionValueImpl(node);
      }
      else if (type == NEW_ATTRIBUTE) {
        return new MdlNewAttributeImpl(node);
      }
      else if (type == RECORD_CHANGE_COMMAND) {
        return new MdlRecordChangeCommandImpl(node);
      }
      else if (type == RECORD_DROP_COMMAND) {
        return new MdlRecordDropCommandImpl(node);
      }
      else if (type == RECORD_RENAME_COMMAND) {
        return new MdlRecordRenameCommandImpl(node);
      }
      else if (type == RENAME_COMMAND) {
        return new MdlRenameCommandImpl(node);
      }
      else if (type == STRING_VALUE) {
        return new MdlStringValueImpl(node);
      }
      else if (type == SUBCOMPONENT_CHANGE_COMMAND) {
        return new MdlSubcomponentChangeCommandImpl(node);
      }
      else if (type == SUBCOMPONENT_COMMAND_LIST) {
        return new MdlSubcomponentCommandListImpl(node);
      }
      else if (type == SUBCOMPONENT_DROP_COMMAND) {
        return new MdlSubcomponentDropCommandImpl(node);
      }
      else if (type == SUBCOMPONENT_RENAME_COMMAND) {
        return new MdlSubcomponentRenameCommandImpl(node);
      }
      else if (type == XML_ASSIGNMENT) {
        return new MdlXmlAssignmentImpl(node);
      }
      else if (type == XML_ATTRIBUTE_VALUE) {
        return new MdlXmlAttributeValueImpl(node);
      }
      else if (type == XML_BLOCK) {
        return new MdlXmlBlockImpl(node);
      }
      else if (type == XML_CLOSED_TAG) {
        return new MdlXmlClosedTagImpl(node);
      }
      else if (type == XML_INFO_TAG) {
        return new MdlXmlInfoTagImpl(node);
      }
      else if (type == XML_OPEN_TAG) {
        return new MdlXmlOpenTagImpl(node);
      }
      else if (type == XML_SELF_CLOSING_TAG) {
        return new MdlXmlSelfClosingTagImpl(node);
      }
      else if (type == XML_TEXT) {
        return new MdlXmlTextImpl(node);
      }
      throw new AssertionError("Unknown element type: " + type);
    }
  }
}
