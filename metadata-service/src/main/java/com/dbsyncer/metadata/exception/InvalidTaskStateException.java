package com.dbsyncer.metadata.exception;

import com.dbsyncer.metadata.entity.TaskStatus;

/**
 * Exception thrown when a task operation is invalid for the current state.
 */
public class InvalidTaskStateException extends RuntimeException {

    public InvalidTaskStateException(String operation, TaskStatus currentStatus) {
        super("Cannot " + operation + " task in state: " + currentStatus);
    }

    public InvalidTaskStateException(String message) {
        super(message);
    }
}
