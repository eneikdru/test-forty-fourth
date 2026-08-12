-- V20260812160946370__add_category_and_status.sql
-- Add category and status columns to materials table

ALTER TABLE materials ADD COLUMN category VARCHAR(255);
ALTER TABLE materials ADD COLUMN status VARCHAR(50);
