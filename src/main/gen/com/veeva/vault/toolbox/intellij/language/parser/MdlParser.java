// This is a generated file. Not intended for manual editing.
package com.veeva.vault.toolbox.intellij.language.parser;

import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiBuilder.Marker;

import static com.veeva.vault.toolbox.intellij.language.psi.MdlTypes.*;
import static com.intellij.lang.parser.GeneratedParserUtilBase.*;

import com.intellij.psi.tree.IElementType;
import com.intellij.lang.ASTNode;
import com.intellij.psi.tree.TokenSet;
import com.intellij.lang.PsiParser;
import com.intellij.lang.LightPsiParser;

@SuppressWarnings({"SimplifiableIfStatement", "UnusedAssignment"})
public class MdlParser implements PsiParser, LightPsiParser {

    public ASTNode parse(IElementType root_, PsiBuilder builder_) {
        parseLight(root_, builder_);
        return builder_.getTreeBuilt();
    }

    public void parseLight(IElementType root_, PsiBuilder builder_) {
        boolean result_;
        builder_ = adapt_builder_(root_, builder_, this, null);
        Marker marker_ = enter_section_(builder_, 0, _COLLAPSE_, null);
        result_ = parse_root_(root_, builder_);
        exit_section_(builder_, 0, marker_, root_, result_, true, TRUE_CONDITION);
    }

    protected boolean parse_root_(IElementType root_, PsiBuilder builder_) {
        return parse_root_(root_, builder_, 0);
    }

    static boolean parse_root_(IElementType root_, PsiBuilder builder_, int level_) {
        return mdlFile(builder_, level_ + 1);
    }

    /* ********************************************************** */
    // START_PAREN (attribute_value (COMMA attribute_value)*)? END_PAREN
    public static boolean attribute_content(PsiBuilder builder_, int level_) {
        if (!recursion_guard_(builder_, level_, "attribute_content")) return false;
        if (!nextTokenIs(builder_, START_PAREN)) return false;
        boolean result_;
        Marker marker_ = enter_section_(builder_);
        result_ = consumeToken(builder_, START_PAREN);
        result_ = result_ && attribute_content_1(builder_, level_ + 1);
        result_ = result_ && consumeToken(builder_, END_PAREN);
        exit_section_(builder_, marker_, ATTRIBUTE_CONTENT, result_);
        return result_;
    }

    // (attribute_value (COMMA attribute_value)*)?
    private static boolean attribute_content_1(PsiBuilder builder_, int level_) {
        if (!recursion_guard_(builder_, level_, "attribute_content_1")) return false;
        attribute_content_1_0(builder_, level_ + 1);
        return true;
    }

    // attribute_value (COMMA attribute_value)*
    private static boolean attribute_content_1_0(PsiBuilder builder_, int level_) {
        if (!recursion_guard_(builder_, level_, "attribute_content_1_0")) return false;
        boolean result_;
        Marker marker_ = enter_section_(builder_);
        result_ = attribute_value(builder_, level_ + 1);
        result_ = result_ && attribute_content_1_0_1(builder_, level_ + 1);
        exit_section_(builder_, marker_, null, result_);
        return result_;
    }

    // (COMMA attribute_value)*
    private static boolean attribute_content_1_0_1(PsiBuilder builder_, int level_) {
        if (!recursion_guard_(builder_, level_, "attribute_content_1_0_1")) return false;
        while (true) {
            int pos_ = current_position_(builder_);
            if (!attribute_content_1_0_1_0(builder_, level_ + 1)) break;
            if (!empty_element_parsed_guard_(builder_, "attribute_content_1_0_1", pos_)) break;
        }
        return true;
    }

    // COMMA attribute_value
    private static boolean attribute_content_1_0_1_0(PsiBuilder builder_, int level_) {
        if (!recursion_guard_(builder_, level_, "attribute_content_1_0_1_0")) return false;
        boolean result_;
        Marker marker_ = enter_section_(builder_);
        result_ = consumeToken(builder_, COMMA);
        result_ = result_ && attribute_value(builder_, level_ + 1);
        exit_section_(builder_, marker_, null, result_);
        return result_;
    }

    /* ********************************************************** */
    // ATTRIBUTE (ATTRIBUTE_COMMAND OPERATOR?)? attribute_content (POST_ATTRIBUTE_COMMAND attribute_content?)?
    public static boolean attribute_pair(PsiBuilder builder_, int level_) {
        if (!recursion_guard_(builder_, level_, "attribute_pair")) return false;
        if (!nextTokenIs(builder_, ATTRIBUTE)) return false;
        boolean result_;
        Marker marker_ = enter_section_(builder_);
        result_ = consumeToken(builder_, ATTRIBUTE);
        result_ = result_ && attribute_pair_1(builder_, level_ + 1);
        result_ = result_ && attribute_content(builder_, level_ + 1);
        result_ = result_ && attribute_pair_3(builder_, level_ + 1);
        exit_section_(builder_, marker_, ATTRIBUTE_PAIR, result_);
        return result_;
    }

    // (ATTRIBUTE_COMMAND OPERATOR?)?
    private static boolean attribute_pair_1(PsiBuilder builder_, int level_) {
        if (!recursion_guard_(builder_, level_, "attribute_pair_1")) return false;
        attribute_pair_1_0(builder_, level_ + 1);
        return true;
    }

