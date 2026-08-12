-- U20260812160934550__full_text_search.sql
-- Manual rollback instructions for Flyway Community Edition
-- Note: Flyway Community does not support automatic undo migrations.

-- Drop the GIN full-text index for materials table:
-- DROP INDEX IF EXISTS materials_fts_idx;

-- Drop materials table:
-- DROP TABLE IF EXISTS materials;
