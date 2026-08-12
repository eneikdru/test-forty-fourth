-- U20260812160946370__add_category_and_status.sql
-- Rollback adding category_id and status columns

ALTER TABLE materials DROP COLUMN IF EXISTS category_id;
ALTER TABLE materials DROP COLUMN IF EXISTS status;