    // ATTRIBUTE_COMMAND OPERATOR?
    private static boolean attribute_pair_1_0(PsiBuilder builder_, int level_) {
        if (!recursion_guard_(builder_, level_, "attribute_pair_1_0")) return false;
        boolean result_;
        Marker marker_ = enter_section_(builder_);
        result_ = consumeToken(builder_, ATTRIBUTE_COMMAND);
        result_ = result_ && attribute_pair_1_0_1(builder_, level_ + 1);
        exit_section_(builder_, marker_, null, result_);
        return result_;
    }

    // OPERATOR?
    private static boolean attribute_pair_1_0_1(PsiBuilder builder_, int level_) {
        if (!recursion_guard_(builder_, level_, "attribute_pair_1_0_1")) return false;
        consumeToken(builder_, OPERATOR);
        return true;
    }

    // (POST_ATTRIBUTE_COMMAND attribute_content?)?
    private static boolean attribute_pair_3(PsiBuilder builder_, int level_) {
        if (!recursion_guard_(builder_, level_, "attribute_pair_3")) return false;
        attribute_pair_3_0(builder_, level_ + 1);
        return true;
    }

    // POST_ATTRIBUTE_COMMAND attribute_content?
    private static boolean attribute_pair_3_0(PsiBuilder builder_, int level_) {
        if (!recursion_guard_(builder_, level_, "attribute_pair_3_0")) return false;
        boolean result_;
        Marker marker_ = enter_section_(builder_);
        result_ = consumeToken(builder_, POST_ATTRIBUTE_COMMAND);
        result_ = result_ && attribute_pair_3_0_1(builder_, level_ + 1);
        exit_section_(builder_, marker_, null, result_);
        return result_;
    }

    // attribute_content?
    private static boolean attribute_pair_3_0_1(PsiBuilder builder_, int level_) {
        if (!recursion_guard_(builder_, level_, "attribute_pair_3_0_1")) return false;
        attribute_content(builder_, level_ + 1);
        return true;
    }

    /* ********************************************************** */
    // xml_block | expression_value | string_value | VALUE
    public static boolean attribute_value(PsiBuilder builder_, int level_) {
        if (!recursion_guard_(builder_, level_, "attribute_value")) return false;
        boolean result_;
        Marker marker_ = enter_section_(builder_, level_, _NONE_, ATTRIBUTE_VALUE, "<attribute value>");
        result_ = xml_block(builder_, level_ + 1);
        if (!result_) result_ = expression_value(builder_, level_ + 1);
        if (!result_) result_ = string_value(builder_, level_ + 1);
        if (!result_) result_ = consumeToken(builder_, VALUE);
        exit_section_(builder_, level_, marker_, result_, false, null);
        return result_;
    }

    /* ********************************************************** */
    // component_change_command | record_change_command
    public static boolean change_command(PsiBuilder builder_, int level_) {
        if (!recursion_guard_(builder_, level_, "change_command")) return false;
        if (!nextTokenIs(builder_, COMMAND)) return false;
        boolean result_;
        Marker marker_ = enter_section_(builder_);
        result_ = component_change_command(builder_, level_ + 1);
        if (!result_) result_ = record_change_command(builder_, level_ + 1);
        exit_section_(builder_, marker_, CHANGE_COMMAND, result_);
        return result_;
    }

    /* ********************************************************** */
    // START_PAREN command_block_content? END_PAREN SEMICOLON?
    public static boolean command_block(PsiBuilder builder_, int level_) {
        if (!recursion_guard_(builder_, level_, "command_block")) return false;
        if (!nextTokenIs(builder_, START_PAREN)) return false;
        boolean result_;
        Marker marker_ = enter_section_(builder_);
        result_ = consumeToken(builder_, START_PAREN);
        result_ = result_ && command_block_1(builder_, level_ + 1);
        result_ = result_ && consumeToken(builder_, END_PAREN);
        result_ = result_ && command_block_3(builder_, level_ + 1);
        exit_section_(builder_, marker_, COMMAND_BLOCK, result_);
        return result_;
    }

    // command_block_content?
    private static boolean command_block_1(PsiBuilder builder_, int level_) {
        if (!recursion_guard_(builder_, level_, "command_block_1")) return false;
        command_block_content(builder_, level_ + 1);
        return true;
    }

    // SEMICOLON?
    private static boolean command_block_3(PsiBuilder builder_, int level_) {
        if (!recursion_guard_(builder_, level_, "command_block_3")) return false;
        consumeToken(builder_, SEMICOLON);
        return true;
    }

    /* ********************************************************** */
    // command_item (COMMA command_item?)*
    static boolean command_block_content(PsiBuilder builder_, int level_) {
        if (!recursion_guard_(builder_, level_, "command_block_content")) return false;
        boolean result_;
        Marker marker_ = enter_section_(builder_);
        result_ = command_item(builder_, level_ + 1);
        result_ = result_ && command_block_content_1(builder_, level_ + 1);
        exit_section_(builder_, marker_, null, result_);
        return result_;
    }

    // (COMMA command_item?)*
    private static boolean command_block_content_1(PsiBuilder builder_, int level_) {
        if (!recursion_guard_(builder_, level_, "command_block_content_1")) return false;
        while (true) {
            int pos_ = current_position_(builder_);
            if (!command_block_content_1_0(builder_, level_ + 1)) break;
            if (!empty_element_parsed_guard_(builder_, "command_block_content_1", pos_)) break;
        }
        return true;
    }

