package com.eneik.production.dto;

import java.time.Instant;

public class MaterialDTO {
    private Long id;
    private String title;
    private String content;
    private String category;
    private String status;
    private Instant updatedAt;

    public MaterialDTO() {}

    public MaterialDTO(Long id, String title, String content, String category, String status, Instant updatedAt) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.category = category;
        this.status = status;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
