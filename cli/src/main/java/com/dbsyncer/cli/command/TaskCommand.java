package com.dbsyncer.cli.command;

import com.dbsyncer.cli.formatter.OutputFormatter;
import com.dbsyncer.cli.service.CliTaskService;
import com.dbsyncer.metadata.dto.TaskCreateRequest;
import com.dbsyncer.metadata.dto.TaskResponse;
import com.dbsyncer.metadata.entity.DatabaseType;
import com.dbsyncer.metadata.entity.TaskStatus;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

import java.util.List;
import java.util.UUID;

@Component
@Command(
    name = "task",
    description = "Manage migration tasks",
    mixinStandardHelpOptions = true,
    subcommands = {
        TaskCommand.CreateCommand.class,
        TaskCommand.ListCommand.class,
        TaskCommand.ShowCommand.class,
        TaskCommand.DeleteCommand.class,
        TaskCommand.StartCommand.class,
        TaskCommand.StopCommand.class,
        TaskCommand.PauseCommand.class,
        TaskCommand.ResumeCommand.class,
        TaskCommand.PropsCommand.class,
        TaskCommand.LogsCommand.class
    }
)
public class TaskCommand implements Runnable {

    @ParentCommand
    private DbSyncerCommand parent;

    @Override
    public void run() {
        System.out.println("Task management commands. Use 'dbsyncer task --help' for available subcommands.");
    }

    @Component
    @Command(name = "props", description = "Manage task properties (source_properties/target_properties)", subcommands = {
            TaskCommand.PropsCommand.SetCommand.class
    })
    public static class PropsCommand implements Runnable {
        @Override
        public void run() {
            System.out.println("Use subcommands: set");
        }

        @Component
        @Command(name = "set", description = "Set a task property")
        public static class SetCommand implements Runnable {

            private final com.dbsyncer.metadata.service.TaskService taskService;
            private final com.dbsyncer.metadata.repository.MigrationTaskRepository taskRepository;

            public SetCommand(com.dbsyncer.metadata.service.TaskService taskService,
                              com.dbsyncer.metadata.repository.MigrationTaskRepository taskRepository) {
                this.taskService = taskService;
                this.taskRepository = taskRepository;
            }

            @Parameters(index = "0", description = "Task ID or name")
            private String taskIdentifier;

            @Option(names = {"--scope"}, description = "Property scope: source|target (default: source)", defaultValue = "source")
            private String scope;

            @Option(names = {"--key"}, required = true, description = "Property key (e.g., typeMapping.rulesPath)")
            private String key;

            @Option(names = {"--value"}, required = true, description = "Property value")
            private String value;

            @Override
            public void run() {
                try {
                    java.util.UUID taskId = resolveTaskId(taskIdentifier);
                    var task = taskRepository.findById(taskId)
                            .orElseThrow(() -> new RuntimeException("Task not found: " + taskIdentifier));

                    var update = new com.dbsyncer.metadata.dto.TaskUpdateRequest();
                    if ("target".equalsIgnoreCase(scope)) {
                        java.util.Map<String, Object> m = task.getTargetProperties();
                        if (m == null) m = new java.util.HashMap<>(); else m = new java.util.HashMap<>(m);
                        m.put(key, value);
                        update.setTargetProperties(m);
                    } else {
                        java.util.Map<String, Object> m = task.getSourceProperties();
                        if (m == null) m = new java.util.HashMap<>(); else m = new java.util.HashMap<>(m);
                        m.put(key, value);
                        update.setSourceProperties(m);
                    }

                    var resp = taskService.updateTask(taskId, update);
                    System.out.println("Property set successfully for task '" + resp.getTaskName() + "': " + scope + "." + key + "=" + value);
                } catch (Exception e) {
                    System.err.println("Error setting task property: " + e.getMessage());
                    System.exit(1);
                }
            }

            private java.util.UUID resolveTaskId(String identifier) {
                try {
                    return java.util.UUID.fromString(identifier);
                } catch (IllegalArgumentException e) {
                    return java.util.UUID.fromString(taskRepository.findByTaskName(identifier)
                            .orElseThrow(() -> new RuntimeException("Task not found: " + identifier)).getId().toString());
                }
            }
        }
    }

    @Component
    @Command(name = "create", description = "Create a new migration task")
    public static class CreateCommand implements Runnable {

