package com.dbsyncer.connectors.config;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for PostgresSourceConnectorConfig.
 */
class PostgresSourceConnectorConfigTest {

    @Test
    void shouldBuildValidConfiguration() {
        PostgresSourceConnectorConfig config = PostgresSourceConnectorConfig.builder()
                .databaseHostname("localhost")
                .databasePort(5432)
                .databaseUser("postgres")
                .databasePassword("password")
                .databaseDbname("testdb")
                .topicPrefix("postgres-source")
                .slotName("debezium_slot")
                .schemaIncludeList(Arrays.asList("public"))
                .tableIncludeList(Arrays.asList("public.users", "public.orders"))
                .build();

        Map<String, String> configMap = config.toConfigMap();

        assertThat(configMap).containsEntry("connector.class", PostgresSourceConnectorConfig.CONNECTOR_CLASS);
        assertThat(configMap).containsEntry("database.hostname", "localhost");
        assertThat(configMap).containsEntry("database.port", "5432");
        assertThat(configMap).containsEntry("database.user", "postgres");
        assertThat(configMap).containsEntry("database.password", "password");
        assertThat(configMap).containsEntry("database.dbname", "testdb");
        assertThat(configMap).containsEntry("topic.prefix", "postgres-source");
        assertThat(configMap).containsEntry("slot.name", "debezium_slot");
        assertThat(configMap).containsEntry("schema.include.list", "public");
        assertThat(configMap).containsEntry("table.include.list", "public.users,public.orders");
    }

    @Test
    void shouldApplyDefaultValues() {
        PostgresSourceConnectorConfig config = PostgresSourceConnectorConfig.builder()
                .databaseHostname("localhost")
                .databasePort(5432)
                .databaseUser("postgres")
                .databasePassword("password")
                .databaseDbname("testdb")
                .topicPrefix("postgres-source")
                .build();

        Map<String, String> configMap = config.toConfigMap();

        assertThat(configMap).containsEntry("plugin.name", "pgoutput");
        assertThat(configMap).containsEntry("slot.name", "debezium");
        assertThat(configMap).containsEntry("snapshot.mode", "initial");
        assertThat(configMap).containsEntry("snapshot.locking.mode", "none");
        assertThat(configMap).containsEntry("tasks.max", "1");
        assertThat(configMap).containsEntry("decimal.handling.mode", "string");
        assertThat(configMap).containsEntry("hstore.handling.mode", "json");
    }

    @Test
    void shouldValidateMissingHostname() {
        PostgresSourceConnectorConfig config = PostgresSourceConnectorConfig.builder()
                .databasePort(5432)
                .databaseUser("postgres")
                .databasePassword("password")
                .databaseDbname("testdb")
                .topicPrefix("postgres-source")
                .build();

        assertThatThrownBy(config::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Database hostname is required");
    }

    @Test
    void shouldValidateMissingDbname() {
        PostgresSourceConnectorConfig config = PostgresSourceConnectorConfig.builder()
                .databaseHostname("localhost")
                .databasePort(5432)
                .databaseUser("postgres")
                .databasePassword("password")
                .topicPrefix("postgres-source")
                .build();

        assertThatThrownBy(config::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Database name is required");
    }

    @Test
    void shouldHandleDecoderPlugin() {
        PostgresSourceConnectorConfig config = PostgresSourceConnectorConfig.builder()
                .databaseHostname("localhost")
                .databasePort(5432)
                .databaseUser("postgres")
                .databasePassword("password")
                .databaseDbname("testdb")
                .topicPrefix("postgres-source")
                .pluginName(PostgresSourceConnectorConfig.DecoderPlugin.DECODERBUFS)
                .build();

        Map<String, String> configMap = config.toConfigMap();

        assertThat(configMap).containsEntry("plugin.name", "decoderbufs");
    }

    @Test
    void shouldIncludePublicationName() {
        PostgresSourceConnectorConfig config = PostgresSourceConnectorConfig.builder()
                .databaseHostname("localhost")
                .databasePort(5432)
                .databaseUser("postgres")
                .databasePassword("password")
                .databaseDbname("testdb")
                .topicPrefix("postgres-source")
                .publicationName("dbsyncer_publication")
                .build();

        Map<String, String> configMap = config.toConfigMap();

        assertThat(configMap).containsEntry("publication.name", "dbsyncer_publication");
    }

    @Test
    void shouldIncludeHeartbeatConfiguration() {
        PostgresSourceConnectorConfig config = PostgresSourceConnectorConfig.builder()
                .databaseHostname("localhost")
                .databasePort(5432)
                .databaseUser("postgres")
                .databasePassword("password")
                .databaseDbname("testdb")
                .topicPrefix("postgres-source")
                .heartbeatIntervalMs(60000)
                .heartbeatTopicsPrefix("__heartbeat")
                .heartbeatActionQuery("INSERT INTO heartbeat (ts) VALUES (NOW())")
                .build();

        Map<String, String> configMap = config.toConfigMap();

        assertThat(configMap).containsEntry("heartbeat.interval.ms", "60000");
        assertThat(configMap).containsEntry("heartbeat.topics.prefix", "__heartbeat");
        assertThat(configMap).containsEntry("heartbeat.action.query", "INSERT INTO heartbeat (ts) VALUES (NOW())");
    }

    @Test
    void shouldIncludeSslConfiguration() {
        PostgresSourceConnectorConfig config = PostgresSourceConnectorConfig.builder()
                .databaseHostname("localhost")
                .databasePort(5432)
                .databaseUser("postgres")
                .databasePassword("password")
                .databaseDbname("testdb")
                .topicPrefix("postgres-source")
                .sslMode("verify-full")
                .sslRootCert("/path/to/root.crt")
                .sslCert("/path/to/client.crt")
                .sslKey("/path/to/client.key")
                .build();

        Map<String, String> configMap = config.toConfigMap();

        assertThat(configMap).containsEntry("database.sslmode", "verify-full");
        assertThat(configMap).containsEntry("database.sslrootcert", "/path/to/root.crt");
        assertThat(configMap).containsEntry("database.sslcert", "/path/to/client.crt");
        assertThat(configMap).containsEntry("database.sslkey", "/path/to/client.key");
    }

    @Test
    void shouldReturnCorrectConnectorClass() {
        PostgresSourceConnectorConfig config = PostgresSourceConnectorConfig.builder()
                .databaseHostname("localhost")
                .databasePort(5432)
                .databaseUser("postgres")
                .databasePassword("password")
                .databaseDbname("testdb")
                .topicPrefix("postgres-source")
                .build();

        assertThat(config.getConnectorClass()).isEqualTo("io.debezium.connector.postgresql.PostgresConnector");
    }
}