    // COMMA command_item?
    private static boolean command_block_content_1_0(PsiBuilder builder_, int level_) {
        if (!recursion_guard_(builder_, level_, "command_block_content_1_0")) return false;
        boolean result_;
        Marker marker_ = enter_section_(builder_);
        result_ = consumeToken(builder_, COMMA);
        result_ = result_ && command_block_content_1_0_1(builder_, level_ + 1);
        exit_section_(builder_, marker_, null, result_);
        return result_;
    }

    // command_item?
    private static boolean command_block_content_1_0_1(PsiBuilder builder_, int level_) {
        if (!recursion_guard_(builder_, level_, "command_block_content_1_0_1")) return false;
        command_item(builder_, level_ + 1);
        return true;
    }

    /* ********************************************************** */
    // new_attribute | subcomponent_command_list | attribute_pair
    public static boolean command_item(PsiBuilder builder_, int level_) {
        if (!recursion_guard_(builder_, level_, "command_item")) return false;
        boolean result_;
        Marker marker_ = enter_section_(builder_, level_, _NONE_, COMMAND_ITEM, "<command item>");
        result_ = new_attribute(builder_, level_ + 1);
        if (!result_) result_ = subcomponent_command_list(builder_, level_ + 1);
        if (!result_) result_ = attribute_pair(builder_, level_ + 1);
        exit_section_(builder_, level_, marker_, result_, false, null);
        return result_;
    }

    /* ********************************************************** */
    // change_command | rename_command | drop_command
    public static boolean command_list(PsiBuilder builder_, int level_) {
        if (!recursion_guard_(builder_, level_, "command_list")) return false;
        boolean result_;
        Marker marker_ = enter_section_(builder_, level_, _NONE_, COMMAND_LIST, "<command list>");
        result_ = change_command(builder_, level_ + 1);
        if (!result_) result_ = rename_command(builder_, level_ + 1);
        if (!result_) result_ = drop_command(builder_, level_ + 1);
        exit_section_(builder_, level_, marker_, result_, false, null);
        return result_;
    }

    /* ********************************************************** */
    // COMMAND COMPONENT_TYPE_LITERAL OPERATOR? COMPONENT_TYPE command_block
    public static boolean component_change_command(PsiBuilder builder_, int level_) {
        if (!recursion_guard_(builder_, level_, "component_change_command")) return false;
        if (!nextTokenIs(builder_, COMMAND)) return false;
        boolean result_;
        Marker marker_ = enter_section_(builder_);
        result_ = consumeTokens(builder_, 0, COMMAND, COMPONENT_TYPE_LITERAL);
        result_ = result_ && component_change_command_2(builder_, level_ + 1);
        result_ = result_ && consumeToken(builder_, COMPONENT_TYPE);
        result_ = result_ && command_block(builder_, level_ + 1);
        exit_section_(builder_, marker_, COMPONENT_CHANGE_COMMAND, result_);
        return result_;
    }

    // OPERATOR?
    private static boolean component_change_command_2(PsiBuilder builder_, int level_) {
        if (!recursion_guard_(builder_, level_, "component_change_command_2")) return false;
        consumeToken(builder_, OPERATOR);
        return true;
    }

    /* ********************************************************** */
    // DROP COMPONENT_TYPE_LITERAL OPERATOR? COMPONENT_TYPE SEMICOLON
    public static boolean component_drop_command(PsiBuilder builder_, int level_) {
        if (!recursion_guard_(builder_, level_, "component_drop_command")) return false;
        if (!nextTokenIs(builder_, DROP)) return false;
        boolean result_;
        Marker marker_ = enter_section_(builder_);
        result_ = consumeTokens(builder_, 0, DROP, COMPONENT_TYPE_LITERAL);
        result_ = result_ && component_drop_command_2(builder_, level_ + 1);
        result_ = result_ && consumeTokens(builder_, 0, COMPONENT_TYPE, SEMICOLON);
        exit_section_(builder_, marker_, COMPONENT_DROP_COMMAND, result_);
        return result_;
    }

    // OPERATOR?
    private static boolean component_drop_command_2(PsiBuilder builder_, int level_) {
        if (!recursion_guard_(builder_, level_, "component_drop_command_2")) return false;
        consumeToken(builder_, OPERATOR);
        return true;
    }

    /* ********************************************************** */
    // RENAME COMPONENT_TYPE_LITERAL OPERATOR? COMPONENT_TYPE TO OPERATOR? COMPONENT_TYPE SEMICOLON
    public static boolean component_rename_command(PsiBuilder builder_, int level_) {
        if (!recursion_guard_(builder_, level_, "component_rename_command")) return false;
        if (!nextTokenIs(builder_, RENAME)) return false;
        boolean result_;
        Marker marker_ = enter_section_(builder_);
        result_ = consumeTokens(builder_, 0, RENAME, COMPONENT_TYPE_LITERAL);
        result_ = result_ && component_rename_command_2(builder_, level_ + 1);
        result_ = result_ && consumeTokens(builder_, 0, COMPONENT_TYPE, TO);
        result_ = result_ && component_rename_command_5(builder_, level_ + 1);
        result_ = result_ && consumeTokens(builder_, 0, COMPONENT_TYPE, SEMICOLON);
        exit_section_(builder_, marker_, COMPONENT_RENAME_COMMAND, result_);
        return result_;
    }

