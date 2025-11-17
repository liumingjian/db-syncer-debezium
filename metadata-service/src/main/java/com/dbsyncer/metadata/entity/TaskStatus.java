package com.dbsyncer.metadata.entity;

/**
 * Enumeration representing the lifecycle status of a migration task.
 */
public enum TaskStatus {
    CREATED,
    CONFIGURING,
    STARTING,
    RUNNING,
    PAUSED,
    STOPPING,
    STOPPED,
    COMPLETED,
    FAILED
}
