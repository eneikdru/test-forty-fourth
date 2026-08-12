-- U20260812160530032__create_categories_table.sql
-- Rollback creating categories table
-- Note: Flyway Community does not support automatic undo migrations.

-- Drop categories table:
DROP TABLE IF EXISTS categories;
