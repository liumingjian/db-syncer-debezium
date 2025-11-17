package com.dbsyncer.metadata.exception;

import java.util.UUID;

/**
 * Exception thrown when a migration task is not found.
 */
public class TaskNotFoundException extends RuntimeException {

    public TaskNotFoundException(UUID taskId) {
        super("Task not found with id: " + taskId);
    }

    public TaskNotFoundException(String taskName) {
        super("Task not found with name: " + taskName);
    }
}
