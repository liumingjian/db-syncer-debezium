package com.dbsyncer.cli.command;

import com.dbsyncer.cli.formatter.OutputFormatter;
import com.dbsyncer.cli.service.CliConfigService;
import com.dbsyncer.metadata.entity.ConnectorConfig;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Command(
    name = "config",
    description = "Manage connector configurations",
    mixinStandardHelpOptions = true,
    subcommands = {
        ConfigCommand.ShowCommand.class,
        ConfigCommand.SetCommand.class,
        ConfigCommand.ListCommand.class,
        ConfigCommand.GenerateCommand.class
    }
)
public class ConfigCommand implements Runnable {

    @Override
    public void run() {
        System.out.println("Configuration management commands. Use 'dbsyncer config --help' for available subcommands.");
    }

    @Component
    @Command(name = "show", description = "Show connector configuration for a task")
    public static class ShowCommand implements Runnable {

        private final CliConfigService configService;
        private final OutputFormatter formatter;

        @Parameters(index = "0", description = "Task ID or name")
        private String taskIdentifier;

        @Option(names = {"--type"}, description = "Connector type (source, sink)")
        private String connectorType;

        @Option(names = {"-f", "--format"}, description = "Output format (table, json, yaml)", defaultValue = "json")
        private String outputFormat;

        public ShowCommand(CliConfigService configService, OutputFormatter formatter) {
            this.configService = configService;
            this.formatter = formatter;
        }

        @Override
        public void run() {
            try {
                List<ConnectorConfig> configs = configService.getConfigs(taskIdentifier);
                if (connectorType != null) {
                    configs = configs.stream()
                        .filter(c -> c.getConnectorType().name().equalsIgnoreCase(connectorType))
                        .toList();
                }
                System.out.println(formatter.formatConnectorConfigs(configs, outputFormat));
            } catch (Exception e) {
                System.err.println("Error showing configuration: " + e.getMessage());
                System.exit(1);
            }
        }
    }

    @Component
    @Command(name = "set", description = "Set a connector configuration property")
    public static class SetCommand implements Runnable {

        private final CliConfigService configService;

        @Parameters(index = "0", description = "Task ID or name")
        private String taskIdentifier;

        @Option(names = {"--type"}, required = true, description = "Connector type (source, sink)")
        private String connectorType;

        @Option(names = {"-p", "--property"}, required = true, description = "Property key=value pairs")
        private Map<String, String> properties = new HashMap<>();

        public SetCommand(CliConfigService configService) {
            this.configService = configService;
        }

        @Override
        public void run() {
            try {
                configService.updateConfig(taskIdentifier, connectorType, properties);
                System.out.println("Configuration updated successfully for task '" + taskIdentifier + "'");
            } catch (Exception e) {
                System.err.println("Error setting configuration: " + e.getMessage());
                System.exit(1);
            }
        }
    }

    @Component
    @Command(name = "list", description = "List all connector configurations")
    public static class ListCommand implements Runnable {

        private final CliConfigService configService;
        private final OutputFormatter formatter;

        @Option(names = {"-f", "--format"}, description = "Output format (table, json, yaml)", defaultValue = "table")
        private String outputFormat;

        public ListCommand(CliConfigService configService, OutputFormatter formatter) {
            this.configService = configService;
            this.formatter = formatter;
        }

        @Override
        public void run() {
            try {
                List<ConnectorConfig> configs = configService.getAllConfigs();
                System.out.println(formatter.formatConnectorConfigList(configs, outputFormat));
            } catch (Exception e) {
                System.err.println("Error listing configurations: " + e.getMessage());
                System.exit(1);
            }
        }
    }

    @Component
    @Command(name = "generate", description = "Generate connector configuration template")
    public static class GenerateCommand implements Runnable {

        private final CliConfigService configService;

        @Parameters(index = "0", description = "Task ID or name")
        private String taskIdentifier;

        @Option(names = {"--type"}, required = true, description = "Connector type (source, sink)")
        private String connectorType;

        @Option(names = {"-o", "--output"}, description = "Output file path")
        private String outputFile;

        @Option(names = {"--enable-type-mapping"}, description = "Enable ApplyTypeMapping SMT on source connector", defaultValue = "false")
        private boolean enableTypeMapping;

        @Option(names = {"--type-mapping-source-db"}, description = "Source DB for type mapping (mysql, oracle, postgresql)")
        private String typeMappingSourceDb;

        @Option(names = {"--type-mapping-enable-time"}, description = "Enable time logical mapping (default: true)", defaultValue = "true")
        private boolean typeMappingEnableTime;

        @Option(names = {"--type-mapping-enable-json"}, description = "Enable JSON logical mapping (default: true)", defaultValue = "true")
        private boolean typeMappingEnableJson;

        public GenerateCommand(CliConfigService configService) {
            this.configService = configService;
        }

        @Override
        public void run() {
            try {
                String template = configService.generateConfigTemplate(taskIdentifier, connectorType,
                        enableTypeMapping, typeMappingSourceDb,
                        typeMappingEnableTime, typeMappingEnableJson);
                if (outputFile != null) {
                    java.nio.file.Files.writeString(java.nio.file.Path.of(outputFile), template);
                    System.out.println("Configuration template written to: " + outputFile);
                } else {
                    System.out.println(template);
                }
            } catch (Exception e) {
                System.err.println("Error generating configuration: " + e.getMessage());
                System.exit(1);
            }
        }
    }
}
