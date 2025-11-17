package com.dbsyncer.connectors.lifecycle;

import com.dbsyncer.connectors.client.KafkaConnectClient;
import com.dbsyncer.connectors.client.exception.ConnectorAlreadyExistsException;
import com.dbsyncer.connectors.client.exception.ConnectorNotFoundException;
import com.dbsyncer.connectors.client.exception.KafkaConnectException;
import com.dbsyncer.connectors.client.model.ConnectorInfo;
import com.dbsyncer.connectors.client.model.ConnectorStatus;
import com.dbsyncer.connectors.config.SourceConnectorConfig;
import lombok.extern.slf4j.Slf4j;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages the lifecycle of Debezium connectors.
 * Provides methods for deploying, deleting, pausing, resuming, and monitoring connectors.
 */
@Slf4j
public class ConnectorLifecycleManager implements Closeable {

    private final KafkaConnectClient client;
    private final Map<String, ConnectorState> connectorStates;

    // Configuration
    private final long defaultWaitTimeoutMs;
    private final long defaultPollIntervalMs;
    private final int maxRestartAttempts;
    private final long restartBackoffMs;

    /**
     * Create a new ConnectorLifecycleManager with default settings.
     *
     * @param kafkaConnectUrl The URL of the Kafka Connect REST API
     */
    public ConnectorLifecycleManager(String kafkaConnectUrl) {
        this(kafkaConnectUrl, 120000, 2000, 3, 10000);
    }

    /**
     * Create a new ConnectorLifecycleManager with custom settings.
     *
     * @param kafkaConnectUrl     The URL of the Kafka Connect REST API
     * @param defaultWaitTimeoutMs Default timeout for wait operations
     * @param defaultPollIntervalMs Default poll interval for status checks
     * @param maxRestartAttempts  Maximum number of restart attempts on failure
     * @param restartBackoffMs    Backoff time between restart attempts
     */
    public ConnectorLifecycleManager(String kafkaConnectUrl,
                                     long defaultWaitTimeoutMs,
                                     long defaultPollIntervalMs,
                                     int maxRestartAttempts,
                                     long restartBackoffMs) {
        this.client = new KafkaConnectClient(kafkaConnectUrl);
        this.connectorStates = new ConcurrentHashMap<>();
        this.defaultWaitTimeoutMs = defaultWaitTimeoutMs;
        this.defaultPollIntervalMs = defaultPollIntervalMs;
        this.maxRestartAttempts = maxRestartAttempts;
        this.restartBackoffMs = restartBackoffMs;
    }

    /**
     * Deploy a new connector with the given configuration.
     *
     * @param connectorName The name of the connector
     * @param config        The connector configuration
     * @return ConnectorInfo for the deployed connector
     * @throws ConnectorAlreadyExistsException if a connector with the same name exists
     * @throws KafkaConnectException           if deployment fails
     */
    public ConnectorInfo deployConnector(String connectorName, SourceConnectorConfig config) {
        log.info("Deploying connector: {}", connectorName);

        // Validate configuration
        config.validate();

        Map<String, String> configMap = config.toConfigMap();
        log.debug("Connector configuration: {}", configMap);

        try {
            ConnectorInfo info = client.createConnector(connectorName, configMap);
            connectorStates.put(connectorName, ConnectorState.DEPLOYING);
            log.info("Connector {} deployed successfully", connectorName);
            return info;
        } catch (ConnectorAlreadyExistsException e) {
            log.error("Connector {} already exists", connectorName);
            throw e;
        } catch (KafkaConnectException e) {
            log.error("Failed to deploy connector {}: {}", connectorName, e.getMessage());
            throw e;
        }
    }

    /**
     * Deploy a connector and wait for it to be running.
     *
     * @param connectorName The name of the connector
     * @param config        The connector configuration
     * @return ConnectorStatus after waiting
     * @throws KafkaConnectException if deployment or startup fails
     */
    public ConnectorStatus deployAndWaitForRunning(String connectorName, SourceConnectorConfig config) {
        deployConnector(connectorName, config);

        log.info("Waiting for connector {} to be running...", connectorName);
        boolean running = client.waitForConnectorRunning(connectorName,
                defaultWaitTimeoutMs, defaultPollIntervalMs);

        if (!running) {
            throw new KafkaConnectException("Timeout waiting for connector " + connectorName + " to be running");
        }

        connectorStates.put(connectorName, ConnectorState.RUNNING);
        return client.getConnectorStatus(connectorName);
    }

