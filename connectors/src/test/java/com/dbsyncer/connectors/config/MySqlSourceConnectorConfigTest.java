package com.dbsyncer.connectors.config;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for MySqlSourceConnectorConfig.
 */
class MySqlSourceConnectorConfigTest {

    @Test
    void shouldBuildValidConfiguration() {
        MySqlSourceConnectorConfig config = MySqlSourceConnectorConfig.builder()
                .databaseHostname("localhost")
                .databasePort(3306)
                .databaseUser("root")
                .databasePassword("password")
                .databaseServerId("1001")
                .topicPrefix("mysql-source")
                .databaseIncludeList(Arrays.asList("testdb"))
                .tableIncludeList(Arrays.asList("testdb.users", "testdb.orders"))
                .build();

        Map<String, String> configMap = config.toConfigMap();

        assertThat(configMap).containsEntry("connector.class", MySqlSourceConnectorConfig.CONNECTOR_CLASS);
        assertThat(configMap).containsEntry("database.hostname", "localhost");
        assertThat(configMap).containsEntry("database.port", "3306");
        assertThat(configMap).containsEntry("database.user", "root");
        assertThat(configMap).containsEntry("database.password", "password");
        assertThat(configMap).containsEntry("database.server.id", "1001");
        assertThat(configMap).containsEntry("topic.prefix", "mysql-source");
        assertThat(configMap).containsEntry("database.include.list", "testdb");
        assertThat(configMap).containsEntry("table.include.list", "testdb.users,testdb.orders");
    }

    @Test
    void shouldApplyDefaultValues() {
        MySqlSourceConnectorConfig config = MySqlSourceConnectorConfig.builder()
                .databaseHostname("localhost")
                .databasePort(3306)
                .databaseUser("root")
                .databasePassword("password")
                .databaseServerId("1001")
                .topicPrefix("mysql-source")
                .build();

        Map<String, String> configMap = config.toConfigMap();

        assertThat(configMap).containsEntry("snapshot.mode", "initial");
        assertThat(configMap).containsEntry("snapshot.locking.mode", "minimal");
        assertThat(configMap).containsEntry("tasks.max", "1");
        assertThat(configMap).containsEntry("decimal.handling.mode", "string");
        assertThat(configMap).containsEntry("tombstones.on.delete", "false");
    }

    @Test
    void shouldValidateMissingHostname() {
        MySqlSourceConnectorConfig config = MySqlSourceConnectorConfig.builder()
                .databasePort(3306)
                .databaseUser("root")
                .databasePassword("password")
                .databaseServerId("1001")
                .topicPrefix("mysql-source")
                .build();

        assertThatThrownBy(config::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Database hostname is required");
    }

    @Test
    void shouldValidateMissingUser() {
        MySqlSourceConnectorConfig config = MySqlSourceConnectorConfig.builder()
                .databaseHostname("localhost")
                .databasePort(3306)
                .databasePassword("password")
                .databaseServerId("1001")
                .topicPrefix("mysql-source")
                .build();

        assertThatThrownBy(config::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Database user is required");
    }

    @Test
    void shouldValidateMissingPassword() {
        MySqlSourceConnectorConfig config = MySqlSourceConnectorConfig.builder()
                .databaseHostname("localhost")
                .databasePort(3306)
                .databaseUser("root")
                .databaseServerId("1001")
                .topicPrefix("mysql-source")
                .build();

        assertThatThrownBy(config::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Database password is required");
    }

    @Test
    void shouldValidateMissingTopicPrefix() {
        MySqlSourceConnectorConfig config = MySqlSourceConnectorConfig.builder()
                .databaseHostname("localhost")
                .databasePort(3306)
                .databaseUser("root")
                .databasePassword("password")
                .databaseServerId("1001")
                .build();

        assertThatThrownBy(config::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Topic prefix is required");
    }

    @Test
    void shouldValidateMissingServerId() {
        MySqlSourceConnectorConfig config = MySqlSourceConnectorConfig.builder()
                .databaseHostname("localhost")
                .databasePort(3306)
                .databaseUser("root")
                .databasePassword("password")
                .topicPrefix("mysql-source")
                .build();

        assertThatThrownBy(config::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Database server ID is required");
    }

    @Test
    void shouldValidateInvalidPort() {
        MySqlSourceConnectorConfig config = MySqlSourceConnectorConfig.builder()
                .databaseHostname("localhost")
                .databasePort(0)
                .databaseUser("root")
                .databasePassword("password")
                .databaseServerId("1001")
                .topicPrefix("mysql-source")
                .build();

        assertThatThrownBy(config::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid database port");
    }

    @Test
    void shouldHandleSnapshotModes() {
        MySqlSourceConnectorConfig config = MySqlSourceConnectorConfig.builder()
                .databaseHostname("localhost")
                .databasePort(3306)
                .databaseUser("root")
                .databasePassword("password")
                .databaseServerId("1001")
                .topicPrefix("mysql-source")
                .snapshotMode(SnapshotMode.NEVER)
                .build();

        Map<String, String> configMap = config.toConfigMap();

        assertThat(configMap).containsEntry("snapshot.mode", "never");
    }

    @Test
    void shouldIncludeSslConfiguration() {
        MySqlSourceConnectorConfig config = MySqlSourceConnectorConfig.builder()
                .databaseHostname("localhost")
                .databasePort(3306)
                .databaseUser("root")
                .databasePassword("password")
                .databaseServerId("1001")
                .topicPrefix("mysql-source")
                .sslMode("required")
                .sslTruststore("/path/to/truststore.jks")
                .sslTruststorePassword("trustpass")
                .build();

        Map<String, String> configMap = config.toConfigMap();

        assertThat(configMap).containsEntry("database.ssl.mode", "required");
        assertThat(configMap).containsEntry("database.ssl.truststore", "/path/to/truststore.jks");
        assertThat(configMap).containsEntry("database.ssl.truststore.password", "trustpass");
    }

    @Test
    void shouldNotIncludeNullOptionalFields() {
        MySqlSourceConnectorConfig config = MySqlSourceConnectorConfig.builder()
                .databaseHostname("localhost")
                .databasePort(3306)
                .databaseUser("root")
                .databasePassword("password")
                .databaseServerId("1001")
                .topicPrefix("mysql-source")
                .build();

        Map<String, String> configMap = config.toConfigMap();

        assertThat(configMap).doesNotContainKey("database.include.list");
        assertThat(configMap).doesNotContainKey("table.include.list");
        assertThat(configMap).doesNotContainKey("database.ssl.mode");
    }

    @Test
    void shouldReturnCorrectConnectorClass() {
        MySqlSourceConnectorConfig config = MySqlSourceConnectorConfig.builder()
                .databaseHostname("localhost")
                .databasePort(3306)
                .databaseUser("root")
                .databasePassword("password")
                .databaseServerId("1001")
                .topicPrefix("mysql-source")
                .build();

        assertThat(config.getConnectorClass()).isEqualTo("io.debezium.connector.mysql.MySqlConnector");
    }
}
