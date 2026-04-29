package com.veeva.vault.toolbox.intellij.language;

import com.intellij.psi.tree.IElementType;
import com.veeva.vault.toolbox.intellij.language.psi.MdlTypes;
import com.intellij.psi.TokenType;

%%

%class MdlLexer
%implements com.intellij.lexer.FlexLexer
%unicode
%function advance
%type IElementType
%eof{  return;
%eof}

A = [Aa]
B = [Bb]
C = [Cc]
D = [Dd]
E = [Ee]
F = [Ff]
G = [Gg]
H = [Hh]
I = [Ii]
J = [Jj]
K = [Kk]
L = [Ll]
M = [Mm]
N = [Nn]
O = [Oo]
P = [Pp]
Q = [Qq]
R = [Rr]
S = [Ss]
T = [Tt]
U = [Uu]
V = [Vv]
W = [Ww]
X = [Xx]
Y = [Yy]
Z = [Zz]

ALTER = {A}{L}{T}{E}{R}
RECREATE = {R}{E}{C}{R}{E}{A}{T}{E}
CREATE = {C}{R}{E}{A}{T}{E}
DROP = {D}{R}{O}{P}
RENAME = {R}{E}{N}{A}{M}{E}
MODIFY = {M}{O}{D}{I}{F}{Y}
ADD = {A}{D}{D}
AFTER = {A}{F}{T}{E}{R}
FIRST = {F}{I}{R}{S}{T}
LAST = {L}{A}{S}{T}

COMMAND = {ALTER}|{RECREATE}|{CREATE}
BUILDING = [a-zA-Z0-9_.]
SUBCOMMAND = {MODIFY} | {ADD}
COMPONENT_NAME = [A-Z][a-z0-9]*
COMPONENT_TYPE_LITERAL = Componenttype
RECORD_NAME = ([a-z][a-z0-9_\.]*__([a-z]+))+

TO = {T}{O}

IF = {I}{F}
NOT = {N}{O}{T}
EXISTS = {E}{X}{I}{S}{T}{S}
IF_EXISTS = {IF}{WHITE_SPACE}+{EXISTS}
IF_NOT_EXISTS = {IF}{WHITE_SPACE}+{NOT}{WHITE_SPACE}+{EXISTS}

ATTRIBUTE = [a-z]+([._a-zA-Z0-9])*
ATTRIBUTE_LITERAL = Attribute

COMMA = ,
SEMICOLON = ;

START_PAREN = \(
END_PAREN = \)

SINGLE_QUOTE = '
DOUBLE_QUOTE = \"
EQUALS = =

OPEN_BRACE = \{
CLOSED_BRACE = \}
OPEN_ANGLE_BRACKET = <
CLOSED_ANGLE_BRACKET = >

OPEN_BRACKET = \[
CLOSED_BRACKET = \]

BOOL = {T}{R}{U}{E} | {F}{A}{L}{S}{E}

CRLF=\R
WHITE_SPACE=[\ \n\t\f]
END_OF_LINE_COMMENT=("#"|"!")[^\r\n]*

%state CHANGE_COMMAND
%state RENAME
%state DROP

%state COMMAND_PIECE_START
%state ATTRIBUTE_COMMAND
%state POST_ATTRIBUTE_COMMAND

%state VALUE

%state STRING_CONTENTS
%state ESCAPE_CHAR
%state END_QUOTE

%state XML_IDENTIFIER
%state XML_CLOSE_TAG_CONTENT
%state XML_START
%state XML_CONTENT
%state XML_CONTENT_END
%state XML_ASSIGNMENT
%state XML_ATTRIBUTE_VALUE

%state EXPRESSION_CONTENTS
%state EXPRESSION_END

%state NEW_ATTRIBUTE

%state SUBCOMPONENT_CHANGE_COMMAND
%state RENAME_SUBCOMPONENT
%state DROP_SUBCOMPONENT

%state NESTED_ATTRIBUTE
%state NESTED_VALUE

%state NESTED_STRING_CONTENTS
%state NESTED_ESCAPE_CHAR
%state NESTED_END_QUOTE

