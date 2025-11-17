package com.dbsyncer.connectors.storage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.debezium.config.Configuration;
import io.debezium.config.Field;
import io.debezium.document.DocumentReader;
import io.debezium.document.DocumentWriter;
import io.debezium.relational.history.AbstractSchemaHistory;
import io.debezium.relational.history.HistoryRecord;
import io.debezium.relational.history.HistoryRecordComparator;
import io.debezium.relational.history.SchemaHistoryException;
import io.debezium.relational.history.SchemaHistoryListener;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * A JDBC-based implementation of SchemaHistory that stores DDL history in PostgreSQL.
 * This allows schema history to be persisted in a database instead of Kafka topics.
 */
@Slf4j
public class JdbcSchemaHistory extends AbstractSchemaHistory {

    public static final Field JDBC_URL = Field.create("schema.history.internal.jdbc.url")
            .withDisplayName("JDBC URL")
            .withDescription("JDBC URL for the schema history database")
            .required();

    public static final Field JDBC_USER = Field.create("schema.history.internal.jdbc.user")
            .withDisplayName("JDBC User")
            .withDescription("JDBC username for the schema history database");

    public static final Field JDBC_PASSWORD = Field.create("schema.history.internal.jdbc.password")
            .withDisplayName("JDBC Password")
            .withDescription("JDBC password for the schema history database");

    public static final Field TABLE_NAME = Field.create("schema.history.internal.jdbc.table")
            .withDisplayName("Table Name")
            .withDescription("Name of the schema history table")
            .withDefault("debezium_schema_history");

    public static final Field CONNECTOR_ID = Field.create("schema.history.internal.jdbc.connector.id")
            .withDisplayName("Connector ID")
            .withDescription("Unique identifier for this connector instance");

    private HikariDataSource dataSource;
    private String tableName;
    private String connectorId;
    private final DocumentWriter writer = DocumentWriter.defaultWriter();
    private final DocumentReader reader = DocumentReader.defaultReader();
    private final AtomicBoolean running = new AtomicBoolean(false);

    // SQL statements
    private String insertSql;
    private String selectSql;
    private String existsSql;

    @Override
    public void configure(Configuration config, HistoryRecordComparator comparator,
                          SchemaHistoryListener listener, boolean useCatalogBeforeSchema) {
        super.configure(config, comparator, listener, useCatalogBeforeSchema);

        String jdbcUrl = config.getString(JDBC_URL);
        String jdbcUser = config.getString(JDBC_USER);
        String jdbcPassword = config.getString(JDBC_PASSWORD);
        this.tableName = config.getString(TABLE_NAME);
        this.connectorId = config.getString(CONNECTOR_ID);

        if (this.connectorId == null || this.connectorId.isBlank()) {
            this.connectorId = config.getString("name");
            if (this.connectorId == null || this.connectorId.isBlank()) {
                this.connectorId = UUID.randomUUID().toString();
            }
        }

        if (jdbcUrl == null || jdbcUrl.isBlank()) {
            throw new SchemaHistoryException("schema.history.internal.jdbc.url is required");
        }

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(jdbcUrl);
        if (jdbcUser != null) {
            hikariConfig.setUsername(jdbcUser);
        }
        if (jdbcPassword != null) {
            hikariConfig.setPassword(jdbcPassword);
        }
        hikariConfig.setPoolName("JdbcSchemaHistory");
        hikariConfig.setMaximumPoolSize(3);
        hikariConfig.setMinimumIdle(1);
        hikariConfig.setConnectionTimeout(30000);
        hikariConfig.setIdleTimeout(600000);
        hikariConfig.setMaxLifetime(1800000);

        this.dataSource = new HikariDataSource(hikariConfig);

        // Prepare SQL statements
        this.insertSql = String.format(
                "INSERT INTO %s (id, connector_id, history_data, created_at) VALUES (?, ?, ?, ?)",
                tableName);

        this.selectSql = String.format(
                "SELECT history_data FROM %s WHERE connector_id = ? ORDER BY id ASC",
                tableName);

        this.existsSql = String.format(
                "SELECT EXISTS(SELECT 1 FROM %s WHERE connector_id = ?)",
                tableName);

        log.info("Configured JdbcSchemaHistory with table: {}, connectorId: {}", tableName, connectorId);
    }

