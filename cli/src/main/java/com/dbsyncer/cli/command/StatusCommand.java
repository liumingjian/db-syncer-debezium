package com.dbsyncer.cli.command;

import com.dbsyncer.cli.formatter.OutputFormatter;
import com.dbsyncer.cli.service.CliTaskService;
import com.dbsyncer.metadata.dto.TaskResponse;
import com.dbsyncer.metadata.entity.TableProgress;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.util.List;

@Component
@Command(
    name = "status",
    description = "Query task status and progress",
    mixinStandardHelpOptions = true
)
public class StatusCommand implements Runnable {

    private final CliTaskService taskService;
    private final OutputFormatter formatter;

    @Parameters(index = "0", arity = "0..1", description = "Task ID or name (optional, shows all if not provided)")
    private String taskIdentifier;

    @Option(names = {"--progress"}, description = "Show detailed progress information")
    private boolean showProgress;

    @Option(names = {"--tables"}, description = "Show table-level progress")
    private boolean showTables;

    @Option(names = {"-f", "--format"}, description = "Output format (table, json, yaml)", defaultValue = "table")
    private String outputFormat;

    @Option(names = {"--watch"}, description = "Continuously watch status (refresh interval in seconds)")
    private Integer watchInterval;

    public StatusCommand(CliTaskService taskService, OutputFormatter formatter) {
        this.taskService = taskService;
        this.formatter = formatter;
    }

    @Override
    public void run() {
        try {
            if (watchInterval != null && watchInterval > 0) {
                watchStatus();
            } else {
                showStatus();
            }
        } catch (Exception e) {
            System.err.println("Error querying status: " + e.getMessage());
            System.exit(1);
        }
    }

    private void showStatus() {
        if (taskIdentifier != null) {
            showTaskStatus();
        } else {
            showAllTasksStatus();
        }
    }

    private void showTaskStatus() {
        TaskResponse task = taskService.getTask(taskIdentifier);
        System.out.println(formatter.formatTaskStatus(task, outputFormat));

        if (showProgress) {
            System.out.println("\n--- Progress Details ---");
            System.out.println(formatter.formatTaskProgress(task, outputFormat));
        }

        if (showTables) {
            System.out.println("\n--- Table Progress ---");
            List<TableProgress> tableProgress = taskService.getTableProgress(task.getId());
            System.out.println(formatter.formatTableProgress(tableProgress, outputFormat));
        }
    }

    private void showAllTasksStatus() {
        List<TaskResponse> tasks = taskService.getAllTasks(0, 100);
        System.out.println(formatter.formatTaskStatusSummary(tasks, outputFormat));
    }

    private void watchStatus() {
        System.out.println("Watching status (Ctrl+C to stop)...\n");
        try {
            while (true) {
                clearScreen();
                showStatus();
                Thread.sleep(watchInterval * 1000L);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("\nWatch stopped.");
        }
    }

    private void clearScreen() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }
}
