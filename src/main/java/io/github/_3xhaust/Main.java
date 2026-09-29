package io.github._3xhaust;

import io.github._3xhaust.ezylang.exception.ParseException;
import io.github._3xhaust.ezylang.lexer.Lexer;
import io.github._3xhaust.ezylang.lexer.Token;
import io.github._3xhaust.ezylang.ast.Ast.Program;
import io.github._3xhaust.ezylang.parser.Parser;
import io.github._3xhaust.interpreter.Interpreter;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class Main {
    private static final long INTERPRETER_STACK_SIZE = 1L << 28;

    public static void main(String[] args) throws InterruptedException {
        Thread worker = new Thread(null, () -> run(args), "ezylang-main", INTERPRETER_STACK_SIZE);
        worker.setUncaughtExceptionHandler((thread, error) -> {
            String message = error.getMessage() != null ? error.getMessage() : error.getClass().getSimpleName();
            System.err.println("error: " + message);
            System.exit(1);
        });
        worker.start();
        worker.join();
    }

    private static void run(String[] args) {
        if (args.length < 1 || args.length > 2) {
            System.out.println("Usage: java -jar ezylang-<version>.jar [test] <source file>");
            System.exit(1);
        }

        boolean testMode = false;
        String fileName;

        if (args.length == 2 && args[0].equals("test")) {
            testMode = true;
            fileName = args[1];
        } else if (args.length == 1) {
            fileName = args[0];
        } else {
            System.out.println("Usage: java -jar ezylang-<version>.jar [test] <source file>");
            System.exit(1);
            return;
        }

        try {
            if (!fileName.endsWith(".ezy")) throw new IOException("invalid file extension: must be '.ezy'");
            if (!java.nio.file.Files.isRegularFile(java.nio.file.Path.of(fileName))) throw new IOException("file not found");

            String input = readFile(fileName);
            Lexer lexer = new Lexer(input);
            List<Token> tokens = lexer.scanTokens();

            Parser parser = new Parser(fileName, input, tokens);
            Program program = parser.parse();

            Interpreter interpreter = new Interpreter(fileName, input);
            interpreter.setTestMode(testMode);
            interpreter.interpret(program);

            if (testMode) {
                System.out.println("\n=== " + interpreter.getTestsPassed() + " passed, " + interpreter.getTestsFailed() + " failed ===");
                if (interpreter.getTestsFailed() > 0) System.exit(1);
            }
        } catch (IOException e) {
            System.err.println(fileName + ": error: " + e.getMessage());
            System.exit(1);
        } catch (ParseException e) {
            System.err.println(e.getFormattedMessage());
            System.exit(1);
        } catch (StackOverflowError e) {
            System.err.println(fileName + ": error: Stack overflow: recursion too deep");
            System.exit(1);
        } catch (RuntimeException e) {
            String message = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            System.err.println(fileName + ": error: " + message);
            System.exit(1);
        }
    }

    public static String readFile(String fileName) throws IOException {
        StringBuilder input = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(fileName, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                input.append(line).append("\n");
            }
        }
        return input.toString();
    }
}