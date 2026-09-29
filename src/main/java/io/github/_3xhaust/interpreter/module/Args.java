package io.github._3xhaust.interpreter.module;

import io.github._3xhaust.interpreter.runtime.Num;

import java.util.List;
import java.util.Map;

public final class Args {
    private Args() {}

    public static void count(List<Object> args, int min, int max) {
        int size = args.size();
        if (size >= min && size <= max) return;
        if (min == max) {
            throw new IllegalArgumentException("expected " + min + " argument" + (min == 1 ? "" : "s") + " but got " + size);
        }
        throw new IllegalArgumentException("expected " + min + " to " + max + " arguments but got " + size);
    }

    public static String string(List<Object> args, int index) {
        Object value = args.get(index);
        if (value instanceof String string) return string;
        throw typeError(index, "a string", value);
    }

    public static Object number(List<Object> args, int index) {
        Object value = args.get(index);
        if (Num.isNumber(value)) return value;
        throw typeError(index, "a number", value);
    }

    public static long integer(List<Object> args, int index) {
        Object value = args.get(index);
        if (value instanceof Long integer) return integer;
        throw typeError(index, "an integer", value);
    }

    public static int intValue(List<Object> args, int index) {
        long value = integer(args, index);
        if (value < Integer.MIN_VALUE || value > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("argument " + (index + 1) + " is outside the supported integer range");
        }
        return (int) value;
    }

    public static List<?> list(List<Object> args, int index) {
        Object value = args.get(index);
        if (value instanceof List<?> list) return list;
        throw typeError(index, "an array", value);
    }

    public static Map<?, ?> map(List<Object> args, int index) {
        Object value = args.get(index);
        if (value instanceof Map<?, ?> map) return map;
        throw typeError(index, "an object", value);
    }

    public static String typeName(Object value) {
        if (value == null) return "null";
        if (Num.isNumber(value)) return "number";
        if (value instanceof String) return "string";
        if (value instanceof Boolean) return "boolean";
        if (value instanceof List<?>) return "array";
        if (value instanceof Map<?, ?>) return "object";
        return value.getClass().getSimpleName();
    }

    private static IllegalArgumentException typeError(int index, String expected, Object value) {
        return new IllegalArgumentException("argument " + (index + 1) + " must be " + expected + ", got " + typeName(value));
    }
}
