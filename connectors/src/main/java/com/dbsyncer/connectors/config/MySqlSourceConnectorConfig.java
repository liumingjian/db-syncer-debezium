package com.dbsyncer.connectors.config;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Configuration generator for Debezium MySQL Source Connector.
 */
@Getter
@Setter
@Builder
public class MySqlSourceConnectorConfig implements SourceConnectorConfig {

    public static final String CONNECTOR_CLASS = "io.debezium.connector.mysql.MySqlConnector";

    // Required connection settings
    private String databaseHostname;
    private int databasePort;
    private String databaseUser;
    private String databasePassword;

    // Server identification
    private String databaseServerId;
    private String topicPrefix;

    // Table selection
    private List<String> databaseIncludeList;
    private List<String> databaseExcludeList;
    private List<String> tableIncludeList;
    private List<String> tableExcludeList;
    private List<String> columnIncludeList;
    private List<String> columnExcludeList;

    // Snapshot configuration
    @Builder.Default
    private SnapshotMode snapshotMode = SnapshotMode.INITIAL;
    @Builder.Default
    private String snapshotLockingMode = "minimal";
    @Builder.Default
    private int snapshotFetchSize = 10240;
    @Builder.Default
    private long snapshotMaxThreads = 1;

    // Binlog configuration
    private String binlogFile;
    private Long binlogPosition;
    private String gtidSourceIncludes;
    private String gtidSourceExcludes;
    @Builder.Default
    private boolean includeSchemaChanges = true;

    // Schema history storage
    @Builder.Default
    private String schemaHistoryInternal = "io.debezium.storage.kafka.history.KafkaSchemaHistory";
    private String schemaHistoryInternalKafkaBootstrapServers;
    private String schemaHistoryInternalKafkaTopic;

    // Offset storage
    private String offsetStorage;
    private String offsetStorageFileName;
    private String offsetStorageKafkaBootstrapServers;
    private String offsetStorageKafkaTopic;

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
    private String timePrecisionMode = "adaptive_time_microseconds";
    @Builder.Default
    private String bigintUnsignedHandlingMode = "long";
    @Builder.Default
    private boolean tombstonesOnDelete = false;
    @Builder.Default
    private int pollIntervalMs = 500;
    @Builder.Default
    private int maxBatchSize = 2048;
    @Builder.Default
    private int maxQueueSize = 8192;

    // SSL configuration
    private String sslMode;
    private String sslKeystore;
    private String sslKeystorePassword;
    private String sslTruststore;
    private String sslTruststorePassword;

