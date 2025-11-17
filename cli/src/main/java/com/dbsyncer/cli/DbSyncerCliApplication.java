package com.dbsyncer.cli;

import com.dbsyncer.cli.command.DbSyncerCommand;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import picocli.CommandLine;
import picocli.CommandLine.IFactory;

@SpringBootApplication
@ComponentScan(basePackages = {"com.dbsyncer.cli", "com.dbsyncer.metadata"})
@EntityScan(basePackages = "com.dbsyncer.metadata.entity")
@EnableJpaRepositories(basePackages = "com.dbsyncer.metadata.repository")
public class DbSyncerCliApplication implements CommandLineRunner, ExitCodeGenerator {

    private final IFactory factory;
    private final DbSyncerCommand dbSyncerCommand;
    private int exitCode;

    public DbSyncerCliApplication(IFactory factory, DbSyncerCommand dbSyncerCommand) {
        this.factory = factory;
        this.dbSyncerCommand = dbSyncerCommand;
    }

    public static void main(String[] args) {
        System.exit(SpringApplication.exit(SpringApplication.run(DbSyncerCliApplication.class, args)));
    }

    @Override
    public void run(String... args) throws Exception {
        exitCode = new CommandLine(dbSyncerCommand, factory).execute(args);
    }

    @Override
    public int getExitCode() {
        return exitCode;
    }
}
