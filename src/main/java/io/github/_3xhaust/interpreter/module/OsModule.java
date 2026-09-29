package io.github._3xhaust.interpreter.module;

import io.github._3xhaust.ezylang.exception.ParseException;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

public class OsModule {
    public static void register(Map<String, NativeFunction> nativeFunctions) {
        nativeFunctions.put("env", args -> {
            Args.count(args, 1, 2);
            String name = Args.string(args, 0);
            String fallback = args.size() == 2 ? Args.string(args, 1) : null;
            String value = System.getenv(name);
            return value != null ? value : fallback;
        });

        nativeFunctions.put("exec", args -> {
            Args.count(args, 1, 1);
            String command = Args.string(args, 0);
            try {
                ProcessBuilder builder = new ProcessBuilder("sh", "-c", command);
                builder.redirectErrorStream(true);
                Process process = builder.start();
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                StringBuilder output = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!output.isEmpty()) output.append('\n');
                    output.append(line);
                }
                process.waitFor();
                return output.toString();
            } catch (Exception e) {
                throw new ParseException("os", "Failed to execute command: " + e.getMessage(), 0, 0, "");
            }
        });

        nativeFunctions.put("exit", args -> {
            Args.count(args, 0, 1);
            int code = args.isEmpty() ? 0 : Args.intValue(args, 0);
            System.exit(code);
            return null;
        });

        nativeFunctions.put("cwd", args -> {
            Args.count(args, 0, 0);
            return System.getProperty("user.dir");
        });

        nativeFunctions.put("homedir", args -> {
            Args.count(args, 0, 0);
            return System.getProperty("user.home");
        });

        nativeFunctions.put("platform", args -> {
            Args.count(args, 0, 0);
            return System.getProperty("os.name");
        });

        nativeFunctions.put("pathJoin", args -> {
            Args.count(args, 1, Integer.MAX_VALUE);
            for (int i = 0; i < args.size(); i++) Args.string(args, i);
            Path path = Paths.get(Args.string(args, 0));
            for (int i = 1; i < args.size(); i++) path = path.resolve(Args.string(args, i));
            return path.toString();
        });

        nativeFunctions.put("pathDir", args -> {
            Args.count(args, 1, 1);
            Path parent = Paths.get(Args.string(args, 0)).getParent();
            return parent != null ? parent.toString() : ".";
        });

        nativeFunctions.put("pathFile", args -> {
            Args.count(args, 1, 1);
            return Paths.get(Args.string(args, 0)).getFileName().toString();
        });

        nativeFunctions.put("pathExt", args -> {
            Args.count(args, 1, 1);
            String name = Paths.get(Args.string(args, 0)).getFileName().toString();
            int dot = name.lastIndexOf('.');
            return dot >= 0 ? name.substring(dot) : "";
        });
    }

    public static void registerConstants(Map<String, Object> constants) {
        constants.put("SEP", java.io.File.separator);
        constants.put("EOL", System.lineSeparator());
    }
}
