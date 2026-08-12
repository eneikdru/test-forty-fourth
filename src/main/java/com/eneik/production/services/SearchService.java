package com.eneik.production.services;

import com.eneik.production.dto.MaterialDTO;
import com.eneik.production.dto.SearchResponse;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class SearchService {

    public SearchResponse search(Connection conn, String q, String category, String status, String sort, int page, int limit) throws SQLException {
        boolean isPostgreSQL = false;
        try {
            String dbName = conn.getMetaData().getDatabaseProductName();
            if (dbName != null && dbName.toLowerCase().contains("postgres")) {
                isPostgreSQL = true;
            }
        } catch (Exception e) {
            // Default to false (use standard/H2 fallback)
        }

        List<Object> countParams = new ArrayList<>();
        StringBuilder whereClause = new StringBuilder();

        if (q != null && !q.trim().isEmpty()) {
            if (isPostgreSQL) {
                whereClause.append("to_tsvector('english', coalesce(m.title, '') || ' ' || coalesce(m.content, '')) @@ plainto_tsquery('english', ?)");
                countParams.add(q.trim());
            } else {
                whereClause.append("(LOWER(m.title) LIKE ? OR LOWER(m.content) LIKE ?)");
                String likePattern = "%" + q.trim().toLowerCase() + "%";
                countParams.add(likePattern);
                countParams.add(likePattern);
            }
        }

        if (category != null && !category.trim().isEmpty()) {
            if (whereClause.length() > 0) {
                whereClause.append(" AND ");
            }
            whereClause.append("c.name = ?");
            countParams.add(category.trim());
        }

        if (status != null && !status.trim().isEmpty()) {
            if (whereClause.length() > 0) {
                whereClause.append(" AND ");
            }
            whereClause.append("m.status = ?");
            countParams.add(status.trim());
        }

        String whereSql = whereClause.length() > 0 ? " WHERE " + whereClause.toString() : "";

        String sortCol = "m.updated_at";
        String sortDir = "DESC";
        if (sort != null && !sort.trim().isEmpty()) {
            String[] parts = sort.split(",");
            if (parts.length > 0) {
                String col = parts[0].trim().toLowerCase();
                if (col.equals("id") || col.equals("title") || col.equals("content") || col.equals("category") || col.equals("status") || col.equals("updated_at")) {
                    if (col.equals("category")) {
                        sortCol = "c.name";
                    } else {
                        sortCol = "m." + col;
                    }
                }
            }
            if (parts.length > 1) {
                String dir = parts[1].trim().toLowerCase();
                if (dir.equals("asc")) {
                    sortDir = "ASC";
                } else if (dir.equals("desc")) {
                    sortDir = "DESC";
                }
            }
        }

        long total = 0;
        String countSql = "SELECT COUNT(*) FROM materials m LEFT JOIN categories c ON m.category_id = c.id" + whereSql;
        try (PreparedStatement pstmt = conn.prepareStatement(countSql)) {
            for (int i = 0; i < countParams.size(); i++) {
                pstmt.setObject(i + 1, countParams.get(i));
            }
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    total = rs.getLong(1);
                }
            }
        }

        int pages = (int) Math.ceil((double) total / limit);
        if (pages == 0) {
            pages = 1;
        }

        int offset = (page - 1) * limit;

        String selectSql = "SELECT m.id, m.title, m.content, c.name AS category_name, m.status, m.updated_at " +
                "FROM materials m LEFT JOIN categories c ON m.category_id = c.id" +
                whereSql +
                " ORDER BY " + sortCol + " " + sortDir +
                " LIMIT ? OFFSET ?";

        List<MaterialDTO> items = new ArrayList<>();
        try (PreparedStatement pstmt = conn.prepareStatement(selectSql)) {
            int idx = 1;
            for (Object param : countParams) {
                pstmt.setObject(idx++, param);
            }
            pstmt.setInt(idx++, limit);
            pstmt.setInt(idx++, offset);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Long id = rs.getLong("id");
                    String title = rs.getString("title");
                    String content = rs.getString("content");
                    String cat = rs.getString("category_name");
                    String stat = rs.getString("status");
                    java.sql.Timestamp ts = rs.getTimestamp("updated_at");
                    Instant updatedAt = ts != null ? ts.toInstant() : Instant.now();

                    items.add(new MaterialDTO(id, title, content, cat, stat, updatedAt));
                }
            }
        }

        return new SearchResponse(items, total, page, limit, pages);
    }
}
