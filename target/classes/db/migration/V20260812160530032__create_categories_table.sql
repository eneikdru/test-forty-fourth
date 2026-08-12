-- V20260812160530032__create_categories_table.sql
-- Create categories table for epidemiological materials

CREATE TABLE IF NOT EXISTS categories (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) UNIQUE NOT NULL
);
