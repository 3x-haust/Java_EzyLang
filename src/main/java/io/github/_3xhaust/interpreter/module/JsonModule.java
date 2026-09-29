package io.github._3xhaust.interpreter.module;

import io.github._3xhaust.ezylang.exception.ParseException;
import io.github._3xhaust.interpreter.runtime.Num;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class JsonModule {
    public static void register(Map<String, NativeFunction> nativeFunctions) {
        nativeFunctions.put("parse", args -> {
            Args.count(args, 1, 1);
            String json = Args.string(args, 0);
            try {
                int[] pos = {0};
                Object value = parseValue(json, pos);
                skipWhitespace(json, pos);
                if (pos[0] != json.length()) {
                    throw error("Unexpected trailing characters", pos[0]);
                }
                return value;
            } catch (RuntimeException e) {
                throw new ParseException("json", "Invalid JSON: " + e.getMessage(), 0, 0, "");
            }
        });

        nativeFunctions.put("stringify", args -> {
            Args.count(args, 1, 1);
            return toJson(args.get(0));
        });

        nativeFunctions.put("get", args -> {
            Args.count(args, 2, 2);
            Map<?, ?> map = Args.map(args, 0);
            String key = Args.string(args, 1);
            return map.get(key);
        });

        nativeFunctions.put("set", args -> {
            Args.count(args, 3, 3);
            Map<?, ?> object = Args.map(args, 0);
            String key = Args.string(args, 1);
            @SuppressWarnings("unchecked")
            Map<Object, Object> map = (Map<Object, Object>) object;
            map.put(key, args.get(2));
            return object;
        });

        nativeFunctions.put("keys", args -> {
            Args.count(args, 1, 1);
            return new ArrayList<>(Args.map(args, 0).keySet());
        });

        nativeFunctions.put("values", args -> {
            Args.count(args, 1, 1);
            return new ArrayList<>(Args.map(args, 0).values());
        });

        nativeFunctions.put("hasKey", args -> {
            Args.count(args, 2, 2);
            Map<?, ?> map = Args.map(args, 0);
            String key = Args.string(args, 1);
            return map.containsKey(key);
        });

        nativeFunctions.put("remove", args -> {
            Args.count(args, 2, 2);
            Map<?, ?> object = Args.map(args, 0);
            String key = Args.string(args, 1);
            @SuppressWarnings("unchecked")
            Map<Object, Object> map = (Map<Object, Object>) object;
            map.remove(key);
            return object;
        });

        nativeFunctions.put("newObject", args -> {
            Args.count(args, 0, 0);
            return new LinkedHashMap<String, Object>();
        });
    }

    private static Object parseValue(String json, int[] pos) {
        skipWhitespace(json, pos);
        if (pos[0] >= json.length()) throw error("Expected a value", pos[0]);
        return switch (json.charAt(pos[0])) {
            case '"' -> parseString(json, pos);
            case '{' -> parseObject(json, pos);
            case '[' -> parseArray(json, pos);
            case 't' -> parseLiteral(json, pos, "true", true);
            case 'f' -> parseLiteral(json, pos, "false", false);
            case 'n' -> parseLiteral(json, pos, "null", null);
            case '-' -> parseNumber(json, pos);
            default -> {
                char current = json.charAt(pos[0]);
                if (current >= '0' && current <= '9') yield parseNumber(json, pos);
                throw error("Unexpected character '" + current + "'", pos[0]);
            }
        };
    }

    private static String parseString(String json, int[] pos) {
        if (pos[0] >= json.length() || json.charAt(pos[0]) != '"') {
            throw error("Expected a string", pos[0]);
        }
        pos[0]++;
        StringBuilder result = new StringBuilder();
        while (pos[0] < json.length()) {
            char current = json.charAt(pos[0]++);
            if (current == '"') return result.toString();
            if (current < 0x20) throw error("Unescaped control character", pos[0] - 1);
            if (current != '\\') {
                result.append(current);
                continue;
            }
            if (pos[0] >= json.length()) throw error("Unterminated escape sequence", pos[0]);
            char escape = json.charAt(pos[0]++);
            switch (escape) {
                case '"' -> result.append('"');
                case '\\' -> result.append('\\');
                case '/' -> result.append('/');
                case 'b' -> result.append('\b');
                case 'f' -> result.append('\f');
                case 'n' -> result.append('\n');
                case 'r' -> result.append('\r');
                case 't' -> result.append('\t');
                case 'u' -> result.append(parseUnicodeEscape(json, pos));
                default -> throw error("Unknown escape sequence \\" + escape, pos[0] - 2);
            }
        }
        throw error("Unterminated string", pos[0]);
    }

    private static char parseUnicodeEscape(String json, int[] pos) {
        if (pos[0] + 4 > json.length()) throw error("Incomplete Unicode escape", pos[0]);
        int value = 0;
        for (int i = 0; i < 4; i++) {
            int digit = Character.digit(json.charAt(pos[0] + i), 16);
            if (digit < 0) throw error("Invalid Unicode escape", pos[0] + i);
            value = value * 16 + digit;
        }
        pos[0] += 4;
        return (char) value;
    }

    private static Map<String, Object> parseObject(String json, int[] pos) {
        pos[0]++;
        Map<String, Object> result = new LinkedHashMap<>();
        skipWhitespace(json, pos);
        if (consume(json, pos, '}')) return result;
        while (true) {
            skipWhitespace(json, pos);
            if (pos[0] >= json.length() || json.charAt(pos[0]) != '"') {
                throw error("Expected an object key", pos[0]);
            }
            String key = parseString(json, pos);
            skipWhitespace(json, pos);
            if (!consume(json, pos, ':')) throw error("Expected ':' after object key", pos[0]);
            result.put(key, parseValue(json, pos));
            skipWhitespace(json, pos);
            if (consume(json, pos, '}')) return result;
            if (!consume(json, pos, ',')) throw error("Expected ',' or '}'", pos[0]);
        }
    }

    private static List<Object> parseArray(String json, int[] pos) {
        pos[0]++;
        List<Object> result = new ArrayList<>();
        skipWhitespace(json, pos);
        if (consume(json, pos, ']')) return result;
        while (true) {
            result.add(parseValue(json, pos));
            skipWhitespace(json, pos);
            if (consume(json, pos, ']')) return result;
            if (!consume(json, pos, ',')) throw error("Expected ',' or ']'", pos[0]);
        }
    }

    private static Object parseNumber(String json, int[] pos) {
        int start = pos[0];
        consume(json, pos, '-');
        if (pos[0] >= json.length()) throw error("Invalid number", pos[0]);
        if (json.charAt(pos[0]) == '0') {
            pos[0]++;
            if (pos[0] < json.length() && isDigit(json.charAt(pos[0]))) {
                throw error("Leading zeros are not allowed", pos[0]);
            }
        } else if (json.charAt(pos[0]) >= '1' && json.charAt(pos[0]) <= '9') {
            while (pos[0] < json.length() && isDigit(json.charAt(pos[0]))) pos[0]++;
        } else {
            throw error("Invalid number", pos[0]);
        }
        boolean integral = true;
        if (consume(json, pos, '.')) {
            integral = false;
            int fractionStart = pos[0];
            while (pos[0] < json.length() && isDigit(json.charAt(pos[0]))) pos[0]++;
            if (pos[0] == fractionStart) throw error("Expected digit after decimal point", pos[0]);
        }
        if (pos[0] < json.length() && (json.charAt(pos[0]) == 'e' || json.charAt(pos[0]) == 'E')) {
            integral = false;
            pos[0]++;
            if (pos[0] < json.length() && (json.charAt(pos[0]) == '+' || json.charAt(pos[0]) == '-')) pos[0]++;
            int exponentStart = pos[0];
            while (pos[0] < json.length() && isDigit(json.charAt(pos[0]))) pos[0]++;
            if (pos[0] == exponentStart) throw error("Expected exponent digits", pos[0]);
        }
        String number = json.substring(start, pos[0]);
        return integral ? Num.parse(number) : Double.parseDouble(number);
    }

    private static Object parseLiteral(String json, int[] pos, String literal, Object value) {
        if (!json.startsWith(literal, pos[0])) throw error("Invalid literal", pos[0]);
        pos[0] += literal.length();
        return value;
    }

    private static boolean consume(String json, int[] pos, char expected) {
        if (pos[0] < json.length() && json.charAt(pos[0]) == expected) {
            pos[0]++;
            return true;
        }
        return false;
    }

    private static boolean isDigit(char value) {
        return value >= '0' && value <= '9';
    }

    private static void skipWhitespace(String json, int[] pos) {
        while (pos[0] < json.length()) {
            char current = json.charAt(pos[0]);
            if (current != ' ' && current != '\t' && current != '\n' && current != '\r') return;
            pos[0]++;
        }
    }

    private static RuntimeException error(String message, int position) {
        return new IllegalArgumentException(message + " at position " + position);
    }

    private static String toJson(Object value) {
        if (value == null) return "null";
        if (value instanceof Boolean) return value.toString();
        if (Num.isNumber(value)) return Num.format(value);
        if (value instanceof String string) return escapeString(string);
        if (value instanceof List<?> list) {
            StringBuilder result = new StringBuilder("[");
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) result.append(',');
                result.append(toJson(list.get(i)));
            }
            return result.append(']').toString();
        }
        if (value instanceof Map<?, ?> map) {
            StringBuilder result = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!first) result.append(',');
                result.append(escapeString(String.valueOf(entry.getKey())))
                        .append(':')
                        .append(toJson(entry.getValue()));
                first = false;
            }
            return result.append('}').toString();
        }
        return escapeString(String.valueOf(value));
    }

    private static String escapeString(String value) {
        StringBuilder result = new StringBuilder("\"");
        for (int i = 0; i < value.length(); i++) {
            char current = value.charAt(i);
            switch (current) {
                case '"' -> result.append("\\\"");
                case '\\' -> result.append("\\\\");
                case '\b' -> result.append("\\b");
                case '\f' -> result.append("\\f");
                case '\n' -> result.append("\\n");
                case '\r' -> result.append("\\r");
                case '\t' -> result.append("\\t");
                default -> {
                    if (current < 0x20) {
                        result.append("\\u00");
                        result.append(Character.forDigit((current >>> 4) & 0xf, 16));
                        result.append(Character.forDigit(current & 0xf, 16));
                    } else {
                        result.append(current);
                    }
                }
            }
        }
        return result.append('"').toString();
    }
}
