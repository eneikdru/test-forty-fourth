-- V20260812160934550__full_text_search.sql
-- Create materials table and configure PostgreSQL full-text search indexes on title and content fields

CREATE TABLE IF NOT EXISTS materials (
    id SERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    content TEXT,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Create GIN index for optimized full-text search.
-- We coalesce NULL values to empty strings to ensure indexing works correctly even if content is NULL.
CREATE INDEX IF NOT EXISTS materials_fts_idx ON materials USING gin (
    to_tsvector('english', coalesce(title, '') || ' ' || coalesce(content, ''))
);