    // OPERATOR?
    private static boolean component_rename_command_2(PsiBuilder builder_, int level_) {
        if (!recursion_guard_(builder_, level_, "component_rename_command_2")) return false;
        consumeToken(builder_, OPERATOR);
        return true;
    }

    // OPERATOR?
    private static boolean component_rename_command_5(PsiBuilder builder_, int level_) {
        if (!recursion_guard_(builder_, level_, "component_rename_command_5")) return false;
        consumeToken(builder_, OPERATOR);
        return true;
    }

    /* ********************************************************** */
    // component_drop_command | record_drop_command
    public static boolean drop_command(PsiBuilder builder_, int level_) {
        if (!recursion_guard_(builder_, level_, "drop_command")) return false;
        if (!nextTokenIs(builder_, DROP)) return false;
        boolean result_;
        Marker marker_ = enter_section_(builder_);
        result_ = component_drop_command(builder_, level_ + 1);
        if (!result_) result_ = record_drop_command(builder_, level_ + 1);
        exit_section_(builder_, marker_, DROP_COMMAND, result_);
        return result_;
    }

    /* ********************************************************** */
    // START_BRACKET VALUE* END_BRACKET
    public static boolean expression_value(PsiBuilder builder_, int level_) {
        if (!recursion_guard_(builder_, level_, "expression_value")) return false;
        if (!nextTokenIs(builder_, START_BRACKET)) return false;
        boolean result_;
        Marker marker_ = enter_section_(builder_);
        result_ = consumeToken(builder_, START_BRACKET);
        result_ = result_ && expression_value_1(builder_, level_ + 1);
        result_ = result_ && consumeToken(builder_, END_BRACKET);
        exit_section_(builder_, marker_, EXPRESSION_VALUE, result_);
        return result_;
    }

    // VALUE*
    private static boolean expression_value_1(PsiBuilder builder_, int level_) {
        if (!recursion_guard_(builder_, level_, "expression_value_1")) return false;
        while (true) {
            int pos_ = current_position_(builder_);
            if (!consumeToken(builder_, VALUE)) break;
            if (!empty_element_parsed_guard_(builder_, "expression_value_1", pos_)) break;
        }
        return true;
    }

    /* ********************************************************** */
    // command_list | COMMENT | CRLF
    static boolean item_(PsiBuilder builder_, int level_) {
        if (!recursion_guard_(builder_, level_, "item_")) return false;
        boolean result_;
        result_ = command_list(builder_, level_ + 1);
        if (!result_) result_ = consumeToken(builder_, COMMENT);
        if (!result_) result_ = consumeToken(builder_, CRLF);
        return result_;
    }

    /* ********************************************************** */
    // item_*
    static boolean mdlFile(PsiBuilder builder_, int level_) {
        if (!recursion_guard_(builder_, level_, "mdlFile")) return false;
        while (true) {
            int pos_ = current_position_(builder_);
            if (!item_(builder_, level_ + 1)) break;
            if (!empty_element_parsed_guard_(builder_, "mdlFile", pos_)) break;
        }
        return true;
    }

    /* ********************************************************** */
    // ATTRIBUTE_LITERAL ATTRIBUTE command_block
    public static boolean new_attribute(PsiBuilder builder_, int level_) {
        if (!recursion_guard_(builder_, level_, "new_attribute")) return false;
        if (!nextTokenIs(builder_, ATTRIBUTE_LITERAL)) return false;
        boolean result_;
        Marker marker_ = enter_section_(builder_);
        result_ = consumeTokens(builder_, 0, ATTRIBUTE_LITERAL, ATTRIBUTE);
        result_ = result_ && command_block(builder_, level_ + 1);
        exit_section_(builder_, marker_, NEW_ATTRIBUTE, result_);
        return result_;
    }

    /* ********************************************************** */
    // COMMAND COMPONENT_TYPE OPERATOR? RECORD_NAME command_block
    public static boolean record_change_command(PsiBuilder builder_, int level_) {
        if (!recursion_guard_(builder_, level_, "record_change_command")) return false;
        if (!nextTokenIs(builder_, COMMAND)) return false;
        boolean result_;
        Marker marker_ = enter_section_(builder_);
        result_ = consumeTokens(builder_, 0, COMMAND, COMPONENT_TYPE);
        result_ = result_ && record_change_command_2(builder_, level_ + 1);
        result_ = result_ && consumeToken(builder_, RECORD_NAME);
        result_ = result_ && command_block(builder_, level_ + 1);
        exit_section_(builder_, marker_, RECORD_CHANGE_COMMAND, result_);
        return result_;
    }

    // OPERATOR?
    private static boolean record_change_command_2(PsiBuilder builder_, int level_) {
        if (!recursion_guard_(builder_, level_, "record_change_command_2")) return false;
        consumeToken(builder_, OPERATOR);
        return true;
    }

    /* ********************************************************** */
    // DROP COMPONENT_TYPE OPERATOR? RECORD_NAME SEMICOLON
    public static boolean record_drop_command(PsiBuilder builder_, int level_) {
        if (!recursion_guard_(builder_, level_, "record_drop_command")) return false;
        if (!nextTokenIs(builder_, DROP)) return false;
        boolean result_;
        Marker marker_ = enter_section_(builder_);
        result_ = consumeTokens(builder_, 0, DROP, COMPONENT_TYPE);
        result_ = result_ && record_drop_command_2(builder_, level_ + 1);
        result_ = result_ && consumeTokens(builder_, 0, RECORD_NAME, SEMICOLON);
        exit_section_(builder_, marker_, RECORD_DROP_COMMAND, result_);
        return result_;
    }

