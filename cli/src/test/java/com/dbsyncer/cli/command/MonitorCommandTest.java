package com.dbsyncer.cli.command;

import com.dbsyncer.cli.service.CliTaskService;
import com.dbsyncer.metadata.dto.TaskResponse;
import com.dbsyncer.metadata.entity.DatabaseType;
import com.dbsyncer.metadata.entity.TableProgress;
import com.dbsyncer.metadata.entity.TaskStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MonitorCommandTest {

    private CliTaskService taskService;
    private MonitorCommand monitorCommand;

    private PrintStream originalOut;
    private ByteArrayOutputStream outContent;

    @BeforeEach
    void setUp() {
        taskService = mock(CliTaskService.class);
        monitorCommand = new MonitorCommand(taskService);

        originalOut = System.out;
        outContent = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outContent));
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
    }

    @Test
    @DisplayName("renderDashboard should print task summary and table progress")
    void renderDashboardShouldPrintTaskSummaryAndTableProgress() throws Exception {
        UUID taskId = UUID.randomUUID();

        TaskResponse task = TaskResponse.builder()
                .id(taskId)
                .taskName("monitor-task")
                .status(TaskStatus.RUNNING)
                .sourceType(DatabaseType.MYSQL)
                .targetType(DatabaseType.POSTGRESQL)
                .totalTables(2)
                .completedTables(1)
                .totalRecords(200L)
                .processedRecords(80L)
                .startedAt(OffsetDateTime.now().minusSeconds(10))
                .build();

        TableProgress tp = TableProgress.builder()
                .id(UUID.randomUUID())
                .task(null)
                .sourceSchema("public")
                .sourceTable("customers")
                .targetSchema("public")
                .targetTable("customers")
                .estimatedRows(100L)
                .snapshotRowsWritten(40L)
                .streamingEventsProcessed(10L)
                .currentLagMs(50L)
                .status(com.dbsyncer.metadata.entity.ProgressStatus.SNAPSHOTTING)
                .build();

        when(taskService.getTask(taskId.toString())).thenReturn(task);
        when(taskService.getTableProgress(taskId)).thenReturn(List.of(tp));
        when(taskService.getTotalRowsProcessed(taskId)).thenReturn(50L);
        when(taskService.getTotalEstimatedRows(taskId)).thenReturn(200L);
        when(taskService.estimateEtaSeconds(taskId)).thenReturn(30L);
        when(taskService.getAverageLag(taskId)).thenReturn(25.0);

        Method render = MonitorCommand.class.getDeclaredMethod("renderDashboard", UUID.class);
        render.setAccessible(true);
        render.invoke(monitorCommand, taskId);

        String output = outContent.toString();

        assertThat(output).contains("=== Task Monitor ===");
        assertThat(output).contains("monitor-task");
        assertThat(output).contains("RUNNING");
        assertThat(output).contains("Progress:");
        assertThat(output).contains("Tables:");
        assertThat(output).contains("Records:");
        assertThat(output).contains("Rate:");
        assertThat(output).contains("ETA:");
        assertThat(output).contains("Avg lag:");

        assertThat(output).contains("customers");
        assertThat(output).contains("SNAPSHOTTING");
        assertThat(output).contains("LAG (ms)");
        assertThat(output).contains("[");
        assertThat(output).contains("]");
    }

    @Test
    @DisplayName("renderDashboard should handle no table progress gracefully")
    void renderDashboardNoTables() throws Exception {
        UUID taskId = UUID.randomUUID();

        TaskResponse task = TaskResponse.builder()
                .id(taskId)
                .taskName("empty-task")
                .status(TaskStatus.RUNNING)
                .totalTables(0)
                .completedTables(0)
                .build();

        when(taskService.getTask(any(String.class))).thenReturn(task);
        when(taskService.getTableProgress(taskId)).thenReturn(Collections.emptyList());
        when(taskService.getTotalRowsProcessed(taskId)).thenReturn(0L);
        when(taskService.getTotalEstimatedRows(taskId)).thenReturn(0L);
        when(taskService.estimateEtaSeconds(taskId)).thenReturn(null);
        when(taskService.getAverageLag(taskId)).thenReturn(null);

        Method render = MonitorCommand.class.getDeclaredMethod("renderDashboard", UUID.class);
        render.setAccessible(true);
        render.invoke(monitorCommand, taskId);

        String output = outContent.toString();
        assertThat(output).contains("No table progress data available.");
    }
}

