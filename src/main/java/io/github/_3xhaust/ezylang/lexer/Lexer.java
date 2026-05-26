package io.github._3xhaust.ezylang.lexer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Lexer {
    private static final Map<String, Token.TokenType> keywords;

    static {
        keywords = new HashMap<>();
        keywords.put("if", Token.TokenType.IF);
        keywords.put("else", Token.TokenType.ELSE);
        keywords.put("while", Token.TokenType.WHILE);
        keywords.put("for", Token.TokenType.FOR);
        keywords.put("break", Token.TokenType.BREAK);
        keywords.put("continue", Token.TokenType.CONTINUE);
        keywords.put("return", Token.TokenType.RETURN);
        keywords.put("is", Token.TokenType.IS);
        keywords.put("as", Token.TokenType.AS);
        keywords.put("func", Token.TokenType.FUNC);
        keywords.put("in", Token.TokenType.IN);
        keywords.put("print", Token.TokenType.PRINT);
        keywords.put("println", Token.TokenType.PRINTLN);
        keywords.put("true", Token.TokenType.BOOLEAN_LITERAL);
        keywords.put("false", Token.TokenType.BOOLEAN_LITERAL);
        keywords.put("null", Token.TokenType.NULL);
        keywords.put("void", Token.TokenType.VOID);
        keywords.put("number", Token.TokenType.NUMBER);
        keywords.put("char", Token.TokenType.CHAR);
        keywords.put("string", Token.TokenType.STRING);
        keywords.put("boolean", Token.TokenType.BOOLEAN);
        keywords.put("import", Token.TokenType.IMPORT);
        keywords.put("from", Token.TokenType.FROM);
        keywords.put("switch", Token.TokenType.SWITCH);
        keywords.put("case", Token.TokenType.CASE);
        keywords.put("default", Token.TokenType.DEFAULT);
        keywords.put("assert", Token.TokenType.ASSERT);
        keywords.put("class", Token.TokenType.CLASS);
        keywords.put("interface", Token.TokenType.INTERFACE);
        keywords.put("new", Token.TokenType.NEW);
        keywords.put("self", Token.TokenType.SELF);
        keywords.put("parent", Token.TokenType.PARENT);
        keywords.put("extends", Token.TokenType.EXTENDS);
        keywords.put("decorator", Token.TokenType.DECORATOR);
        keywords.put("entry", Token.TokenType.ENTRY);
    }

    private final String source;
    private final List<Token> tokens = new ArrayList<>();
    private int start = 0;
    private int current = 0;
    private int line = 1;
    private int column = 1;
    private int tokenStartColumn = 1;
    private int tokenStartLine = 1;

    public Lexer(String source) {
        this.source = source;
    }

    public List<Token> scanTokens() {
        while (!isAtEnd()) {
            start = current;
            tokenStartColumn = column;
            tokenStartLine = line;
            scanToken();
        }

        tokens.add(new Token(Token.TokenType.EOF, "", line, column));
        return tokens;
    }

    private void scanToken() {
        char c = advance();
        switch (c) {
            case '(' -> addToken(Token.TokenType.LEFT_PAREN);
            case ')' -> addToken(Token.TokenType.RIGHT_PAREN);
            case '{' -> addToken(Token.TokenType.LEFT_BRACE);
            case '}' -> addToken(Token.TokenType.RIGHT_BRACE);
            case '[' -> addToken(Token.TokenType.LEFT_BRACKET);
            case ']' -> addToken(Token.TokenType.RIGHT_BRACKET);
            case ',' -> addToken(Token.TokenType.COMMA);
            case '.' -> {
                if (match('.')) {
                    addToken(Token.TokenType.DOT_DOT);
                } else {
                    addToken(Token.TokenType.DOT);
                }
            }
            case ':' -> addToken(Token.TokenType.COLON);
            case ';' -> addToken(Token.TokenType.SEMICOLON);
            case '$' -> addToken(Token.TokenType.DOLLAR);
            case '@' -> addToken(Token.TokenType.AT);

            case '+' -> {
                if (match('=')) {
                    addToken(Token.TokenType.PLUS_EQUAL);
                } else if (match('+')) {
                    addToken(Token.TokenType.PLUS_PLUS);
                } else {
                    addToken(Token.TokenType.PLUS);
                }
            }
            case '-' -> {
                if (match('=')) {
                    addToken(Token.TokenType.MINUS_EQUAL);
                } else if (match('-')) {
                    addToken(Token.TokenType.MINUS_MINUS);
                } else if (match('>')) {
                    addToken(Token.TokenType.ARROW);
                } else {
                    addToken(Token.TokenType.MINUS);
                }
            }
            case '*' -> {
                if (match('=')) {
                    addToken(Token.TokenType.ASTERISK_EQUAL);
                } else {
                    addToken(Token.TokenType.ASTERISK);
                }
            }
            case '/' -> {
                if (match('/')) {
                    while (peek() != '\n' && !isAtEnd()) advance();
                } else if (match('*')) {
                    blockComment();
                } else if (match('=')) {
                    addToken(Token.TokenType.SLASH_EQUAL);
                } else {
                    addToken(Token.TokenType.SLASH);
                }
            }
            case '%' -> {
                if (match('=')) {
                    addToken(Token.TokenType.PERCENT_EQUAL);
                } else {
                    addToken(Token.TokenType.PERCENT);
                }
            }
            case '!' -> {
                if (match('=')) {
                    addToken(Token.TokenType.NOT_EQUAL);
                } else {
                    addToken(Token.TokenType.BANG);
                }
            }
            case '=' -> {
                if (match('=')) {
                    addToken(Token.TokenType.EQUAL_EQUAL);
                } else {
                    addToken(Token.TokenType.EQUAL);
                }
            }
            case '<' -> {
                if (match('=')) {
                    addToken(Token.TokenType.LESS_THAN_OR_EQUAL);
                } else if (match('<')) {
                    addToken(Token.TokenType.LEFT_SHIFT);
                } else {
                    addToken(Token.TokenType.LESS_THAN);
                }
            }
            case '>' -> {
                if (match('=')) {
                    addToken(Token.TokenType.GREATER_THAN_OR_EQUAL);
                } else if (match('>')) {
                    addToken(Token.TokenType.RIGHT_SHIFT);
                } else {
                    addToken(Token.TokenType.GREATER_THAN);
                }
            }
            case '&' -> {
                if (match('&')) {
                    addToken(Token.TokenType.AND);
                } else {
                    addToken(Token.TokenType.BITWISE_AND);
                }
            }
            case '|' -> {
                if (match('|')) {
                    addToken(Token.TokenType.OR);
                } else {
                    addToken(Token.TokenType.BITWISE_OR);
                }
            }
            case '^' -> addToken(Token.TokenType.BITWISE_XOR);
            case '~' -> addToken(Token.TokenType.BITWISE_NOT);

            case '"' -> string();
            case '\'' -> charLiteral();

            case ' ', '\r', '\t' -> {}
            case '\n' -> {
                line++;
                column = 1;
            }

            default -> {
                if (isDigit(c)) {
                    number();
                } else if (isAlpha(c)) {
                    identifier();
                } else {
                    throw new RuntimeException("Unexpected character: " + c + " at line " + line + ", column " + column);
                }
            }
        }
    }

    private void blockComment() {
        while (!isAtEnd()) {
            if (peek() == '*' && peekNext() == '/') {
                advance();
                advance();
                return;
            } else if (peek() == '\n') {
                line++;
                column = 1;
                advance();
            } else {
                advance();
            }
        }

        throw new RuntimeException("Unclosed block comment at line " + line + ", column " + column);
    }

    private void string() {
        StringBuilder value = new StringBuilder();
        int braceDepth = 0;

        while (!isAtEnd()) {
            if (peek() == '"' && braceDepth == 0) break;

            if (peek() == '\n') {
                line++;
                column = 1;
            }

            char c = advance();

            if (c == '\\') {
                if (isAtEnd()) throw new RuntimeException("Unterminated string at line " + line + ", column " + column);
                c = advance();
                switch (c) {
                    case 'n' -> value.append('\n');
                    case 't' -> value.append('\t');
                    case '\\' -> value.append('\\');
                    case '"' -> value.append('\"');
                    default ->
                            throw new RuntimeException("Invalid escape sequence at line " + line + ", column " + column);
                }
            } else if (c == '$' && peek() == '{') {
                value.append(c);
                value.append(advance());
                braceDepth++;
            } else if (c == '{' && braceDepth > 0) {
                value.append(c);
                braceDepth++;
            } else if (c == '}' && braceDepth > 0) {
                value.append(c);
                braceDepth--;
            } else if (c == '"' && braceDepth > 0) {
                value.append(c);
            } else {
                value.append(c);
            }
        }

        if (isAtEnd()) {
            throw new RuntimeException("Unterminated string at line " + line + ", column " + column);
        }

        advance();

        addToken(Token.TokenType.STRING_LITERAL, value.toString());
    }

    private void charLiteral() {
        if (peek() == '\\') {
            advance();
            char c = advance();

            switch (c) {
                case 'n' -> addToken(Token.TokenType.CHAR_LITERAL, "\n");
                case 't' -> addToken(Token.TokenType.CHAR_LITERAL, "\t");
                case '\\' -> addToken(Token.TokenType.CHAR_LITERAL, "\\");
                case '\'' -> addToken(Token.TokenType.CHAR_LITERAL, "'");
                default -> throw new RuntimeException("Invalid escape sequence at line " + line + ", column " + column);
            }
        } else {
            advance();
        }

        if (peek() != '\'') {
            throw new RuntimeException("Unterminated character literal at line " + line + ", column " + column);
        }

        advance();
    }


    private void number() {
        while (isDigit(peek())) advance();

        if (peek() == '.' && isDigit(peekNext())) {

            do advance();
            while (isDigit(peek()));
        }

        addToken(Token.TokenType.NUMBER_LITERAL,
                source.substring(start, current));
    }

    private void identifier() {
        while (isAlphaNumeric(peek())) advance();

        String text = source.substring(start, current);
        Token.TokenType type = keywords.getOrDefault(text, Token.TokenType.IDENTIFIER);
        addToken(type, text);
    }

    private boolean match(char expected) {
        if (isAtEnd()) return false;
        if (source.charAt(current) != expected) return false;

        current++;
        column++;
        return true;
    }

    private char peek() {
        if (isAtEnd()) return '\0';
        return source.charAt(current);
    }

    private char peekNext() {
        if (current + 1 >= source.length()) return '\0';
        return source.charAt(current + 1);
    }

    private boolean isAlpha(char c) {
        return (c >= 'a' && c <= 'z') ||
                (c >= 'A' && c <= 'Z') ||
                c == '_';
    }

    private boolean isAlphaNumeric(char c) {
        return isAlpha(c) || isDigit(c);
    }

    private boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private char advance() {
        current++;
        column++;
        return source.charAt(current - 1);
    }

    private void addToken(Token.TokenType type) {
        addToken(type, null);
    }

    private void addToken(Token.TokenType type, String value) {
        tokens.add(new Token(type, value, tokenStartLine, tokenStartColumn));
    }

    private boolean isAtEnd() {
        return current >= source.length();
    }
}