    // OPERATOR?
    private static boolean record_drop_command_2(PsiBuilder builder_, int level_) {
        if (!recursion_guard_(builder_, level_, "record_drop_command_2")) return false;
        consumeToken(builder_, OPERATOR);
        return true;
    }

    /* ********************************************************** */
    // RENAME COMPONENT_TYPE OPERATOR? RECORD_NAME TO OPERATOR? RECORD_NAME SEMICOLON
    public static boolean record_rename_command(PsiBuilder builder_, int level_) {
        if (!recursion_guard_(builder_, level_, "record_rename_command")) return false;
        if (!nextTokenIs(builder_, RENAME)) return false;
        boolean result_;
        Marker marker_ = enter_section_(builder_);
        result_ = consumeTokens(builder_, 0, RENAME, COMPONENT_TYPE);
        result_ = result_ && record_rename_command_2(builder_, level_ + 1);
        result_ = result_ && consumeTokens(builder_, 0, RECORD_NAME, TO);
        result_ = result_ && record_rename_command_5(builder_, level_ + 1);
        result_ = result_ && consumeTokens(builder_, 0, RECORD_NAME, SEMICOLON);
        exit_section_(builder_, marker_, RECORD_RENAME_COMMAND, result_);
        return result_;
    }

    // OPERATOR?
    private static boolean record_rename_command_2(PsiBuilder builder_, int level_) {
        if (!recursion_guard_(builder_, level_, "record_rename_command_2")) return false;
        consumeToken(builder_, OPERATOR);
        return true;
    }

    // OPERATOR?
    private static boolean record_rename_command_5(PsiBuilder builder_, int level_) {
        if (!recursion_guard_(builder_, level_, "record_rename_command_5")) return false;
        consumeToken(builder_, OPERATOR);
        return true;
    }

    /* ********************************************************** */
    // component_rename_command | record_rename_command
    public static boolean rename_command(PsiBuilder builder_, int level_) {
        if (!recursion_guard_(builder_, level_, "rename_command")) return false;
        if (!nextTokenIs(builder_, RENAME)) return false;
        boolean result_;
        Marker marker_ = enter_section_(builder_);
        result_ = component_rename_command(builder_, level_ + 1);
        if (!result_) result_ = record_rename_command(builder_, level_ + 1);
        exit_section_(builder_, marker_, RENAME_COMMAND, result_);
        return result_;
    }

    /* ********************************************************** */
    // START_QUOTE VALUE* END_QUOTE
    public static boolean string_value(PsiBuilder builder_, int level_) {
        if (!recursion_guard_(builder_, level_, "string_value")) return false;
        if (!nextTokenIs(builder_, START_QUOTE)) return false;
        boolean result_;
        Marker marker_ = enter_section_(builder_);
        result_ = consumeToken(builder_, START_QUOTE);
        result_ = result_ && string_value_1(builder_, level_ + 1);
        result_ = result_ && consumeToken(builder_, END_QUOTE);
        exit_section_(builder_, marker_, STRING_VALUE, result_);
        return result_;
    }

    // VALUE*
    private static boolean string_value_1(PsiBuilder builder_, int level_) {
        if (!recursion_guard_(builder_, level_, "string_value_1")) return false;
        while (true) {
            int pos_ = current_position_(builder_);
            if (!consumeToken(builder_, VALUE)) break;
            if (!empty_element_parsed_guard_(builder_, "string_value_1", pos_)) break;
        }
        return true;
    }

    /* ********************************************************** */
    // SUBCOMMAND? COMPONENT_TYPE OPERATOR? RECORD_NAME command_block
    public static boolean subcomponent_change_command(PsiBuilder builder_, int level_) {
        if (!recursion_guard_(builder_, level_, "subcomponent_change_command")) return false;
        if (!nextTokenIs(builder_, "<subcomponent change command>", COMPONENT_TYPE, SUBCOMMAND)) return false;
        boolean result_;
        Marker marker_ = enter_section_(builder_, level_, _NONE_, SUBCOMPONENT_CHANGE_COMMAND, "<subcomponent change command>");
        result_ = subcomponent_change_command_0(builder_, level_ + 1);
        result_ = result_ && consumeToken(builder_, COMPONENT_TYPE);
        result_ = result_ && subcomponent_change_command_2(builder_, level_ + 1);
        result_ = result_ && consumeToken(builder_, RECORD_NAME);
        result_ = result_ && command_block(builder_, level_ + 1);
        exit_section_(builder_, level_, marker_, result_, false, null);
        return result_;
    }

    // SUBCOMMAND?
    private static boolean subcomponent_change_command_0(PsiBuilder builder_, int level_) {
        if (!recursion_guard_(builder_, level_, "subcomponent_change_command_0")) return false;
        consumeToken(builder_, SUBCOMMAND);
        return true;
    }

    // OPERATOR?
    private static boolean subcomponent_change_command_2(PsiBuilder builder_, int level_) {
        if (!recursion_guard_(builder_, level_, "subcomponent_change_command_2")) return false;
        consumeToken(builder_, OPERATOR);
        return true;
    }

