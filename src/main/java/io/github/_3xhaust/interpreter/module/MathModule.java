package io.github._3xhaust.interpreter.module;

import io.github._3xhaust.interpreter.runtime.Num;

import java.util.Map;

public class MathModule {
    public static void registerConstants(Map<String, Object> constants) {
        constants.put("PI", Math.PI);
        constants.put("E", Math.E);
    }

    public static void register(Map<String, NativeFunction> nativeFunctions) {
        nativeFunctions.put("sqrt", args -> {
            Args.count(args, 1, 1);
            return Math.sqrt(Num.toDouble(Args.number(args, 0)));
        });
        nativeFunctions.put("abs", args -> {
            Args.count(args, 1, 1);
            Object value = Args.number(args, 0);
            if (value instanceof Double number) return Math.abs(number);
            return Num.compare(value, 0L) < 0 ? Num.negate(value) : value;
        });
        nativeFunctions.put("pow", args -> {
            Args.count(args, 2, 2);
            return Math.pow(Num.toDouble(Args.number(args, 0)), Num.toDouble(Args.number(args, 1)));
        });
        nativeFunctions.put("min", args -> {
            Args.count(args, 2, 2);
            Object first = Args.number(args, 0);
            Object second = Args.number(args, 1);
            return Num.compare(first, second) <= 0 ? first : second;
        });
        nativeFunctions.put("max", args -> {
            Args.count(args, 2, 2);
            Object first = Args.number(args, 0);
            Object second = Args.number(args, 1);
            return Num.compare(first, second) >= 0 ? first : second;
        });
        nativeFunctions.put("floor", args -> {
            Args.count(args, 1, 1);
            return Num.norm(Math.floor(Num.toDouble(Args.number(args, 0))));
        });
        nativeFunctions.put("ceil", args -> {
            Args.count(args, 1, 1);
            return Num.norm(Math.ceil(Num.toDouble(Args.number(args, 0))));
        });
        nativeFunctions.put("round", args -> {
            Args.count(args, 1, 1);
            return Math.round(Num.toDouble(Args.number(args, 0)));
        });
        nativeFunctions.put("sin", args -> {
            Args.count(args, 1, 1);
            return Math.sin(Num.toDouble(Args.number(args, 0)));
        });
        nativeFunctions.put("cos", args -> {
            Args.count(args, 1, 1);
            return Math.cos(Num.toDouble(Args.number(args, 0)));
        });
        nativeFunctions.put("tan", args -> {
            Args.count(args, 1, 1);
            return Math.tan(Num.toDouble(Args.number(args, 0)));
        });
        nativeFunctions.put("asin", args -> {
            Args.count(args, 1, 1);
            return Math.asin(Num.toDouble(Args.number(args, 0)));
        });
        nativeFunctions.put("acos", args -> {
            Args.count(args, 1, 1);
            return Math.acos(Num.toDouble(Args.number(args, 0)));
        });
        nativeFunctions.put("atan", args -> {
            Args.count(args, 1, 1);
            return Math.atan(Num.toDouble(Args.number(args, 0)));
        });
        nativeFunctions.put("log", args -> {
            Args.count(args, 1, 1);
            return Math.log(Num.toDouble(Args.number(args, 0)));
        });
        nativeFunctions.put("log10", args -> {
            Args.count(args, 1, 1);
            return Math.log10(Num.toDouble(Args.number(args, 0)));
        });
        nativeFunctions.put("random", args -> {
            Args.count(args, 0, 0);
            return Math.random();
        });
        nativeFunctions.put("toRadians", args -> {
            Args.count(args, 1, 1);
            return Math.toRadians(Num.toDouble(Args.number(args, 0)));
        });
        nativeFunctions.put("toDegrees", args -> {
            Args.count(args, 1, 1);
            return Math.toDegrees(Num.toDouble(Args.number(args, 0)));
        });
    }
}
