package io.github._3xhaust.ezylang.lexer;

import lombok.Getter;

public class Token {
    private final TokenType type;
    @Getter
    private final String value;
    @Getter
    private final int line;
    @Getter
    private final int column;

    public Token(TokenType type, String value, int line, int column) {
        this.type = type;
        this.value = value != null ? value : type.toString();
        this.line = line;
        this.column = column;
    }

    public TokenType getToken() {
        return type;
    }

    @Override
    public String toString() {
        return "Token{" +
                "type='" + type.toString() + '\'' +
                ", value='" + value + '\'' +
                ", line=" + line +
                ", column=" + column +
                '}';
    }

    public enum TokenType {
        PLUS,
        MINUS,
        ASTERISK,
        SLASH,
        PERCENT,

        EQUAL_EQUAL,
        NOT_EQUAL,
        LESS_THAN,
        GREATER_THAN,
        LESS_THAN_OR_EQUAL,
        GREATER_THAN_OR_EQUAL,

        AND,
        OR,
        BANG,

        EQUAL,
        PLUS_EQUAL,
        MINUS_EQUAL,
        ASTERISK_EQUAL,
        SLASH_EQUAL,
        PERCENT_EQUAL,

        PLUS_PLUS,
        MINUS_MINUS,

        BITWISE_AND,
        BITWISE_OR,
        BITWISE_XOR,
        BITWISE_NOT,
        LEFT_SHIFT,
        RIGHT_SHIFT,

        NUMBER,
        CHAR,
        STRING,
        BOOLEAN,
        NULL,
        VOID,

        IDENTIFIER,
        NUMBER_LITERAL,
        CHAR_LITERAL,
        STRING_LITERAL,
        BOOLEAN_LITERAL,
        VARIABLE_LITERAL,

        PRINT,
        PRINTLN,

        IF,
        ELSE,
        ELSE_IF,
        WHILE,
        FOR,
        BREAK,
        CONTINUE,
        RETURN,
        IS,
        AS,

        FUNC,
        MEMO,

        IMPORT,
        FROM,
        AS_IDENTIFIER,

        IN,

        SWITCH,
        CASE,
        DEFAULT,

        TEST,
        ASSERT,

        CLASS,
        INTERFACE,
        NEW,
        SELF,
        PARENT,
        OVERRIDE,
        EXTENDS,
        DECORATOR,
        ENTRY,
        AT,

        LEFT_PAREN,
        RIGHT_PAREN,
        LEFT_BRACE,
        RIGHT_BRACE,
        LEFT_BRACKET,
        RIGHT_BRACKET,
        DOLLAR,
        ARROW,
        COMMA,
        DOT,
        DOT_DOT,
        COLON,
        SEMICOLON,

        EOF;

        @Override
        public String toString() {
            return super.toString().toLowerCase().replace("_", " ");
        }
    }

}