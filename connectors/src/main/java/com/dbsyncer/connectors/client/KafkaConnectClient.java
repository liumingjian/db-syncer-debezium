package com.dbsyncer.connectors.client;

import com.dbsyncer.connectors.client.exception.ConnectorAlreadyExistsException;
import com.dbsyncer.connectors.client.exception.ConnectorNotFoundException;
import com.dbsyncer.connectors.client.exception.KafkaConnectException;
import com.dbsyncer.connectors.client.model.ConnectorInfo;
import com.dbsyncer.connectors.client.model.ConnectorPlugin;
import com.dbsyncer.connectors.client.model.ConnectorStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.util.Timeout;

import java.io.Closeable;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST API client for Kafka Connect.
 * Provides methods to manage connectors through the Kafka Connect REST API.
 */
@Slf4j
public class KafkaConnectClient implements Closeable {

    private final String baseUrl;
    private final CloseableHttpClient httpClient;
    private final ObjectMapper objectMapper;

    /**
     * Create a new KafkaConnectClient with default settings.
     *
     * @param baseUrl The base URL of the Kafka Connect REST API (e.g., "http://localhost:8083")
     */
    public KafkaConnectClient(String baseUrl) {
        this(baseUrl, 30000, 60000);
    }

    /**
     * Create a new KafkaConnectClient with custom timeout settings.
     *
     * @param baseUrl          The base URL of the Kafka Connect REST API
     * @param connectTimeoutMs Connection timeout in milliseconds
     * @param readTimeoutMs    Read timeout in milliseconds
     */
    public KafkaConnectClient(String baseUrl, int connectTimeoutMs, int readTimeoutMs) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.objectMapper = new ObjectMapper();

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(Timeout.ofMilliseconds(connectTimeoutMs))
                .setResponseTimeout(Timeout.ofMilliseconds(readTimeoutMs))
                .build();

