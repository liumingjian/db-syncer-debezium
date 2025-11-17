package com.dbsyncer.connectors.client.exception;

/**
 * Exception thrown when a connector is not found.
 */
public class ConnectorNotFoundException extends KafkaConnectException {

    public ConnectorNotFoundException(String connectorName) {
        super("Connector not found: " + connectorName, 404);
    }
}
