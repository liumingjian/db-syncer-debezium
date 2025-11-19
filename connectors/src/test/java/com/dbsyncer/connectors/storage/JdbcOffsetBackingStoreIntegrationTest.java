package com.dbsyncer.connectors.storage;

import org.apache.kafka.connect.runtime.WorkerConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Integration test for JdbcOffsetBackingStore using Testcontainers.
 */
@Testcontainers(disabledWithoutDocker = true)
class JdbcOffsetBackingStoreIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("test_offsets")
            .withUsername("test")
            .withPassword("test");

    private JdbcOffsetBackingStore store;

    @BeforeEach
    void setUp() {
        store = new JdbcOffsetBackingStore();

        Map<String, String> props = new HashMap<>();
        props.put(JdbcOffsetBackingStore.JDBC_URL_CONFIG, postgres.getJdbcUrl());
        props.put(JdbcOffsetBackingStore.JDBC_USER_CONFIG, postgres.getUsername());
        props.put(JdbcOffsetBackingStore.JDBC_PASSWORD_CONFIG, postgres.getPassword());
        props.put(JdbcOffsetBackingStore.TABLE_NAME_CONFIG, "test_offsets");

        WorkerConfig config = mock(WorkerConfig.class);
        when(config.originalsStrings()).thenReturn(props);

        store.configure(config);
        store.start();
    }

    @AfterEach
    void tearDown() {
        if (store != null) {
            store.stop();
        }
    }

    @Test
    void shouldStoreAndRetrieveOffset() throws Exception {
        ByteBuffer key = ByteBuffer.wrap("test-key".getBytes(StandardCharsets.UTF_8));
        ByteBuffer value = ByteBuffer.wrap("test-value".getBytes(StandardCharsets.UTF_8));

        Map<ByteBuffer, ByteBuffer> offsets = new HashMap<>();
        offsets.put(key, value);

        Future<Void> setFuture = store.set(offsets, null);
        setFuture.get(10, TimeUnit.SECONDS);

        Future<Map<ByteBuffer, ByteBuffer>> getFuture = store.get(Arrays.asList(key));
        Map<ByteBuffer, ByteBuffer> result = getFuture.get(10, TimeUnit.SECONDS);

        assertThat(result).hasSize(1);
        assertThat(new String(toBytes(result.get(key)), StandardCharsets.UTF_8)).isEqualTo("test-value");
    }

    @Test
    void shouldUpdateExistingOffset() throws Exception {
        ByteBuffer key = ByteBuffer.wrap("update-key".getBytes(StandardCharsets.UTF_8));
        ByteBuffer value1 = ByteBuffer.wrap("value1".getBytes(StandardCharsets.UTF_8));
        ByteBuffer value2 = ByteBuffer.wrap("value2".getBytes(StandardCharsets.UTF_8));

        // Set initial value
        Map<ByteBuffer, ByteBuffer> offsets1 = new HashMap<>();
        offsets1.put(key, value1);
        store.set(offsets1, null).get(10, TimeUnit.SECONDS);

        // Update value
        Map<ByteBuffer, ByteBuffer> offsets2 = new HashMap<>();
        offsets2.put(key, value2);
        store.set(offsets2, null).get(10, TimeUnit.SECONDS);

        // Retrieve and verify
        Map<ByteBuffer, ByteBuffer> result = store.get(Arrays.asList(key)).get(10, TimeUnit.SECONDS);

        assertThat(new String(toBytes(result.get(key)), StandardCharsets.UTF_8)).isEqualTo("value2");
    }

    @Test
    void shouldRetrieveMultipleOffsets() throws Exception {
        ByteBuffer key1 = ByteBuffer.wrap("key1".getBytes(StandardCharsets.UTF_8));
        ByteBuffer key2 = ByteBuffer.wrap("key2".getBytes(StandardCharsets.UTF_8));
        ByteBuffer value1 = ByteBuffer.wrap("value1".getBytes(StandardCharsets.UTF_8));
        ByteBuffer value2 = ByteBuffer.wrap("value2".getBytes(StandardCharsets.UTF_8));

        Map<ByteBuffer, ByteBuffer> offsets = new HashMap<>();
        offsets.put(key1, value1);
        offsets.put(key2, value2);
        store.set(offsets, null).get(10, TimeUnit.SECONDS);

        Map<ByteBuffer, ByteBuffer> result = store.get(Arrays.asList(key1, key2)).get(10, TimeUnit.SECONDS);

        assertThat(result).hasSize(2);
        assertThat(new String(toBytes(result.get(key1)), StandardCharsets.UTF_8)).isEqualTo("value1");
        assertThat(new String(toBytes(result.get(key2)), StandardCharsets.UTF_8)).isEqualTo("value2");
    }

    @Test
    void shouldReturnEmptyForNonExistentKeys() throws Exception {
        ByteBuffer key = ByteBuffer.wrap("non-existent".getBytes(StandardCharsets.UTF_8));

        Map<ByteBuffer, ByteBuffer> result = store.get(Arrays.asList(key)).get(10, TimeUnit.SECONDS);

        assertThat(result).isEmpty();
    }

    @Test
    void shouldHandleEmptyOffsetSet() throws Exception {
        Map<ByteBuffer, ByteBuffer> offsets = new HashMap<>();

        Future<Void> setFuture = store.set(offsets, null);
        setFuture.get(10, TimeUnit.SECONDS);

        // Should not throw an exception
    }

    @Test
    void shouldHandleEmptyGetRequest() throws Exception {
        Map<ByteBuffer, ByteBuffer> result = store.get(new HashSet<>()).get(10, TimeUnit.SECONDS);

        assertThat(result).isEmpty();
    }

    private byte[] toBytes(ByteBuffer buffer) {
        buffer = buffer.duplicate();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        return bytes;
    }
}
