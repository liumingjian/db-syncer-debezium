package com.dbsyncer.connectors.config;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Configuration generator for Debezium Oracle Source Connector.
 */
@Getter
@Setter
@Builder
public class OracleSourceConnectorConfig implements SourceConnectorConfig {

    public static final String CONNECTOR_CLASS = "io.debezium.connector.oracle.OracleConnector";

    /**
     * Oracle adapter type.
     */
    public enum OracleAdapter {
        LOGMINER("LogMiner"),
        XSTREAM("XStream");

        private final String value;

        OracleAdapter(String value) {
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
    private String databasePdbName;

    // Server identification
    private String topicPrefix;

    // Adapter configuration
    @Builder.Default
    private OracleAdapter databaseConnectionAdapter = OracleAdapter.LOGMINER;

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
    private String snapshotLockingMode = "shared";
    @Builder.Default
    private int snapshotFetchSize = 10240;

    // LogMiner configuration
    @Builder.Default
    private String logMiningStrategy = "redo_log_catalog";
    @Builder.Default
    private boolean logMiningContinuousMine = true;
    @Builder.Default
    private int logMiningBatchSizeMin = 1000;
    @Builder.Default
    private int logMiningBatchSizeMax = 100000;
    @Builder.Default
    private long logMiningSleepTimeMinMs = 0;
    @Builder.Default
    private long logMiningSleepTimeMaxMs = 3000;
    @Builder.Default
    private long logMiningSleepTimeIncrementMs = 200;
    @Builder.Default
    private boolean logMiningArchiveLogOnlyMode = false;
    @Builder.Default
    private int logMiningArchiveDestinationName = 0;

    // SCN configuration
    private Long startScn;
    private String snapshotSelectStatementOverrides;

    // Schema history storage
    @Builder.Default
    private String schemaHistoryInternal = "io.debezium.storage.kafka.history.KafkaSchemaHistory";
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
    private String intervalHandlingMode = "numeric";
    @Builder.Default
    private boolean tombstonesOnDelete = false;
    @Builder.Default
    private int pollIntervalMs = 500;
    @Builder.Default
    private int maxBatchSize = 2048;
    @Builder.Default
    private int maxQueueSize = 8192;

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
        if (databasePdbName != null && !databasePdbName.isBlank()) {
            config.put("database.pdb.name", databasePdbName);
        }
        config.put("topic.prefix", topicPrefix);

        // Adapter configuration
        config.put("database.connection.adapter", databaseConnectionAdapter.getValue());

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

        // LogMiner configuration
        if (databaseConnectionAdapter == OracleAdapter.LOGMINER) {
            config.put("log.mining.strategy", logMiningStrategy);
            config.put("log.mining.continuous.mine", String.valueOf(logMiningContinuousMine));
            config.put("log.mining.batch.size.min", String.valueOf(logMiningBatchSizeMin));
            config.put("log.mining.batch.size.max", String.valueOf(logMiningBatchSizeMax));
            config.put("log.mining.sleep.time.min.ms", String.valueOf(logMiningSleepTimeMinMs));
            config.put("log.mining.sleep.time.max.ms", String.valueOf(logMiningSleepTimeMaxMs));
            config.put("log.mining.sleep.time.increment.ms", String.valueOf(logMiningSleepTimeIncrementMs));
            config.put("log.mining.archive.log.only.mode", String.valueOf(logMiningArchiveLogOnlyMode));
            if (logMiningArchiveDestinationName > 0) {
                config.put("log.mining.archive.destination.name",
                        String.valueOf(logMiningArchiveDestinationName));
            }
        }

        // SCN configuration
        if (startScn != null) {
            config.put("database.out.server.name", String.valueOf(startScn));
        }
        if (snapshotSelectStatementOverrides != null) {
            config.put("snapshot.select.statement.overrides", snapshotSelectStatementOverrides);
        }

        // Schema history storage
        config.put("schema.history.internal", schemaHistoryInternal);
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
        config.put("interval.handling.mode", intervalHandlingMode);
        config.put("tombstones.on.delete", String.valueOf(tombstonesOnDelete));
        config.put("poll.interval.ms", String.valueOf(pollIntervalMs));
        config.put("max.batch.size", String.valueOf(maxBatchSize));
        config.put("max.queue.size", String.valueOf(maxQueueSize));

        return config;
    }

    /**
     * Create a builder with sensible defaults for Oracle connector.
     */
    public static OracleSourceConnectorConfigBuilder builder() {
        return new OracleSourceConnectorConfigBuilder()
                .databasePort(1521)
                .databaseConnectionAdapter(OracleAdapter.LOGMINER)
                .snapshotMode(SnapshotMode.INITIAL)
                .snapshotLockingMode("shared")
                .snapshotFetchSize(10240)
                .logMiningStrategy("redo_log_catalog")
                .logMiningContinuousMine(true)
                .logMiningBatchSizeMin(1000)
                .logMiningBatchSizeMax(100000)
                .logMiningSleepTimeMinMs(0)
                .logMiningSleepTimeMaxMs(3000)
                .logMiningSleepTimeIncrementMs(200)
                .logMiningArchiveLogOnlyMode(false)
                .logMiningArchiveDestinationName(0)
                .tasksMax(1)
                .keyConverter("org.apache.kafka.connect.json.JsonConverter")
                .valueConverter("org.apache.kafka.connect.json.JsonConverter")
                .keyConverterSchemasEnable(false)
                .valueConverterSchemasEnable(false)
                .decimalHandlingMode("string")
                .intervalHandlingMode("numeric")
                .tombstonesOnDelete(false)
                .pollIntervalMs(500)
                .maxBatchSize(2048)
                .maxQueueSize(8192)
                .schemaHistoryInternal("io.debezium.storage.kafka.history.KafkaSchemaHistory");
    }
}
