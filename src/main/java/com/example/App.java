
package com.example;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;

public class App {

    public static void main(String[] args) throws IOException {

        int port = 8080;

        HttpServer server = HttpServer.create(
                new InetSocketAddress("0.0.0.0", port), 0);

        // Homepage
        server.createContext("/", exchange -> {
            String path = exchange.getRequestURI().getPath();

            if (path.equals("/api/hello")) {
                sendResponse(exchange, 200,
                        "application/json",
                        "{\"message\":\"Hello from Java application on AWS EC2!\"}");
            } else if (path.equals("/style.css")) {
                serveResource(exchange, "style.css", "text/css");
            } else if (path.equals("/")) {
                serveResource(exchange, "index.html", "text/html");
            } else {
                sendResponse(exchange, 404,
                        "text/plain", "Page not found");
            }
        });

        server.start();

        System.out.println("Website running on port " + port);
    }

    private static void serveResource(
            HttpExchange exchange,
            String fileName,
            String contentType) throws IOException {

        try (InputStream input =
                     App.class.getResourceAsStream("/" + fileName)) {

            if (input == null) {
                sendResponse(exchange, 404,
                        "text/plain", fileName + " not found in JAR");
                return;
            }

            byte[] response = input.readAllBytes();

            exchange.getResponseHeaders().set(
                    "Content-Type", contentType + "; charset=UTF-8");

            exchange.sendResponseHeaders(200, response.length);

            try (OutputStream output = exchange.getResponseBody()) {
                output.write(response);
            }
        }
    }

    private static void sendResponse(
            HttpExchange exchange,
            int status,
            String contentType,
            String body) throws IOException {

        byte[] response = body.getBytes(
                java.nio.charset.StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set(
                "Content-Type", contentType + "; charset=UTF-8");

        exchange.sendResponseHeaders(status, response.length);

        try (OutputStream output = exchange.getResponseBody()) {
            output.write(response);
        }
    }
}
