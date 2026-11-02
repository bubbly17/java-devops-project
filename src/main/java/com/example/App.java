package com.example;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;

public class App {

    public static void main(String[] args) throws IOException {

        int port = 8080;

        HttpServer server = HttpServer.create(
                new InetSocketAddress("0.0.0.0", port), 0);

        server.createContext("/", exchange -> {
            serveFile(exchange, "index.html", "text/html");
        });

        server.createContext("/style.css", exchange -> {
            serveFile(exchange, "style.css", "text/css");
        });

        server.start();

        System.out.println("Website running on port " + port);
    }

    private static void serveFile(
            HttpExchange exchange,
            String fileName,
            String contentType) throws IOException {

        Path filePath = Path.of("src", "main", "resources", fileName);

        byte[] response = Files.readAllBytes(filePath);

        exchange.getResponseHeaders().set(
                "Content-Type", contentType + "; charset=UTF-8");

        exchange.sendResponseHeaders(200, response.length);

        try (OutputStream output = exchange.getResponseBody()) {
            output.write(response);
        }
    }
}
