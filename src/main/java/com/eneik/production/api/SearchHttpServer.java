package com.eneik.production.api;

import com.eneik.production.dto.ErrorResponse;
import com.eneik.production.dto.MaterialDTO;
import com.eneik.production.dto.SearchResponse;
import com.eneik.production.dto.JsonParser;
import com.eneik.production.services.SearchService;
import com.eneik.production.services.MaterialsService;
import com.eneik.production.services.ValidationException;
import com.eneik.production.services.NotFoundException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.util.*;

public class SearchHttpServer {

    private final HttpServer server;
    private final SearchService searchService;
    private final MaterialsService materialsService;
    private final ConnectionSupplier connectionSupplier;

    public interface ConnectionSupplier {
        Connection getConnection() throws SQLException;
    }

    public SearchHttpServer(int port, ConnectionSupplier connectionSupplier) throws IOException {
        this.searchService = new SearchService();
        this.materialsService = new MaterialsService();
        this.connectionSupplier = connectionSupplier;
        this.server = HttpServer.create(new InetSocketAddress(port), 0);
        this.server.createContext("/api/materials/search", new SearchHandler());
        this.server.createContext("/api/materials", new MaterialsHandler());
    }

    public void start() {
        server.start();
    }

    public void stop(int delay) {
        server.stop(delay);
    }

    public int getPort() {
        return server.getAddress().getPort();
    }