    /**
     * Update an existing connector's configuration.
     *
     * @param connectorName The name of the connector
     * @param config        The new configuration
     * @return ConnectorInfo for the updated connector
     * @throws ConnectorNotFoundException if the connector doesn't exist
     * @throws KafkaConnectException      if the update fails
     */
    public ConnectorInfo updateConnector(String connectorName, SourceConnectorConfig config) {
        log.info("Updating connector: {}", connectorName);

        config.validate();
        Map<String, String> configMap = config.toConfigMap();

        try {
            ConnectorInfo info = client.updateConnector(connectorName, configMap);
            connectorStates.put(connectorName, ConnectorState.RESTARTING);
            log.info("Connector {} updated successfully", connectorName);
            return info;
        } catch (ConnectorNotFoundException e) {
            log.error("Connector {} not found", connectorName);
            throw e;
        } catch (KafkaConnectException e) {
            log.error("Failed to update connector {}: {}", connectorName, e.getMessage());
            throw e;
        }
    }

    /**
     * Delete a connector.
     *
     * @param connectorName The name of the connector
     * @throws ConnectorNotFoundException if the connector doesn't exist
     * @throws KafkaConnectException      if deletion fails
     */
    public void deleteConnector(String connectorName) {
        log.info("Deleting connector: {}", connectorName);

        try {
            client.deleteConnector(connectorName);
            connectorStates.remove(connectorName);
            log.info("Connector {} deleted successfully", connectorName);
        } catch (ConnectorNotFoundException e) {
            connectorStates.remove(connectorName);
            log.warn("Connector {} was not found for deletion", connectorName);
            throw e;
        } catch (KafkaConnectException e) {
            log.error("Failed to delete connector {}: {}", connectorName, e.getMessage());
            throw e;
        }
    }

    /**
     * Delete a connector if it exists, without throwing an exception if not found.
     *
     * @param connectorName The name of the connector
     * @return true if the connector was deleted, false if it didn't exist
     */
    public boolean deleteConnectorIfExists(String connectorName) {
        try {
            deleteConnector(connectorName);
            return true;
        } catch (ConnectorNotFoundException e) {
            return false;
        }
    }

    /**
     * Pause a running connector.
     *
     * @param connectorName The name of the connector
     * @throws ConnectorNotFoundException if the connector doesn't exist
     * @throws KafkaConnectException      if pausing fails
     */
    public void pauseConnector(String connectorName) {
        log.info("Pausing connector: {}", connectorName);

        try {
            client.pauseConnector(connectorName);
            connectorStates.put(connectorName, ConnectorState.PAUSED);
            log.info("Connector {} paused successfully", connectorName);
        } catch (KafkaConnectException e) {
            log.error("Failed to pause connector {}: {}", connectorName, e.getMessage());
            throw e;
        }
    }

    /**
     * Resume a paused connector.
     *
     * @param connectorName The name of the connector
     * @throws ConnectorNotFoundException if the connector doesn't exist
     * @throws KafkaConnectException      if resuming fails
     */
    public void resumeConnector(String connectorName) {
        log.info("Resuming connector: {}", connectorName);

        try {
            client.resumeConnector(connectorName);
            connectorStates.put(connectorName, ConnectorState.RUNNING);
            log.info("Connector {} resumed successfully", connectorName);
        } catch (KafkaConnectException e) {
            log.error("Failed to resume connector {}: {}", connectorName, e.getMessage());
            throw e;
        }
    }

    /**
     * Restart a connector.
     *
     * @param connectorName The name of the connector
     * @throws ConnectorNotFoundException if the connector doesn't exist
     * @throws KafkaConnectException      if restarting fails
     */
    public void restartConnector(String connectorName) {
        log.info("Restarting connector: {}", connectorName);

        try {
            client.restartConnector(connectorName);
            connectorStates.put(connectorName, ConnectorState.RESTARTING);
            log.info("Connector {} restart initiated", connectorName);
        } catch (KafkaConnectException e) {
            log.error("Failed to restart connector {}: {}", connectorName, e.getMessage());
            throw e;
        }
    }