    /* ********************************************************** */
    // subcomponent_change_command | subcomponent_rename_command | subcomponent_drop_command
    public static boolean subcomponent_command_list(PsiBuilder builder_, int level_) {
        if (!recursion_guard_(builder_, level_, "subcomponent_command_list")) return false;
        if (!nextTokenIs(builder_, "<subcomponent command list>", COMPONENT_TYPE, SUBCOMMAND)) return false;
        boolean result_;
        Marker marker_ = enter_section_(builder_, level_, _NONE_, SUBCOMPONENT_COMMAND_LIST, "<subcomponent command list>");
        result_ = subcomponent_change_command(builder_, level_ + 1);
        if (!result_) result_ = subcomponent_rename_command(builder_, level_ + 1);
        if (!result_) result_ = subcomponent_drop_command(builder_, level_ + 1);
        exit_section_(builder_, level_, marker_, result_, false, null);
        return result_;
    }

    /* ********************************************************** */
    // SUBCOMMAND COMPONENT_TYPE OPERATOR? RECORD_NAME
    public static boolean subcomponent_drop_command(PsiBuilder builder_, int level_) {
        if (!recursion_guard_(builder_, level_, "subcomponent_drop_command")) return false;
        if (!nextTokenIs(builder_, SUBCOMMAND)) return false;
        boolean result_;
        Marker marker_ = enter_section_(builder_);
        result_ = consumeTokens(builder_, 0, SUBCOMMAND, COMPONENT_TYPE);
        result_ = result_ && subcomponent_drop_command_2(builder_, level_ + 1);
        result_ = result_ && consumeToken(builder_, RECORD_NAME);
        exit_section_(builder_, marker_, SUBCOMPONENT_DROP_COMMAND, result_);
        return result_;
    }

    // OPERATOR?
    private static boolean subcomponent_drop_command_2(PsiBuilder builder_, int level_) {
        if (!recursion_guard_(builder_, level_, "subcomponent_drop_command_2")) return false;
        consumeToken(builder_, OPERATOR);
        return true;
    }

    /* ********************************************************** */
    // SUBCOMMAND COMPONENT_TYPE OPERATOR? RECORD_NAME TO OPERATOR? RECORD_NAME
    public static boolean subcomponent_rename_command(PsiBuilder builder_, int level_) {
        if (!recursion_guard_(builder_, level_, "subcomponent_rename_command")) return false;
        if (!nextTokenIs(builder_, SUBCOMMAND)) return false;
        boolean result_;
        Marker marker_ = enter_section_(builder_);
        result_ = consumeTokens(builder_, 0, SUBCOMMAND, COMPONENT_TYPE);
        result_ = result_ && subcomponent_rename_command_2(builder_, level_ + 1);
        result_ = result_ && consumeTokens(builder_, 0, RECORD_NAME, TO);
        result_ = result_ && subcomponent_rename_command_5(builder_, level_ + 1);
        result_ = result_ && consumeToken(builder_, RECORD_NAME);
        exit_section_(builder_, marker_, SUBCOMPONENT_RENAME_COMMAND, result_);
        return result_;
    }

    // OPERATOR?
    private static boolean subcomponent_rename_command_2(PsiBuilder builder_, int level_) {
        if (!recursion_guard_(builder_, level_, "subcomponent_rename_command_2")) return false;
        consumeToken(builder_, OPERATOR);
        return true;
    }

    // OPERATOR?
    private static boolean subcomponent_rename_command_5(PsiBuilder builder_, int level_) {
        if (!recursion_guard_(builder_, level_, "subcomponent_rename_command_5")) return false;
        consumeToken(builder_, OPERATOR);
        return true;
    }

    /* ********************************************************** */
    // XML_ATTRIBUTE EQUALS XML_VALUE
    public static boolean xml_assignment(PsiBuilder builder_, int level_) {
        if (!recursion_guard_(builder_, level_, "xml_assignment")) return false;
        if (!nextTokenIs(builder_, XML_ATTRIBUTE)) return false;
        boolean result_;
        Marker marker_ = enter_section_(builder_);
        result_ = consumeTokens(builder_, 0, XML_ATTRIBUTE, EQUALS, XML_VALUE);
        exit_section_(builder_, marker_, XML_ASSIGNMENT, result_);
        return result_;
    }

    /* ********************************************************** */
    // (xml_open_tag (xml_attribute_value)* xml_closed_tag) | (xml_open_tag xml_text+ xml_closed_tag) | xml_self_closing_tag | xml_info_tag
    public static boolean xml_attribute_value(PsiBuilder builder_, int level_) {
        if (!recursion_guard_(builder_, level_, "xml_attribute_value")) return false;
        if (!nextTokenIs(builder_, OPEN_ANGLE_BRACKET)) return false;
        boolean result_;
        Marker marker_ = enter_section_(builder_);
        result_ = xml_attribute_value_0(builder_, level_ + 1);
        if (!result_) result_ = xml_attribute_value_1(builder_, level_ + 1);
        if (!result_) result_ = xml_self_closing_tag(builder_, level_ + 1);
        if (!result_) result_ = xml_info_tag(builder_, level_ + 1);
        exit_section_(builder_, marker_, XML_ATTRIBUTE_VALUE, result_);
        return result_;
    }