        private final CliTaskService taskService;
        private final OutputFormatter formatter;

        @Option(names = {"-n", "--name"}, required = true, description = "Task name")
        private String taskName;

        @Option(names = {"-d", "--description"}, description = "Task description")
        private String description;

        @Option(names = {"--source-type"}, required = true, description = "Source database type (MYSQL, POSTGRESQL, ORACLE)")
        private DatabaseType sourceType;

        @Option(names = {"--source-host"}, required = true, description = "Source database host")
        private String sourceHost;

        @Option(names = {"--source-port"}, required = true, description = "Source database port")
        private Integer sourcePort;

        @Option(names = {"--source-db"}, required = true, description = "Source database name")
        private String sourceDatabase;

        @Option(names = {"--source-user"}, required = true, description = "Source database username")
        private String sourceUsername;

        @Option(names = {"--source-pass"}, required = true, description = "Source database password")
        private String sourcePassword;

        @Option(names = {"--target-type"}, required = true, description = "Target database type (MYSQL, POSTGRESQL, ORACLE)")
        private DatabaseType targetType;

        @Option(names = {"--target-host"}, required = true, description = "Target database host")
        private String targetHost;

        @Option(names = {"--target-port"}, required = true, description = "Target database port")
        private Integer targetPort;

        @Option(names = {"--target-db"}, required = true, description = "Target database name")
        private String targetDatabase;

        @Option(names = {"--target-user"}, required = true, description = "Target database username")
        private String targetUsername;

        @Option(names = {"--target-pass"}, required = true, description = "Target database password")
        private String targetPassword;

        @Option(names = {"--snapshot-mode"}, description = "Debezium snapshot.mode (initial, initial_only, never, schema_only, no_data)", defaultValue = "initial")
        private String snapshotMode = "initial";

        @Option(names = {"--incremental-snapshot"}, description = "Enable Debezium incremental snapshot (initial_only + chunking)")
        private Boolean incrementalSnapshot = false;

        @Option(names = {"--snapshot-chunk-size"}, description = "Incremental snapshot chunk size", defaultValue = "10000")
        private Integer snapshotChunkSize = 10000;

        @Option(names = {"--parallel-tables"}, description = "Parallel tables (JDBC sink tasks.max)", defaultValue = "1")
        private Integer parallelTables = 1;

        @Option(names = {"-f", "--format"}, description = "Output format (table, json, yaml)", defaultValue = "table")
        private String outputFormat;

        public CreateCommand(CliTaskService taskService, OutputFormatter formatter) {
            this.taskService = taskService;
            this.formatter = formatter;
        }

        @Override
        public void run() {
            try {
                TaskCreateRequest request = new TaskCreateRequest();
                request.setTaskName(taskName);
                request.setDescription(description);
                request.setSourceType(sourceType);
                request.setSourceHost(sourceHost);
                request.setSourcePort(sourcePort);
                request.setSourceDatabase(sourceDatabase);
                request.setSourceUsername(sourceUsername);
                request.setSourcePassword(sourcePassword);
                request.setTargetType(targetType);
                request.setTargetHost(targetHost);
                request.setTargetPort(targetPort);
                request.setTargetDatabase(targetDatabase);
                request.setTargetUsername(targetUsername);
                request.setTargetPassword(targetPassword);
                request.setSnapshotMode(snapshotMode);
                request.setIncrementalSnapshot(incrementalSnapshot);
                request.setSnapshotChunkSize(snapshotChunkSize);
                request.setParallelTables(parallelTables);

                TaskResponse response = taskService.createTask(request);
                System.out.println("Task created successfully!");
                System.out.println(formatter.formatTask(response, outputFormat));
            } catch (Exception e) {
                System.err.println("Error creating task: " + e.getMessage());
                System.exit(1);
            }
        }
    }

    @Component
    @Command(name = "list", description = "List all migration tasks")
    public static class ListCommand implements Runnable {

        private final CliTaskService taskService;
        private final OutputFormatter formatter;

        @Option(names = {"--status"}, description = "Filter by status")
        private TaskStatus status;

        @Option(names = {"--page"}, description = "Page number", defaultValue = "0")
        private int page;

        @Option(names = {"--size"}, description = "Page size", defaultValue = "10")
        private int size;

