package com.dbsyncer.transformations.schema;

import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaBuilder;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class SchemaConverterTest {

    @Test
    void mapsBasicRowSchema() {
        Schema row = SchemaBuilder.struct().name("row")
                .field("id", Schema.INT64_SCHEMA)
                .field("name", SchemaBuilder.string().parameter("length", "200").build())
                .field("price", SchemaBuilder.string().name("org.apache.kafka.connect.data.Decimal").parameter("scale", "2").parameter("connect.decimal.precision", "18").build())
                .field("payload", SchemaBuilder.string().name("io.debezium.data.Json").optional().build())
                .field("created_at", SchemaBuilder.int64().name("io.debezium.time.Timestamp").build())
                .build();

        SchemaConverter converter = new SchemaConverter();
        List<ColumnDefinition> cols = converter.toPostgresColumns("mysql", row);

        assertThat(cols).extracting(ColumnDefinition::getName)
                .containsExactly("id", "name", "price", "payload", "created_at");
        assertThat(cols.get(0).getPostgresType()).isEqualTo("bigint");
        assertThat(cols.get(1).getPostgresType()).isEqualTo("varchar(200)");
        assertThat(cols.get(2).getPostgresType()).isEqualTo("numeric(18,2)");
        assertThat(cols.get(3).getPostgresType()).isEqualTo("jsonb");
        assertThat(cols.get(4).getPostgresType()).contains("timestamp");
    }
}