        this.httpClient = HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .build();
    }

    /**
     * Get the list of available connector plugins.
     *
     * @return List of connector plugins
     * @throws KafkaConnectException if the request fails
     */
    public List<ConnectorPlugin> getConnectorPlugins() {
        String url = baseUrl + "/connector-plugins";
        log.debug("Getting connector plugins from {}", url);

        try {
            String response = executeGet(url);
            return objectMapper.readValue(response, new TypeReference<List<ConnectorPlugin>>() {
            });
        } catch (JsonProcessingException e) {
            throw new KafkaConnectException("Failed to parse connector plugins response", e);
        }
    }

    /**
     * Get the list of all connectors.
     *
     * @return List of connector names
     * @throws KafkaConnectException if the request fails
     */
    public List<String> getConnectors() {
        String url = baseUrl + "/connectors";
        log.debug("Getting connectors from {}", url);

        try {
            String response = executeGet(url);
            return objectMapper.readValue(response, new TypeReference<List<String>>() {
            });
        } catch (JsonProcessingException e) {
            throw new KafkaConnectException("Failed to parse connectors response", e);
        }
    }

    /**
     * Get information about a specific connector.
     *
     * @param connectorName The name of the connector
     * @return Connector information
     * @throws ConnectorNotFoundException if the connector doesn't exist
     * @throws KafkaConnectException      if the request fails
     */
    public ConnectorInfo getConnector(String connectorName) {
        String url = baseUrl + "/connectors/" + connectorName;
        log.debug("Getting connector {} from {}", connectorName, url);

        try {
            String response = executeGet(url);
            return objectMapper.readValue(response, ConnectorInfo.class);
        } catch (JsonProcessingException e) {
            throw new KafkaConnectException("Failed to parse connector response", e);
        }
    }

    /**
     * Get the status of a specific connector.
     *
     * @param connectorName The name of the connector
     * @return Connector status
     * @throws ConnectorNotFoundException if the connector doesn't exist
     * @throws KafkaConnectException      if the request fails
     */
    public ConnectorStatus getConnectorStatus(String connectorName) {
        String url = baseUrl + "/connectors/" + connectorName + "/status";
        log.debug("Getting connector status {} from {}", connectorName, url);

        try {
            String response = executeGet(url);
            return objectMapper.readValue(response, ConnectorStatus.class);
        } catch (JsonProcessingException e) {
            throw new KafkaConnectException("Failed to parse connector status response", e);
        }
    }

    /**
     * Create a new connector.
     *
     * @param connectorName The name of the connector
     * @param config        The connector configuration
     * @return Connector information
     * @throws ConnectorAlreadyExistsException if the connector already exists
     * @throws KafkaConnectException           if the request fails
     */
    public ConnectorInfo createConnector(String connectorName, Map<String, String> config) {
        String url = baseUrl + "/connectors";
        log.info("Creating connector {} at {}", connectorName, url);

        Map<String, Object> payload = new HashMap<>();
        payload.put("name", connectorName);
        payload.put("config", config);

        try {
            String jsonPayload = objectMapper.writeValueAsString(payload);
            String response = executePost(url, jsonPayload);
            return objectMapper.readValue(response, ConnectorInfo.class);
        } catch (JsonProcessingException e) {
            throw new KafkaConnectException("Failed to create connector", e);
        }
    }

    /**
     * Update a connector's configuration.
     *
     * @param connectorName The name of the connector
     * @param config        The new connector configuration
     * @return Connector information
     * @throws ConnectorNotFoundException if the connector doesn't exist
     * @throws KafkaConnectException      if the request fails
     */
    public ConnectorInfo updateConnector(String connectorName, Map<String, String> config) {
        String url = baseUrl + "/connectors/" + connectorName + "/config";
        log.info("Updating connector {} at {}", connectorName, url);

        try {
            String jsonPayload = objectMapper.writeValueAsString(config);
            String response = executePut(url, jsonPayload);
            return objectMapper.readValue(response, ConnectorInfo.class);
        } catch (JsonProcessingException e) {
            throw new KafkaConnectException("Failed to update connector", e);
        }
    }

    /**
     * Delete a connector.
     *
     * @param connectorName The name of the connector
     * @throws ConnectorNotFoundException if the connector doesn't exist
     * @throws KafkaConnectException      if the request fails
     */
    public void deleteConnector(String connectorName) {
        String url = baseUrl + "/connectors/" + connectorName;
        log.info("Deleting connector {} at {}", connectorName, url);

        executeDelete(url);
    }

    /**
     * Pause a connector.
     *
     * @param connectorName The name of the connector
     * @throws ConnectorNotFoundException if the connector doesn't exist
     * @throws KafkaConnectException      if the request fails
     */
    public void pauseConnector(String connectorName) {
        String url = baseUrl + "/connectors/" + connectorName + "/pause";
        log.info("Pausing connector {} at {}", connectorName, url);

        executePut(url, null);
    }

    /**
     * Resume a paused connector.
     *
     * @param connectorName The name of the connector
     * @throws ConnectorNotFoundException if the connector doesn't exist
     * @throws KafkaConnectException      if the request fails
     */
    public void resumeConnector(String connectorName) {
        String url = baseUrl + "/connectors/" + connectorName + "/resume";
        log.info("Resuming connector {} at {}", connectorName, url);

        executePut(url, null);
    }

    /**
     * Restart a connector.
     *
     * @param connectorName The name of the connector
     * @throws ConnectorNotFoundException if the connector doesn't exist
     * @throws KafkaConnectException      if the request fails
     */
    public void restartConnector(String connectorName) {
        String url = baseUrl + "/connectors/" + connectorName + "/restart";
        log.info("Restarting connector {} at {}", connectorName, url);

        executePost(url, null);
    }

    /**
     * Restart a specific task of a connector.
     *
     * @param connectorName The name of the connector
     * @param taskId        The task ID
     * @throws ConnectorNotFoundException if the connector doesn't exist
     * @throws KafkaConnectException      if the request fails
     */
    public void restartTask(String connectorName, int taskId) {
        String url = baseUrl + "/connectors/" + connectorName + "/tasks/" + taskId + "/restart";
        log.info("Restarting task {} of connector {} at {}", taskId, connectorName, url);

        executePost(url, null);
    }

    /**
     * Check if a connector exists.
     *
     * @param connectorName The name of the connector
     * @return true if the connector exists, false otherwise
     */
    public boolean connectorExists(String connectorName) {
        try {
            getConnector(connectorName);
            return true;
        } catch (ConnectorNotFoundException e) {
            return false;
        }
    }

    /**
     * Wait for a connector to be running.
     *
     * @param connectorName   The name of the connector
     * @param timeoutMs       Timeout in milliseconds
     * @param pollIntervalMs  Poll interval in milliseconds
     * @return true if the connector is running, false if timeout
     * @throws KafkaConnectException if the connector fails
     */
    public boolean waitForConnectorRunning(String connectorName, long timeoutMs, long pollIntervalMs) {
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            try {
                ConnectorStatus status = getConnectorStatus(connectorName);
                if (status.isRunning()) {
                    log.info("Connector {} is running", connectorName);
                    return true;
                }
                if (status.isFailed()) {
                    String errorTrace = status.getErrorTrace();
                    throw new KafkaConnectException("Connector " + connectorName + " failed: " + errorTrace);
                }
                log.debug("Connector {} not yet running, waiting...", connectorName);
                Thread.sleep(pollIntervalMs);
            } catch (ConnectorNotFoundException e) {
                // Connector may not be immediately available after creation; keep waiting
                log.debug("Connector {} not found yet, waiting...", connectorName);
                try {
                    Thread.sleep(pollIntervalMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new KafkaConnectException("Interrupted while waiting for connector", ie);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new KafkaConnectException("Interrupted while waiting for connector", e);
            }
        }
        log.warn("Timeout waiting for connector {} to be running", connectorName);
        return false;
    }

    private String executeGet(String url) {
        HttpGet request = new HttpGet(url);
        return executeRequest(request);
    }

    private String executePost(String url, String body) {
        HttpPost request = new HttpPost(url);
        if (body != null) {
            request.setEntity(new StringEntity(body, ContentType.APPLICATION_JSON));
        }
        return executeRequest(request);
    }

    private String executePut(String url, String body) {
        HttpPut request = new HttpPut(url);
        if (body != null) {
            request.setEntity(new StringEntity(body, ContentType.APPLICATION_JSON));
        }
        return executeRequest(request);
    }

    private void executeDelete(String url) {
        HttpDelete request = new HttpDelete(url);
        executeRequest(request);
    }

    private String executeRequest(org.apache.hc.core5.http.ClassicHttpRequest request) {
        try {
            return httpClient.execute(request, this::handleResponse);
        } catch (IOException e) {
            throw new KafkaConnectException("Failed to execute request: " + request.getRequestUri(), e);
        }
    }

    private String handleResponse(ClassicHttpResponse response) throws IOException {
        int statusCode = response.getCode();
        HttpEntity entity = response.getEntity();
        String body = "";
        if (entity != null) {
            try {
                body = EntityUtils.toString(entity);
            } catch (org.apache.hc.core5.http.ParseException e) {
                throw new IOException("Failed to parse response body", e);
            }
        }

        if (statusCode >= 200 && statusCode < 300) {
            return body;
        }

        if (statusCode == 404) {
            throw new ConnectorNotFoundException("Resource not found");
        }

        if (statusCode == 409) {
            throw new ConnectorAlreadyExistsException("Resource already exists");
        }

        throw new KafkaConnectException("Request failed with status " + statusCode + ": " + body, statusCode);
    }

    @Override
    public void close() throws IOException {
        if (httpClient != null) {
            httpClient.close();
        }
    }
}
