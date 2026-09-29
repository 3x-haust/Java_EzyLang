package io.github._3xhaust.interpreter.module;

import java.util.Map;

public class StrModule {
    public static void register(Map<String, NativeFunction> nativeFunctions) {
        nativeFunctions.put("toUpperCase", args -> {
            Args.count(args, 1, 1);
            return Args.string(args, 0).toUpperCase();
        });
        nativeFunctions.put("toLowerCase", args -> {
            Args.count(args, 1, 1);
            return Args.string(args, 0).toLowerCase();
        });
        nativeFunctions.put("trim", args -> {
            Args.count(args, 1, 1);
            return Args.string(args, 0).trim();
        });
        nativeFunctions.put("startsWith", args -> {
            Args.count(args, 2, 2);
            return Args.string(args, 0).startsWith(Args.string(args, 1));
        });
        nativeFunctions.put("endsWith", args -> {
            Args.count(args, 2, 2);
            return Args.string(args, 0).endsWith(Args.string(args, 1));
        });
        nativeFunctions.put("substring", args -> {
            Args.count(args, 3, 3);
            String value = Args.string(args, 0);
            int start = Args.intValue(args, 1);
            int end = Args.intValue(args, 2);
            int length = value.codePointCount(0, value.length());
            if (start < 0 || end < start || end > length) {
                throw new IllegalArgumentException("substring indexes out of range");
            }
            int startOffset = value.offsetByCodePoints(0, start);
            int endOffset = value.offsetByCodePoints(0, end);
            return value.substring(startOffset, endOffset);
        });
        nativeFunctions.put("replace", args -> {
            Args.count(args, 3, 3);
            return Args.string(args, 0).replace(Args.string(args, 1), Args.string(args, 2));
        });
        nativeFunctions.put("strContains", args -> {
            Args.count(args, 2, 2);
            return Args.string(args, 0).contains(Args.string(args, 1));
        });
        nativeFunctions.put("strIndexOf", args -> {
            Args.count(args, 2, 2);
            String value = Args.string(args, 0);
            String search = Args.string(args, 1);
            int offset = value.indexOf(search);
            return offset < 0 ? -1L : (long) value.codePointCount(0, offset);
        });
        nativeFunctions.put("strLength", args -> {
            Args.count(args, 1, 1);
            String value = Args.string(args, 0);
            return (long) value.codePointCount(0, value.length());
        });
        nativeFunctions.put("padLeft", args -> {
            Args.count(args, 3, 3);
            String value = Args.string(args, 0);
            int targetLength = Args.intValue(args, 1);
            String padding = Args.string(args, 2);
            int needed = targetLength - value.codePointCount(0, value.length());
            if (needed <= 0) return value;
            if (padding.isEmpty()) throw new IllegalArgumentException("padding string cannot be empty");
            return repeatCodePoints(padding, needed) + value;
        });
        nativeFunctions.put("padRight", args -> {
            Args.count(args, 3, 3);
            String value = Args.string(args, 0);
            int targetLength = Args.intValue(args, 1);
            String padding = Args.string(args, 2);
            int needed = targetLength - value.codePointCount(0, value.length());
            if (needed <= 0) return value;
            if (padding.isEmpty()) throw new IllegalArgumentException("padding string cannot be empty");
            return value + repeatCodePoints(padding, needed);
        });
        nativeFunctions.put("reverse", args -> {
            Args.count(args, 1, 1);
            int[] codePoints = Args.string(args, 0).codePoints().toArray();
            StringBuilder result = new StringBuilder();
            for (int i = codePoints.length - 1; i >= 0; i--) result.appendCodePoint(codePoints[i]);
            return result.toString();
        });
        nativeFunctions.put("format", args -> {
            Args.count(args, 2, 2);
            double number = ((Number) Args.number(args, 0)).doubleValue();
            int decimals = Args.intValue(args, 1);
            return String.format("%." + decimals + "f", number);
        });
    }

    private static String repeatCodePoints(String padding, int count) {
        int[] codePoints = padding.codePoints().toArray();
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < count; i++) result.appendCodePoint(codePoints[i % codePoints.length]);
        return result.toString();
    }
}
