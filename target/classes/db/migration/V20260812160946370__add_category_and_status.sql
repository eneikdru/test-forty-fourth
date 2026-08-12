-- V20260812160946370__add_category_and_status.sql
-- Add category_id (foreign key) and status columns to materials table

ALTER TABLE materials ADD COLUMN category_id INT REFERENCES categories(id);
ALTER TABLE materials ADD COLUMN status VARCHAR(50);
