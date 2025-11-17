package com.dbsyncer.cli.formatter;

import com.dbsyncer.metadata.dto.TaskResponse;
import com.dbsyncer.metadata.entity.ConnectorConfig;
import com.dbsyncer.metadata.entity.TableProgress;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Component
public class OutputFormatter {

    private final ObjectMapper jsonMapper;
    private final Yaml yamlMapper;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public OutputFormatter() {
        this.jsonMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .enable(SerializationFeature.INDENT_OUTPUT)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        this.yamlMapper = new Yaml(options);
    }

    public String formatTask(TaskResponse task, String format) {
        return switch (format.toLowerCase()) {
            case "json" -> formatAsJson(task);
            case "yaml" -> formatAsYaml(task);
            default -> formatTaskAsTable(task);
        };
    }

    public String formatTaskList(List<TaskResponse> tasks, String format) {
        return switch (format.toLowerCase()) {
            case "json" -> formatAsJson(tasks);
            case "yaml" -> formatAsYaml(tasks);
            default -> formatTaskListAsTable(tasks);
        };
    }

    public String formatTaskDetail(TaskResponse task, String format) {
        return switch (format.toLowerCase()) {
            case "json" -> formatAsJson(task);
            case "yaml" -> formatAsYaml(task);
            default -> formatTaskDetailAsTable(task);
        };
    }

    public String formatTaskStatus(TaskResponse task, String format) {
        return switch (format.toLowerCase()) {
            case "json" -> formatAsJson(Map.of(
                "id", task.getId(),
                "name", task.getTaskName(),
                "status", task.getStatus(),
                "progress", calculateProgress(task)
            ));
            case "yaml" -> formatAsYaml(Map.of(
                "id", task.getId(),
                "name", task.getTaskName(),
                "status", task.getStatus(),
                "progress", calculateProgress(task)
            ));
            default -> formatTaskStatusAsTable(task);
        };
    }

    public String formatTaskProgress(TaskResponse task, String format) {
        Map<String, Object> progress = Map.of(
            "totalTables", task.getTotalTables(),
            "completedTables", task.getCompletedTables(),
            "totalRecords", task.getTotalRecords(),
            "processedRecords", task.getProcessedRecords(),
            "progressPercentage", calculateProgress(task)
        );

        return switch (format.toLowerCase()) {
            case "json" -> formatAsJson(progress);
            case "yaml" -> formatAsYaml(progress);
            default -> formatProgressAsTable(progress);
        };
    }

    public String formatTaskStatusSummary(List<TaskResponse> tasks, String format) {
        return switch (format.toLowerCase()) {
            case "json" -> formatAsJson(tasks);
            case "yaml" -> formatAsYaml(tasks);
            default -> formatStatusSummaryAsTable(tasks);
        };
    }

    public String formatTableProgress(List<TableProgress> progressList, String format) {
        return switch (format.toLowerCase()) {
            case "json" -> formatAsJson(progressList);
            case "yaml" -> formatAsYaml(progressList);
            default -> formatTableProgressAsTable(progressList);
        };
    }

    public String formatConnectorConfigs(List<ConnectorConfig> configs, String format) {
        return switch (format.toLowerCase()) {
            case "json" -> formatAsJson(configs);
            case "yaml" -> formatAsYaml(configs);
            default -> formatConnectorConfigsAsTable(configs);
        };
    }

    public String formatConnectorConfigList(List<ConnectorConfig> configs, String format) {
        return switch (format.toLowerCase()) {
            case "json" -> formatAsJson(configs);
            case "yaml" -> formatAsYaml(configs);
            default -> formatConnectorConfigListAsTable(configs);
        };
    }

    private String formatAsJson(Object obj) {
        try {
            return jsonMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "Error formatting as JSON: " + e.getMessage();
        }
    }

    private String formatAsYaml(Object obj) {
        try {
            return yamlMapper.dump(obj);
        } catch (Exception e) {
            return "Error formatting as YAML: " + e.getMessage();
        }
    }

    private String formatTaskAsTable(TaskResponse task) {
        return String.format("""
            Task: %s
            ID:     %s
            Status: %s
            Source: %s://%s:%d/%s
            Target: %s://%s:%d/%s
            """,
            task.getTaskName(),
            task.getId(),
            task.getStatus(),
            task.getSourceType(), task.getSourceHost(), task.getSourcePort(), task.getSourceDatabase(),
            task.getTargetType(), task.getTargetHost(), task.getTargetPort(), task.getTargetDatabase()
        );
    }

    private String formatTaskListAsTable(List<TaskResponse> tasks) {
        if (tasks.isEmpty()) {
            return "No tasks found.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%-36s  %-20s  %-12s  %-10s  %-10s%n",
            "ID", "NAME", "STATUS", "SOURCE", "TARGET"));
        sb.append("-".repeat(100)).append("\n");

        for (TaskResponse task : tasks) {
            sb.append(String.format("%-36s  %-20s  %-12s  %-10s  %-10s%n",
                task.getId(),
                truncate(task.getTaskName(), 20),
                task.getStatus(),
                task.getSourceType(),
                task.getTargetType()
            ));
        }

