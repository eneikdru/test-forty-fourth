package com.eneik.production;

import com.eneik.production.api.SearchHttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MaterialsIntegrationTest {

    private static final String JDBC_URL = "jdbc:h2:mem:materials_test_db;DB_CLOSE_DELAY=-1";
    private static Connection dbConn;
    private static SearchHttpServer httpServer;
    private static int port;
    private static HttpClient httpClient;

    @BeforeAll
    public static void setUp() throws Exception {
        dbConn = DriverManager.getConnection(JDBC_URL, "sa", "");

        try (Statement stmt = dbConn.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS materials CASCADE");
            stmt.execute("DROP TABLE IF EXISTS categories CASCADE");

            stmt.execute("CREATE TABLE categories (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "name VARCHAR(255) UNIQUE NOT NULL" +
                    ")");

            stmt.execute("CREATE TABLE materials (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "title VARCHAR(255) NOT NULL, " +
                    "content CLOB, " +
                    "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                    ")");

            stmt.execute("ALTER TABLE materials ADD COLUMN category_id INT REFERENCES categories(id)");
            stmt.execute("ALTER TABLE materials ADD COLUMN status VARCHAR(50)");
        }

        httpServer = new SearchHttpServer(0, () -> DriverManager.getConnection(JDBC_URL, "sa", ""));
        httpServer.start();
        port = httpServer.getPort();

        httpClient = HttpClient.newHttpClient();
    }

    @AfterAll
    public static void tearDown() throws Exception {
        if (httpServer != null) {
            httpServer.stop(0);
        }
        if (dbConn != null) {
            try (Statement stmt = dbConn.createStatement()) {
                stmt.execute("SHUTDOWN");
            }
            dbConn.close();
        }
    }

    @Test
    public void testCreateMaterialHappyPath() throws Exception {
        String payload = "{\n" +
                "  \"title\": \"Ebola Control Protocol\",\n" +
                "  \"content\": \"SOP for standard clinical containment of active Ebola virus disease outbreaks.\",\n" +
                "  \"category\": \"Viral Strains\",\n" +
                "  \"status\": \"Urgent\"\n" +
                "}";

        String url = "http://localhost:" + port + "/api/materials";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(201, response.statusCode());
        String body = response.body();

        assertTrue(body.contains("\"id\":"));
        assertTrue(body.contains("\"title\":\"Ebola Control Protocol\""));
        assertTrue(body.contains("\"content\":\"SOP for standard clinical containment of active Ebola virus disease outbreaks.\""));
        assertTrue(body.contains("\"category\":\"Viral Strains\""));
        assertTrue(body.contains("\"status\":\"Urgent\""));
    }

    @Test
    public void testCreateMaterialValidationFailures() throws Exception {
        // Missing title
        String payloadMissingTitle = "{\n" +
                "  \"content\": \"No title here.\",\n" +
                "  \"category\": \"Viral Strains\",\n" +
                "  \"status\": \"Urgent\"\n" +
                "}";

        String url = "http://localhost:" + port + "/api/materials";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payloadMissingTitle))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(400, response.statusCode());
        assertTrue(response.body().contains("\"error\":\"BAD_REQUEST\""));
        assertTrue(response.body().contains("Title is required"));

        // Invalid status
        String payloadInvalidStatus = "{\n" +
                "  \"title\": \"Valid Title\",\n" +
                "  \"content\": \"Valid Content\",\n" +
                "  \"category\": \"Viral Strains\",\n" +
                "  \"status\": \"UnknownStatus\"\n" +
                "}";

        request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payloadInvalidStatus))
                .build();

        response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(400, response.statusCode());
        assertTrue(response.body().contains("\"error\":\"BAD_REQUEST\""));
        assertTrue(response.body().contains("Invalid status: must be one of"));
    }

    @Test
    public void testGetUpdateAndDeleteMaterial() throws Exception {
        // 1. Create a material first
        String payload = "{\n" +
                "  \"title\": \"Test Material CRUD\",\n" +
                "  \"content\": \"Initial testing content.\",\n" +
                "  \"category\": \"Viral Strains\",\n" +
                "  \"status\": \"Pending\"\n" +
                "}";

        String url = "http://localhost:" + port + "/api/materials";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(201, response.statusCode());

        // Extract ID from JSON body: "id":<number>
        String body = response.body();
        int idIndex = body.indexOf("\"id\":") + 5;
        int idEndIndex = body.indexOf(",", idIndex);
        String idStr = body.substring(idIndex, idEndIndex).trim();
        long materialId = Long.parseLong(idStr);

        // 2. GET the material
        String getUrl = "http://localhost:" + port + "/api/materials/" + materialId;
        HttpRequest getRequest = HttpRequest.newBuilder()
                .uri(URI.create(getUrl))
                .GET()
                .build();

        HttpResponse<String> getResponse = httpClient.send(getRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, getResponse.statusCode());
        assertTrue(getResponse.body().contains("\"title\":\"Test Material CRUD\""));

        // 3. PUT (Update) the material
        String updatePayload = "{\n" +
                "  \"title\": \"Updated Test Material CRUD\",\n" +
                "  \"content\": \"Modified content here.\",\n" +
                "  \"category\": \"Bacterial Cultures\",\n" +
                "  \"status\": \"Analyzed\"\n" +
                "}";

        HttpRequest updateRequest = HttpRequest.newBuilder()
                .uri(URI.create(getUrl))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(updatePayload))
                .build();

        HttpResponse<String> updateResponse = httpClient.send(updateRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, updateResponse.statusCode());
        assertTrue(updateResponse.body().contains("\"title\":\"Updated Test Material CRUD\""));
        assertTrue(updateResponse.body().contains("\"category\":\"Bacterial Cultures\""));
        assertTrue(updateResponse.body().contains("\"status\":\"Analyzed\""));

        // 4. DELETE the material
        HttpRequest deleteRequest = HttpRequest.newBuilder()
                .uri(URI.create(getUrl))
                .DELETE()
                .build();

        HttpResponse<String> deleteResponse = httpClient.send(deleteRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(204, deleteResponse.statusCode());

        // 5. GET again should yield 404
        HttpResponse<String> getResponseAfterDelete = httpClient.send(getRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(404, getResponseAfterDelete.statusCode());
        assertTrue(getResponseAfterDelete.body().contains("\"error\":\"NOT_FOUND\""));
    }
}
