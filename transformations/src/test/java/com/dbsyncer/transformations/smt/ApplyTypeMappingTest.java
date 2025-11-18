package com.dbsyncer.transformations.smt;

import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaBuilder;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.source.SourceRecord;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;

public class ApplyTypeMappingTest {

    @Test
    void rewritesDebeziumDateAndJson() {
        // Build a Debezium envelope-like record with 'after' struct
        Schema afterSchema = SchemaBuilder.struct().name("dbz.Value")
                .field("d", SchemaBuilder.int32().name("io.debezium.time.Date").optional().build())
                .field("j", SchemaBuilder.string().name("io.debezium.data.Json").optional().build())
                .build();
        Schema valueSchema = SchemaBuilder.struct().name("dbz.Envelope")
                .field("before", afterSchema)
                .field("after", afterSchema)
                .field("op", Schema.STRING_SCHEMA)
                .field("ts_ms", Schema.OPTIONAL_INT64_SCHEMA)
                .build();

        Struct after = new Struct(afterSchema)
                .put("d", 2)
                .put("j", "{\"a\":1}");
        Struct value = new Struct(valueSchema)
                .put("before", new Struct(afterSchema).put("d", null).put("j", null))
                .put("after", after)
                .put("op", "c")
                .put("ts_ms", 0L);

        SourceRecord record = new SourceRecord(new HashMap<>(), new HashMap<>(),
                "topic", null, null, null, valueSchema, value);

        ApplyTypeMapping<SourceRecord> smt = new ApplyTypeMapping<>();
        smt.configure(new HashMap<>() {{
            put(ApplyTypeMapping.CONFIG_SOURCE_DB, "mysql");
        }});

        SourceRecord out = smt.apply(record);

        Schema outSchema = out.valueSchema();
        Struct outValue = (Struct) out.value();

        // Only 'after' schema/value changed
        Schema outAfterSchema = outSchema.field("after").schema();
        Struct outAfter = (Struct) outValue.get("after");
        assertThat(outAfterSchema.field("d").schema().name())
                .isEqualTo(org.apache.kafka.connect.data.Date.SCHEMA.name());
        assertThat(outAfter.get("d")).isInstanceOf(Date.class);

        assertThat(outAfterSchema.field("j").schema().type()).isEqualTo(Schema.Type.STRING);
        assertThat(outAfterSchema.field("j").schema().name()).isNull();
        assertThat((String) outAfter.get("j")).isEqualTo("{\"a\":1}");

        // Metadata fields untouched
        assertThat(outSchema.field("op").schema().type()).isEqualTo(Schema.Type.STRING);
        assertThat(outSchema.field("ts_ms").schema().type()).isEqualTo(Schema.Type.INT64);

        smt.close();
    }

    @Test
    void rewritesVariableScaleDecimalToString() {
        // Build VariableScaleDecimal struct
        Schema vsd = SchemaBuilder.struct().name("io.debezium.data.VariableScaleDecimal")
                .field("scale", Schema.INT32_SCHEMA)
                .field("value", Schema.BYTES_SCHEMA)
                .optional()
                .build();

        Schema afterSchema = SchemaBuilder.struct().name("dbz.Value")
                .field("v", vsd)
                .build();

        Schema valueSchema = SchemaBuilder.struct().name("dbz.Envelope")
                .field("before", afterSchema)
                .field("after", afterSchema)
                .field("op", Schema.STRING_SCHEMA)
                .build();

        java.math.BigInteger bi = java.math.BigInteger.valueOf(12345);
        byte[] bytes = bi.toByteArray();
        Struct vsdVal = new Struct(vsd)
                .put("scale", 2)
                .put("value", bytes);
        Struct after = new Struct(afterSchema)
                .put("v", vsdVal);
        Struct value = new Struct(valueSchema)
                .put("before", new Struct(afterSchema).put("v", null))
                .put("after", after)
                .put("op", "c");

        SourceRecord record = new SourceRecord(new HashMap<>(), new HashMap<>(),
                "topic", null, null, null, valueSchema, value);

        ApplyTypeMapping<SourceRecord> smt = new ApplyTypeMapping<>();
        smt.configure(new HashMap<>() {{
            put(ApplyTypeMapping.CONFIG_SOURCE_DB, "mysql");
            put(ApplyTypeMapping.CONFIG_ENABLE_DECIMAL_MAPPING, true);
            put(ApplyTypeMapping.CONFIG_DECIMAL_TARGET, "string");
        }});

        SourceRecord out = smt.apply(record);
        Struct outAfter = (Struct) ((Struct) out.value()).get("after");
        Object v = outAfter.get("v");
        assertThat(v).isInstanceOf(String.class);
        assertThat((String) v).isEqualTo("123.45");
        smt.close();
    }

    @Test
    void rewritesVariableScaleDecimalToConnectDecimal() {
        Schema vsd = SchemaBuilder.struct().name("io.debezium.data.VariableScaleDecimal")
                .field("scale", Schema.INT32_SCHEMA)
                .field("value", Schema.BYTES_SCHEMA)
                .optional()
                .build();

        Schema afterSchema = SchemaBuilder.struct().name("dbz.Value")
                .field("v", vsd)
                .build();

        Schema valueSchema = SchemaBuilder.struct().name("dbz.Envelope")
                .field("before", afterSchema)
                .field("after", afterSchema)
                .field("op", Schema.STRING_SCHEMA)
                .build();

        java.math.BigInteger bi = java.math.BigInteger.valueOf(12345);
        byte[] bytes = bi.toByteArray();
        Struct vsdVal = new Struct(vsd)
                .put("scale", 2)
                .put("value", bytes);
        Struct after = new Struct(afterSchema)
                .put("v", vsdVal);
        Struct value = new Struct(valueSchema)
                .put("before", new Struct(afterSchema).put("v", null))
                .put("after", after)
                .put("op", "c");

        SourceRecord record = new SourceRecord(new HashMap<>(), new HashMap<>(),
                "topic", null, null, null, valueSchema, value);

        ApplyTypeMapping<SourceRecord> smt = new ApplyTypeMapping<>();
        smt.configure(new HashMap<>() {{
            put(ApplyTypeMapping.CONFIG_SOURCE_DB, "mysql");
            put(ApplyTypeMapping.CONFIG_ENABLE_DECIMAL_MAPPING, true);
            put(ApplyTypeMapping.CONFIG_DECIMAL_TARGET, "decimal");
        }});

        SourceRecord out = smt.apply(record);
        Struct outAfter = (Struct) ((Struct) out.value()).get("after");
        Object v = outAfter.get("v");
        assertThat(v).isInstanceOf(java.math.BigDecimal.class);
        assertThat(((java.math.BigDecimal) v).toPlainString()).isEqualTo("123.45");
        smt.close();
    }
}
