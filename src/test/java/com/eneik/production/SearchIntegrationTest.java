package com.eneik.production;

import com.eneik.production.api.SearchHttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class SearchIntegrationTest {

    private static final String JDBC_URL = "jdbc:h2:mem:integration_test_db;DB_CLOSE_DELAY=-1";
    private static Connection dbConn;
    private static SearchHttpServer httpServer;
    private static int port;
    private static HttpClient httpClient;

    @BeforeAll
    public static void setUp() throws Exception {
        // Initialize H2 Database
        dbConn = DriverManager.getConnection(JDBC_URL, "sa", "");

        // Construct schema manually to avoid running PostgreSQL-specific GIN index DDL on H2
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

            // Align with V20260812160946370__add_category_and_status.sql
            stmt.execute("ALTER TABLE materials ADD COLUMN category_id INT REFERENCES categories(id)");
            stmt.execute("ALTER TABLE materials ADD COLUMN status VARCHAR(50)");
        }

        // Populate test data
        populateTestData();

        // Start Search HTTP Server
        httpServer = new SearchHttpServer(0, () -> DriverManager.getConnection(JDBC_URL, "sa", ""));
        httpServer.start();
        port = httpServer.getPort();

        httpClient = HttpClient.newHttpClient();
    }

    private static void populateTestData() throws SQLException {
        // First insert categories
        long viralStrainsId;
        long bacterialCulturesId;
        long outbreakDataId;

        String insertCategorySql = "INSERT INTO categories (name) VALUES (?)";
        try (PreparedStatement pstmt = dbConn.prepareStatement(insertCategorySql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, "Viral Strains");
            pstmt.executeUpdate();
            try (var rs = pstmt.getGeneratedKeys()) {
                rs.next();
                viralStrainsId = rs.getLong(1);
            }

            pstmt.setString(1, "Bacterial Cultures");
            pstmt.executeUpdate();
            try (var rs = pstmt.getGeneratedKeys()) {
                rs.next();
                bacterialCulturesId = rs.getLong(1);
            }

            pstmt.setString(1, "Outbreak Data");
            pstmt.executeUpdate();
            try (var rs = pstmt.getGeneratedKeys()) {
                rs.next();
                outbreakDataId = rs.getLong(1);
            }
        }

        String insertSql = "INSERT INTO materials (title, content, category_id, status) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = dbConn.prepareStatement(insertSql)) {
            // Material 1
            pstmt.setString(1, "Influenza A(H1N1) Standard Operating Procedure");
            pstmt.setString(2, "Standard operating procedure for the detection, reporting, and preliminary epidemiological investigation of suspected human cases of novel influenza A.");
            pstmt.setLong(3, viralStrainsId);
            pstmt.setString(4, "Analyzed");
            pstmt.executeUpdate();

            // Material 2
            pstmt.setString(1, "Avian Influenza (H5N1) Field Sampling Guidelines");
            pstmt.setString(2, "Comprehensive field manual for environmental swabbing and live-bird sampling in wet markets.");
            pstmt.setLong(3, viralStrainsId);
            pstmt.setString(4, "Pending");
            pstmt.executeUpdate();

            // Material 3
            pstmt.setString(1, "Bacterial Culture Preservation Guidelines");
            pstmt.setString(2, "Standard laboratory protocol for the safe preservation, freezing, and revitalization of pathogenic bacterial cultures.");
            pstmt.setLong(3, bacterialCulturesId);
            pstmt.setString(4, "Archived");
            pstmt.executeUpdate();

            // Material 4
            pstmt.setString(1, "Urgent Cholera Outbreak Alert");
            pstmt.setString(2, "Critical communication and response protocol for immediate containment of sudden cholera outbreak in urban environments.");
            pstmt.setLong(3, outbreakDataId);
            pstmt.setString(4, "Urgent");
            pstmt.executeUpdate();
        }
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
    public void testSchemaReferentialIntegrity() {
        // Assert that inserting a material with a non-existent category_id fails due to foreign key constraint
        String insertSql = "INSERT INTO materials (title, content, category_id, status) VALUES (?, ?, ?, ?)";
        assertThrows(SQLException.class, () -> {
            try (PreparedStatement pstmt = dbConn.prepareStatement(insertSql)) {
                pstmt.setString(1, "Test Referential Integrity");
                pstmt.setString(2, "Sample Description");
                pstmt.setLong(3, 999999L); // Invalid foreign key
                pstmt.setString(4, "Pending");
                pstmt.executeUpdate();
            }
        });
    }

    @Test
    public void testSearchNoFiltersReturnsAll() throws Exception {
        String url = "http://localhost:" + port + "/api/materials/search";
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        String body = response.body();

        assertTrue(body.contains("\"total\":4"));
        assertTrue(body.contains("\"page\":1"));
        assertTrue(body.contains("\"limit\":10"));
        assertTrue(body.contains("\"pages\":1"));
        assertTrue(body.contains("Influenza A(H1N1) Standard Operating Procedure"));
        assertTrue(body.contains("Bacterial Culture Preservation Guidelines"));
        assertTrue(body.contains("Urgent Cholera Outbreak Alert"));
    }

    @Test
    public void testSearchKeywordMatches() throws Exception {
        // Match Influenza
        String url = "http://localhost:" + port + "/api/materials/search?q=Influenza";
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        String body = response.body();
        assertTrue(body.contains("\"total\":2"), "Expected 2 matching materials for 'Influenza'.");
        assertTrue(body.contains("H1N1"));
        assertTrue(body.contains("H5N1"));

        // Match specific word "Preservation"
        url = "http://localhost:" + port + "/api/materials/search?q=Preservation";
        request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
        response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        body = response.body();
        assertTrue(body.contains("\"total\":1"));
        assertTrue(body.contains("Bacterial Culture Preservation Guidelines"));
    }

    @Test
    public void testFilterByCategory() throws Exception {
        String url = "http://localhost:" + port + "/api/materials/search?category=Viral+Strains";
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        String body = response.body();
        assertTrue(body.contains("\"total\":2"));
        assertTrue(body.contains("Influenza A(H1N1)"));
        assertTrue(body.contains("Avian Influenza (H5N1)"));
    }

    @Test
    public void testFilterByStatus() throws Exception {
        String url = "http://localhost:" + port + "/api/materials/search?status=Urgent";
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        String body = response.body();
        assertTrue(body.contains("\"total\":1"));
        assertTrue(body.contains("Urgent Cholera Outbreak Alert"));
    }

    @Test
    public void testCombinedSearchAndFilter() throws Exception {
        String url = "http://localhost:" + port + "/api/materials/search?q=Influenza&category=Viral+Strains&status=Pending";
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        String body = response.body();
        assertTrue(body.contains("\"total\":1"));
        assertTrue(body.contains("Avian Influenza (H5N1) Field Sampling Guidelines"));
    }

    @Test
    public void testSortingAndPagination() throws Exception {
        // Sort by title ascending, page 1, limit 2
        String url = "http://localhost:" + port + "/api/materials/search?sort=title,asc&page=1&limit=2";
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        String body = response.body();
        assertTrue(body.contains("\"total\":4"));
        assertTrue(body.contains("\"limit\":2"));
        assertTrue(body.contains("\"page\":1"));
        assertTrue(body.contains("\"pages\":2"));

        // Title order asc:
        // 1. Avian Influenza (H5N1) Field Sampling Guidelines
        // 2. Bacterial Culture Preservation Guidelines
        // Let's verify these are present on page 1
        assertTrue(body.contains("Avian Influenza (H5N1)"));
        assertTrue(body.contains("Bacterial Culture Preservation Guidelines"));

        // Page 2:
        url = "http://localhost:" + port + "/api/materials/search?sort=title,asc&page=2&limit=2";
        request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
        response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        body = response.body();
        assertTrue(body.contains("\"page\":2"));
        // 3. Influenza A(H1N1) Standard Operating Procedure
        // 4. Urgent Cholera Outbreak Alert
        assertTrue(body.contains("Influenza A(H1N1)"));
        assertTrue(body.contains("Urgent Cholera Outbreak Alert"));
    }

    @Test
    public void testValidationErrors() throws Exception {
        // Invalid status value
        String url = "http://localhost:" + port + "/api/materials/search?status=InvalidStatus";
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(400, response.statusCode());
        String body = response.body();
        assertTrue(body.contains("\"error\":\"BAD_REQUEST\""));
        assertTrue(body.contains("Invalid status parameter"));

        // Page out of bounds (0)
        url = "http://localhost:" + port + "/api/materials/search?page=0";
        request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
        response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(400, response.statusCode());
        body = response.body();
        assertTrue(body.contains("Invalid page parameter: must be greater than or equal to 1"));

        // Limit out of bounds (101)
        url = "http://localhost:" + port + "/api/materials/search?limit=101";
        request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
        response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(400, response.statusCode());
        body = response.body();
        assertTrue(body.contains("Invalid limit parameter: must be between 1 and 100"));
    }
}
