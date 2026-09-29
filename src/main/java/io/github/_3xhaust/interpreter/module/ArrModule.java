package io.github._3xhaust.interpreter.module;

import io.github._3xhaust.interpreter.runtime.Num;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ArrModule {
    public static void register(Map<String, NativeFunction> nativeFunctions) {
        nativeFunctions.put("range", args -> {
            Args.count(args, 2, 3);
            Object start = Args.number(args, 0);
            Object end = Args.number(args, 1);
            Object step = args.size() == 3 ? Args.number(args, 2) : 1L;
            if (Num.isZero(step)) throw new IllegalArgumentException("step cannot be zero");
            List<Object> result = new ArrayList<>();
            boolean ascending = Num.compare(step, 0L) > 0;
            for (Object value = start;
                 ascending ? Num.compare(value, end) <= 0 : Num.compare(value, end) >= 0;) {
                result.add(value);
                Object next = Num.add(value, step);
                if (Num.equal(next, value)) throw new IllegalArgumentException("step does not advance range");
                value = next;
            }
            return result;
        });
        nativeFunctions.put("fill", args -> {
            Args.count(args, 2, 2);
            int size = Args.intValue(args, 0);
            Object value = args.get(1);
            List<Object> result = new ArrayList<>();
            for (int i = 0; i < size; i++) result.add(value);
            return result;
        });
        nativeFunctions.put("sum", args -> {
            Args.count(args, 1, 1);
            List<?> list = Args.list(args, 0);
            validateNumbers(list);
            Object sum = 0L;
            for (Object item : list) sum = Num.add(sum, item);
            return sum;
        });
        nativeFunctions.put("avg", args -> {
            Args.count(args, 1, 1);
            List<?> list = Args.list(args, 0);
            validateNumbers(list);
            if (list.isEmpty()) throw new IllegalArgumentException("cannot average an empty array");
            Object sum = 0L;
            for (Object item : list) sum = Num.add(sum, item);
            return Num.div(sum, (long) list.size());
        });
        nativeFunctions.put("arrMin", args -> {
            Args.count(args, 1, 1);
            List<?> list = Args.list(args, 0);
            validateNumbers(list);
            if (list.isEmpty()) throw new IllegalArgumentException("empty array has no minimum");
            Object min = list.get(0);
            for (int i = 1; i < list.size(); i++) {
                Object value = list.get(i);
                if (Num.compare(value, min) < 0) min = value;
            }
            return min;
        });
        nativeFunctions.put("arrMax", args -> {
            Args.count(args, 1, 1);
            List<?> list = Args.list(args, 0);
            validateNumbers(list);
            if (list.isEmpty()) throw new IllegalArgumentException("empty array has no maximum");
            Object max = list.get(0);
            for (int i = 1; i < list.size(); i++) {
                Object value = list.get(i);
                if (Num.compare(value, max) > 0) max = value;
            }
            return max;
        });
        nativeFunctions.put("flatten", args -> {
            Args.count(args, 1, 1);
            List<?> list = Args.list(args, 0);
            List<Object> result = new ArrayList<>();
            for (Object item : list) {
                if (item instanceof List<?> inner) result.addAll(inner);
                else result.add(item);
            }
            return result;
        });
        nativeFunctions.put("zip", args -> {
            Args.count(args, 2, 2);
            List<?> a = Args.list(args, 0);
            List<?> b = Args.list(args, 1);
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
            Args.count(args, 2, 3);
            List<?> list = Args.list(args, 0);
            int start = Args.intValue(args, 1);
            int end = args.size() == 3 ? Args.intValue(args, 2) : list.size();
            if (start < 0 || end < start || end > list.size()) {
                throw new IllegalArgumentException("slice indexes out of range");
            }
            return new ArrayList<>(list.subList(start, end));
        });
        nativeFunctions.put("count", args -> {
            Args.count(args, 2, 2);
            List<?> list = Args.list(args, 0);
            Object target = args.get(1);
            long count = 0;
            for (Object item : list) {
                boolean equal = Num.isNumber(item) && Num.isNumber(target)
                        ? Num.equal(item, target)
                        : Objects.equals(item, target);
                if (equal) count++;
            }
            return count;
        });
    }

    private static void validateNumbers(List<?> list) {
        for (int i = 0; i < list.size(); i++) {
            Object value = list.get(i);
            if (!Num.isNumber(value)) {
                throw new IllegalArgumentException("array element " + (i + 1) + " must be a number, got " + Args.typeName(value));
            }
        }
    }
}
