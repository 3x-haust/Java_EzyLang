package io.github._3xhaust.interpreter.module;

import io.github._3xhaust.ezylang.exception.ParseException;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Map;

public class IoModule {
    private static final BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));

    public static void register(Map<String, NativeFunction> nativeFunctions) {
        nativeFunctions.put("readFile", args -> {
            Args.count(args, 1, 1);
            String path = Args.string(args, 0);
            try {
                return Files.readString(Paths.get(path));
            } catch (Exception e) {
                throw new ParseException("io", "Failed to read file: " + e.getMessage(), 0, 0, "");
            }
        });

        nativeFunctions.put("writeFile", args -> {
            Args.count(args, 2, 2);
            String path = Args.string(args, 0);
            String content = Args.string(args, 1);
            try {
                Files.writeString(Paths.get(path), content);
                return null;
            } catch (Exception e) {
                throw new ParseException("io", "Failed to write file: " + e.getMessage(), 0, 0, "");
            }
        });

        nativeFunctions.put("appendFile", args -> {
            Args.count(args, 2, 2);
            String path = Args.string(args, 0);
            String content = Args.string(args, 1);
            try {
                Files.writeString(Paths.get(path), content, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                return null;
            } catch (Exception e) {
                throw new ParseException("io", "Failed to append file: " + e.getMessage(), 0, 0, "");
            }
        });

        nativeFunctions.put("readLines", args -> {
            Args.count(args, 1, 1);
            String path = Args.string(args, 0);
            try {
                return Files.readAllLines(Paths.get(path));
            } catch (Exception e) {
                throw new ParseException("io", "Failed to read lines: " + e.getMessage(), 0, 0, "");
            }
        });

        nativeFunctions.put("fileExists", args -> {
            Args.count(args, 1, 1);
            return Files.exists(Paths.get(Args.string(args, 0)));
        });

        nativeFunctions.put("deleteFile", args -> {
            Args.count(args, 1, 1);
            String path = Args.string(args, 0);
            try {
                return Files.deleteIfExists(Paths.get(path));
            } catch (Exception e) {
                throw new ParseException("io", "Failed to delete file: " + e.getMessage(), 0, 0, "");
            }
        });

        nativeFunctions.put("readLine", args -> {
            Args.count(args, 0, 1);
            String prompt = args.isEmpty() ? null : Args.string(args, 0);
            try {
                if (prompt != null) System.out.print(prompt);
                return stdin.readLine();
            } catch (Exception e) {
                throw new ParseException("io", "Failed to read input: " + e.getMessage(), 0, 0, "");
            }
        });

        nativeFunctions.put("listDir", args -> {
            Args.count(args, 1, 1);
            String path = Args.string(args, 0);
            try (var entries = Files.list(Paths.get(path))) {
                return entries.map(entry -> entry.getFileName().toString())
                        .collect(java.util.stream.Collectors.toList());
            } catch (Exception e) {
                throw new ParseException("io", "Failed to list directory: " + e.getMessage(), 0, 0, "");
            }
        });

        nativeFunctions.put("mkdir", args -> {
            Args.count(args, 1, 1);
            String path = Args.string(args, 0);
            try {
                Files.createDirectories(Paths.get(path));
                return null;
            } catch (Exception e) {
                throw new ParseException("io", "Failed to create directory: " + e.getMessage(), 0, 0, "");
            }
        });

        nativeFunctions.put("isDir", args -> {
            Args.count(args, 1, 1);
            return Files.isDirectory(Paths.get(Args.string(args, 0)));
        });

        nativeFunctions.put("isFile", args -> {
            Args.count(args, 1, 1);
            return Files.isRegularFile(Paths.get(Args.string(args, 0)));
        });

        nativeFunctions.put("fileSize", args -> {
            Args.count(args, 1, 1);
            String path = Args.string(args, 0);
            try {
                return Files.size(Paths.get(path));
            } catch (Exception e) {
                throw new ParseException("io", "Failed to get file size: " + e.getMessage(), 0, 0, "");
            }
        });
    }
}
