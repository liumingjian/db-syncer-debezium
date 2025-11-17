package com.dbsyncer.cli.formatter;

import com.dbsyncer.metadata.dto.TaskResponse;
import com.dbsyncer.metadata.entity.DatabaseType;
import com.dbsyncer.metadata.entity.TaskStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OutputFormatterTest {

    private OutputFormatter formatter;
    private TaskResponse sampleTask;

    @BeforeEach
    void setUp() {
        formatter = new OutputFormatter();

        sampleTask = TaskResponse.builder()
            .id(UUID.randomUUID())
            .taskName("test-migration")
            .description("Test migration task")
            .status(TaskStatus.RUNNING)
            .sourceType(DatabaseType.MYSQL)
            .sourceHost("source-host")
            .sourcePort(3306)
            .sourceDatabase("sourcedb")
            .sourceUsername("root")
            .targetType(DatabaseType.POSTGRESQL)
            .targetHost("target-host")
            .targetPort(5432)
            .targetDatabase("targetdb")
            .targetUsername("postgres")
            .totalTables(10)
            .completedTables(5)
            .totalRecords(100000L)
            .processedRecords(50000L)
            .createdAt(OffsetDateTime.now().minusHours(2))
            .startedAt(OffsetDateTime.now().minusHours(1))
            .build();
    }

    @Test
    @DisplayName("Should format task as table")
    void shouldFormatTaskAsTable() {
        String result = formatter.formatTask(sampleTask, "table");

        assertThat(result).contains("test-migration");
        assertThat(result).contains("RUNNING");
        assertThat(result).contains("MYSQL");
        assertThat(result).contains("POSTGRESQL");
        assertThat(result).contains("source-host");
        assertThat(result).contains("3306");
    }

    @Test
    @DisplayName("Should format task as JSON")
    void shouldFormatTaskAsJson() {
        String result = formatter.formatTask(sampleTask, "json");

        assertThat(result).contains("\"taskName\"");
        assertThat(result).contains("\"test-migration\"");
        assertThat(result).contains("\"status\"");
        assertThat(result).contains("\"RUNNING\"");
        assertThat(result).startsWith("{");
        assertThat(result).endsWith("}");
    }

    @Test
    @DisplayName("Should format task as YAML")
    void shouldFormatTaskAsYaml() {
        String result = formatter.formatTask(sampleTask, "yaml");

        assertThat(result).contains("taskName:");
        assertThat(result).contains("test-migration");
        assertThat(result).contains("status:");
        assertThat(result).contains("RUNNING");
    }

    @Test
    @DisplayName("Should format task list as table")
    void shouldFormatTaskListAsTable() {
        TaskResponse task2 = TaskResponse.builder()
            .id(UUID.randomUUID())
            .taskName("another-task")
            .status(TaskStatus.CREATED)
            .sourceType(DatabaseType.ORACLE)
            .targetType(DatabaseType.POSTGRESQL)
            .totalTables(0)
            .completedTables(0)
            .totalRecords(0L)
            .processedRecords(0L)
            .build();

        String result = formatter.formatTaskList(List.of(sampleTask, task2), "table");

        assertThat(result).contains("ID");
        assertThat(result).contains("NAME");
        assertThat(result).contains("STATUS");
        assertThat(result).contains("test-migration");
        assertThat(result).contains("another-task");
        assertThat(result).contains("RUNNING");
        assertThat(result).contains("CREATED");
    }

    @Test
    @DisplayName("Should format empty task list")
    void shouldFormatEmptyTaskList() {
        String result = formatter.formatTaskList(List.of(), "table");

        assertThat(result).contains("No tasks found");
    }

    @Test
    @DisplayName("Should format task detail as table")
    void shouldFormatTaskDetailAsTable() {
        String result = formatter.formatTaskDetail(sampleTask, "table");

        assertThat(result).contains("Task Details");
        assertThat(result).contains("test-migration");
        assertThat(result).contains("Test migration task");
        assertThat(result).contains("RUNNING");
        assertThat(result).contains("Source Database");
        assertThat(result).contains("Target Database");
        assertThat(result).contains("Progress");
        assertThat(result).contains("5 / 10");
        assertThat(result).contains("50000 / 100000");
    }

    @Test
    @DisplayName("Should calculate progress percentage")
    void shouldCalculateProgressPercentage() {
        String result = formatter.formatTaskProgress(sampleTask, "table");

        assertThat(result).contains("50.00%");
    }

    @Test
    @DisplayName("Should handle zero records progress")
    void shouldHandleZeroRecordsProgress() {
        TaskResponse taskNoRecords = TaskResponse.builder()
            .id(UUID.randomUUID())
            .taskName("no-records-task")
            .status(TaskStatus.CREATED)
            .totalTables(10)
            .completedTables(3)
            .totalRecords(0L)
            .processedRecords(0L)
            .build();

        String result = formatter.formatTaskProgress(taskNoRecords, "table");

        assertThat(result).contains("30.00%");
    }

    @Test
    @DisplayName("Should format task status")
    void shouldFormatTaskStatus() {
        String result = formatter.formatTaskStatus(sampleTask, "table");

        assertThat(result).contains("test-migration");
        assertThat(result).contains("RUNNING");
        assertThat(result).contains("50.00%");
        assertThat(result).contains("5/10 tables");
        assertThat(result).contains("50000/100000 records");
    }

    @Test
    @DisplayName("Should truncate long strings")
    void shouldTruncateLongStrings() {
        TaskResponse taskLongName = TaskResponse.builder()
            .id(UUID.randomUUID())
            .taskName("this-is-a-very-long-task-name-that-should-be-truncated")
            .status(TaskStatus.CREATED)
            .sourceType(DatabaseType.MYSQL)
            .targetType(DatabaseType.POSTGRESQL)
            .totalTables(0)
            .completedTables(0)
            .totalRecords(0L)
            .processedRecords(0L)
            .build();

        String result = formatter.formatTaskList(List.of(taskLongName), "table");

        assertThat(result).contains("...");
        assertThat(result).doesNotContain("this-is-a-very-long-task-name-that-should-be-truncated");
    }

    @Test
    @DisplayName("Should format status summary")
    void shouldFormatStatusSummary() {
        TaskResponse task2 = TaskResponse.builder()
            .id(UUID.randomUUID())
            .taskName("paused-task")
            .status(TaskStatus.PAUSED)
            .totalTables(20)
            .completedTables(10)
            .totalRecords(0L)
            .processedRecords(0L)
            .build();

        String result = formatter.formatTaskStatusSummary(List.of(sampleTask, task2), "table");

        assertThat(result).contains("TASK");
        assertThat(result).contains("STATUS");
        assertThat(result).contains("PROGRESS");
        assertThat(result).contains("test-migration");
        assertThat(result).contains("paused-task");
        assertThat(result).contains("RUNNING");
        assertThat(result).contains("PAUSED");
    }
}
