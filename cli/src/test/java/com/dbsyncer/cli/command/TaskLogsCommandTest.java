package com.dbsyncer.cli.command;

import com.dbsyncer.cli.service.CliTaskService;
import com.dbsyncer.metadata.dto.TaskResponse;
import com.dbsyncer.metadata.entity.TaskStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TaskLogsCommandTest {

    private CliTaskService taskService;
    private TaskCommand.LogsCommand logsCommand;

    private PrintStream originalOut;
    private ByteArrayOutputStream outContent;

    @BeforeEach
    void setUp() {
        taskService = mock(CliTaskService.class);
        logsCommand = new TaskCommand.LogsCommand(taskService);

        originalOut = System.out;
        outContent = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outContent));
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
    }

    @Test
    @DisplayName("logs command should print task logs table")
    void logsCommandShouldPrintTaskLogs() throws Exception {
        UUID taskId = UUID.randomUUID();
        TaskResponse task = TaskResponse.builder()
                .id(taskId)
                .taskName("log-task")
                .status(TaskStatus.FAILED)
                .build();

        when(taskService.getTask("log-task")).thenReturn(task);
        when(taskService.getTaskLogs(eq(taskId), any(), eq(50)))
                .thenReturn(java.util.Collections.emptyList());

        setField(logsCommand, "taskIdentifier", "log-task");
        setField(logsCommand, "limit", 50);
        setField(logsCommand, "level", null);

        logsCommand.run();

        String output = outContent.toString();
        assertThat(output).contains("=== Task Logs ===");
        assertThat(output).contains("log-task");
        assertThat(output).contains("No logs found for this task.");
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(target, value);
    }
}
