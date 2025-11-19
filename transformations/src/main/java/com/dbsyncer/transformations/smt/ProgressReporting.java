package com.dbsyncer.transformations.smt;

import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.connect.connector.ConnectRecord;
import org.apache.kafka.connect.transforms.Transformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SMT that reports per-table streaming progress back to the metadata-service.
 * <p>
 * It batches processed record counts per (taskId, sourceSchema, sourceTable)
 * and periodically posts aggregated updates to a lightweight HTTP endpoint.
 * <p>
 * Configuration (all prefixed by {@code transforms.<alias>.} in connector config):
 * <ul>
 *     <li>{@code task.id} (required): Migration task UUID.</li>
 *     <li>{@code metadata.service.url} (optional): Base URL of metadata-service
 *         (default {@code http://metadata-service:8080}).</li>
 *     <li>{@code batch.size} (optional): Max records per flush, default 100.</li>
 *     <li>{@code flush.interval.ms} (optional): Max interval between flushes, default 2000.</li>
 * </ul>
 */
public class ProgressReporting<R extends ConnectRecord<R>> implements Transformation<R> {

    private static final Logger log = LoggerFactory.getLogger(ProgressReporting.class);

    public static final String CONFIG_TASK_ID = "task.id";
    public static final String CONFIG_METADATA_URL = "metadata.service.url";
    public static final String CONFIG_BATCH_SIZE = "batch.size";
    public static final String CONFIG_FLUSH_INTERVAL_MS = "flush.interval.ms";

    private String taskId;
    private String metadataServiceUrl;
    private int batchSize = 100;
    private long flushIntervalMs = 2000L;

    private HttpClient httpClient;
    private final Map<TableKey, Aggregate> aggregates = new ConcurrentHashMap<>();

    @Override
    public R apply(R record) {
        if (record == null || record.value() == null) {
            return record;
        }
        if (taskId == null || taskId.isBlank()) {
            // Misconfigured; skip reporting but do not break the pipeline.
            return record;
        }
        if (metadataServiceUrl == null || metadataServiceUrl.isBlank()) {
            return record;
        }

        String topic = record.topic();
        TableKey key = parseKey(topic);
        if (key == null) {
            return record;
        }

        long eventTs = record.timestamp() != null ? record.timestamp() : System.currentTimeMillis();
        long now = System.currentTimeMillis();
        long lagMs = Math.max(0L, now - eventTs);

        Aggregate agg = aggregates.computeIfAbsent(key, k -> new Aggregate(now));
        agg.add(1L, eventTs, lagMs, now);

        if (agg.shouldFlush(batchSize, flushIntervalMs, now)) {
            flush(key, agg);
        }

        return record;
    }

    @Override
    public ConfigDef config() {
        return new ConfigDef()
                .define(CONFIG_TASK_ID, ConfigDef.Type.STRING, ConfigDef.NO_DEFAULT_VALUE, ConfigDef.Importance.HIGH,
                        "Migration task UUID for which progress is reported.")
                .define(CONFIG_METADATA_URL, ConfigDef.Type.STRING, "http://metadata-service:8080",
                        ConfigDef.Importance.MEDIUM,
                        "Base URL of metadata-service (e.g. http://metadata-service:8080).")
                .define(CONFIG_BATCH_SIZE, ConfigDef.Type.INT, 100, ConfigDef.Importance.LOW,
                        "Maximum number of processed records to batch before sending a progress update.")
                .define(CONFIG_FLUSH_INTERVAL_MS, ConfigDef.Type.LONG, 2000L, ConfigDef.Importance.LOW,
                        "Maximum interval in milliseconds between progress updates.");
    }

    @Override
    public void close() {
        long now = System.currentTimeMillis();
        for (Map.Entry<TableKey, Aggregate> entry : aggregates.entrySet()) {
            flush(entry.getKey(), entry.getValue(), now);
        }
        aggregates.clear();
    }

    @Override
    public void configure(Map<String, ?> configs) {
        Object taskIdCfg = configs.get(CONFIG_TASK_ID);
        if (taskIdCfg == null || taskIdCfg.toString().isBlank()) {
            throw new IllegalArgumentException(CONFIG_TASK_ID + " is required for ProgressReporting SMT");
        }
        this.taskId = taskIdCfg.toString();

        Object urlCfg = configs.get(CONFIG_METADATA_URL);
        this.metadataServiceUrl = urlCfg != null && !urlCfg.toString().isBlank()
                ? normalizeBaseUrl(urlCfg.toString())
                : "http://metadata-service:8080";

        Object batchCfg = configs.get(CONFIG_BATCH_SIZE);
        if (batchCfg != null) {
            try {
                this.batchSize = Integer.parseInt(batchCfg.toString());
            } catch (NumberFormatException e) {
                log.warn("Invalid {} value '{}', falling back to default {}", CONFIG_BATCH_SIZE, batchCfg, batchSize);
            }
        }

        Object flushCfg = configs.get(CONFIG_FLUSH_INTERVAL_MS);
        if (flushCfg != null) {
            try {
                this.flushIntervalMs = Long.parseLong(flushCfg.toString());
            } catch (NumberFormatException e) {
                log.warn("Invalid {} value '{}', falling back to default {}", CONFIG_FLUSH_INTERVAL_MS, flushCfg, flushIntervalMs);
            }
        }

        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
    }

    private void flush(TableKey key, Aggregate agg) {
        long now = System.currentTimeMillis();
        flush(key, agg, now);
    }

    private void flush(TableKey key, Aggregate agg, long now) {
        long delta;
        long lastEventTs;
        long lagMs;

        synchronized (agg) {
            if (agg.pendingCount == 0) {
                return;
            }
            delta = agg.pendingCount;
            lastEventTs = agg.lastEventTimestamp;
            lagMs = agg.lastLagMs;
            agg.pendingCount = 0;
            agg.lastFlushTime = now;
        }

        sendUpdate(key, delta, lastEventTs, lagMs);
    }

    private void sendUpdate(TableKey key, long processedDelta, long eventTimestamp, long lagMs) {
        try {
            String url = metadataServiceUrl + "/api/v1/tasks/" + taskId + "/progress/streaming";
            String body = buildJsonBody(key, processedDelta, eventTimestamp, lagMs);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(5))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            httpClient.sendAsync(request, HttpResponse.BodyHandlers.discarding())
                    .whenComplete((resp, ex) -> {
                        if (ex != null) {
                            log.debug("ProgressReporting HTTP error for task {} table {}.{}: {}",
                                    taskId, key.sourceSchema, key.sourceTable, ex.getMessage());
                        } else if (resp.statusCode() / 100 != 2) {
                            log.debug("ProgressReporting non-2xx status for task {} table {}.{}: {}",
                                    taskId, key.sourceSchema, key.sourceTable, resp.statusCode());
                        }
                    });
        } catch (Exception e) {
            log.debug("ProgressReporting failed to send update for task {} table {}.{}: {}",
                    taskId, key.sourceSchema, key.sourceTable, e.getMessage());
        }
    }

    private String buildJsonBody(TableKey key, long processedDelta, long eventTimestamp, long lagMs) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"sourceSchema\":").append(toJsonString(key.sourceSchema)).append(",");
        sb.append("\"sourceTable\":").append(toJsonString(key.sourceTable)).append(",");
        sb.append("\"processedDelta\":").append(processedDelta).append(",");
        sb.append("\"eventTimestamp\":").append(eventTimestamp).append(",");
        sb.append("\"lagMs\":").append(lagMs);
        sb.append("}");
        return sb.toString();
    }

    private TableKey parseKey(String topic) {
        if (topic == null || topic.isBlank()) {
            return null;
        }
        String[] parts = topic.split("\\.");
        if (parts.length < 2) {
            return null;
        }
        String sourceTable = parts[parts.length - 1];
        String sourceSchema = parts.length >= 3 ? parts[parts.length - 2] : null;
        return new TableKey(taskId, sourceSchema, sourceTable);
    }

    private static String toJsonString(String value) {
        if (value == null) {
            return "null";
        }
        String escaped = value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
        return "\"" + escaped + "\"";
    }

    private static String normalizeBaseUrl(String url) {
        String trimmed = url.trim();
        if (trimmed.endsWith("/")) {
            return trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    private static final class TableKey {
        private final String taskId;
        private final String sourceSchema;
        private final String sourceTable;

        private TableKey(String taskId, String sourceSchema, String sourceTable) {
            this.taskId = taskId;
            this.sourceSchema = sourceSchema;
            this.sourceTable = sourceTable;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TableKey tableKey = (TableKey) o;
            return Objects.equals(taskId, tableKey.taskId)
                    && Objects.equals(sourceSchema, tableKey.sourceSchema)
                    && Objects.equals(sourceTable, tableKey.sourceTable);
        }

        @Override
        public int hashCode() {
            return Objects.hash(taskId, sourceSchema, sourceTable);
        }
    }

    private static final class Aggregate {
        private long pendingCount;
        private long lastEventTimestamp;
        private long lastLagMs;
        private long lastFlushTime;

        private Aggregate(long now) {
            this.lastFlushTime = now;
        }

        private void add(long delta, long eventTimestamp, long lagMs, long now) {
            synchronized (this) {
                this.pendingCount += delta;
                this.lastEventTimestamp = eventTimestamp;
                this.lastLagMs = lagMs;
                if (lastFlushTime == 0L) {
                    lastFlushTime = now;
                }
            }
        }

        private boolean shouldFlush(int batchSize, long flushIntervalMs, long now) {
            synchronized (this) {
                if (pendingCount >= batchSize) {
                    return true;
                }
                long elapsed = now - lastFlushTime;
                return elapsed >= flushIntervalMs && pendingCount > 0;
            }
        }
    }
}

