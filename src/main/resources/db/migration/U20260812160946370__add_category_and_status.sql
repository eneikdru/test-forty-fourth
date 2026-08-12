-- U20260812160946370__add_category_and_status.sql
-- Rollback adding category and status columns

ALTER TABLE materials DROP COLUMN IF EXISTS category;
ALTER TABLE materials DROP COLUMN IF EXISTS status;