    private class SearchHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendError(exchange, 405, "METHOD_NOT_ALLOWED", "Only GET method is supported.");
                return;
            }

            Map<String, List<String>> params = parseQueryParams(exchange.getRequestURI().getRawQuery());

            String q = getFirstParam(params, "q");
            String category = getFirstParam(params, "category");
            String status = getFirstParam(params, "status");
            String sort = getFirstParam(params, "sort");
            String pageStr = getFirstParam(params, "page");
            String limitStr = getFirstParam(params, "limit");

            int page = 1;
            if (pageStr != null) {
                try {
                    page = Integer.parseInt(pageStr);
                    if (page < 1) {
                        sendError(exchange, 400, "BAD_REQUEST", "Invalid page parameter: must be greater than or equal to 1");
                        return;
                    }
                } catch (NumberFormatException e) {
                    sendError(exchange, 400, "BAD_REQUEST", "Invalid page parameter format: must be an integer");
                    return;
                }
            }

            int limit = 10;
            if (limitStr != null) {
                try {
                    limit = Integer.parseInt(limitStr);
                    if (limit < 1 || limit > 100) {
                        sendError(exchange, 400, "BAD_REQUEST", "Invalid limit parameter: must be between 1 and 100");
                        return;
                    }
                } catch (NumberFormatException e) {
                    sendError(exchange, 400, "BAD_REQUEST", "Invalid limit parameter format: must be an integer");
                    return;
                }
            }

            if (status != null && !status.trim().isEmpty()) {
                String statusTrimmed = status.trim();
                if (!statusTrimmed.equals("Analyzed") && !statusTrimmed.equals("Pending") && !statusTrimmed.equals("Archived") && !statusTrimmed.equals("Urgent")) {
                    sendError(exchange, 400, "BAD_REQUEST", "Invalid status parameter: must be one of Analyzed, Pending, Archived, Urgent");
                    return;
                }
            }

            try (Connection conn = connectionSupplier.getConnection()) {
                SearchResponse response = searchService.search(conn, q, category, status, sort, page, limit);
                String json = toJson(response);
                sendResponse(exchange, 200, json);
            } catch (Exception e) {
                e.printStackTrace();
                sendError(exchange, 500, "INTERNAL_SERVER_ERROR", "An internal error occurred: " + e.getMessage());
            }
        }
    }

    private class MaterialsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod();

            // Match exact path /api/materials or subpath /api/materials/{id}
            String subPath = path.substring("/api/materials".length());
            if (subPath.startsWith("/")) {
                subPath = subPath.substring(1);
            }

            try (Connection conn = connectionSupplier.getConnection()) {
                if (subPath.isEmpty()) {
                    // /api/materials endpoint
                    if ("POST".equalsIgnoreCase(method)) {
                        handleCreate(exchange, conn);
                    } else {
                        sendError(exchange, 405, "METHOD_NOT_ALLOWED", "Method " + method + " not supported on /api/materials");
                    }
                } else {
                    // /api/materials/{id} endpoint
                    Long id;
                    try {
                        id = Long.parseLong(subPath);
                    } catch (NumberFormatException e) {
                        sendError(exchange, 400, "BAD_REQUEST", "Invalid material ID format");
                        return;
                    }

                    if ("GET".equalsIgnoreCase(method)) {
                        handleGet(exchange, conn, id);
                    } else if ("PUT".equalsIgnoreCase(method)) {
                        handleUpdate(exchange, conn, id);
                    } else if ("DELETE".equalsIgnoreCase(method)) {
                        handleDelete(exchange, conn, id);
                    } else {
                        sendError(exchange, 405, "METHOD_NOT_ALLOWED", "Method " + method + " not supported on /api/materials/{id}");
                    }
                }
            } catch (ValidationException e) {
                sendError(exchange, 400, "BAD_REQUEST", e.getMessage());
            } catch (NotFoundException e) {
                sendError(exchange, 404, "NOT_FOUND", e.getMessage());
            } catch (Exception e) {
                e.printStackTrace();
                sendError(exchange, 500, "INTERNAL_SERVER_ERROR", "An internal error occurred: " + e.getMessage());
            }
        }

        private void handleCreate(HttpExchange exchange, Connection conn) throws IOException, SQLException {
            String body = readRequestBody(exchange);
            Map<String, String> payload = JsonParser.parse(body);

            String title = payload.get("title");
            String content = payload.get("content");
            String category = payload.get("category");
            String status = payload.get("status");

            MaterialDTO m = materialsService.createMaterial(conn, title, content, category, status);
            sendResponse(exchange, 201, toJson(m));
        }

        private void handleGet(HttpExchange exchange, Connection conn, Long id) throws IOException, SQLException {
            MaterialDTO m = materialsService.getMaterial(conn, id);
            sendResponse(exchange, 200, toJson(m));
        }

        private void handleUpdate(HttpExchange exchange, Connection conn, Long id) throws IOException, SQLException {
            String body = readRequestBody(exchange);
            Map<String, String> payload = JsonParser.parse(body);

            String title = payload.get("title");
            String content = payload.get("content");
            String category = payload.get("category");
            String status = payload.get("status");

            MaterialDTO m = materialsService.updateMaterial(conn, id, title, content, category, status);
            sendResponse(exchange, 200, toJson(m));
        }

        private void handleDelete(HttpExchange exchange, Connection conn, Long id) throws IOException, SQLException {
            materialsService.deleteMaterial(conn, id);
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
        }

        private String readRequestBody(HttpExchange exchange) throws IOException {
            InputStream is = exchange.getRequestBody();
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int len;
            while ((len = is.read(buffer)) != -1) {
                bos.write(buffer, 0, len);
            }
            return bos.toString(StandardCharsets.UTF_8);
        }
    }

    private static Map<String, List<String>> parseQueryParams(String query) {
        Map<String, List<String>> params = new HashMap<>();
        if (query == null || query.trim().isEmpty()) {
            return params;
        }
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf("=");
            String key;
            String val;
            try {
                if (idx > 0) {
                    key = java.net.URLDecoder.decode(pair.substring(0, idx), "UTF-8");
                    val = java.net.URLDecoder.decode(pair.substring(idx + 1), "UTF-8");
                } else {
                    key = java.net.URLDecoder.decode(pair, "UTF-8");
                    val = "";
                }
                params.computeIfAbsent(key, k -> new ArrayList<>()).add(val);
            } catch (Exception e) {
                // ignore
            }
        }
        return params;
    }

    private static String getFirstParam(Map<String, List<String>> params, String key) {
        List<String> values = params.get(key);
        if (values != null && !values.isEmpty()) {
            return values.get(0);
        }
        return null;
    }

    private static void sendResponse(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static void sendError(HttpExchange exchange, int status, String errorCode, String message) throws IOException {
        ErrorResponse err = new ErrorResponse(errorCode, message, Instant.now());
        String json = toJson(err);
        sendResponse(exchange, status, json);
    }

    private static String escapeJson(String s) {
        if (s == null) return "null";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            switch (ch) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (ch < ' ') {
                        String hex = Integer.toHexString(ch);
                        sb.append("\\u").append("0".repeat(4 - hex.length())).append(hex);
                    } else {
                        sb.append(ch);
                    }
            }
        }
        return sb.toString();
    }

    private static String toJson(MaterialDTO m) {
        return "{" +
                "\"id\":" + m.getId() + "," +
                "\"title\":\"" + escapeJson(m.getTitle()) + "\"," +
                "\"content\":\"" + escapeJson(m.getContent()) + "\"," +
                "\"category\":" + (m.getCategory() == null ? "null" : "\"" + escapeJson(m.getCategory()) + "\"") + "," +
                "\"status\":" + (m.getStatus() == null ? "null" : "\"" + escapeJson(m.getStatus()) + "\"") + "," +
                "\"updated_at\":\"" + m.getUpdatedAt().toString() + "\"" +
                "}";
    }

    private static String toJson(SearchResponse r) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"items\":[");
        for (int i = 0; i < r.getItems().size(); i++) {
            sb.append(toJson(r.getItems().get(i)));
            if (i < r.getItems().size() - 1) {
                sb.append(",");
            }
        }
        sb.append("],");
        sb.append("\"total\":").append(r.getTotal()).append(",");
        sb.append("\"page\":").append(r.getPage()).append(",");
        sb.append("\"limit\":").append(r.getLimit()).append(",");
        sb.append("\"pages\":").append(r.getPages());
        sb.append("}");
        return sb.toString();
    }

    private static String toJson(ErrorResponse e) {
        return "{" +
                "\"error\":\"" + escapeJson(e.getError()) + "\"," +
                "\"message\":\"" + escapeJson(e.getMessage()) + "\"," +
                "\"timestamp\":\"" + e.getTimestamp().toString() + "\"" +
                "}";
    }
}
