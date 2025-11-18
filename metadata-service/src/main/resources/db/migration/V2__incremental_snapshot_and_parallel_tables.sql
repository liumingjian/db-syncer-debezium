-- V2__incremental_snapshot_and_parallel_tables.sql
-- Add configuration columns for incremental snapshot and parallel table migration.

ALTER TABLE migration_tasks
    ADD COLUMN IF NOT EXISTS incremental_snapshot BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS snapshot_chunk_size INTEGER DEFAULT 10000,
    ADD COLUMN IF NOT EXISTS parallel_tables INTEGER DEFAULT 1;

