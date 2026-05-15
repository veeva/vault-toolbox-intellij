package com.veeva.vault.toolbox.intellij.language;

import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SyntaxHighlighter;
import com.intellij.openapi.options.colors.AttributesDescriptor;
import com.intellij.openapi.options.colors.ColorDescriptor;
import com.intellij.openapi.options.colors.ColorSettingsPage;
import icons.ToolboxIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.util.Map;

/**
 * Color settings page that lets the user customize the IDE colors used for MDL
 * syntax highlighting under {@code Settings | Editor | Color Scheme | MDL}.
 */
public class MdlColorSettingsPage implements ColorSettingsPage {
    private static final AttributesDescriptor[] DESCRIPTORS = {
            new AttributesDescriptor("Command", MdlSyntaxHighlighter.COMMAND),
            new AttributesDescriptor("Component type", MdlSyntaxHighlighter.COMPONENT_TYPE),
            new AttributesDescriptor("Record name", MdlSyntaxHighlighter.RECORD_NAME),
            new AttributesDescriptor("XML identifier", MdlSyntaxHighlighter.XML_IDENTIFIER),
            new AttributesDescriptor("XML tag content", MdlSyntaxHighlighter.XML_TEXT),
            new AttributesDescriptor("Character", MdlSyntaxHighlighter.CHARACTER),
            new AttributesDescriptor("Attribute", MdlSyntaxHighlighter.ATTRIBUTE),
            new AttributesDescriptor("Value", MdlSyntaxHighlighter.VALUE),
            new AttributesDescriptor("Bad value", MdlSyntaxHighlighter.BAD_CHARACTER)
    };

    @Override
    public Icon getIcon() {
        return ToolboxIcons.Mdl;
    }

    @NotNull
    @Override
    public SyntaxHighlighter getHighlighter() {
        return new MdlSyntaxHighlighter();
    }

    @NotNull
    @Override
    public String getDemoText() {
        return """
RECREATE Picklist IF NOT EXISTS my_picklist__c (
   label('My Picklist'),
   active(true),
   can_add_values(true),
   can_reorder_values(true),
   Picklistentry my_first_entry__c(
     value('Entry 1'),
     order(1),
     active(true)
  ),
  xml({<?xml version="1.0" encoding="UTF-8"?>
      <securityOptions>
          <systemMaintained>true</systemMaintained>
      </securityOptions>
  }),
  DROP Picklistentry my_second_entry__c;
);
        """;
    }

    @Nullable
    @Override
    public Map<String, TextAttributesKey> getAdditionalHighlightingTagToDescriptorMap() {
        return null;
    }

    @Override
    public AttributesDescriptor @NotNull [] getAttributeDescriptors() {
        return DESCRIPTORS;
    }

    @Override
    public ColorDescriptor @NotNull [] getColorDescriptors() {
        return ColorDescriptor.EMPTY_ARRAY;
    }

    @NotNull
    @Override
    public String getDisplayName() {
        return "MDL";
    }
}
