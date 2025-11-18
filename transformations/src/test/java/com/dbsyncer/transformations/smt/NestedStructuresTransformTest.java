package com.dbsyncer.transformations.smt;

import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaBuilder;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.source.SourceRecord;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class NestedStructuresTransformTest {

    @Test
    void transformsArrayOfStructsWithDebeziumDate() {
        Schema rowSchema = SchemaBuilder.struct().name("row")
                .field("d", SchemaBuilder.int32().name("io.debezium.time.Date").optional().build())
                .build();
        Schema afterSchema = SchemaBuilder.struct().name("dbz.Value")
                .field("rows", SchemaBuilder.array(rowSchema).optional().build())
                .build();
        Schema env = SchemaBuilder.struct().name("dbz.Envelope")
                .field("before", afterSchema)
                .field("after", afterSchema)
                .field("op", Schema.STRING_SCHEMA)
                .build();

        List<Object> rows = new ArrayList<>();
        rows.add(new Struct(rowSchema).put("d", 1));
        rows.add(new Struct(rowSchema).put("d", 2));

        Struct after = new Struct(afterSchema).put("rows", rows);
        Struct value = new Struct(env)
                .put("before", new Struct(afterSchema).put("rows", null))
                .put("after", after)
                .put("op", "c");

        SourceRecord record = new SourceRecord(new HashMap<>(), new HashMap<>(),
                "topic", null, null, null, env, value);

        ApplyTypeMapping<SourceRecord> smt = new ApplyTypeMapping<>();
        smt.configure(new HashMap<>() {{
            put(ApplyTypeMapping.CONFIG_SOURCE_DB, "mysql");
        }});

        SourceRecord out = smt.apply(record);
        Struct outAfter = (Struct) ((Struct) out.value()).get("after");
        Schema outRowSchema = ((Schema) outAfter.schema().field("rows").schema().valueSchema());
        assertThat(outRowSchema.field("d").schema().name())
                .isEqualTo(org.apache.kafka.connect.data.Date.SCHEMA.name());

        List<?> outRows = (List<?>) outAfter.get("rows");
        assertThat(((Struct) outRows.get(0)).get("d")).isInstanceOf(java.util.Date.class);
        smt.close();
    }

    @Test
    void transformsMapValuesWithDebeziumJson() {
        Schema mapVal = SchemaBuilder.string().name("io.debezium.data.Json").optional().build();
        Schema afterSchema = SchemaBuilder.struct().name("dbz.Value")
                .field("attrs", SchemaBuilder.map(Schema.STRING_SCHEMA, mapVal).optional().build())
                .build();
        Schema env = SchemaBuilder.struct().name("dbz.Envelope")
                .field("before", afterSchema)
                .field("after", afterSchema)
                .field("op", Schema.STRING_SCHEMA)
                .build();

        Map<String, Object> attrs = new HashMap<>();
        attrs.put("a", "{\"x\":1}");

        Struct after = new Struct(afterSchema).put("attrs", attrs);
        Struct value = new Struct(env)
                .put("before", new Struct(afterSchema).put("attrs", null))
                .put("after", after)
                .put("op", "c");

        SourceRecord record = new SourceRecord(new HashMap<>(), new HashMap<>(),
                "topic", null, null, null, env, value);

        ApplyTypeMapping<SourceRecord> smt = new ApplyTypeMapping<>();
        smt.configure(new HashMap<>() {{
            put(ApplyTypeMapping.CONFIG_SOURCE_DB, "mysql");
        }});

        SourceRecord out = smt.apply(record);
        Struct outAfter = (Struct) ((Struct) out.value()).get("after");
        Schema outMapValSchema = outAfter.schema().field("attrs").schema().valueSchema();
        assertThat(outMapValSchema.name()).isNull();
        assertThat(outMapValSchema.type()).isEqualTo(Schema.Type.STRING);
        smt.close();
    }
}