%state NESTED_XML_START
%state NESTED_XML_CONTENT
%state NESTED_XML_IDENTIFIER
%state NESTED_XML_CLOSE_CONTENT
%state NESTED_XML_CONTENT_END
%state NESTED_XML_ASSIGNMENT
%state NESTED_XML_ATTRIBUTE_VALUE

%state NESTED_EXPRESSION_CONTENTS
%state NESTED_EXPRESSION_END

%state NESTED_POST_ATTRIBUTE_COMMAND
%state COMMAND_BLOCK_END
%state COMMAND_END
%%
/**
----- MDL COMMAND SECTION START -----
 */
<YYINITIAL> {
    {COMMAND}
        { yybegin(CHANGE_COMMAND); return MdlTypes.COMMAND; }
    {RENAME}
        { yybegin(RENAME); return MdlTypes.RENAME; }
    {DROP}
        { yybegin(DROP); return MdlTypes.DROP; }
    {END_OF_LINE_COMMENT}
        { yybegin(YYINITIAL); return MdlTypes.COMMENT; }
}

// Create/update component
<CHANGE_COMMAND> {
    {COMPONENT_TYPE_LITERAL}
      { yybegin(CHANGE_COMMAND); return MdlTypes.COMPONENT_TYPE_LITERAL; }
    {COMPONENT_NAME}
      { yybegin(CHANGE_COMMAND); return MdlTypes.COMPONENT_TYPE; }
    {IF_EXISTS}|{IF_NOT_EXISTS}
      { yybegin(CHANGE_COMMAND); return MdlTypes.OPERATOR; }
    {RECORD_NAME}
      { yybegin(CHANGE_COMMAND); return MdlTypes.RECORD_NAME; }
    {START_PAREN}
      { yybegin(COMMAND_PIECE_START); return MdlTypes.START_PAREN; }
}

<RENAME> {
    {COMPONENT_TYPE_LITERAL}
      { yybegin(RENAME); return MdlTypes.COMPONENT_TYPE_LITERAL; }
    {COMPONENT_NAME}
      { yybegin(RENAME); return MdlTypes.COMPONENT_TYPE; }
    {IF_EXISTS}|{IF_NOT_EXISTS}
      { yybegin(RENAME); return MdlTypes.OPERATOR; }
    {RECORD_NAME}
      { yybegin(RENAME); return MdlTypes.RECORD_NAME; }
    {TO}
      { yybegin(RENAME); return MdlTypes.TO; }
    {SEMICOLON}
      { yybegin(YYINITIAL); return MdlTypes.SEMICOLON; }
}

<DROP> {
    {COMPONENT_TYPE_LITERAL}
      { yybegin(DROP); return MdlTypes.COMPONENT_TYPE_LITERAL; }
    {COMPONENT_NAME}
      { yybegin(DROP); return MdlTypes.COMPONENT_TYPE; }
    {IF_EXISTS}|{IF_NOT_EXISTS}
      { yybegin(DROP); return MdlTypes.OPERATOR; }
    {RECORD_NAME}
      { yybegin(DROP); return MdlTypes.RECORD_NAME; }
    {SEMICOLON}
      { yybegin(YYINITIAL); return MdlTypes.SEMICOLON; }
}

/**
----- COMMAND BLOCK SECTION START -----
 */

// Command pieces
<COMMAND_PIECE_START> {
    {ATTRIBUTE}
        { yybegin(ATTRIBUTE_COMMAND); return MdlTypes.ATTRIBUTE; }
    {ATTRIBUTE_LITERAL}
        { yybegin(NEW_ATTRIBUTE); return MdlTypes.ATTRIBUTE_LITERAL; }
    {COMPONENT_NAME}
        { yybegin(SUBCOMPONENT_CHANGE_COMMAND); return MdlTypes.COMPONENT_TYPE; }
    {SUBCOMMAND}
        { yybegin(SUBCOMPONENT_CHANGE_COMMAND); return MdlTypes.SUBCOMMAND; }
    {DROP}
        { yybegin(DROP_SUBCOMPONENT) ; return MdlTypes.SUBCOMMAND; }
    {RENAME}
        { yybegin(RENAME_SUBCOMPONENT) ; return MdlTypes.SUBCOMMAND; }
    {END_PAREN}
        { yybegin(COMMAND_END); return MdlTypes.END_PAREN; }
}