    // xml_open_tag (xml_attribute_value)* xml_closed_tag
    private static boolean xml_attribute_value_0(PsiBuilder builder_, int level_) {
        if (!recursion_guard_(builder_, level_, "xml_attribute_value_0")) return false;
        boolean result_;
        Marker marker_ = enter_section_(builder_);
        result_ = xml_open_tag(builder_, level_ + 1);
        result_ = result_ && xml_attribute_value_0_1(builder_, level_ + 1);
        result_ = result_ && xml_closed_tag(builder_, level_ + 1);
        exit_section_(builder_, marker_, null, result_);
        return result_;
    }

    // (xml_attribute_value)*
    private static boolean xml_attribute_value_0_1(PsiBuilder builder_, int level_) {
        if (!recursion_guard_(builder_, level_, "xml_attribute_value_0_1")) return false;
        while (true) {
            int pos_ = current_position_(builder_);
            if (!xml_attribute_value_0_1_0(builder_, level_ + 1)) break;
            if (!empty_element_parsed_guard_(builder_, "xml_attribute_value_0_1", pos_)) break;
        }
        return true;
    }

    // (xml_attribute_value)
    private static boolean xml_attribute_value_0_1_0(PsiBuilder builder_, int level_) {
        if (!recursion_guard_(builder_, level_, "xml_attribute_value_0_1_0")) return false;
        boolean result_;
        Marker marker_ = enter_section_(builder_);
        result_ = xml_attribute_value(builder_, level_ + 1);
        exit_section_(builder_, marker_, null, result_);
        return result_;
    }

    // xml_open_tag xml_text+ xml_closed_tag
    private static boolean xml_attribute_value_1(PsiBuilder builder_, int level_) {
        if (!recursion_guard_(builder_, level_, "xml_attribute_value_1")) return false;
        boolean result_;
        Marker marker_ = enter_section_(builder_);
        result_ = xml_open_tag(builder_, level_ + 1);
        result_ = result_ && xml_attribute_value_1_1(builder_, level_ + 1);
        result_ = result_ && xml_closed_tag(builder_, level_ + 1);
        exit_section_(builder_, marker_, null, result_);
        return result_;
    }

    // xml_text+
    private static boolean xml_attribute_value_1_1(PsiBuilder builder_, int level_) {
        if (!recursion_guard_(builder_, level_, "xml_attribute_value_1_1")) return false;
        boolean result_;
        Marker marker_ = enter_section_(builder_);
        result_ = xml_text(builder_, level_ + 1);
        while (result_) {
            int pos_ = current_position_(builder_);
            if (!xml_text(builder_, level_ + 1)) break;
            if (!empty_element_parsed_guard_(builder_, "xml_attribute_value_1_1", pos_)) break;
        }
        exit_section_(builder_, marker_, null, result_);
        return result_;
    }

    /* ********************************************************** */
    // START_BRACE xml_attribute_value* END_BRACE
    public static boolean xml_block(PsiBuilder builder_, int level_) {
        if (!recursion_guard_(builder_, level_, "xml_block")) return false;
        if (!nextTokenIs(builder_, START_BRACE)) return false;
        boolean result_;
        Marker marker_ = enter_section_(builder_);
        result_ = consumeToken(builder_, START_BRACE);
        result_ = result_ && xml_block_1(builder_, level_ + 1);
        result_ = result_ && consumeToken(builder_, END_BRACE);
        exit_section_(builder_, marker_, XML_BLOCK, result_);
        return result_;
    }

    // xml_attribute_value*
    private static boolean xml_block_1(PsiBuilder builder_, int level_) {
        if (!recursion_guard_(builder_, level_, "xml_block_1")) return false;
        while (true) {
            int pos_ = current_position_(builder_);
            if (!xml_attribute_value(builder_, level_ + 1)) break;
            if (!empty_element_parsed_guard_(builder_, "xml_block_1", pos_)) break;
        }
        return true;
    }

    /* ********************************************************** */
    // OPEN_ANGLE_BRACKET XML_SLASH XML_IDENTIFIER CLOSED_ANGLE_BRACKET
    public static boolean xml_closed_tag(PsiBuilder builder_, int level_) {
        if (!recursion_guard_(builder_, level_, "xml_closed_tag")) return false;
        if (!nextTokenIs(builder_, OPEN_ANGLE_BRACKET)) return false;
        boolean result_;
        Marker marker_ = enter_section_(builder_);
        result_ = consumeTokens(builder_, 0, OPEN_ANGLE_BRACKET, XML_SLASH, XML_IDENTIFIER, CLOSED_ANGLE_BRACKET);
        exit_section_(builder_, marker_, XML_CLOSED_TAG, result_);
        return result_;
    }

    /* ********************************************************** */
    // OPEN_ANGLE_BRACKET QUESTION XML_IDENTIFIER (xml_assignment)* QUESTION CLOSED_ANGLE_BRACKET
    public static boolean xml_info_tag(PsiBuilder builder_, int level_) {
        if (!recursion_guard_(builder_, level_, "xml_info_tag")) return false;
        if (!nextTokenIs(builder_, OPEN_ANGLE_BRACKET)) return false;
        boolean result_;
        Marker marker_ = enter_section_(builder_);
        result_ = consumeTokens(builder_, 0, OPEN_ANGLE_BRACKET, QUESTION, XML_IDENTIFIER);
        result_ = result_ && xml_info_tag_3(builder_, level_ + 1);
        result_ = result_ && consumeTokens(builder_, 0, QUESTION, CLOSED_ANGLE_BRACKET);
        exit_section_(builder_, marker_, XML_INFO_TAG, result_);
        return result_;
    }

