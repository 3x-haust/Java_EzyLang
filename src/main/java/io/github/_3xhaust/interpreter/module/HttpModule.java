package io.github._3xhaust.interpreter.module;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.github._3xhaust.ezylang.exception.ParseException;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HttpModule {
    private static final Map<String, Map<String, String>> routes = new HashMap<>();
    private static NativeFunction dispatcher;

    public static void register(Map<String, NativeFunction> nativeFunctions) {
        routes.clear();

        nativeFunctions.put("routeGet", args -> {
            registerRoute(args, "GET");
            return null;
        });

        nativeFunctions.put("routePost", args -> {
            registerRoute(args, "POST");
            return null;
        });

        nativeFunctions.put("routePut", args -> {
            registerRoute(args, "PUT");
            return null;
        });

        nativeFunctions.put("routeDelete", args -> {
            registerRoute(args, "DELETE");
            return null;
        });

        nativeFunctions.put("serve", args -> {
            Args.count(args, 1, 1);
            int port = Args.intValue(args, 0);
            if (port < 0 || port > 65535) throw new IllegalArgumentException("port must be between 0 and 65535");
            try {
                HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
                for (Map.Entry<String, Map<String, String>> routeEntry : routes.entrySet()) {
                    String path = routeEntry.getKey();
                    Map<String, String> methodHandlers = routeEntry.getValue();
                    server.createContext(path, exchange -> {
                        try {
                            String method = exchange.getRequestMethod();
                            String handlerName = methodHandlers.get(method);
                            if (handlerName == null) {
                                sendResponse(exchange, 405, "Method Not Allowed");
                                return;
                            }
                            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                            String query = exchange.getRequestURI().getQuery();
                            List<Object> dispatchArgs = new ArrayList<>();
                            dispatchArgs.add(handlerName);
                            dispatchArgs.add(body);
                            dispatchArgs.add(query != null ? query : "");
                            Object result = dispatcher.execute(dispatchArgs);
                            String response = result != null ? String.valueOf(result) : "";
                            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
                            sendResponse(exchange, 200, response);
                        } catch (Exception e) {
                            try {
                                String message = String.valueOf(e.getMessage()).replace("\"", "'");
                                sendResponse(exchange, 500, "{\"error\": \"" + message + "\"}");
                            } catch (IOException responseError) {
                                e.addSuppressed(responseError);
                            }
                        }
                    });
                }
                server.setExecutor(java.util.concurrent.Executors.newFixedThreadPool(4));
                server.start();
                System.out.println("Server running on http://localhost:" + port);
                Thread.currentThread().join();
            } catch (Exception e) {
                throw new ParseException("http", "Failed to start server: " + e.getMessage(), 0, 0, "");
            }
            return null;
        });
    }

    public static void setDispatcher(NativeFunction fn) {
        dispatcher = fn;
    }

    private static void registerRoute(List<Object> args, String method) {
        Args.count(args, 2, 2);
        String path = Args.string(args, 0);
        String handler = Args.string(args, 1);
        routes.computeIfAbsent(path, key -> new HashMap<>()).put(method, handler);
    }

    private static void sendResponse(HttpExchange exchange, int statusCode, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }
}
