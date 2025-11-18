package com.dbsyncer.transformations.schema;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class PostgresDdlGeneratorTest {

    @Test
    void generatesCreateTableDdl() {
        List<ColumnDefinition> cols = List.of(
                ColumnDefinition.builder().name("id").postgresType("bigint").build(),
                ColumnDefinition.builder().name("name").postgresType("varchar(200)").build()
        );

        PostgresDdlGenerator generator = new PostgresDdlGenerator();
        String ddl = generator.generateCreateTable("public", "users", cols);

        assertThat(ddl).isEqualTo("CREATE TABLE IF NOT EXISTS \"public\".\"users\" (\"id\" bigint, \"name\" varchar(200));");
    }

    @Test
    void computesSchemaDiff() {
        List<ColumnDefinition> existing = List.of(
                ColumnDefinition.builder().name("id").postgresType("bigint").build(),
                ColumnDefinition.builder().name("old_col").postgresType("text").build(),
                ColumnDefinition.builder().name("price").postgresType("numeric(10,2)").build()
        );
        List<ColumnDefinition> desired = List.of(
                ColumnDefinition.builder().name("id").postgresType("bigint").build(),
                ColumnDefinition.builder().name("name").postgresType("varchar(200)").build(),
                ColumnDefinition.builder().name("price").postgresType("numeric(18,2)").build()
        );

        PostgresDdlGenerator generator = new PostgresDdlGenerator();
        SchemaDiff diff = generator.diff(existing, desired);

        assertThat(diff.getAddedColumns()).extracting(ColumnDefinition::getName)
                .containsExactly("name");
        assertThat(diff.getRemovedColumns()).extracting(ColumnDefinition::getName)
                .containsExactly("old_col");
        assertThat(diff.getChangedColumns()).extracting(ColumnDefinition::getName)
                .containsExactly("price");
    }
}