// Attribute pair
<ATTRIBUTE_COMMAND> {
    {ADD}|{DROP}
        { yybegin(ATTRIBUTE_COMMAND); return MdlTypes.ATTRIBUTE_COMMAND; }
    {IF_EXISTS}|{IF_NOT_EXISTS}
        { yybegin(ATTRIBUTE_COMMAND); return MdlTypes.OPERATOR; }
    {START_PAREN}
        { yybegin(VALUE); return MdlTypes.START_PAREN; }
}

// Attribute pair values
<VALUE> {
    [0-9.]+
        { yybegin(VALUE); return MdlTypes.VALUE; }
    {BOOL}
          { yybegin(VALUE); return MdlTypes.VALUE; }
    {SINGLE_QUOTE}
        { yybegin(STRING_CONTENTS); return MdlTypes.START_QUOTE; }
    {OPEN_BRACE}
        { yybegin(XML_START); return MdlTypes.START_BRACE; }
    {OPEN_BRACKET}
        { yybegin(EXPRESSION_CONTENTS); return MdlTypes.START_BRACKET; }
    {COMMA}
        { yybegin(VALUE); return MdlTypes.COMMA; }
    {END_PAREN}
        { yybegin(POST_ATTRIBUTE_COMMAND); return MdlTypes.END_PAREN; }
}

