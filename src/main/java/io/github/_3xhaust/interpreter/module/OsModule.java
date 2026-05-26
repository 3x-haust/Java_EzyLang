package io.github._3xhaust.interpreter.module;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.util.Map;

public class OsModule {
    public static void register(Map<String, NativeFunction> nativeFunctions) {
        nativeFunctions.put("env", args -> {
            String value = System.getenv(String.valueOf(args.get(0)));
            return value != null ? value : (args.size() > 1 ? String.valueOf(args.get(1)) : null);
        });

        nativeFunctions.put("exec", args -> {
            try {
                String command = String.valueOf(args.get(0));
                ProcessBuilder pb = new ProcessBuilder("sh", "-c", command);
                pb.redirectErrorStream(true);
                Process process = pb.start();
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                StringBuilder output = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    if (output.length() > 0) output.append("\n");
                    output.append(line);
                }
                process.waitFor();
                return output.toString();
            } catch (Exception e) {
                throw new io.github._3xhaust.ezylang.exception.ParseException("os", "Failed to execute command: " + e.getMessage(), 0, 0, "");
            }
        });

        nativeFunctions.put("exit", args -> {
            int code = args.isEmpty() ? 0 : ((Double) args.get(0)).intValue();
            System.exit(code);
            return null;
        });

        nativeFunctions.put("cwd", args -> System.getProperty("user.dir"));

        nativeFunctions.put("homedir", args -> System.getProperty("user.home"));

        nativeFunctions.put("platform", args -> System.getProperty("os.name"));

        nativeFunctions.put("pathJoin", args -> {
            if (args.isEmpty()) return "";
            String first = String.valueOf(args.get(0));
            java.nio.file.Path path = Paths.get(first);
            for (int i = 1; i < args.size(); i++) {
                path = path.resolve(String.valueOf(args.get(i)));
            }
            return path.toString();
        });

        nativeFunctions.put("pathDir", args -> {
            java.nio.file.Path parent = Paths.get(String.valueOf(args.get(0))).getParent();
            return parent != null ? parent.toString() : ".";
        });

        nativeFunctions.put("pathFile", args ->
                Paths.get(String.valueOf(args.get(0))).getFileName().toString());

        nativeFunctions.put("pathExt", args -> {
            String name = Paths.get(String.valueOf(args.get(0))).getFileName().toString();
            int dot = name.lastIndexOf('.');
            return dot >= 0 ? name.substring(dot) : "";
        });
    }

    public static void registerConstants(Map<String, Object> constants) {
        constants.put("SEP", java.io.File.separator);
        constants.put("EOL", System.lineSeparator());
    }
}
