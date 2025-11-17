package com.dbsyncer.connectors.config;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Configuration generator for Debezium PostgreSQL Source Connector.
 */
@Getter
@Setter
@Builder
public class PostgresSourceConnectorConfig implements SourceConnectorConfig {

    public static final String CONNECTOR_CLASS = "io.debezium.connector.postgresql.PostgresConnector";

    /**
     * PostgreSQL plugin type for logical decoding.
     */
    public enum DecoderPlugin {
        PGOUTPUT("pgoutput"),
        DECODERBUFS("decoderbufs");

        private final String value;

        DecoderPlugin(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    // Required connection settings
    private String databaseHostname;
    private int databasePort;
    private String databaseUser;
    private String databasePassword;
    private String databaseDbname;

    // Server identification
    private String topicPrefix;

    // Replication configuration
    @Builder.Default
    private DecoderPlugin pluginName = DecoderPlugin.PGOUTPUT;
    @Builder.Default
    private String slotName = "debezium";
    private String publicationName;
    @Builder.Default
    private boolean slotDropOnStop = false;
    @Builder.Default
    private String publicationAutocreateMode = "filtered";

    // Table selection
    private List<String> schemaIncludeList;
    private List<String> schemaExcludeList;
    private List<String> tableIncludeList;
    private List<String> tableExcludeList;
    private List<String> columnIncludeList;
    private List<String> columnExcludeList;

    // Snapshot configuration
    @Builder.Default
    private SnapshotMode snapshotMode = SnapshotMode.INITIAL;
    @Builder.Default
    private String snapshotLockingMode = "none";
    @Builder.Default
    private int snapshotFetchSize = 10240;

    // Heartbeat configuration
    @Builder.Default
    private int heartbeatIntervalMs = 0;
    private String heartbeatTopicsPrefix;
    private String heartbeatActionQuery;

    // Schema history storage (PostgreSQL doesn't need schema history by default)
    private String schemaHistoryInternal;
    private String schemaHistoryInternalKafkaBootstrapServers;
    private String schemaHistoryInternalKafkaTopic;

    // Kafka settings
    @Builder.Default
    private int tasksMax = 1;
    @Builder.Default
    private String keyConverter = "org.apache.kafka.connect.json.JsonConverter";
    @Builder.Default
    private String valueConverter = "org.apache.kafka.connect.json.JsonConverter";
    @Builder.Default
    private boolean keyConverterSchemasEnable = false;
    @Builder.Default
    private boolean valueConverterSchemasEnable = false;

    // Advanced settings
    @Builder.Default
    private String decimalHandlingMode = "string";
    @Builder.Default
    private String hstoreHandlingMode = "json";
    @Builder.Default
    private String intervalHandlingMode = "numeric";
    @Builder.Default
    private boolean tombstonesOnDelete = false;
    @Builder.Default
    private int pollIntervalMs = 500;
    @Builder.Default
    private int maxBatchSize = 2048;
    @Builder.Default
    private int maxQueueSize = 8192;
    @Builder.Default
    private boolean includeUnknownDatatypes = false;

    // SSL configuration
    private String sslMode;
    private String sslRootCert;
    private String sslCert;
    private String sslKey;
    private String sslPassword;

    @Override
    public String getConnectorClass() {
        return CONNECTOR_CLASS;
    }

    @Override
    public void validate() {
        if (databaseHostname == null || databaseHostname.isBlank()) {
            throw new IllegalArgumentException("Database hostname is required");
        }
        if (databasePort <= 0 || databasePort > 65535) {
            throw new IllegalArgumentException("Invalid database port: " + databasePort);
        }
        if (databaseUser == null || databaseUser.isBlank()) {
            throw new IllegalArgumentException("Database user is required");
        }
        if (databasePassword == null || databasePassword.isBlank()) {
            throw new IllegalArgumentException("Database password is required");
        }
        if (databaseDbname == null || databaseDbname.isBlank()) {
            throw new IllegalArgumentException("Database name is required");
        }
        if (topicPrefix == null || topicPrefix.isBlank()) {
            throw new IllegalArgumentException("Topic prefix is required");
        }
        if (slotName == null || slotName.isBlank()) {
            throw new IllegalArgumentException("Slot name is required");
        }
    }

    @Override
    public Map<String, String> toConfigMap() {
        validate();

        Map<String, String> config = new HashMap<>();

        // Connector class
        config.put("connector.class", CONNECTOR_CLASS);

        // Required connection settings
        config.put("database.hostname", databaseHostname);
        config.put("database.port", String.valueOf(databasePort));
        config.put("database.user", databaseUser);
        config.put("database.password", databasePassword);
        config.put("database.dbname", databaseDbname);
        config.put("topic.prefix", topicPrefix);

        // Replication configuration
        config.put("plugin.name", pluginName.getValue());
        config.put("slot.name", slotName);
        if (publicationName != null && !publicationName.isBlank()) {
            config.put("publication.name", publicationName);
        }
        config.put("slot.drop.on.stop", String.valueOf(slotDropOnStop));
        config.put("publication.autocreate.mode", publicationAutocreateMode);

        // Table selection
        if (schemaIncludeList != null && !schemaIncludeList.isEmpty()) {
            config.put("schema.include.list", String.join(",", schemaIncludeList));
        }
        if (schemaExcludeList != null && !schemaExcludeList.isEmpty()) {
            config.put("schema.exclude.list", String.join(",", schemaExcludeList));
        }
        if (tableIncludeList != null && !tableIncludeList.isEmpty()) {
            config.put("table.include.list", String.join(",", tableIncludeList));
        }
        if (tableExcludeList != null && !tableExcludeList.isEmpty()) {
            config.put("table.exclude.list", String.join(",", tableExcludeList));
        }
        if (columnIncludeList != null && !columnIncludeList.isEmpty()) {
            config.put("column.include.list", String.join(",", columnIncludeList));
        }
        if (columnExcludeList != null && !columnExcludeList.isEmpty()) {
            config.put("column.exclude.list", String.join(",", columnExcludeList));
        }

        // Snapshot configuration
        config.put("snapshot.mode", snapshotMode.getValue());
        config.put("snapshot.locking.mode", snapshotLockingMode);
        config.put("snapshot.fetch.size", String.valueOf(snapshotFetchSize));

        // Heartbeat configuration
        if (heartbeatIntervalMs > 0) {
            config.put("heartbeat.interval.ms", String.valueOf(heartbeatIntervalMs));
        }
        if (heartbeatTopicsPrefix != null && !heartbeatTopicsPrefix.isBlank()) {
            config.put("heartbeat.topics.prefix", heartbeatTopicsPrefix);
        }
        if (heartbeatActionQuery != null && !heartbeatActionQuery.isBlank()) {
            config.put("heartbeat.action.query", heartbeatActionQuery);
        }

        // Schema history storage (optional for PostgreSQL)
        if (schemaHistoryInternal != null) {
            config.put("schema.history.internal", schemaHistoryInternal);
        }
        if (schemaHistoryInternalKafkaBootstrapServers != null) {
            config.put("schema.history.internal.kafka.bootstrap.servers",
                    schemaHistoryInternalKafkaBootstrapServers);
        }
        if (schemaHistoryInternalKafkaTopic != null) {
            config.put("schema.history.internal.kafka.topic", schemaHistoryInternalKafkaTopic);
        }

        // Kafka settings
        config.put("tasks.max", String.valueOf(tasksMax));
        config.put("key.converter", keyConverter);
        config.put("value.converter", valueConverter);
        config.put("key.converter.schemas.enable", String.valueOf(keyConverterSchemasEnable));
        config.put("value.converter.schemas.enable", String.valueOf(valueConverterSchemasEnable));

        // Advanced settings
        config.put("decimal.handling.mode", decimalHandlingMode);
        config.put("hstore.handling.mode", hstoreHandlingMode);
        config.put("interval.handling.mode", intervalHandlingMode);
        config.put("tombstones.on.delete", String.valueOf(tombstonesOnDelete));
        config.put("poll.interval.ms", String.valueOf(pollIntervalMs));
        config.put("max.batch.size", String.valueOf(maxBatchSize));
        config.put("max.queue.size", String.valueOf(maxQueueSize));
        config.put("include.unknown.datatypes", String.valueOf(includeUnknownDatatypes));

        // SSL configuration
        if (sslMode != null) {
            config.put("database.sslmode", sslMode);
        }
        if (sslRootCert != null) {
            config.put("database.sslrootcert", sslRootCert);
        }
        if (sslCert != null) {
            config.put("database.sslcert", sslCert);
        }
        if (sslKey != null) {
            config.put("database.sslkey", sslKey);
        }
        if (sslPassword != null) {
            config.put("database.sslpassword", sslPassword);
        }

        return config;
    }

    /**
     * Create a builder with sensible defaults for PostgreSQL connector.
     */
    public static PostgresSourceConnectorConfigBuilder builder() {
        return new PostgresSourceConnectorConfigBuilder()
                .databasePort(5432)
                .pluginName(DecoderPlugin.PGOUTPUT)
                .slotName("debezium")
                .slotDropOnStop(false)
                .publicationAutocreateMode("filtered")
                .snapshotMode(SnapshotMode.INITIAL)
                .snapshotLockingMode("none")
                .snapshotFetchSize(10240)
                .heartbeatIntervalMs(0)
                .tasksMax(1)
                .keyConverter("org.apache.kafka.connect.json.JsonConverter")
                .valueConverter("org.apache.kafka.connect.json.JsonConverter")
                .keyConverterSchemasEnable(false)
                .valueConverterSchemasEnable(false)
                .decimalHandlingMode("string")
                .hstoreHandlingMode("json")
                .intervalHandlingMode("numeric")
                .tombstonesOnDelete(false)
                .pollIntervalMs(500)
                .maxBatchSize(2048)
                .maxQueueSize(8192)
                .includeUnknownDatatypes(false);
    }
}
