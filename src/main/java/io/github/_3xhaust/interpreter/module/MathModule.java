package io.github._3xhaust.interpreter.module;

import java.util.HashMap;
import java.util.Map;

public class MathModule {
    public static void registerConstants(Map<String, Object> constants) {
        constants.put("PI", Math.PI);
        constants.put("E", Math.E);
    }

    public static void register(Map<String, NativeFunction> nativeFunctions) {
        nativeFunctions.put("sqrt", args -> Math.sqrt(((Number) args.get(0)).doubleValue()));
        nativeFunctions.put("abs", args -> Math.abs(((Number) args.get(0)).doubleValue()));
        nativeFunctions.put("pow", args -> Math.pow(((Number) args.get(0)).doubleValue(), ((Number) args.get(1)).doubleValue()));
        nativeFunctions.put("min", args -> Math.min(((Number) args.get(0)).doubleValue(), ((Number) args.get(1)).doubleValue()));
        nativeFunctions.put("max", args -> Math.max(((Number) args.get(0)).doubleValue(), ((Number) args.get(1)).doubleValue()));
        nativeFunctions.put("floor", args -> Math.floor(((Number) args.get(0)).doubleValue()));
        nativeFunctions.put("ceil", args -> Math.ceil(((Number) args.get(0)).doubleValue()));
        nativeFunctions.put("round", args -> (double) Math.round(((Number) args.get(0)).doubleValue()));
        nativeFunctions.put("sin", args -> Math.sin(((Number) args.get(0)).doubleValue()));
        nativeFunctions.put("cos", args -> Math.cos(((Number) args.get(0)).doubleValue()));
        nativeFunctions.put("tan", args -> Math.tan(((Number) args.get(0)).doubleValue()));
        nativeFunctions.put("asin", args -> Math.asin(((Number) args.get(0)).doubleValue()));
        nativeFunctions.put("acos", args -> Math.acos(((Number) args.get(0)).doubleValue()));
        nativeFunctions.put("atan", args -> Math.atan(((Number) args.get(0)).doubleValue()));
        nativeFunctions.put("log", args -> Math.log(((Number) args.get(0)).doubleValue()));
        nativeFunctions.put("log10", args -> Math.log10(((Number) args.get(0)).doubleValue()));
        nativeFunctions.put("random", args -> Math.random());
        nativeFunctions.put("toRadians", args -> Math.toRadians(((Number) args.get(0)).doubleValue()));
        nativeFunctions.put("toDegrees", args -> Math.toDegrees(((Number) args.get(0)).doubleValue()));
    }
}
