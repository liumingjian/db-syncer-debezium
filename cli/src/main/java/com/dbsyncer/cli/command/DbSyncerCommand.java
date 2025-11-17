package com.dbsyncer.cli.command;

import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Component
@Command(
    name = "dbsyncer",
    description = "DB Syncer - Heterogeneous database migration tool based on Debezium CDC",
    mixinStandardHelpOptions = true,
    version = "1.0.0-SNAPSHOT",
    subcommands = {
        TaskCommand.class,
        StatusCommand.class,
        ConfigCommand.class
    }
)
public class DbSyncerCommand implements Runnable {

    @Option(names = {"-v", "--verbose"}, description = "Enable verbose output")
    private boolean verbose;

    @Option(names = {"--server"}, description = "Metadata service URL", defaultValue = "http://localhost:8080")
    private String serverUrl;

    @Override
    public void run() {
        System.out.println("DB Syncer CLI - Use --help for available commands");
        System.out.println();
        System.out.println("Available commands:");
        System.out.println("  task     Manage migration tasks");
        System.out.println("  status   Query task status and progress");
        System.out.println("  config   Manage connector configurations");
        System.out.println();
        System.out.println("Use 'dbsyncer <command> --help' for more information about a command.");
    }

    public boolean isVerbose() {
        return verbose;
    }

    public String getServerUrl() {
        return serverUrl;
    }
}