    /**
     * Restart a connector with automatic retry on failure.
     *
     * @param connectorName The name of the connector
     * @return true if restart was successful, false otherwise
     */
    public boolean restartConnectorWithRetry(String connectorName) {
        for (int attempt = 1; attempt <= maxRestartAttempts; attempt++) {
            try {
                log.info("Attempting to restart connector {} (attempt {}/{})",
                        connectorName, attempt, maxRestartAttempts);

                restartConnector(connectorName);

                // Wait for connector to be running
                boolean running = client.waitForConnectorRunning(connectorName,
                        defaultWaitTimeoutMs, defaultPollIntervalMs);

                if (running) {
                    connectorStates.put(connectorName, ConnectorState.RUNNING);
                    log.info("Connector {} restarted successfully", connectorName);
                    return true;
                }

            } catch (KafkaConnectException e) {
                log.warn("Restart attempt {} failed for connector {}: {}",
                        attempt, connectorName, e.getMessage());
            }

            if (attempt < maxRestartAttempts) {
                try {
                    long backoff = restartBackoffMs * attempt;
                    log.info("Waiting {}ms before next restart attempt...", backoff);
                    Thread.sleep(backoff);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.error("Interrupted during restart backoff");
                    break;
                }
            }
        }

        connectorStates.put(connectorName, ConnectorState.FAILED);
        log.error("Failed to restart connector {} after {} attempts",
                connectorName, maxRestartAttempts);
        return false;
    }

    /**
     * Get the status of a connector.
     *
     * @param connectorName The name of the connector
     * @return ConnectorStatus
     * @throws ConnectorNotFoundException if the connector doesn't exist
     */
    public ConnectorStatus getConnectorStatus(String connectorName) {
        ConnectorStatus status = client.getConnectorStatus(connectorName);

        // Update internal state based on actual status
        if (status.isRunning()) {
            connectorStates.put(connectorName, ConnectorState.RUNNING);
        } else if (status.isPaused()) {
            connectorStates.put(connectorName, ConnectorState.PAUSED);
        } else if (status.isFailed()) {
            connectorStates.put(connectorName, ConnectorState.FAILED);
        }

        return status;
    }

    /**
     * Get the list of all managed connectors.
     *
     * @return List of connector names
     */
    public List<String> listConnectors() {
        return client.getConnectors();
    }

    /**
     * Check if a connector exists.
     *
     * @param connectorName The name of the connector
     * @return true if the connector exists
     */
    public boolean connectorExists(String connectorName) {
        return client.connectorExists(connectorName);
    }

    /**
     * Monitor a connector and handle failures automatically.
     *
     * @param connectorName The name of the connector
     * @return true if the connector is healthy, false if it's failed and couldn't be restarted
     */
    public boolean monitorAndRecoverConnector(String connectorName) {
        try {
            ConnectorStatus status = getConnectorStatus(connectorName);

            if (status.isRunning()) {
                log.debug("Connector {} is running normally", connectorName);
                return true;
            }

            if (status.isPaused()) {
                log.info("Connector {} is paused", connectorName);
                return true;
            }

            if (status.isFailed()) {
                log.warn("Connector {} has failed, attempting recovery...", connectorName);
                String errorTrace = status.getErrorTrace();
                log.error("Failure reason: {}", errorTrace);

                return restartConnectorWithRetry(connectorName);
            }

            log.warn("Connector {} is in unknown state", connectorName);
            return false;

        } catch (ConnectorNotFoundException e) {
            log.error("Connector {} not found during monitoring", connectorName);
            return false;
        }
    }

    /**
     * Get the internal state of a connector.
     *
     * @param connectorName The name of the connector
     * @return ConnectorState or null if not tracked
     */
    public ConnectorState getInternalState(String connectorName) {
        return connectorStates.get(connectorName);
    }

    /**
     * Get the underlying Kafka Connect client.
     *
     * @return KafkaConnectClient
     */
    public KafkaConnectClient getClient() {
        return client;
    }

    @Override
    public void close() throws IOException {
        client.close();
    }

    /**
     * Internal connector state tracking.
     */
    public enum ConnectorState {
        DEPLOYING,
        RUNNING,
        PAUSED,
        RESTARTING,
        STOPPING,
        FAILED,
        DELETED
    }
}