    @Override
    public void start() {
        super.start();
        log.info("Starting JdbcSchemaHistory");
        ensureTableExists();
        running.set(true);
    }

    private void ensureTableExists() {
        String createTableSql = String.format(
                "CREATE TABLE IF NOT EXISTS %s ("
                        + "id BIGSERIAL PRIMARY KEY, "
                        + "connector_id VARCHAR(255) NOT NULL, "
                        + "history_data TEXT NOT NULL, "
                        + "created_at TIMESTAMP WITH TIME ZONE NOT NULL"
                        + ")",
                tableName);

        String createIndexSql = String.format(
                "CREATE INDEX IF NOT EXISTS idx_%s_connector_id ON %s (connector_id)",
                tableName.replace(".", "_"), tableName);

        try (Connection conn = dataSource.getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(createTableSql)) {
                stmt.execute();
            }
            try (PreparedStatement stmt = conn.prepareStatement(createIndexSql)) {
                stmt.execute();
            }
            log.info("Ensured schema history table {} exists", tableName);
        } catch (SQLException e) {
            log.error("Failed to create schema history table", e);
            throw new SchemaHistoryException("Failed to create schema history table", e);
        }
    }

    @Override
    public void stop() {
        running.set(false);
        if (dataSource != null) {
            dataSource.close();
        }
        super.stop();
        log.info("Stopped JdbcSchemaHistory");
    }

    @Override
    protected void storeRecord(HistoryRecord record) throws SchemaHistoryException {
        if (!running.get()) {
            throw new SchemaHistoryException("Schema history not started");
        }

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(insertSql)) {

            String historyData = writer.write(record.document());

            stmt.setLong(1, System.nanoTime()); // Use nano time as unique ID
            stmt.setString(2, connectorId);
            stmt.setString(3, historyData);
            stmt.setTimestamp(4, Timestamp.from(Instant.now()));

            stmt.executeUpdate();
            log.debug("Stored schema history record for connector {}", connectorId);

        } catch (SQLException | IOException e) {
            log.error("Failed to store schema history record", e);
            throw new SchemaHistoryException("Failed to store schema history record", e);
        }
    }

    @Override
    protected void recoverRecords(Consumer<HistoryRecord> records) {
        if (!running.get()) {
            throw new SchemaHistoryException("Schema history not started");
        }

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(selectSql)) {

            stmt.setString(1, connectorId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String historyData = rs.getString(1);
                    HistoryRecord record = new HistoryRecord(reader.read(historyData));
                    records.accept(record);
                }
            }

            log.info("Recovered schema history records for connector {}", connectorId);

        } catch (SQLException | IOException e) {
            log.error("Failed to recover schema history records", e);
            throw new SchemaHistoryException("Failed to recover schema history records", e);
        }
    }

    @Override
    public boolean exists() {
        if (!running.get()) {
            return false;
        }

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(existsSql)) {

            stmt.setString(1, connectorId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getBoolean(1);
                }
            }

        } catch (SQLException e) {
            log.error("Failed to check schema history existence", e);
        }

        return false;
    }

    @Override
    public boolean storageExists() {
        // Check if the table exists
        String checkTableSql = String.format(
                "SELECT EXISTS(SELECT 1 FROM information_schema.tables WHERE table_name = '%s')",
                tableName);

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(checkTableSql);
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                return rs.getBoolean(1);
            }

        } catch (SQLException e) {
            log.error("Failed to check storage existence", e);
        }

        return false;
    }

    /**
     * Get the connector ID used by this schema history.
     *
     * @return The connector ID
     */
    public String getConnectorId() {
        return connectorId;
    }

    /**
     * Get the table name used by this schema history.
     *
     * @return The table name
     */
    public String getTableName() {
        return tableName;
    }
}
