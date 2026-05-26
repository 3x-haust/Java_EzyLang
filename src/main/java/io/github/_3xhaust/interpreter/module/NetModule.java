package io.github._3xhaust.interpreter.module;

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
            try {
                String url = String.valueOf(args.get(0));
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .GET()
                        .build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                return response.body();
            } catch (Exception e) {
                throw new io.github._3xhaust.ezylang.exception.ParseException("net", "HTTP GET failed: " + e.getMessage(), 0, 0, "");
            }
        });

        nativeFunctions.put("httpPost", args -> {
            try {
                String url = String.valueOf(args.get(0));
                String body = args.size() > 1 ? String.valueOf(args.get(1)) : "";
                String contentType = args.size() > 2 ? String.valueOf(args.get(2)) : "application/json";
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Content-Type", contentType)
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                return response.body();
            } catch (Exception e) {
                throw new io.github._3xhaust.ezylang.exception.ParseException("net", "HTTP POST failed: " + e.getMessage(), 0, 0, "");
            }
        });

        nativeFunctions.put("httpPut", args -> {
            try {
                String url = String.valueOf(args.get(0));
                String body = args.size() > 1 ? String.valueOf(args.get(1)) : "";
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Content-Type", "application/json")
                        .PUT(HttpRequest.BodyPublishers.ofString(body))
                        .build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                return response.body();
            } catch (Exception e) {
                throw new io.github._3xhaust.ezylang.exception.ParseException("net", "HTTP PUT failed: " + e.getMessage(), 0, 0, "");
            }
        });

        nativeFunctions.put("httpDelete", args -> {
            try {
                String url = String.valueOf(args.get(0));
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .DELETE()
                        .build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                return response.body();
            } catch (Exception e) {
                throw new io.github._3xhaust.ezylang.exception.ParseException("net", "HTTP DELETE failed: " + e.getMessage(), 0, 0, "");
            }
        });

        nativeFunctions.put("httpStatus", args -> {
            try {
                String url = String.valueOf(args.get(0));
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .GET()
                        .build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                return (double) response.statusCode();
            } catch (Exception e) {
                throw new io.github._3xhaust.ezylang.exception.ParseException("net", "HTTP request failed: " + e.getMessage(), 0, 0, "");
            }
        });
    }
}
