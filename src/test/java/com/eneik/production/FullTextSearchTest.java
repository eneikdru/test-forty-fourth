package com.eneik.production;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FullTextSearchTest {

    private Connection conn;

    @BeforeEach
    public void setUp() throws Exception {
        // Initialize an in-memory H2 database
        conn = DriverManager.getConnection("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1", "sa", "");
    }

    @AfterEach
    public void tearDown() throws Exception {
        if (conn != null) {
            Statement stmt = conn.createStatement();
            stmt.execute("SHUTDOWN");
            conn.close();
        }
    }

    @Test
    public void testMigrationSyntaxLoadedAndDocumented() throws Exception {
        // Verify that the migration file exists and contains valid PostgreSQL DDL with a GIN index on to_tsvector
        InputStream migrationStream = getClass().getResourceAsStream("/db/migration/V20260812160934550__full_text_search.sql");
        assertNotNull(migrationStream, "Migration file must be present on the classpath.");

        String ddl;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(migrationStream, StandardCharsets.UTF_8))) {
            ddl = reader.lines().collect(Collectors.joining("\n"));
        }

        assertTrue(ddl.contains("CREATE TABLE IF NOT EXISTS materials"), "Migration must create the materials table.");
        assertTrue(ddl.contains("CREATE INDEX IF NOT EXISTS materials_fts_idx"), "Migration must create the full-text search index.");
        assertTrue(ddl.contains("USING gin"), "Migration index must use GIN index for optimized PostgreSQL search.");
        assertTrue(ddl.contains("to_tsvector"), "Migration must use to_tsvector for full-text search vector generation.");
    }

    @Test
    public void testFullTextSearchLogicConceptVerifiedOnH2() throws Exception {
        Statement stmt = conn.createStatement();

        // Establish the materials schema on H2
        stmt.execute("CREATE TABLE IF NOT EXISTS materials (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "title VARCHAR(255) NOT NULL, " +
                "content CLOB, " +
                "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")");

        // Initialize FT index on H2 in-memory db
        stmt.execute("CREATE ALIAS IF NOT EXISTS FT_INIT FOR \"org.h2.fulltext.FullText.init\"");
        stmt.execute("CALL FT_INIT()");
        stmt.execute("CALL FT_CREATE_INDEX('PUBLIC', 'MATERIALS', 'TITLE,CONTENT')");

        // Insert sample documents
        String insertSql = "INSERT INTO materials (title, content) VALUES (?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
            pstmt.setString(1, "Influenza A(H1N1) Standard Operating Procedure");
            pstmt.setString(2, "Standard operating procedure for the detection, reporting, and preliminary epidemiological investigation of suspected human cases.");
            pstmt.executeUpdate();

            pstmt.setString(1, "Avian Influenza (H5N1) Field Sampling Guidelines");
            pstmt.setString(2, "Comprehensive field manual for environmental swabbing and live-bird sampling in wet markets and commercial poultry operations.");
            pstmt.executeUpdate();

            pstmt.setString(1, "Seasonal Influenza Vaccine Effectiveness Cohort Study");
            pstmt.setString(2, "Study design for assessing the mid-season effectiveness of the 2021-2022 Northern Hemisphere quadrivalent influenza vaccine.");
            pstmt.executeUpdate();
        }

        // Search using full-text search index (FT_SEARCH_DATA)
        String searchSql = "SELECT * FROM FT_SEARCH_DATA('Influenza', 0, 0)";
        int matchCount = 0;
        try (PreparedStatement pstmt = conn.prepareStatement(searchSql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                matchCount++;
                String keys = rs.getString("KEYS");
                assertTrue(keys.contains("1") || keys.contains("2") || keys.contains("3"), "Matching record keys should be retrieved from index.");
            }
        }

        // We expect all 3 records to match the word "Influenza"
        assertEquals(3, matchCount, "Should return 3 search results for keyword 'Influenza'");

        // Match with specific term like "Sampling" which is only in card 2
        String searchSpecificSql = "SELECT * FROM FT_SEARCH_DATA('Sampling', 0, 0)";
        int specificCount = 0;
        try (PreparedStatement pstmt = conn.prepareStatement(searchSpecificSql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                specificCount++;
                String keys = rs.getString("KEYS");
                assertTrue(keys.contains("2"), "Only card 2 should match 'Sampling'");
            }
        }
        assertEquals(1, specificCount, "Should return exactly 1 result for 'Sampling'");
    }
}
