package com.eneik.production;

import com.eneik.production.dto.MaterialDTO;
import com.eneik.production.dto.SearchResponse;
import com.eneik.production.services.SearchService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SearchFlowsVerificationTest {

    private static final String JDBC_URL = "jdbc:h2:mem:search_flows_test_db;DB_CLOSE_DELAY=-1";
    private static Connection dbConn;
    private static SearchService searchService;

    private static long viralStrainsId;
    private static long bacterialCulturesId;
    private static long zoonoticId;

    @BeforeAll
    public static void setUp() throws Exception {
        dbConn = DriverManager.getConnection(JDBC_URL, "sa", "");
        searchService = new SearchService();

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

        populateTestData();
    }

    private static void populateTestData() throws SQLException {
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

            pstmt.setString(1, "Zoonotic");
            pstmt.executeUpdate();
            try (var rs = pstmt.getGeneratedKeys()) {
                rs.next();
                zoonoticId = rs.getLong(1);
            }
        }

        String insertSql = "INSERT INTO materials (title, content, category_id, status) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = dbConn.prepareStatement(insertSql)) {
            // Material 1 - Viral Strains, Analyzed
            pstmt.setString(1, "Ebola Virus Isolation Standard");
            pstmt.setString(2, "Reference guidelines for isolating Ebola virus from active clinical settings under containment.");
            pstmt.setLong(3, viralStrainsId);
            pstmt.setString(4, "Analyzed");
            pstmt.executeUpdate();

            // Material 2 - Viral Strains, Pending
            pstmt.setString(1, "Marburg Virus Assay Standard");
            pstmt.setString(2, "Analytical reference standard for quantitative assays of Marburg virus particles.");
            pstmt.setLong(3, viralStrainsId);
            pstmt.setString(4, "Pending");
            pstmt.executeUpdate();

            // Material 3 - Bacterial Cultures, Archived
            pstmt.setString(1, "Anthrax Spore Verification SOP");
            pstmt.setString(2, "Standard operating procedure for viability assays of Bacillus anthracis spores.");
            pstmt.setLong(3, bacterialCulturesId);
            pstmt.setString(4, "Archived");
            pstmt.executeUpdate();

            // Material 4 - Zoonotic, Urgent
            pstmt.setString(1, "Rabies Rabid Animal Field Check");
            pstmt.setString(2, "Urgent containment and diagnostic guidelines for rabid animal reporting in rural communities.");
            pstmt.setLong(3, zoonoticId);
            pstmt.setString(4, "Urgent");
            pstmt.executeUpdate();
        }
    }

    @AfterAll
    public static void tearDown() throws Exception {
        if (dbConn != null) {
            try (Statement stmt = dbConn.createStatement()) {
                stmt.execute("SHUTDOWN");
            }
            dbConn.close();
        }
    }

    @Test
    public void testSearchKnownMaterialMatchesExactly() throws SQLException {
        // Given search query "Marburg"
        // When searched on the service layer
        SearchResponse response = searchService.search(dbConn, "Marburg", null, null, null, 1, 10);

        // Then we get exactly 1 result matching "Marburg Virus Assay Standard"
        assertEquals(1, response.getTotal());
        MaterialDTO result = response.getItems().get(0);
        assertEquals("Marburg Virus Assay Standard", result.getTitle());
        assertEquals("Pending", result.getStatus());
        assertEquals("Viral Strains", result.getCategory());
    }

    @Test
    public void testComplexFiltersCombinationStrictAdherence() throws SQLException {
        // Given category "Viral Strains" and status "Analyzed"
        // When searched on the service layer
        SearchResponse response = searchService.search(dbConn, null, "Viral Strains", "Analyzed", null, 1, 10);

        // Then only 1 material fits both conditions (Ebola Virus Isolation Standard)
        assertEquals(1, response.getTotal());
        MaterialDTO result = response.getItems().get(0);
        assertEquals("Ebola Virus Isolation Standard", result.getTitle());
        assertEquals("Analyzed", result.getStatus());
        assertEquals("Viral Strains", result.getCategory());
    }

    @Test
    public void testFalsificationWithContradictoryFiltersReturnsEmpty() throws SQLException {
        // Given category "Zoonotic" but status "Archived" (Rabies is Urgent, Anthrax is Archived but in Bacterial Cultures)
        // When searched on the service layer
        SearchResponse response = searchService.search(dbConn, null, "Zoonotic", "Archived", null, 1, 10);

        // Then the result set must be empty (0 records)
        assertEquals(0, response.getTotal());
        assertTrue(response.getItems().isEmpty());
    }

    @Test
    public void testSearchKeywordAndCategoryCombination() throws SQLException {
        // Given query "SOP" and category "Bacterial Cultures"
        SearchResponse response = searchService.search(dbConn, "SOP", "Bacterial Cultures", null, null, 1, 10);

        // Then we get Anthrax Spore Verification SOP
        assertEquals(1, response.getTotal());
        assertEquals("Anthrax Spore Verification SOP", response.getItems().get(0).getTitle());
    }
}
