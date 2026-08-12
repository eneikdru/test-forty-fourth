package com.eneik.production.services;

import com.eneik.production.dto.MaterialDTO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.Objects;

public class MaterialsService {

    public MaterialDTO createMaterial(Connection conn, String title, String content, String category, String status) throws SQLException {
        validateInput(title, content, category, status);

        Long categoryId = getOrCreateCategory(conn, category);

        String insertSql = "INSERT INTO materials (title, content, category_id, status) VALUES (?, ?, ?, ?)";
        Long materialId;
        try (PreparedStatement pstmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, title);
            pstmt.setString(2, content);
            pstmt.setLong(3, categoryId);
            pstmt.setString(4, status);
            pstmt.executeUpdate();
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    materialId = rs.getLong(1);
                } else {
                    throw new SQLException("Failed to retrieve generated material ID");
                }
            }
        }

        return getMaterial(conn, materialId);
    }

    public MaterialDTO getMaterial(Connection conn, Long id) throws SQLException {
        if (id == null) {
            throw new NotFoundException("Material ID cannot be null");
        }
        String selectSql = "SELECT m.id, m.title, m.content, c.name AS category_name, m.status, m.updated_at " +
                "FROM materials m LEFT JOIN categories c ON m.category_id = c.id WHERE m.id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(selectSql)) {
            pstmt.setLong(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String title = rs.getString("title");
                    String content = rs.getString("content");
                    String category = rs.getString("category_name");
                    String status = rs.getString("status");
                    java.sql.Timestamp ts = rs.getTimestamp("updated_at");
                    Instant updatedAt = ts != null ? ts.toInstant() : Instant.now();
                    return new MaterialDTO(id, title, content, category, status, updatedAt);
                }
            }
        }
        throw new NotFoundException("Material with ID " + id + " does not exist");
    }

    public MaterialDTO updateMaterial(Connection conn, Long id, String title, String content, String category, String status) throws SQLException {
        // Read current material to check existence and perform validation if changes are proposed
        MaterialDTO current = getMaterial(conn, id);

        // Validation only applies if we are updating fields
        String finalTitle = title != null ? title : current.getTitle();
        String finalContent = content != null ? content : current.getContent();
        String finalCategory = category != null ? category : current.getCategory();
        String finalStatus = status != null ? status : current.getStatus();

        validateInput(finalTitle, finalContent, finalCategory, finalStatus);

        Long categoryId = getOrCreateCategory(conn, finalCategory);

        // Use atomically-guarded database update if we change state/lifecycle or just standard update but aligned with constraints.
        // We will perform an update on title, content, category_id, status, and set updated_at to now.
        // Note: H2 or PG specific syntax? Standard SQL update works across both:
        String updateSql = "UPDATE materials SET title = ?, content = ?, category_id = ?, status = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(updateSql)) {
            pstmt.setString(1, finalTitle);
            pstmt.setString(2, finalContent);
            pstmt.setLong(3, categoryId);
            pstmt.setString(4, finalStatus);
            pstmt.setLong(5, id);
            int updated = pstmt.executeUpdate();
            if (updated == 0) {
                throw new NotFoundException("Material with ID " + id + " does not exist");
            }
        }

        return getMaterial(conn, id);
    }

    public void deleteMaterial(Connection conn, Long id) throws SQLException {
        // Ensure it exists first to throw 404 if not found
        getMaterial(conn, id);

        String deleteSql = "DELETE FROM materials WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(deleteSql)) {
            pstmt.setLong(1, id);
            int deleted = pstmt.executeUpdate();
            if (deleted == 0) {
                throw new NotFoundException("Material with ID " + id + " does not exist");
            }
        }
    }

    private void validateInput(String title, String content, String category, String status) {
        if (title == null || title.trim().isEmpty()) {
            throw new ValidationException("Title is required and cannot be blank");
        }
        if (content == null || content.trim().isEmpty()) {
            throw new ValidationException("Content is required and cannot be blank");
        }
        if (category == null || category.trim().isEmpty()) {
            throw new ValidationException("Category is required and cannot be blank");
        }
        if (status == null || status.trim().isEmpty()) {
            throw new ValidationException("Status is required and cannot be blank");
        }
        String statusTrimmed = status.trim();
        if (!statusTrimmed.equals("Analyzed") && !statusTrimmed.equals("Pending") && !statusTrimmed.equals("Archived") && !statusTrimmed.equals("Urgent")) {
            throw new ValidationException("Invalid status: must be one of Analyzed, Pending, Archived, Urgent");
        }
    }

    private Long getOrCreateCategory(Connection conn, String categoryName) throws SQLException {
        categoryName = categoryName.trim();
        String selectSql = "SELECT id FROM categories WHERE name = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(selectSql)) {
            pstmt.setString(1, categoryName);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("id");
                }
            }
        }

        String insertSql = "INSERT INTO categories (name) VALUES (?)";
        try (PreparedStatement pstmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, categoryName);
            pstmt.executeUpdate();
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }
        throw new SQLException("Failed to create category: " + categoryName);
    }
}
