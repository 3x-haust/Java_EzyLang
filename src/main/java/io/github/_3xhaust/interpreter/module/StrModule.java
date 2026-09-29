package io.github._3xhaust.interpreter.module;

import java.util.Map;

public class StrModule {
    public static void register(Map<String, NativeFunction> nativeFunctions) {
        nativeFunctions.put("toUpperCase", args -> args.get(0).toString().toUpperCase());
        nativeFunctions.put("toLowerCase", args -> args.get(0).toString().toLowerCase());
        nativeFunctions.put("trim", args -> args.get(0).toString().trim());
        nativeFunctions.put("startsWith", args -> args.get(0).toString().startsWith(args.get(1).toString()));
        nativeFunctions.put("endsWith", args -> args.get(0).toString().endsWith(args.get(1).toString()));
        nativeFunctions.put("substring", args -> {
            String s = args.get(0).toString();
            int start = ((Number) args.get(1)).intValue();
            int end = ((Number) args.get(2)).intValue();
            return s.substring(start, end);
        });
        nativeFunctions.put("replace", args -> args.get(0).toString().replace(args.get(1).toString(), args.get(2).toString()));
        nativeFunctions.put("strContains", args -> args.get(0).toString().contains(args.get(1).toString()));
        nativeFunctions.put("strIndexOf", args -> (double) args.get(0).toString().indexOf(args.get(1).toString()));
        nativeFunctions.put("strLength", args -> (double) args.get(0).toString().length());
        nativeFunctions.put("padLeft", args -> {
            String s = args.get(0).toString();
            int len = ((Number) args.get(1)).intValue();
            String pad = args.get(2).toString();
            while (s.length() < len) s = pad + s;
            return s;
        });
        nativeFunctions.put("padRight", args -> {
            String s = args.get(0).toString();
            int len = ((Number) args.get(1)).intValue();
            String pad = args.get(2).toString();
            while (s.length() < len) s = s + pad;
            return s;
        });
        nativeFunctions.put("reverse", args -> new StringBuilder(args.get(0).toString()).reverse().toString());
        nativeFunctions.put("format", args -> {
            Double num = ((Number) args.get(0)).doubleValue();
            int decimals = ((Number) args.get(1)).intValue();
            return String.format("%." + decimals + "f", num);
        });
    }
}
