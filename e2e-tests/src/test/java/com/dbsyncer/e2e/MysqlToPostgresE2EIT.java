package com.dbsyncer.e2e;

import com.dbsyncer.metadata.MetadataServiceApplication;
import com.dbsyncer.metadata.dto.TaskCreateRequest;
import com.dbsyncer.metadata.dto.TaskResponse;
import com.dbsyncer.metadata.entity.DatabaseType;
import com.dbsyncer.metadata.entity.TaskStatus;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.util.UriComponentsBuilder;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end skeleton test for MySQL -> PostgreSQL CDC pipeline.
 *
 * This test is intentionally marked as {@link Disabled} and focuses on
 * providing a starting point for a full E2E scenario:
 *
 * 1) Start MySQL (source), PostgreSQL (metadata + target), Kafka, Kafka Connect via Testcontainers.
 * 2) Start MetadataServiceApplication on a random port and point it to the metadata PostgreSQL and Connect REST.
 * 3) Use REST API (or CLI) to:
 *    - Create a migration task (MySQL -> PostgreSQL).
 *    - Start the task and wait for it to reach RUNNING state.
 * 4) Insert test data into the source MySQL.
 * 5) Assert that corresponding data appears in the target PostgreSQL table.
 *
 * Once the full flow is implemented and verified in a suitable environment
 * with Docker available, the @Disabled annotation can be removed.
 */
