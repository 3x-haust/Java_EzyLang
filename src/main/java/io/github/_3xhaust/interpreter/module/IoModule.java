package io.github._3xhaust.interpreter.module;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class IoModule {
    private static final BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));

    public static void register(Map<String, NativeFunction> nativeFunctions) {
        nativeFunctions.put("readFile", args -> {
            try {
                return Files.readString(Paths.get(String.valueOf(args.get(0))));
            } catch (Exception e) {
                throw new io.github._3xhaust.ezylang.exception.ParseException("io", "Failed to read file: " + e.getMessage(), 0, 0, "");
            }
        });

        nativeFunctions.put("writeFile", args -> {
            try {
                Files.writeString(Paths.get(String.valueOf(args.get(0))), String.valueOf(args.get(1)));
                return null;
            } catch (Exception e) {
                throw new io.github._3xhaust.ezylang.exception.ParseException("io", "Failed to write file: " + e.getMessage(), 0, 0, "");
            }
        });

        nativeFunctions.put("appendFile", args -> {
            try {
                Files.writeString(Paths.get(String.valueOf(args.get(0))), String.valueOf(args.get(1)), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                return null;
            } catch (Exception e) {
                throw new io.github._3xhaust.ezylang.exception.ParseException("io", "Failed to append file: " + e.getMessage(), 0, 0, "");
            }
        });

        nativeFunctions.put("readLines", args -> {
            try {
                return Files.readAllLines(Paths.get(String.valueOf(args.get(0))));
            } catch (Exception e) {
                throw new io.github._3xhaust.ezylang.exception.ParseException("io", "Failed to read lines: " + e.getMessage(), 0, 0, "");
            }
        });

        nativeFunctions.put("fileExists", args -> Files.exists(Paths.get(String.valueOf(args.get(0)))));

        nativeFunctions.put("deleteFile", args -> {
            try {
                return Files.deleteIfExists(Paths.get(String.valueOf(args.get(0))));
            } catch (Exception e) {
                throw new io.github._3xhaust.ezylang.exception.ParseException("io", "Failed to delete file: " + e.getMessage(), 0, 0, "");
            }
        });

        nativeFunctions.put("readLine", args -> {
            try {
                if (!args.isEmpty()) {
                    System.out.print(String.valueOf(args.get(0)));
                }
                return stdin.readLine();
            } catch (Exception e) {
                throw new io.github._3xhaust.ezylang.exception.ParseException("io", "Failed to read input: " + e.getMessage(), 0, 0, "");
            }
        });

        nativeFunctions.put("listDir", args -> {
            try {
                return Files.list(Paths.get(String.valueOf(args.get(0))))
                        .map(p -> p.getFileName().toString())
                        .collect(java.util.stream.Collectors.toList());
            } catch (Exception e) {
                throw new io.github._3xhaust.ezylang.exception.ParseException("io", "Failed to list directory: " + e.getMessage(), 0, 0, "");
            }
        });

        nativeFunctions.put("mkdir", args -> {
            try {
                Files.createDirectories(Paths.get(String.valueOf(args.get(0))));
                return null;
            } catch (Exception e) {
                throw new io.github._3xhaust.ezylang.exception.ParseException("io", "Failed to create directory: " + e.getMessage(), 0, 0, "");
            }
        });

        nativeFunctions.put("isDir", args -> Files.isDirectory(Paths.get(String.valueOf(args.get(0)))));

        nativeFunctions.put("isFile", args -> Files.isRegularFile(Paths.get(String.valueOf(args.get(0)))));

        nativeFunctions.put("fileSize", args -> {
            try {
                return (double) Files.size(Paths.get(String.valueOf(args.get(0))));
            } catch (Exception e) {
                throw new io.github._3xhaust.ezylang.exception.ParseException("io", "Failed to get file size: " + e.getMessage(), 0, 0, "");
            }
        });
    }
}