// String value
<STRING_CONTENTS> {
    [^'\\]* (\\)
        { yybegin(ESCAPE_CHAR); return MdlTypes.VALUE; }
    [^'\\]+ / {SINGLE_QUOTE}
        { yybegin(END_QUOTE); return MdlTypes.VALUE; }
    {SINGLE_QUOTE}
        { yybegin(VALUE); return MdlTypes.END_QUOTE; }
}

<ESCAPE_CHAR> {
    [^\\]
        { yybegin(STRING_CONTENTS); return MdlTypes.VALUE; }
    \\
        { yybegin(END_QUOTE); return MdlTypes.VALUE; }
}

<END_QUOTE> {
    [^']
        { yybegin(STRING_CONTENTS); return MdlTypes.VALUE; }
    {SINGLE_QUOTE}
        { yybegin(VALUE); return MdlTypes.END_QUOTE; }
}

// XML value
<XML_START> {
    {OPEN_ANGLE_BRACKET}
        { yybegin(XML_IDENTIFIER); return MdlTypes.OPEN_ANGLE_BRACKET; }
    {CLOSED_BRACE}
        { yybegin(VALUE); return MdlTypes.END_BRACE; }
    [^><\}\{\\\n\ ]+
        { yybegin(XML_START); return MdlTypes.XML_TAG_CONTENT; }
}

<XML_IDENTIFIER> {
    (\?)
        { yybegin(XML_IDENTIFIER); return MdlTypes.QUESTION; }
    (\/)
        { yybegin(XML_CLOSE_TAG_CONTENT); return MdlTypes.XML_SLASH; }
    [^\?\ \/>]+
        { yybegin(XML_CONTENT); return MdlTypes.XML_IDENTIFIER; }
}

<XML_CLOSE_TAG_CONTENT> [^\/>=]+
    { yybegin(XML_CONTENT_END); return MdlTypes.XML_IDENTIFIER;}

<XML_CONTENT> {
    [^\ \/>=]+
        { yybegin(XML_ASSIGNMENT); return MdlTypes.XML_ATTRIBUTE; }
    (\/) / {CLOSED_ANGLE_BRACKET}
        { yybegin(XML_CONTENT_END); return MdlTypes.XML_SLASH; }
    (\?) / {CLOSED_ANGLE_BRACKET}
        { yybegin(XML_CONTENT_END); return MdlTypes.QUESTION; }

    <XML_CONTENT_END> {
        {CLOSED_ANGLE_BRACKET}
            { yybegin(XML_START); return MdlTypes.CLOSED_ANGLE_BRACKET; }
    }
}

<XML_ASSIGNMENT> {EQUALS}
    { yybegin(XML_ATTRIBUTE_VALUE); return MdlTypes.EQUALS; }

<XML_ATTRIBUTE_VALUE> {DOUBLE_QUOTE}[^\"]*{DOUBLE_QUOTE}
    { yybegin(XML_CONTENT); return MdlTypes.XML_VALUE; }

// MDL Expression
<EXPRESSION_CONTENTS> {
    [^\]]+
        { yybegin(EXPRESSION_END); return MdlTypes.VALUE; }
    {CLOSED_BRACKET}
        { yybegin(VALUE); return MdlTypes.END_BRACKET; }
}

<EXPRESSION_END> {CLOSED_BRACKET}
    { yybegin(VALUE); return MdlTypes.END_BRACKET; }

// Post attribute commands
<POST_ATTRIBUTE_COMMAND> {
    {FIRST}|{LAST}
        { yybegin(COMMAND_BLOCK_END); return MdlTypes.POST_ATTRIBUTE_COMMAND; }
    {AFTER}
        { yybegin(ATTRIBUTE_COMMAND); return MdlTypes.POST_ATTRIBUTE_COMMAND; }
}

// Attribute literal
<NEW_ATTRIBUTE> {
    {ATTRIBUTE}
        { yybegin(NEW_ATTRIBUTE); return MdlTypes.ATTRIBUTE; }
    {START_PAREN}
         { yybegin(NESTED_ATTRIBUTE); return MdlTypes.START_PAREN; }
}

// Subcomponent change command
<SUBCOMPONENT_CHANGE_COMMAND> {
    {COMPONENT_NAME}
        { yybegin(SUBCOMPONENT_CHANGE_COMMAND); return MdlTypes.COMPONENT_TYPE; }
    {IF_EXISTS}|{IF_NOT_EXISTS}
        { yybegin(SUBCOMPONENT_CHANGE_COMMAND); return MdlTypes.OPERATOR; }
    {RECORD_NAME}
         { yybegin(SUBCOMPONENT_CHANGE_COMMAND); return MdlTypes.RECORD_NAME; }
    {START_PAREN}
         { yybegin(NESTED_ATTRIBUTE); return MdlTypes.START_PAREN; }
}

// Subcomponent rename
<RENAME_SUBCOMPONENT> {
    {COMPONENT_NAME}
        { yybegin(RENAME_SUBCOMPONENT) ; return MdlTypes.COMPONENT_TYPE; }
    {IF_EXISTS}|{IF_NOT_EXISTS}
        { yybegin(RENAME_SUBCOMPONENT); return MdlTypes.OPERATOR; }
    {RECORD_NAME}
        { yybegin(RENAME_SUBCOMPONENT); return MdlTypes.RECORD_NAME; }
    {TO}
        { yybegin(RENAME_SUBCOMPONENT); return MdlTypes.TO; }
    {COMMA}
        { yybegin(COMMAND_PIECE_START); return MdlTypes.COMMA; }
    {END_PAREN}
        { yybegin(COMMAND_END); return MdlTypes.END_PAREN; }
}

// Subcomponent drop
<DROP_SUBCOMPONENT> {
    {COMPONENT_NAME}
        { yybegin(DROP_SUBCOMPONENT); return MdlTypes.COMPONENT_TYPE; }
    {IF_EXISTS}|{IF_NOT_EXISTS}
        { yybegin(DROP_SUBCOMPONENT); return MdlTypes.OPERATOR; }
    {RECORD_NAME}
        { yybegin(DROP_SUBCOMPONENT); return MdlTypes.RECORD_NAME; }
    {COMMA}
         { yybegin(COMMAND_PIECE_START); return MdlTypes.COMMA; }
    {END_PAREN}
         { yybegin(COMMAND_END); return MdlTypes.END_PAREN; }
}

/**
----- NESTED BLOCK SECTION START -----
 */

// Nested attribute pair
<NESTED_ATTRIBUTE> {
    {ADD} | {DROP}
        { yybegin(NESTED_ATTRIBUTE); return MdlTypes.ATTRIBUTE_COMMAND; }
    {ATTRIBUTE}
        { yybegin(NESTED_ATTRIBUTE); return MdlTypes.ATTRIBUTE; }
    {END_PAREN}
        { yybegin(COMMAND_BLOCK_END); return MdlTypes.END_PAREN; }
    {IF_EXISTS}|{IF_NOT_EXISTS}
        { yybegin(NESTED_ATTRIBUTE); return MdlTypes.OPERATOR; }
    {START_PAREN}
        { yybegin(NESTED_VALUE); return MdlTypes.START_PAREN; }
}

// Nested attribute pair values
<NESTED_VALUE> {
    [0-9.]+
        { yybegin(NESTED_VALUE); return MdlTypes.VALUE; }
    {BOOL}
        { yybegin(NESTED_VALUE); return MdlTypes.VALUE; }
    {SINGLE_QUOTE}
        { yybegin(NESTED_STRING_CONTENTS); return MdlTypes.START_QUOTE; }
    {OPEN_BRACE}
        { yybegin(NESTED_XML_START); return MdlTypes.START_BRACE; }
    {OPEN_BRACKET}
        { yybegin(NESTED_EXPRESSION_CONTENTS); return MdlTypes.START_BRACKET; }
    {END_PAREN}
        { yybegin(NESTED_POST_ATTRIBUTE_COMMAND); return MdlTypes.END_PAREN; }
    {COMMA}
        { yybegin(NESTED_VALUE); return MdlTypes.COMMA; }
}

// String value
<NESTED_STRING_CONTENTS> {
    [^'\\]* (\\)
        { yybegin(NESTED_ESCAPE_CHAR); return MdlTypes.VALUE; }
    [^'\\]+ / {SINGLE_QUOTE}
        { yybegin(NESTED_END_QUOTE); return MdlTypes.VALUE; }
    {SINGLE_QUOTE}
        { yybegin(NESTED_VALUE); return MdlTypes.END_QUOTE; }
}

<NESTED_ESCAPE_CHAR> {
    [^\\]
        { yybegin(NESTED_STRING_CONTENTS); return MdlTypes.VALUE; }
    \\
        { yybegin(NESTED_END_QUOTE); return MdlTypes.VALUE; }
}

<NESTED_END_QUOTE> {
    {SINGLE_QUOTE}
        { yybegin(NESTED_VALUE); return MdlTypes.END_QUOTE; }
    [^']
        { yybegin(NESTED_STRING_CONTENTS); return MdlTypes.VALUE; }
}

// Nested XML Value
<NESTED_XML_START> {
    {OPEN_ANGLE_BRACKET}
        { yybegin(NESTED_XML_IDENTIFIER); return MdlTypes.OPEN_ANGLE_BRACKET; }
    {CLOSED_BRACE}
        { yybegin(NESTED_VALUE); return MdlTypes.END_BRACE; }
    [^><\}\{\\\n\ ]+
        { yybegin(NESTED_XML_START); return MdlTypes.XML_TAG_CONTENT; }
}

<NESTED_XML_IDENTIFIER> {
    (\?)
        { yybegin(NESTED_XML_IDENTIFIER); return MdlTypes.QUESTION; }
    (\/)
        { yybegin(NESTED_XML_CLOSE_CONTENT); return MdlTypes.XML_SLASH; }
    [^\?\ \/>]+
        { yybegin(NESTED_XML_CONTENT); return MdlTypes.XML_IDENTIFIER; }
}

<NESTED_XML_CLOSE_CONTENT> [^\/>=]+
    { yybegin(NESTED_XML_CONTENT_END); return MdlTypes.XML_IDENTIFIER;}

<NESTED_XML_CONTENT> {
    [^\ \/>=]+
        { yybegin(NESTED_XML_ASSIGNMENT); return MdlTypes.XML_ATTRIBUTE; }
    (\/) / {CLOSED_ANGLE_BRACKET}
        { yybegin(NESTED_XML_CONTENT_END); return MdlTypes.XML_SLASH; }
    (\?) / {CLOSED_ANGLE_BRACKET}
        { yybegin(NESTED_XML_CONTENT_END); return MdlTypes.QUESTION; }

    <NESTED_XML_CONTENT_END> {
        {CLOSED_ANGLE_BRACKET}
            { yybegin(NESTED_XML_START); return MdlTypes.CLOSED_ANGLE_BRACKET; }
    }
}

<NESTED_XML_ASSIGNMENT> {EQUALS}
    { yybegin(NESTED_XML_ATTRIBUTE_VALUE); return MdlTypes.EQUALS; }

<NESTED_XML_ATTRIBUTE_VALUE> {DOUBLE_QUOTE}[^\"]*{DOUBLE_QUOTE}
    { yybegin(NESTED_XML_CONTENT); return MdlTypes.XML_VALUE; }

// Expression
<NESTED_EXPRESSION_CONTENTS> {
    [^\]]+ / {CLOSED_BRACKET}
        { yybegin(NESTED_EXPRESSION_END); return MdlTypes.VALUE; }
    {CLOSED_BRACKET}
        { yybegin(NESTED_VALUE); return MdlTypes.END_BRACKET; }
}

<NESTED_EXPRESSION_END> {CLOSED_BRACKET}
    { yybegin(NESTED_VALUE); return MdlTypes.END_BRACKET; }

/**
----- BLOCK ENDING SECTION -----
 */

<NESTED_POST_ATTRIBUTE_COMMAND> {
    {FIRST}|{LAST}
        { yybegin(NESTED_POST_ATTRIBUTE_COMMAND); return MdlTypes.POST_ATTRIBUTE_COMMAND; }
    {AFTER}
        { yybegin(NESTED_ATTRIBUTE); return MdlTypes.POST_ATTRIBUTE_COMMAND; }
    {COMMA}
        { yybegin(NESTED_ATTRIBUTE); return MdlTypes.COMMA; }
    {END_PAREN}
        { yybegin(COMMAND_BLOCK_END); return MdlTypes.END_PAREN; }
}

<POST_ATTRIBUTE_COMMAND, COMMAND_BLOCK_END> {
    {COMMA}
        { yybegin(COMMAND_PIECE_START); return MdlTypes.COMMA; }
    {END_PAREN}
        { yybegin(COMMAND_END); return MdlTypes.END_PAREN; }
}

<COMMAND_END> {SEMICOLON}
    { yybegin(YYINITIAL); return MdlTypes.SEMICOLON; }

/**
----- INPUT PROCESSING RULES -----
 */

<YYINITIAL> {BUILDING}+
  { return MdlTypes.COMMAND; }

<ATTRIBUTE_COMMAND> {BUILDING}+
  {return MdlTypes.ATTRIBUTE_COMMAND; }

<POST_ATTRIBUTE_COMMAND,
NESTED_POST_ATTRIBUTE_COMMAND> {BUILDING}+
  { return MdlTypes.POST_ATTRIBUTE_COMMAND; }

<RENAME,
DROP,
CHANGE_COMMAND,
SUBCOMPONENT_CHANGE_COMMAND,
DROP_SUBCOMPONENT,
RENAME_SUBCOMPONENT> [a-z]+{BUILDING}+
  { return MdlTypes.RECORD_NAME; }

<COMMAND_PIECE_START> {BUILDING}+
  { return MdlTypes.ATTRIBUTE; }

<RENAME,
DROP,
CHANGE_COMMAND,
COMMAND_PIECE_START,
SUBCOMPONENT_CHANGE_COMMAND,
DROP_SUBCOMPONENT,
RENAME_SUBCOMPONENT> [A-Z]+{BUILDING}+
  { return MdlTypes.COMPONENT_TYPE; }

({CRLF}|{WHITE_SPACE})+
    { return TokenType.WHITE_SPACE; }
[^]
    { return TokenType.BAD_CHARACTER; }
