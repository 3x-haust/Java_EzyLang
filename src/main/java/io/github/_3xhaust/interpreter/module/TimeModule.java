package io.github._3xhaust.interpreter.module;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public class TimeModule {
    public static void register(Map<String, NativeFunction> nativeFunctions) {
        nativeFunctions.put("now", args -> (double) System.currentTimeMillis());

        nativeFunctions.put("nanoTime", args -> (double) System.nanoTime());

        nativeFunctions.put("sleep", args -> {
            try {
                Thread.sleep(((Double) args.get(0)).longValue());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return null;
        });

        nativeFunctions.put("format", args -> {
            long millis = ((Double) args.get(0)).longValue();
            String pattern = args.size() > 1 ? String.valueOf(args.get(1)) : "yyyy-MM-dd HH:mm:ss";
            LocalDateTime dt = LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault());
            return dt.format(DateTimeFormatter.ofPattern(pattern));
        });

        nativeFunctions.put("year", args -> {
            long millis = args.isEmpty() ? System.currentTimeMillis() : ((Double) args.get(0)).longValue();
            return (double) LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault()).getYear();
        });

        nativeFunctions.put("month", args -> {
            long millis = args.isEmpty() ? System.currentTimeMillis() : ((Double) args.get(0)).longValue();
            return (double) LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault()).getMonthValue();
        });

        nativeFunctions.put("day", args -> {
            long millis = args.isEmpty() ? System.currentTimeMillis() : ((Double) args.get(0)).longValue();
            return (double) LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault()).getDayOfMonth();
        });

        nativeFunctions.put("hour", args -> {
            long millis = args.isEmpty() ? System.currentTimeMillis() : ((Double) args.get(0)).longValue();
            return (double) LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault()).getHour();
        });

        nativeFunctions.put("minute", args -> {
            long millis = args.isEmpty() ? System.currentTimeMillis() : ((Double) args.get(0)).longValue();
            return (double) LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault()).getMinute();
        });

        nativeFunctions.put("second", args -> {
            long millis = args.isEmpty() ? System.currentTimeMillis() : ((Double) args.get(0)).longValue();
            return (double) LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault()).getSecond();
        });

        nativeFunctions.put("toSeconds", args -> ((Double) args.get(0)) / 1000.0);

        nativeFunctions.put("toMillis", args -> ((Double) args.get(0)) * 1000.0);
    }
}
