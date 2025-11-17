package com.dbsyncer.metadata.exception;

/**
 * Exception thrown when attempting to create a task that already exists.
 */
public class TaskAlreadyExistsException extends RuntimeException {

    public TaskAlreadyExistsException(String taskName) {
        super("Task already exists with name: " + taskName);
    }
}
