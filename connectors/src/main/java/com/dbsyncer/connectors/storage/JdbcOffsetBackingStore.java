package com.dbsyncer.connectors.storage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.connect.runtime.WorkerConfig;
import org.apache.kafka.connect.storage.OffsetBackingStore;
import org.apache.kafka.connect.util.Callback;

import java.nio.ByteBuffer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * A JDBC-based implementation of OffsetBackingStore that stores Kafka Connect offsets in PostgreSQL.
 * This allows offsets to be persisted in a database instead of Kafka topics.
 */
@Slf4j
public class JdbcOffsetBackingStore implements OffsetBackingStore {

    public static final String JDBC_URL_CONFIG = "offset.storage.jdbc.url";
    public static final String JDBC_USER_CONFIG = "offset.storage.jdbc.user";
    public static final String JDBC_PASSWORD_CONFIG = "offset.storage.jdbc.password";
    public static final String TABLE_NAME_CONFIG = "offset.storage.jdbc.table";
    public static final String DEFAULT_TABLE_NAME = "kafka_connect_offsets";

    private HikariDataSource dataSource;
    private String tableName;
    private ExecutorService executor;

    // SQL statements
    private String selectSql;
    private String upsertSql;

    @Override
    public void configure(WorkerConfig config) {
        Map<String, String> props = config.originalsStrings();

        String jdbcUrl = props.get(JDBC_URL_CONFIG);
        String jdbcUser = props.get(JDBC_USER_CONFIG);
        String jdbcPassword = props.get(JDBC_PASSWORD_CONFIG);
        this.tableName = props.getOrDefault(TABLE_NAME_CONFIG, DEFAULT_TABLE_NAME);

        if (jdbcUrl == null || jdbcUrl.isBlank()) {
            throw new IllegalArgumentException(JDBC_URL_CONFIG + " is required");
        }

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(jdbcUrl);
        if (jdbcUser != null) {
            hikariConfig.setUsername(jdbcUser);
        }
        if (jdbcPassword != null) {
            hikariConfig.setPassword(jdbcPassword);
        }
        hikariConfig.setPoolName("JdbcOffsetBackingStore");
        hikariConfig.setMaximumPoolSize(5);
        hikariConfig.setMinimumIdle(1);
        hikariConfig.setConnectionTimeout(30000);
        hikariConfig.setIdleTimeout(600000);
        hikariConfig.setMaxLifetime(1800000);

        this.dataSource = new HikariDataSource(hikariConfig);
        this.executor = Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, "JdbcOffsetBackingStore");
            thread.setDaemon(true);
            return thread;
        });

        // Prepare SQL statements
        this.selectSql = String.format(
                "SELECT offset_value FROM %s WHERE offset_key = ?", tableName);
        this.upsertSql = String.format(
                "INSERT INTO %s (offset_key, offset_value, updated_at) VALUES (?, ?, NOW()) "
                        + "ON CONFLICT (offset_key) DO UPDATE SET offset_value = EXCLUDED.offset_value, "
                        + "updated_at = EXCLUDED.updated_at",
                tableName);

        log.info("Configured JdbcOffsetBackingStore with table: {}", tableName);
    }

    @Override
    public void start() {
        log.info("Starting JdbcOffsetBackingStore");
        ensureTableExists();
    }

    private void ensureTableExists() {
        String createTableSql = String.format(
                "CREATE TABLE IF NOT EXISTS %s ("
                        + "offset_key BYTEA PRIMARY KEY, "
                        + "offset_value BYTEA, "
                        + "created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(), "
                        + "updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()"
                        + ")",
                tableName);

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(createTableSql)) {
            stmt.execute();
            log.info("Ensured offset table {} exists", tableName);
        } catch (SQLException e) {
            log.error("Failed to create offset table", e);
            throw new RuntimeException("Failed to create offset table", e);
        }
    }

    @Override
    public void stop() {
        log.info("Stopping JdbcOffsetBackingStore");
        if (executor != null) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                executor.shutdownNow();
            }
        }
        if (dataSource != null) {
            dataSource.close();
        }
    }

    @Override
    public Future<Map<ByteBuffer, ByteBuffer>> get(Collection<ByteBuffer> keys) {
        return executor.submit(() -> {
            Map<ByteBuffer, ByteBuffer> result = new HashMap<>();

            try (Connection conn = dataSource.getConnection()) {
                for (ByteBuffer key : keys) {
                    ByteBuffer value = getOffset(conn, key);
                    if (value != null) {
                        result.put(key, value);
                    }
                }
            } catch (SQLException e) {
                log.error("Failed to get offsets", e);
                throw new RuntimeException("Failed to get offsets", e);
            }

            return result;
        });
    }

    private ByteBuffer getOffset(Connection conn, ByteBuffer key) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(selectSql)) {
            byte[] keyBytes = toBytes(key);
            stmt.setBytes(1, keyBytes);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    byte[] valueBytes = rs.getBytes(1);
                    if (valueBytes != null) {
                        return ByteBuffer.wrap(valueBytes);
                    }
                }
            }
        }
        return null;
    }

    @Override
    public Future<Void> set(Map<ByteBuffer, ByteBuffer> values, Callback<Void> callback) {
        return executor.submit(() -> {
            try (Connection conn = dataSource.getConnection()) {
                conn.setAutoCommit(false);
                try {
                    for (Map.Entry<ByteBuffer, ByteBuffer> entry : values.entrySet()) {
                        setOffset(conn, entry.getKey(), entry.getValue());
                    }
                    conn.commit();
                    log.debug("Successfully stored {} offsets", values.size());
                    if (callback != null) {
                        callback.onCompletion(null, null);
                    }
                } catch (SQLException e) {
                    conn.rollback();
                    throw e;
                }
            } catch (Exception e) {
                log.error("Failed to set offsets", e);
                if (callback != null) {
                    callback.onCompletion(e, null);
                }
                throw new RuntimeException("Failed to set offsets", e);
            }
            return null;
        });
    }

    private void setOffset(Connection conn, ByteBuffer key, ByteBuffer value) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(upsertSql)) {
            byte[] keyBytes = toBytes(key);
            byte[] valueBytes = value != null ? toBytes(value) : null;

            stmt.setBytes(1, keyBytes);
            stmt.setBytes(2, valueBytes);
            stmt.executeUpdate();
        }
    }

    private byte[] toBytes(ByteBuffer buffer) {
        if (buffer == null) {
            return null;
        }
        buffer = buffer.duplicate();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        return bytes;
    }

    @Override
    public Set<Map<String, Object>> connectorPartitions(String connectorName) {
        // This method is required by the OffsetBackingStore interface in Kafka 3.7+
        // Return empty set as we don't track partitions by connector name
        log.debug("connectorPartitions called for connector: {}", connectorName);
        return java.util.Collections.emptySet();
    }
}