        return sb.toString();
    }

    private String formatTaskDetailAsTable(TaskResponse task) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Task Details ===\n\n");

        sb.append(String.format("ID:           %s%n", task.getId()));
        sb.append(String.format("Name:         %s%n", task.getTaskName()));
        sb.append(String.format("Description:  %s%n", task.getDescription() != null ? task.getDescription() : "N/A"));
        sb.append(String.format("Status:       %s%n", task.getStatus()));
        sb.append("\n--- Source Database ---\n");
        sb.append(String.format("Type:         %s%n", task.getSourceType()));
        sb.append(String.format("Host:         %s%n", task.getSourceHost()));
        sb.append(String.format("Port:         %d%n", task.getSourcePort()));
        sb.append(String.format("Database:     %s%n", task.getSourceDatabase()));
        sb.append(String.format("Username:     %s%n", task.getSourceUsername()));

        sb.append("\n--- Target Database ---\n");
        sb.append(String.format("Type:         %s%n", task.getTargetType()));
        sb.append(String.format("Host:         %s%n", task.getTargetHost()));
        sb.append(String.format("Port:         %d%n", task.getTargetPort()));
        sb.append(String.format("Database:     %s%n", task.getTargetDatabase()));
        sb.append(String.format("Username:     %s%n", task.getTargetUsername()));

        sb.append("\n--- Progress ---\n");
        sb.append(String.format("Tables:       %d / %d%n", task.getCompletedTables(), task.getTotalTables()));
        sb.append(String.format("Records:      %d / %d%n", task.getProcessedRecords(), task.getTotalRecords()));
        sb.append(String.format("Progress:     %.2f%%%n", calculateProgress(task)));

        sb.append("\n--- Timestamps ---\n");
        sb.append(String.format("Created:      %s%n",
            task.getCreatedAt() != null ? task.getCreatedAt().format(DATE_FORMAT) : "N/A"));
        sb.append(String.format("Started:      %s%n",
            task.getStartedAt() != null ? task.getStartedAt().format(DATE_FORMAT) : "N/A"));
        sb.append(String.format("Completed:    %s%n",
            task.getCompletedAt() != null ? task.getCompletedAt().format(DATE_FORMAT) : "N/A"));

        if (task.getErrorMessage() != null) {
            sb.append("\n--- Error ---\n");
            sb.append(task.getErrorMessage()).append("\n");
        }

        return sb.toString();
    }

    private String formatTaskStatusAsTable(TaskResponse task) {
        return String.format("""
            Task: %s
            Status: %s
            Progress: %.2f%% (%d/%d tables, %d/%d records)
            """,
            task.getTaskName(),
            task.getStatus(),
            calculateProgress(task),
            task.getCompletedTables(), task.getTotalTables(),
            task.getProcessedRecords(), task.getTotalRecords()
        );
    }

    private String formatProgressAsTable(Map<String, Object> progress) {
        return String.format("""
            Tables:     %d / %d
            Records:    %d / %d
            Progress:   %.2f%%
            """,
            progress.get("completedTables"), progress.get("totalTables"),
            progress.get("processedRecords"), progress.get("totalRecords"),
            progress.get("progressPercentage")
        );
    }

    private String formatStatusSummaryAsTable(List<TaskResponse> tasks) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%-20s  %-12s  %-8s%n", "TASK", "STATUS", "PROGRESS"));
        sb.append("-".repeat(50)).append("\n");

        for (TaskResponse task : tasks) {
            sb.append(String.format("%-20s  %-12s  %.2f%%%n",
                truncate(task.getTaskName(), 20),
                task.getStatus(),
                calculateProgress(task)
            ));
        }

        return sb.toString();
    }

    private String formatTableProgressAsTable(List<TableProgress> progressList) {
        if (progressList.isEmpty()) {
            return "No table progress data available.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%-30s  %-12s  %-15s  %-10s%n",
            "TABLE", "STATUS", "ROWS", "LAG (ms)"));
        sb.append("-".repeat(80)).append("\n");

        for (TableProgress p : progressList) {
            String tableName = p.getSourceSchema() + "." + p.getSourceTable();
            String rows = p.getSnapshotRowsWritten() + " / " + p.getEstimatedRows();
            String lag = p.getCurrentLagMs() != null ? p.getCurrentLagMs().toString() : "N/A";

            sb.append(String.format("%-30s  %-12s  %-15s  %-10s%n",
                truncate(tableName, 30),
                p.getStatus(),
                rows,
                lag
            ));
        }

        return sb.toString();
    }

    private String formatConnectorConfigsAsTable(List<ConnectorConfig> configs) {
        if (configs.isEmpty()) {
            return "No connector configurations found.";
        }

        StringBuilder sb = new StringBuilder();
        for (ConnectorConfig config : configs) {
            sb.append(String.format("=== %s Connector ===%n", config.getConnectorType()));
            sb.append(String.format("Name: %s%n", config.getConnectorName()));
            sb.append(String.format("Status: %s%n", config.getStatus()));
            sb.append("Configuration:\n");
            if (config.getConfig() != null) {
                config.getConfig().forEach((key, value) ->
                    sb.append(String.format("  %s: %s%n", key, value)));
            }
            sb.append("\n");
        }

        return sb.toString();
    }

    private String formatConnectorConfigListAsTable(List<ConnectorConfig> configs) {
        if (configs.isEmpty()) {
            return "No connector configurations found.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%-36s  %-30s  %-10s  %-12s%n",
            "ID", "NAME", "TYPE", "STATUS"));
        sb.append("-".repeat(100)).append("\n");

        for (ConnectorConfig config : configs) {
            sb.append(String.format("%-36s  %-30s  %-10s  %-12s%n",
                config.getId(),
                truncate(config.getConnectorName(), 30),
                config.getConnectorType(),
                config.getStatus()
            ));
        }

        return sb.toString();
    }

    private double calculateProgress(TaskResponse task) {
        if (task.getTotalRecords() == 0) {
            if (task.getTotalTables() == 0) {
                return 0.0;
            }
            return (double) task.getCompletedTables() / task.getTotalTables() * 100;
        }
        return (double) task.getProcessedRecords() / task.getTotalRecords() * 100;
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
}
