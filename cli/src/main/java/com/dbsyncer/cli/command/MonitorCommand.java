package com.dbsyncer.cli.command;

import com.dbsyncer.cli.service.CliTaskService;
import com.dbsyncer.metadata.dto.TaskResponse;
import com.dbsyncer.metadata.entity.TableProgress;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Component
@Command(
    name = "monitor",
    description = "Monitor a migration task with a terminal dashboard",
    mixinStandardHelpOptions = true
)
public class MonitorCommand implements Runnable {

    private final CliTaskService taskService;

    @Parameters(index = "0", description = "Task ID or name")
    private String taskIdentifier;

    @Option(names = {"-i", "--interval"}, description = "Refresh interval in seconds", defaultValue = "2")
    private int refreshInterval;

    public MonitorCommand(CliTaskService taskService) {
        this.taskService = taskService;
    }

    @Override
    public void run() {
        try {
            UUID taskId = resolveTaskId(taskIdentifier);
            System.out.println("Monitoring task '" + taskIdentifier + "' (Ctrl+C to stop)...");
            while (true) {
                clearScreen();
                renderDashboard(taskId);
                Thread.sleep(refreshInterval * 1000L);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("\nMonitor stopped.");
        } catch (Exception e) {
            System.err.println("Error monitoring task: " + e.getMessage());
            System.exit(1);
        }
    }

    private void renderDashboard(UUID taskId) {
        TaskResponse task = taskService.getTask(taskId.toString());
        List<TableProgress> tables = taskService.getTableProgress(taskId);
        Long totalProcessed = taskService.getTotalRowsProcessed(taskId);
        Long totalEstimated = taskService.getTotalEstimatedRows(taskId);
        Long etaSeconds = taskService.estimateEtaSeconds(taskId);
        Double avgLag = taskService.getAverageLag(taskId);

        if (totalProcessed == null) {
            totalProcessed = 0L;
        }
        if (totalEstimated == null) {
            totalEstimated = 0L;
        }

        double overallPercent = calculateOverallPercent(totalProcessed, totalEstimated, task);
        String overallBar = progressBar(overallPercent, 40);

        System.out.println("=== Task Monitor ===");
        System.out.println("ID:      " + task.getId());
        System.out.println("Name:    " + task.getTaskName());
        System.out.println("Status:  " + task.getStatus());
        System.out.println();

        System.out.printf("Progress: %s  %5.1f%%%n", overallBar, overallPercent);
        System.out.printf("Tables:   %d completed / %d total%n",
            task.getCompletedTables() != null ? task.getCompletedTables() : 0,
            task.getTotalTables() != null ? task.getTotalTables() : 0);
        System.out.printf("Records:  %d processed / %d total%n",
            totalProcessed,
            totalEstimated > 0 ? totalEstimated : 0);

        Double rate = calculateThroughput(task, totalProcessed);
        System.out.printf("Rate:     %s records/s%n", rate != null ? String.format("%.1f", rate) : "N/A");
        System.out.printf("ETA:      %s%n", etaSeconds != null ? formatDuration(etaSeconds) : "N/A");
        System.out.printf("Avg lag:  %s ms%n", avgLag != null ? String.format("%.0f", avgLag) : "N/A");

        System.out.println();
        System.out.println("--- Table Progress ---");
        if (tables.isEmpty()) {
            System.out.println("No table progress data available.");
        } else {
            System.out.printf("%-30s  %-12s  %-20s  %-10s%n",
                "TABLE", "STATUS", "PROGRESS", "LAG (ms)");
            System.out.println("-".repeat(80));
            for (TableProgress tp : tables) {
                long est = tp.getEstimatedRows() != null ? tp.getEstimatedRows() : 0L;
                long processed = tp.getTotalRowsProcessed();
                double pct = est > 0 ? processed * 100.0 / est : 0.0;
                String bar = progressBar(pct, 20);
                String tableName = (tp.getSourceSchema() != null ? tp.getSourceSchema() + "." : "") + tp.getSourceTable();
                String lag = tp.getCurrentLagMs() != null ? tp.getCurrentLagMs().toString() : "N/A";
                System.out.printf("%-30s  %-12s  %s %5.1f%%%s  %-10s%n",
                    truncate(tableName, 30),
                    tp.getStatus(),
                    bar,
                    pct,
                    pct < 10 ? " " : "",
                    lag
                );
            }
        }
    }

    private double calculateOverallPercent(Long totalProcessed, Long totalEstimated, TaskResponse task) {
        if (totalEstimated != null && totalEstimated > 0) {
            return totalProcessed * 100.0 / totalEstimated;
        }
        if (task.getProgressPercentage() != null) {
            return task.getProgressPercentage();
        }
        if (task.getTotalTables() != null && task.getTotalTables() > 0 &&
            task.getCompletedTables() != null) {
            return task.getCompletedTables() * 100.0 / task.getTotalTables();
        }
        return 0.0;
    }

    private Double calculateThroughput(TaskResponse task, Long totalProcessed) {
        OffsetDateTime startedAt = task.getStartedAt();
        if (startedAt == null || totalProcessed == null || totalProcessed <= 0) {
            return null;
        }
        long elapsedSeconds = Duration.between(startedAt, OffsetDateTime.now()).getSeconds();
        if (elapsedSeconds <= 0) {
            return null;
        }
        return totalProcessed / (double) elapsedSeconds;
    }

    private String progressBar(double percent, int width) {
        if (percent < 0) {
            percent = 0;
        }
        if (percent > 100) {
            percent = 100;
        }
        int filled = (int) Math.round(percent * width / 100.0);
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < width; i++) {
            sb.append(i < filled ? "#" : ".");
        }
        sb.append("]");
        return sb.toString();
    }

    private String formatDuration(long seconds) {
        long s = seconds;
        long h = s / 3600;
        s %= 3600;
        long m = s / 60;
        s %= 60;
        if (h > 0) {
            return String.format("%dh %02dm %02ds", h, m, s);
        }
        if (m > 0) {
            return String.format("%dm %02ds", m, s);
        }
        return s + "s";
    }

    private void clearScreen() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    private String truncate(String str, int maxLength) {
        if (str == null) {
            return "";
        }
        if (str.length() <= maxLength) {
            return str;
        }
        return str.substring(0, maxLength - 3) + "...";
    }

    private UUID resolveTaskId(String identifier) {
        try {
            return UUID.fromString(identifier);
        } catch (IllegalArgumentException e) {
            TaskResponse task = taskService.getTask(identifier);
            return task.getId();
        }
    }
}