        @Option(names = {"-f", "--format"}, description = "Output format (table, json, yaml)", defaultValue = "table")
        private String outputFormat;

        public ListCommand(CliTaskService taskService, OutputFormatter formatter) {
            this.taskService = taskService;
            this.formatter = formatter;
        }

        @Override
        public void run() {
            try {
                List<TaskResponse> tasks;
                if (status != null) {
                    tasks = taskService.getTasksByStatus(status);
                } else {
                    tasks = taskService.getAllTasks(page, size);
                }
                System.out.println(formatter.formatTaskList(tasks, outputFormat));
            } catch (Exception e) {
                System.err.println("Error listing tasks: " + e.getMessage());
                System.exit(1);
            }
        }
    }

    @Component
    @Command(name = "show", description = "Show details of a specific task")
    public static class ShowCommand implements Runnable {

        private final CliTaskService taskService;
        private final OutputFormatter formatter;

        @Parameters(index = "0", description = "Task ID or name")
        private String taskIdentifier;

        @Option(names = {"-f", "--format"}, description = "Output format (table, json, yaml)", defaultValue = "table")
        private String outputFormat;

        public ShowCommand(CliTaskService taskService, OutputFormatter formatter) {
            this.taskService = taskService;
            this.formatter = formatter;
        }

        @Override
        public void run() {
            try {
                TaskResponse task = taskService.getTask(taskIdentifier);
                System.out.println(formatter.formatTaskDetail(task, outputFormat));
            } catch (Exception e) {
                System.err.println("Error showing task: " + e.getMessage());
                System.exit(1);
            }
        }
    }

    @Component
    @Command(name = "delete", description = "Delete a migration task")
    public static class DeleteCommand implements Runnable {

        private final CliTaskService taskService;

        @Parameters(index = "0", description = "Task ID or name")
        private String taskIdentifier;

        @Option(names = {"--force"}, description = "Force deletion without confirmation")
        private boolean force;

        public DeleteCommand(CliTaskService taskService) {
            this.taskService = taskService;
        }

        @Override
        public void run() {
            try {
                if (!force) {
                    System.out.print("Are you sure you want to delete task '" + taskIdentifier + "'? (yes/no): ");
                    java.util.Scanner scanner = new java.util.Scanner(System.in);
                    String response = scanner.nextLine();
                    if (!response.equalsIgnoreCase("yes") && !response.equalsIgnoreCase("y")) {
                        System.out.println("Deletion cancelled.");
                        return;
                    }
                }

                taskService.deleteTask(taskIdentifier);
                System.out.println("Task '" + taskIdentifier + "' deleted successfully.");
            } catch (Exception e) {
                System.err.println("Error deleting task: " + e.getMessage());
                System.exit(1);
            }
        }
    }

    @Component
    @Command(name = "start", description = "Start a migration task")
    public static class StartCommand implements Runnable {

        private final CliTaskService taskService;
        private final OutputFormatter formatter;

        @Parameters(index = "0", description = "Task ID or name")
        private String taskIdentifier;

        @Option(names = {"-f", "--format"}, description = "Output format (table, json, yaml)", defaultValue = "table")
        private String outputFormat;

        public StartCommand(CliTaskService taskService, OutputFormatter formatter) {
            this.taskService = taskService;
            this.formatter = formatter;
        }

        @Override
        public void run() {
            try {
                TaskResponse task = taskService.startTask(taskIdentifier);
                System.out.println("Task started successfully!");
                System.out.println(formatter.formatTask(task, outputFormat));
            } catch (Exception e) {
                System.err.println("Error starting task: " + e.getMessage());
                System.exit(1);
            }
        }
    }

    @Component
    @Command(name = "stop", description = "Stop a running migration task")
    public static class StopCommand implements Runnable {

        private final CliTaskService taskService;
        private final OutputFormatter formatter;

        @Parameters(index = "0", description = "Task ID or name")
        private String taskIdentifier;

        @Option(names = {"-f", "--format"}, description = "Output format (table, json, yaml)", defaultValue = "table")
        private String outputFormat;

        public StopCommand(CliTaskService taskService, OutputFormatter formatter) {
            this.taskService = taskService;
            this.formatter = formatter;
        }