    // Optional transformation injection
    @Builder.Default
    private boolean enableTypeMappingTransform = false;
    private String typeMappingSourceDb; // e.g. "mysql"

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
        if (topicPrefix == null || topicPrefix.isBlank()) {
            throw new IllegalArgumentException("Topic prefix is required");
        }
        if (databaseServerId == null || databaseServerId.isBlank()) {
            throw new IllegalArgumentException("Database server ID is required");
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
        config.put("database.server.id", databaseServerId);
        config.put("topic.prefix", topicPrefix);

        // Table selection
        if (databaseIncludeList != null && !databaseIncludeList.isEmpty()) {
            config.put("database.include.list", String.join(",", databaseIncludeList));
        }
        if (databaseExcludeList != null && !databaseExcludeList.isEmpty()) {
            config.put("database.exclude.list", String.join(",", databaseExcludeList));
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
        config.put("snapshot.max.threads", String.valueOf(snapshotMaxThreads));

        // Binlog configuration
        if (binlogFile != null && !binlogFile.isBlank()) {
            config.put("database.history.skip.unparseable.ddl", "true");
        }
        if (gtidSourceIncludes != null && !gtidSourceIncludes.isBlank()) {
            config.put("gtid.source.includes", gtidSourceIncludes);
        }
        if (gtidSourceExcludes != null && !gtidSourceExcludes.isBlank()) {
            config.put("gtid.source.excludes", gtidSourceExcludes);
        }
        config.put("include.schema.changes", String.valueOf(includeSchemaChanges));

        // Schema history storage
        config.put("schema.history.internal", schemaHistoryInternal);
        if (schemaHistoryInternalKafkaBootstrapServers != null) {
            config.put("schema.history.internal.kafka.bootstrap.servers",
                    schemaHistoryInternalKafkaBootstrapServers);
        }
        if (schemaHistoryInternalKafkaTopic != null) {
            config.put("schema.history.internal.kafka.topic", schemaHistoryInternalKafkaTopic);
        }

        // Offset storage (for embedded connectors)
        if (offsetStorage != null) {
            config.put("offset.storage", offsetStorage);
        }
        if (offsetStorageFileName != null) {
            config.put("offset.storage.file.filename", offsetStorageFileName);
        }
        if (offsetStorageKafkaBootstrapServers != null) {
            config.put("offset.storage.kafka.bootstrap.servers", offsetStorageKafkaBootstrapServers);
        }
        if (offsetStorageKafkaTopic != null) {
            config.put("offset.storage.topic", offsetStorageKafkaTopic);
        }

        // Kafka settings
        config.put("tasks.max", String.valueOf(tasksMax));
        config.put("key.converter", keyConverter);
        config.put("value.converter", valueConverter);
        config.put("key.converter.schemas.enable", String.valueOf(keyConverterSchemasEnable));
        config.put("value.converter.schemas.enable", String.valueOf(valueConverterSchemasEnable));

        // Advanced settings
        config.put("decimal.handling.mode", decimalHandlingMode);
        config.put("time.precision.mode", timePrecisionMode);
        config.put("bigint.unsigned.handling.mode", bigintUnsignedHandlingMode);
        config.put("tombstones.on.delete", String.valueOf(tombstonesOnDelete));
        config.put("poll.interval.ms", String.valueOf(pollIntervalMs));
        config.put("max.batch.size", String.valueOf(maxBatchSize));
        config.put("max.queue.size", String.valueOf(maxQueueSize));

        // SSL configuration
        if (sslMode != null) {
            config.put("database.ssl.mode", sslMode);
        }
        if (sslKeystore != null) {
            config.put("database.ssl.keystore", sslKeystore);
        }
        if (sslKeystorePassword != null) {
            config.put("database.ssl.keystore.password", sslKeystorePassword);
        }
        if (sslTruststore != null) {
            config.put("database.ssl.truststore", sslTruststore);
        }
        if (sslTruststorePassword != null) {
            config.put("database.ssl.truststore.password", sslTruststorePassword);
        }

        // Optional SMT transform to normalize schema/types
        if (enableTypeMappingTransform && typeMappingSourceDb != null && !typeMappingSourceDb.isBlank()) {
            config.put("transforms", "applyTypeMapping");
            config.put("transforms.applyTypeMapping.type",
                    "com.dbsyncer.transformations.smt.ApplyTypeMapping");
            config.put("transforms.applyTypeMapping.source.db", typeMappingSourceDb);
        }

        return config;
    }

    /**
     * Create a builder with sensible defaults for MySQL connector.
     */
    public static MySqlSourceConnectorConfigBuilder builder() {
        return new MySqlSourceConnectorConfigBuilder()
                .databasePort(3306)
                .snapshotMode(SnapshotMode.INITIAL)
                .snapshotLockingMode("minimal")
                .snapshotFetchSize(10240)
                .snapshotMaxThreads(1)
                .includeSchemaChanges(true)
                .tasksMax(1)
                .keyConverter("org.apache.kafka.connect.json.JsonConverter")
                .valueConverter("org.apache.kafka.connect.json.JsonConverter")
                .keyConverterSchemasEnable(false)
                .valueConverterSchemasEnable(false)
                .decimalHandlingMode("string")
                .timePrecisionMode("adaptive_time_microseconds")
                .bigintUnsignedHandlingMode("long")
                .tombstonesOnDelete(false)
                .pollIntervalMs(500)
                .maxBatchSize(2048)
                .maxQueueSize(8192)
                .schemaHistoryInternal("io.debezium.storage.kafka.history.KafkaSchemaHistory");
    }
}
