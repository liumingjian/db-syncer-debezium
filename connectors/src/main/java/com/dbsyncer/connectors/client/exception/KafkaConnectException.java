package com.dbsyncer.connectors.client.exception;

/**
 * Exception thrown when Kafka Connect operations fail.
 */
public class KafkaConnectException extends RuntimeException {

    private final int statusCode;

    public KafkaConnectException(String message) {
        super(message);
        this.statusCode = 0;
    }

    public KafkaConnectException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = 0;
    }

    public KafkaConnectException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public KafkaConnectException(String message, int statusCode, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