    // (xml_assignment)*
    private static boolean xml_info_tag_3(PsiBuilder builder_, int level_) {
        if (!recursion_guard_(builder_, level_, "xml_info_tag_3")) return false;
        while (true) {
            int pos_ = current_position_(builder_);
            if (!xml_info_tag_3_0(builder_, level_ + 1)) break;
            if (!empty_element_parsed_guard_(builder_, "xml_info_tag_3", pos_)) break;
        }
        return true;
    }

    // (xml_assignment)
    private static boolean xml_info_tag_3_0(PsiBuilder builder_, int level_) {
        if (!recursion_guard_(builder_, level_, "xml_info_tag_3_0")) return false;
        boolean result_;
        Marker marker_ = enter_section_(builder_);
        result_ = xml_assignment(builder_, level_ + 1);
        exit_section_(builder_, marker_, null, result_);
        return result_;
    }

    /* ********************************************************** */
    // OPEN_ANGLE_BRACKET XML_IDENTIFIER (xml_assignment)* CLOSED_ANGLE_BRACKET
    public static boolean xml_open_tag(PsiBuilder builder_, int level_) {
        if (!recursion_guard_(builder_, level_, "xml_open_tag")) return false;
        if (!nextTokenIs(builder_, OPEN_ANGLE_BRACKET)) return false;
        boolean result_;
        Marker marker_ = enter_section_(builder_);
        result_ = consumeTokens(builder_, 0, OPEN_ANGLE_BRACKET, XML_IDENTIFIER);
        result_ = result_ && xml_open_tag_2(builder_, level_ + 1);
        result_ = result_ && consumeToken(builder_, CLOSED_ANGLE_BRACKET);
        exit_section_(builder_, marker_, XML_OPEN_TAG, result_);
        return result_;
    }

    // (xml_assignment)*
    private static boolean xml_open_tag_2(PsiBuilder builder_, int level_) {
        if (!recursion_guard_(builder_, level_, "xml_open_tag_2")) return false;
        while (true) {
            int pos_ = current_position_(builder_);
            if (!xml_open_tag_2_0(builder_, level_ + 1)) break;
            if (!empty_element_parsed_guard_(builder_, "xml_open_tag_2", pos_)) break;
        }
        return true;
    }

    // (xml_assignment)
    private static boolean xml_open_tag_2_0(PsiBuilder builder_, int level_) {
        if (!recursion_guard_(builder_, level_, "xml_open_tag_2_0")) return false;
        boolean result_;
        Marker marker_ = enter_section_(builder_);
        result_ = xml_assignment(builder_, level_ + 1);
        exit_section_(builder_, marker_, null, result_);
        return result_;
    }

    /* ********************************************************** */
    // OPEN_ANGLE_BRACKET XML_IDENTIFIER (xml_assignment)* XML_SLASH CLOSED_ANGLE_BRACKET
    public static boolean xml_self_closing_tag(PsiBuilder builder_, int level_) {
        if (!recursion_guard_(builder_, level_, "xml_self_closing_tag")) return false;
        if (!nextTokenIs(builder_, OPEN_ANGLE_BRACKET)) return false;
        boolean result_;
        Marker marker_ = enter_section_(builder_);
        result_ = consumeTokens(builder_, 0, OPEN_ANGLE_BRACKET, XML_IDENTIFIER);
        result_ = result_ && xml_self_closing_tag_2(builder_, level_ + 1);
        result_ = result_ && consumeTokens(builder_, 0, XML_SLASH, CLOSED_ANGLE_BRACKET);
        exit_section_(builder_, marker_, XML_SELF_CLOSING_TAG, result_);
        return result_;
    }

    // (xml_assignment)*
    private static boolean xml_self_closing_tag_2(PsiBuilder builder_, int level_) {
        if (!recursion_guard_(builder_, level_, "xml_self_closing_tag_2")) return false;
        while (true) {
            int pos_ = current_position_(builder_);
            if (!xml_self_closing_tag_2_0(builder_, level_ + 1)) break;
            if (!empty_element_parsed_guard_(builder_, "xml_self_closing_tag_2", pos_)) break;
        }
        return true;
    }

    // (xml_assignment)
    private static boolean xml_self_closing_tag_2_0(PsiBuilder builder_, int level_) {
        if (!recursion_guard_(builder_, level_, "xml_self_closing_tag_2_0")) return false;
        boolean result_;
        Marker marker_ = enter_section_(builder_);
        result_ = xml_assignment(builder_, level_ + 1);
        exit_section_(builder_, marker_, null, result_);
        return result_;
    }

    /* ********************************************************** */
    // XML_TAG_CONTENT
    public static boolean xml_text(PsiBuilder builder_, int level_) {
        if (!recursion_guard_(builder_, level_, "xml_text")) return false;
        if (!nextTokenIs(builder_, XML_TAG_CONTENT)) return false;
        boolean result_;
        Marker marker_ = enter_section_(builder_);
        result_ = consumeToken(builder_, XML_TAG_CONTENT);
        exit_section_(builder_, marker_, XML_TEXT, result_);
        return result_;
    }

}
