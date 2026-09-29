package io.github._3xhaust.interpreter.module;

import io.github._3xhaust.interpreter.runtime.Num;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public class TimeModule {
    public static void register(Map<String, NativeFunction> nativeFunctions) {
        nativeFunctions.put("now", args -> {
            Args.count(args, 0, 0);
            return System.currentTimeMillis();
        });

        nativeFunctions.put("nanoTime", args -> {
            Args.count(args, 0, 0);
            return System.nanoTime();
        });

        nativeFunctions.put("sleep", args -> {
            Args.count(args, 1, 1);
            long millis = Args.integer(args, 0);
            try {
                Thread.sleep(millis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalArgumentException("sleep interrupted");
            }
            return null;
        });

        nativeFunctions.put("format", args -> {
            Args.count(args, 1, 2);
            long millis = ((Number) Args.number(args, 0)).longValue();
            String pattern = args.size() == 2 ? Args.string(args, 1) : "yyyy-MM-dd HH:mm:ss";
            LocalDateTime dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault());
            return dateTime.format(DateTimeFormatter.ofPattern(pattern));
        });

        nativeFunctions.put("year", args -> datePart(args, DatePart.YEAR));
        nativeFunctions.put("month", args -> datePart(args, DatePart.MONTH));
        nativeFunctions.put("day", args -> datePart(args, DatePart.DAY));
        nativeFunctions.put("hour", args -> datePart(args, DatePart.HOUR));
        nativeFunctions.put("minute", args -> datePart(args, DatePart.MINUTE));
        nativeFunctions.put("second", args -> datePart(args, DatePart.SECOND));

        nativeFunctions.put("toSeconds", args -> {
            Args.count(args, 1, 1);
            return Num.div(Args.number(args, 0), 1000L);
        });

        nativeFunctions.put("toMillis", args -> {
            Args.count(args, 1, 1);
            return Num.mul(Args.number(args, 0), 1000L);
        });
    }

    private static long datePart(java.util.List<Object> args, DatePart part) {
        Args.count(args, 0, 1);
        long millis = args.isEmpty() ? System.currentTimeMillis() : ((Number) Args.number(args, 0)).longValue();
        LocalDateTime dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault());
        return switch (part) {
            case YEAR -> dateTime.getYear();
            case MONTH -> dateTime.getMonthValue();
            case DAY -> dateTime.getDayOfMonth();
            case HOUR -> dateTime.getHour();
            case MINUTE -> dateTime.getMinute();
            case SECOND -> dateTime.getSecond();
        };
    }

    private enum DatePart {
        YEAR,
        MONTH,
        DAY,
        HOUR,
        MINUTE,
        SECOND
    }
}