        @Override
        public void run() {
            try {
                TaskResponse task = taskService.stopTask(taskIdentifier);
                System.out.println("Task stopped successfully!");
                System.out.println(formatter.formatTask(task, outputFormat));
            } catch (Exception e) {
                System.err.println("Error stopping task: " + e.getMessage());
                System.exit(1);
            }
        }
    }

    @Component
    @Command(name = "pause", description = "Pause a running migration task")
    public static class PauseCommand implements Runnable {

        private final CliTaskService taskService;
        private final OutputFormatter formatter;

        @Parameters(index = "0", description = "Task ID or name")
        private String taskIdentifier;

        @Option(names = {"-f", "--format"}, description = "Output format (table, json, yaml)", defaultValue = "table")
        private String outputFormat;

        public PauseCommand(CliTaskService taskService, OutputFormatter formatter) {
            this.taskService = taskService;
            this.formatter = formatter;
        }

        @Override
        public void run() {
            try {
                TaskResponse task = taskService.pauseTask(taskIdentifier);
                System.out.println("Task paused successfully!");
                System.out.println(formatter.formatTask(task, outputFormat));
            } catch (Exception e) {
                System.err.println("Error pausing task: " + e.getMessage());
                System.exit(1);
            }
        }
    }

    @Component
    @Command(name = "resume", description = "Resume a paused migration task")
    public static class ResumeCommand implements Runnable {

        private final CliTaskService taskService;
        private final OutputFormatter formatter;

        @Parameters(index = "0", description = "Task ID or name")
        private String taskIdentifier;

        @Option(names = {"-f", "--format"}, description = "Output format (table, json, yaml)", defaultValue = "table")
        private String outputFormat;

        public ResumeCommand(CliTaskService taskService, OutputFormatter formatter) {
            this.taskService = taskService;
            this.formatter = formatter;
        }

        @Override
        public void run() {
            try {
                TaskResponse task = taskService.resumeTask(taskIdentifier);
                System.out.println("Task resumed successfully!");
                System.out.println(formatter.formatTask(task, outputFormat));
            } catch (Exception e) {
                System.err.println("Error resuming task: " + e.getMessage());
                System.exit(1);
            }
        }
    }

    @Component
    @Command(name = "logs", description = "Show execution logs for a task")
    public static class LogsCommand implements Runnable {

        private final CliTaskService taskService;

        @Parameters(index = "0", description = "Task ID or name")
        private String taskIdentifier;

        @Option(names = {"--level"}, description = "Filter by log level (INFO, WARN, ERROR)")
        private String level;

        @Option(names = {"--limit"}, description = "Max number of log entries to show", defaultValue = "100")
        private int limit;

        public LogsCommand(CliTaskService taskService) {
            this.taskService = taskService;
        }

        @Override
        public void run() {
            try {
                TaskResponse task = taskService.getTask(taskIdentifier);
                java.util.UUID taskId = task.getId();

                System.out.println("=== Task Logs ===");
                System.out.println("ID:   " + task.getId());
                System.out.println("Name: " + task.getTaskName());
                System.out.println("Showing last " + limit + " log entries"
                        + (level != null && !level.isBlank() ? " with level " + level.toUpperCase() : "") + ":\n");

                java.util.List<com.dbsyncer.metadata.entity.TaskLog> logs =
                        taskService.getTaskLogs(taskId, level, limit);

                if (logs.isEmpty()) {
                    System.out.println("No logs found for this task.");
                    return;
                }

                System.out.printf("%-23s  %-5s  %-15s  %s%n",
                        "TIME", "LVL", "SOURCE", "MESSAGE");
                System.out.println("-".repeat(80));

                for (com.dbsyncer.metadata.entity.TaskLog log : logs) {
                    String time = log.getLoggedAt() != null
                            ? log.getLoggedAt().toString()
                            : "";
                    String lvl = log.getLogLevel() != null ? log.getLogLevel() : "";
                    String src = log.getSourceComponent() != null ? log.getSourceComponent() : "";
                    String msg = log.getMessage() != null ? log.getMessage() : "";
                    System.out.printf("%-23s  %-5s  %-15s  %s%n",
                            truncate(time, 23),
                            truncate(lvl, 5),
                            truncate(src, 15),
                            msg);
                }
            } catch (Exception e) {
                System.err.println("Error showing task logs: " + e.getMessage());
                System.exit(1);
            }
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
}
