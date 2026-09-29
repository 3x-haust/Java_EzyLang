package io.github._3xhaust.interpreter.module;

import io.github._3xhaust.interpreter.runtime.Num;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ArrModule {
    public static void register(Map<String, NativeFunction> nativeFunctions) {
        nativeFunctions.put("range", args -> {
            double start = ((Number) args.get(0)).doubleValue();
            double end = ((Number) args.get(1)).doubleValue();
            double step = args.size() > 2 ? ((Number) args.get(2)).doubleValue() : 1.0;
            List<Object> result = new ArrayList<>();
            for (double i = start; i <= end; i += step) result.add(i);
            return result;
        });
        nativeFunctions.put("fill", args -> {
            int size = ((Number) args.get(0)).intValue();
            Object value = args.get(1);
            List<Object> result = new ArrayList<>();
            for (int i = 0; i < size; i++) result.add(value);
            return result;
        });
        nativeFunctions.put("sum", args -> {
            List<?> list = (List<?>) args.get(0);
            Object sum = 0L;
            for (Object item : list) sum = Num.add(sum, Num.norm(item));
            return sum;
        });
        nativeFunctions.put("avg", args -> {
            List<?> list = (List<?>) args.get(0);
            if (list.isEmpty()) throw new IllegalArgumentException("cannot average an empty array");
            Object sum = 0L;
            for (Object item : list) sum = Num.add(sum, Num.norm(item));
            return Num.div(sum, (long) list.size());
        });
        nativeFunctions.put("arrMin", args -> {
            List<?> list = (List<?>) args.get(0);
            if (list.isEmpty()) throw new IllegalArgumentException("empty array has no minimum");
            double min = Double.MAX_VALUE;
            for (Object item : list) { double v = ((Number) item).doubleValue(); if (v < min) min = v; }
            return min;
        });
        nativeFunctions.put("arrMax", args -> {
            List<?> list = (List<?>) args.get(0);
            if (list.isEmpty()) throw new IllegalArgumentException("empty array has no maximum");
            double max = -Double.MAX_VALUE;
            for (Object item : list) { double v = ((Number) item).doubleValue(); if (v > max) max = v; }
            return max;
        });
        nativeFunctions.put("flatten", args -> {
            List<?> list = (List<?>) args.get(0);
            List<Object> result = new ArrayList<>();
            for (Object item : list) {
                if (item instanceof List<?> inner) result.addAll(inner);
                else result.add(item);
            }
            return result;
        });
        nativeFunctions.put("zip", args -> {
            List<?> a = (List<?>) args.get(0);
            List<?> b = (List<?>) args.get(1);
            List<Object> result = new ArrayList<>();
            int len = Math.min(a.size(), b.size());
            for (int i = 0; i < len; i++) {
                List<Object> pair = new ArrayList<>();
                pair.add(a.get(i));
                pair.add(b.get(i));
                result.add(pair);
            }
            return result;
        });
        nativeFunctions.put("slice", args -> {
            List<?> list = (List<?>) args.get(0);
            int start = ((Number) args.get(1)).intValue();
            int end = args.size() > 2 ? ((Number) args.get(2)).intValue() : list.size();
            return new ArrayList<>(list.subList(start, end));
        });
        nativeFunctions.put("count", args -> {
            List<?> list = (List<?>) args.get(0);
            Object target = args.get(1);
            double count = 0;
            for (Object item : list) if (item.equals(target)) count++;
            return count;
        });
    }
}
