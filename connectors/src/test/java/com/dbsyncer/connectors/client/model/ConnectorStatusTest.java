package com.dbsyncer.connectors.client.model;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for ConnectorStatus.
 */
class ConnectorStatusTest {

    @Test
    void shouldDetectRunningStatus() {
        ConnectorStatus status = createStatus("RUNNING", "RUNNING");

        assertThat(status.isRunning()).isTrue();
        assertThat(status.isPaused()).isFalse();
        assertThat(status.isFailed()).isFalse();
    }

    @Test
    void shouldDetectPausedStatus() {
        ConnectorStatus status = createStatus("PAUSED", "PAUSED");

        assertThat(status.isRunning()).isFalse();
        assertThat(status.isPaused()).isTrue();
        assertThat(status.isFailed()).isFalse();
    }

    @Test
    void shouldDetectFailedConnectorStatus() {
        ConnectorStatus status = createStatus("FAILED", "RUNNING");

        assertThat(status.isRunning()).isFalse();
        assertThat(status.isPaused()).isFalse();
        assertThat(status.isFailed()).isTrue();
    }

    @Test
    void shouldDetectFailedTaskStatus() {
        ConnectorStatus status = createStatus("RUNNING", "FAILED");

        assertThat(status.isRunning()).isFalse();
        assertThat(status.isPaused()).isFalse();
        assertThat(status.isFailed()).isTrue();
    }

    @Test
    void shouldReturnErrorTraceFromConnector() {
        ConnectorStatus.ConnectorState connectorState = new ConnectorStatus.ConnectorState();
        connectorState.setState("FAILED");
        connectorState.setTrace("Connection refused");

        ConnectorStatus status = new ConnectorStatus();
        status.setConnector(connectorState);
        status.setTasks(Collections.emptyList());

        assertThat(status.getErrorTrace()).isEqualTo("Connection refused");
    }

    @Test
    void shouldReturnErrorTraceFromTask() {
        ConnectorStatus.ConnectorState connectorState = new ConnectorStatus.ConnectorState();
        connectorState.setState("RUNNING");

        ConnectorStatus.TaskState taskState = new ConnectorStatus.TaskState();
        taskState.setId(0);
        taskState.setState("FAILED");
        taskState.setTrace("Binlog position not found");

        ConnectorStatus status = new ConnectorStatus();
        status.setConnector(connectorState);
        status.setTasks(Arrays.asList(taskState));

        assertThat(status.getErrorTrace()).isEqualTo("Binlog position not found");
    }

    @Test
    void shouldReturnNullErrorTraceWhenNoFailure() {
        ConnectorStatus status = createStatus("RUNNING", "RUNNING");

        assertThat(status.getErrorTrace()).isNull();
    }

    @Test
    void shouldHandleNullConnector() {
        ConnectorStatus status = new ConnectorStatus();
        status.setConnector(null);
        status.setTasks(Collections.emptyList());

        assertThat(status.isRunning()).isFalse();
        assertThat(status.isPaused()).isFalse();
        assertThat(status.isFailed()).isFalse();
    }

    @Test
    void shouldHandleNullTasks() {
        ConnectorStatus.ConnectorState connectorState = new ConnectorStatus.ConnectorState();
        connectorState.setState("RUNNING");

        ConnectorStatus status = new ConnectorStatus();
        status.setConnector(connectorState);
        status.setTasks(null);

        assertThat(status.isRunning()).isFalse();
    }

    @Test
    void shouldHandleEmptyTasks() {
        ConnectorStatus.ConnectorState connectorState = new ConnectorStatus.ConnectorState();
        connectorState.setState("RUNNING");

        ConnectorStatus status = new ConnectorStatus();
        status.setConnector(connectorState);
        status.setTasks(Collections.emptyList());

        assertThat(status.isRunning()).isFalse();
    }

    @Test
    void shouldDetectPartiallyRunningTasks() {
        ConnectorStatus.ConnectorState connectorState = new ConnectorStatus.ConnectorState();
        connectorState.setState("RUNNING");

        ConnectorStatus.TaskState task1 = new ConnectorStatus.TaskState();
        task1.setId(0);
        task1.setState("RUNNING");

        ConnectorStatus.TaskState task2 = new ConnectorStatus.TaskState();
        task2.setId(1);
        task2.setState("FAILED");

        ConnectorStatus status = new ConnectorStatus();
        status.setConnector(connectorState);
        status.setTasks(Arrays.asList(task1, task2));

        assertThat(status.isRunning()).isFalse();
        assertThat(status.isFailed()).isTrue();
    }

    private ConnectorStatus createStatus(String connectorState, String taskState) {
        ConnectorStatus.ConnectorState connector = new ConnectorStatus.ConnectorState();
        connector.setState(connectorState);
        connector.setWorkerId("worker1");

        ConnectorStatus.TaskState task = new ConnectorStatus.TaskState();
        task.setId(0);
        task.setState(taskState);
        task.setWorkerId("worker1");

        ConnectorStatus status = new ConnectorStatus();
        status.setName("test-connector");
        status.setConnector(connector);
        status.setTasks(Arrays.asList(task));
        status.setType("source");

        return status;
    }
}