@Testcontainers
@SpringBootTest(
        classes = MetadataServiceApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@Disabled("E2E skeleton - requires Docker and full pipeline wiring")
class MysqlToPostgresE2EIT {

    private static final Network NETWORK = Network.newNetwork();

    @Container
    static MySQLContainer<?> mysqlSource = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("source_db")
            .withUsername("dbuser")
            .withPassword("dbpass")
            // Align with docker-compose MySQL settings used for Debezium
            .withCommand(
                    "--server-id=1",
                    "--log-bin=mysql-bin",
                    "--binlog-format=ROW",
                    "--binlog-row-image=FULL",
                    "--gtid-mode=ON",
                    "--enforce-gtid-consistency=ON"
            )
            .withNetwork(NETWORK)
            .withNetworkAliases("mysql-source");

    @Container
    static PostgreSQLContainer<?> metadataPostgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("dbsyncer_metadata")
            .withUsername("dbsyncer")
            .withPassword("dbsyncer_pass")
            .withNetwork(NETWORK)
            .withNetworkAliases("metadata-postgres");

    @Container
    static PostgreSQLContainer<?> targetPostgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("target_db")
            .withUsername("targetuser")
            .withPassword("targetpass")
            .withNetwork(NETWORK)
            .withNetworkAliases("postgres-target");

    @Container
    static KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.6.0"))
            .withNetwork(NETWORK)
            .withNetworkAliases("kafka");

    @Container
    static GenericContainer<?> connect = new GenericContainer<>(
            DockerImageName.parse("debezium/connect:2.6"))
            .withExposedPorts(8083)
            .withNetwork(NETWORK)
            .withNetworkAliases("kafka-connect")
            // Align with docker/docker-compose.yml Kafka Connect settings
            .withEnv("BOOTSTRAP_SERVERS", "kafka:9092")
            .withEnv("GROUP_ID", "db-syncer-connect-cluster")
            .withEnv("CONFIG_STORAGE_TOPIC", "db-syncer-connect-configs")
            .withEnv("OFFSET_STORAGE_TOPIC", "db-syncer-connect-offsets")
            .withEnv("STATUS_STORAGE_TOPIC", "db-syncer-connect-status")
            .withEnv("CONFIG_STORAGE_REPLICATION_FACTOR", "1")
            .withEnv("OFFSET_STORAGE_REPLICATION_FACTOR", "1")
            .withEnv("STATUS_STORAGE_REPLICATION_FACTOR", "1")
            .withEnv("KEY_CONVERTER", "org.apache.kafka.connect.json.JsonConverter")
            .withEnv("VALUE_CONVERTER", "org.apache.kafka.connect.json.JsonConverter")
            .withEnv("CONNECT_KEY_CONVERTER_SCHEMAS_ENABLE", "false")
            .withEnv("CONNECT_VALUE_CONVERTER_SCHEMAS_ENABLE", "false");

    @LocalServerPort
    private int metadataServicePort;

    @Autowired
    private TestRestTemplate restTemplate;

    @DynamicPropertySource
    static void overrideMetadataServiceProperties(DynamicPropertyRegistry registry) {
        // Metadata PostgreSQL configuration
        registry.add("DB_HOST", metadataPostgres::getHost);
        registry.add("DB_PORT", () -> metadataPostgres.getMappedPort(5432));
        registry.add("DB_NAME", metadataPostgres::getDatabaseName);
        registry.add("DB_USERNAME", metadataPostgres::getUsername);
        registry.add("DB_PASSWORD", metadataPostgres::getPassword);

        // Kafka Connect REST URL (inside Testcontainers network)
        registry.add("CONNECT_REST_URL", () ->
                String.format("http://%s:%d", connect.getHost(), connect.getMappedPort(8083)));
    }

    @Test
    void endToEndSync_mysqlToPostgres_skeleton() {
        // NOTE: This method provides a concrete E2E flow outline.
        // It is still marked @Disabled at class level to avoid
        // failures on environments without Docker / full wiring.

        // 1) Initialize source schema and seed data
        initializeSourceSchema();

        // 2) Initialize target schema (optional if Sink auto-creates)
        initializeTargetSchema();

        // 3) Create migration task via REST
        TaskResponse task = createMigrationTask("e2e-mysql-to-pg");

        // 4) Start task
        startMigrationTask(task.getId());

        // 5) Insert additional rows into source after task started
        insertAdditionalSourceData();

        // 6) Wait and verify data appears in target
        awaitTargetRowCount("customers", 2);
    }

    private void initializeSourceSchema() {
        String url = mysqlSource.getJdbcUrl();
        try (Connection conn = DriverManager.getConnection(url, mysqlSource.getUsername(),
                mysqlSource.getPassword());
             Statement stmt = conn.createStatement()) {
            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS customers (
                        id INT PRIMARY KEY AUTO_INCREMENT,
                        name VARCHAR(100),
                        email VARCHAR(100),
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    )
                    """);
            stmt.executeUpdate("""
                    INSERT INTO customers(name, email)
                    VALUES ('Alice', 'alice@example.com')
                    """);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize source schema", e);
        }
    }

    private void initializeTargetSchema() {
        // In many setups the JDBC sink will auto-create tables.
        // This method keeps a placeholder for explicit target DDL if needed.
        // For now, we do nothing and rely on the connector behavior.
    }

    private TaskResponse createMigrationTask(String taskName) {
        TaskCreateRequest request = new TaskCreateRequest();
        request.setTaskName(taskName);
        request.setDescription("E2E MySQL -> Postgres test task");

        // Source configuration (MySQL)
        request.setSourceType(DatabaseType.MYSQL);
        request.setSourceHost("mysql-source");
        request.setSourcePort(3306);
        request.setSourceDatabase("source_db");
        request.setSourceUsername("dbuser");
        request.setSourcePassword("dbpass");

        // Target configuration (PostgreSQL)
        request.setTargetType(DatabaseType.POSTGRESQL);
        request.setTargetHost("postgres-target");
        request.setTargetPort(5432);
        request.setTargetDatabase("target_db");
        request.setTargetUsername("targetuser");
        request.setTargetPassword("targetpass");

        // Basic task properties and metadata
        request.setIncludeTables(List.of("customers"));
        request.setSnapshotMode("initial");
        request.setIncrementalSnapshot(false);
        request.setParallelTables(1);
        request.setCreatedBy("e2e-test");
        request.setTags(List.of("e2e", "mysql-to-pg"));

        String url = UriComponentsBuilder
                .fromHttpUrl("http://localhost")
                .port(metadataServicePort)
                .path("/api/v1/tasks")
                .toUriString();

        TaskResponse response = restTemplate.postForObject(url, request, TaskResponse.class);
        assertThat(response).as("task create response").isNotNull();
        assertThat(response.getId()).as("task id").isNotNull();
        return response;
    }

    private void startMigrationTask(UUID taskId) {
        String url = UriComponentsBuilder
                .fromHttpUrl("http://localhost")
                .port(metadataServicePort)
                .path("/api/v1/tasks/{id}/start")
                .buildAndExpand(taskId)
                .toUriString();

        TaskResponse response = restTemplate.postForObject(url, null, TaskResponse.class);
        assertThat(response).as("task start response").isNotNull();

        // Optionally poll until task status becomes RUNNING
        long deadline = System.nanoTime() + Duration.ofMinutes(2).toNanos();
        while (System.nanoTime() < deadline) {
            TaskResponse current = getTask(taskId);
            if (current.getStatus() == TaskStatus.RUNNING) {
                return;
            }
            try {
                Thread.sleep(2000);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        throw new IllegalStateException("Task did not enter RUNNING state within timeout");
    }

    private TaskResponse getTask(UUID taskId) {
        String url = UriComponentsBuilder
                .fromHttpUrl("http://localhost")
                .port(metadataServicePort)
                .path("/api/v1/tasks/{id}")
                .buildAndExpand(taskId)
                .toUriString();
        return restTemplate.getForObject(url, TaskResponse.class);
    }

    private void insertAdditionalSourceData() {
        String url = mysqlSource.getJdbcUrl();
        try (Connection conn = DriverManager.getConnection(url, mysqlSource.getUsername(),
                mysqlSource.getPassword());
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("""
                    INSERT INTO customers(name, email)
                    VALUES ('Bob', 'bob@example.com')
                    """);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to insert additional source data", e);
        }
    }

    private void awaitTargetRowCount(String tableName, int expectedRows) {
        String url = targetPostgres.getJdbcUrl();
        long deadline = System.nanoTime() + Duration.ofMinutes(5).toNanos();
        int lastCount = -1;
        while (System.nanoTime() < deadline) {
            try (Connection conn = DriverManager.getConnection(url, targetPostgres.getUsername(),
                    targetPostgres.getPassword());
                 Statement stmt = conn.createStatement()) {
                ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM " + tableName);
                if (rs.next()) {
                    lastCount = rs.getInt(1);
                    if (lastCount >= expectedRows) {
                        return;
                    }
                }
            } catch (Exception e) {
                // Table may not exist yet or other transient errors; retry until deadline
            }
            try {
                Thread.sleep(3000);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        throw new IllegalStateException("Expected at least " + expectedRows
                + " rows in target table '" + tableName + "', but last observed count was " + lastCount);
    }
}
