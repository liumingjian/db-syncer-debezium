package com.dbsyncer.metadata.entity;

/**
 * Enumeration representing the progress status of a table migration.
 */
public enum ProgressStatus {
    PENDING,
    SNAPSHOTTING,
    STREAMING,
    COMPLETED,
    FAILED
}
