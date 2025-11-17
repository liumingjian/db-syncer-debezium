package com.dbsyncer.connectors.client.exception;

/**
 * Exception thrown when a connector already exists.
 */
public class ConnectorAlreadyExistsException extends KafkaConnectException {

    public ConnectorAlreadyExistsException(String connectorName) {
        super("Connector already exists: " + connectorName, 409);
    }
}
