package io.github._3xhaust.interpreter.module;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JsonModule {
    public static void register(Map<String, NativeFunction> nativeFunctions) {
        nativeFunctions.put("parse", args -> {
            String json = String.valueOf(args.get(0)).trim();
            try {
                return parseValue(json, new int[]{0});
            } catch (Exception e) {
                throw new io.github._3xhaust.ezylang.exception.ParseException("json", "Invalid JSON: " + e.getMessage(), 0, 0, "");
            }
        });

        nativeFunctions.put("stringify", args -> {
            return toJson(args.get(0));
        });

        nativeFunctions.put("get", args -> {
            Object obj = args.get(0);
            String key = String.valueOf(args.get(1));
            if (obj instanceof Map) {
                return ((Map<?, ?>) obj).get(key);
            }
            return null;
        });

        nativeFunctions.put("set", args -> {
            Object obj = args.get(0);
            String key = String.valueOf(args.get(1));
            Object value = args.get(2);
            if (obj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>) obj;
                map.put(key, value);
            }
            return obj;
        });

        nativeFunctions.put("keys", args -> {
            Object obj = args.get(0);
            if (obj instanceof Map) {
                return new ArrayList<>(((Map<?, ?>) obj).keySet());
            }
            return new ArrayList<>();
        });

        nativeFunctions.put("values", args -> {
            Object obj = args.get(0);
            if (obj instanceof Map) {
                return new ArrayList<>(((Map<?, ?>) obj).values());
            }
            return new ArrayList<>();
        });

        nativeFunctions.put("hasKey", args -> {
            Object obj = args.get(0);
            String key = String.valueOf(args.get(1));
            if (obj instanceof Map) {
                return ((Map<?, ?>) obj).containsKey(key);
            }
            return false;
        });

        nativeFunctions.put("remove", args -> {
            Object obj = args.get(0);
            String key = String.valueOf(args.get(1));
            if (obj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>) obj;
                map.remove(key);
            }
            return obj;
        });

        nativeFunctions.put("newObject", args -> new HashMap<String, Object>());
    }

    private static Object parseValue(String json, int[] pos) {
        skipWhitespace(json, pos);
        if (pos[0] >= json.length()) return null;

        char c = json.charAt(pos[0]);
        if (c == '"') return parseString(json, pos);
        if (c == '{') return parseObject(json, pos);
        if (c == '[') return parseArray(json, pos);
        if (c == 't' || c == 'f') return parseBoolean(json, pos);
        if (c == 'n') return parseNull(json, pos);
        if (c == '-' || Character.isDigit(c)) return parseNumber(json, pos);
        throw new RuntimeException("Unexpected character: " + c);
    }

    private static String parseString(String json, int[] pos) {
        pos[0]++;
        StringBuilder sb = new StringBuilder();
        while (pos[0] < json.length()) {
            char c = json.charAt(pos[0]);
            if (c == '"') { pos[0]++; return sb.toString(); }
            if (c == '\\') {
                pos[0]++;
                char esc = json.charAt(pos[0]);
                switch (esc) {
                    case '"', '\\', '/' -> sb.append(esc);
                    case 'n' -> sb.append('\n');
                    case 't' -> sb.append('\t');
                    case 'r' -> sb.append('\r');
                    default -> { sb.append('\\'); sb.append(esc); }
                }
            } else {
                sb.append(c);
            }
            pos[0]++;
        }
        throw new RuntimeException("Unterminated string");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> parseObject(String json, int[] pos) {
        pos[0]++;
        Map<String, Object> map = new HashMap<>();
        skipWhitespace(json, pos);
        if (json.charAt(pos[0]) == '}') { pos[0]++; return map; }
        while (pos[0] < json.length()) {
            skipWhitespace(json, pos);
            String key = parseString(json, pos);
            skipWhitespace(json, pos);
            pos[0]++;
            Object value = parseValue(json, pos);
            map.put(key, value);
            skipWhitespace(json, pos);
            if (json.charAt(pos[0]) == '}') { pos[0]++; return map; }
            pos[0]++;
        }
        throw new RuntimeException("Unterminated object");
    }

    private static List<Object> parseArray(String json, int[] pos) {
        pos[0]++;
        List<Object> list = new ArrayList<>();
        skipWhitespace(json, pos);
        if (json.charAt(pos[0]) == ']') { pos[0]++; return list; }
        while (pos[0] < json.length()) {
            list.add(parseValue(json, pos));
            skipWhitespace(json, pos);
            if (json.charAt(pos[0]) == ']') { pos[0]++; return list; }
            pos[0]++;
        }
        throw new RuntimeException("Unterminated array");
    }

    private static Double parseNumber(String json, int[] pos) {
        int start = pos[0];
        if (json.charAt(pos[0]) == '-') pos[0]++;
        while (pos[0] < json.length() && (Character.isDigit(json.charAt(pos[0])) || json.charAt(pos[0]) == '.' || json.charAt(pos[0]) == 'e' || json.charAt(pos[0]) == 'E' || json.charAt(pos[0]) == '+' || json.charAt(pos[0]) == '-')) {
            if ((json.charAt(pos[0]) == '+' || json.charAt(pos[0]) == '-') && pos[0] > start + 1 && json.charAt(pos[0] - 1) != 'e' && json.charAt(pos[0] - 1) != 'E') break;
            pos[0]++;
        }
        return Double.parseDouble(json.substring(start, pos[0]));
    }

    private static Boolean parseBoolean(String json, int[] pos) {
        if (json.startsWith("true", pos[0])) { pos[0] += 4; return true; }
        if (json.startsWith("false", pos[0])) { pos[0] += 5; return false; }
        throw new RuntimeException("Invalid boolean");
    }

    private static Object parseNull(String json, int[] pos) {
        if (json.startsWith("null", pos[0])) { pos[0] += 4; return null; }
        throw new RuntimeException("Invalid null");
    }

    private static void skipWhitespace(String json, int[] pos) {
        while (pos[0] < json.length() && Character.isWhitespace(json.charAt(pos[0]))) pos[0]++;
    }

    @SuppressWarnings("unchecked")
    private static String toJson(Object value) {
        if (value == null) return "null";
        if (value instanceof Boolean) return value.toString();
        if (value instanceof Double d) {
            if (d == Math.floor(d) && !Double.isInfinite(d) && Math.abs(d) < 1e15) {
                return String.valueOf(d.longValue());
            }
            return d.toString();
        }
        if (value instanceof String s) {
            return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\t", "\\t") + "\"";
        }
        if (value instanceof List<?> list) {
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append(toJson(list.get(i)));
            }
            sb.append("]");
            return sb.toString();
        }
        if (value instanceof Map<?, ?> map) {
            StringBuilder sb = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!first) sb.append(",");
                sb.append("\"").append(entry.getKey()).append("\":").append(toJson(entry.getValue()));
                first = false;
            }
            sb.append("}");
            return sb.toString();
        }
        return "\"" + value + "\"";
    }
}
