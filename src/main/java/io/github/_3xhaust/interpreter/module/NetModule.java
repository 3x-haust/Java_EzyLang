package io.github._3xhaust.interpreter.module;

import io.github._3xhaust.ezylang.exception.ParseException;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

public class NetModule {
    private static final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    public static void register(Map<String, NativeFunction> nativeFunctions) {
        nativeFunctions.put("httpGet", args -> {
            Args.count(args, 1, 1);
            String url = Args.string(args, 0);
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .GET()
                        .build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                return response.body();
            } catch (Exception e) {
                throw new ParseException("net", "HTTP GET failed: " + e.getMessage(), 0, 0, "");
            }
        });

        nativeFunctions.put("httpPost", args -> {
            Args.count(args, 1, 3);
            String url = Args.string(args, 0);
            String body = args.size() > 1 ? Args.string(args, 1) : "";
            String contentType = args.size() > 2 ? Args.string(args, 2) : "application/json";
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Content-Type", contentType)
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                return response.body();
            } catch (Exception e) {
                throw new ParseException("net", "HTTP POST failed: " + e.getMessage(), 0, 0, "");
            }
        });

        nativeFunctions.put("httpPut", args -> {
            Args.count(args, 1, 2);
            String url = Args.string(args, 0);
            String body = args.size() > 1 ? Args.string(args, 1) : "";
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Content-Type", "application/json")
                        .PUT(HttpRequest.BodyPublishers.ofString(body))
                        .build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                return response.body();
            } catch (Exception e) {
                throw new ParseException("net", "HTTP PUT failed: " + e.getMessage(), 0, 0, "");
            }
        });

        nativeFunctions.put("httpDelete", args -> {
            Args.count(args, 1, 1);
            String url = Args.string(args, 0);
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .DELETE()
                        .build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                return response.body();
            } catch (Exception e) {
                throw new ParseException("net", "HTTP DELETE failed: " + e.getMessage(), 0, 0, "");
            }
        });

        nativeFunctions.put("httpStatus", args -> {
            Args.count(args, 1, 1);
            String url = Args.string(args, 0);
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .GET()
                        .build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                return (long) response.statusCode();
            } catch (Exception e) {
                throw new ParseException("net", "HTTP request failed: " + e.getMessage(), 0, 0, "");
            }
        });
    }
}
