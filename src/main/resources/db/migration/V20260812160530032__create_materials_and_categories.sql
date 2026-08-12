-- V20260812160530032__create_materials_and_categories.sql
-- Migration file to create tables for materials and categories in the epidemiological knowledge base.

CREATE TABLE categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    description VARCHAR(1000)
);

CREATE TABLE materials (
    id VARCHAR(255) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    category_id BIGINT NOT NULL,
    date_added TIMESTAMP NOT NULL,
    status VARCHAR(50) NOT NULL,
    CONSTRAINT fk_materials_category FOREIGN KEY (category_id) REFERENCES categories(id)
);
