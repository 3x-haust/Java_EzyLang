package io.github._3xhaust.ezylang.exception;

public class ParseException extends Exception {
    private final String fileName;
    private final int line;
    private final int column;
    private final String errorMessage;
    private final String errorLine;

    public ParseException(String fileName, String errorMessage, int line, int column, String errorLine) {
        super(errorMessage);
        this.fileName = fileName;
        this.errorMessage = errorMessage;
        this.line = line;
        this.column = column;
        this.errorLine = errorLine;
    }

    public ParseException detach() {
        return new ParseException(fileName, errorMessage, line, column, errorLine);
    }

    public int getLine() {
        return line;
    }

    public String getFormattedMessage() {
        if (line <= 0 || column <= 0) {
            return String.format("%s: error: %s", fileName, errorMessage);
        }
        return String.format("%s:%d:%d: error: %s\n%s\n%s^",
                fileName, line, column, errorMessage, errorLine, " ".repeat(column - 1));
    }
}