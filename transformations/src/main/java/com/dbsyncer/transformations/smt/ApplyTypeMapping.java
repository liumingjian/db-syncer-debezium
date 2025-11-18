package com.dbsyncer.transformations.smt;

import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.connect.connector.ConnectRecord;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaBuilder;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.data.Decimal;
import org.apache.kafka.connect.data.Time;
import org.apache.kafka.connect.data.Timestamp;
import org.apache.kafka.connect.data.Date;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.transforms.Transformation;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * SMT that normalizes Debezium logical types (Date/Json/VariableScaleDecimal)
 * into standard Kafka Connect logical/primitive types.
 */
public class ApplyTypeMapping<R extends ConnectRecord<R>> implements Transformation<R> {

    public static final String CONFIG_SOURCE_DB = "source.db";
    public static final String CONFIG_ENABLE_DECIMAL_MAPPING = "enable.decimal.mapping";
    public static final String CONFIG_DECIMAL_TARGET = "decimal.target";

    private String sourceDb;
    private boolean enableTimeMapping = true;
    private boolean enableJsonMapping = true;
    private boolean enableDecimalMapping;
    private String decimalTarget = "string";

    @Override
    public R apply(R record) {
        Schema valueSchema = record.valueSchema();
        Object value = record.value();
        if (valueSchema == null || !(value instanceof Struct)) {
            return record;
        }

        Schema transformedSchema = transformSchema(valueSchema);
        Object transformedValue = transformValue(value, valueSchema, transformedSchema);

        return record.newRecord(
                record.topic(),
                record.kafkaPartition(),
                record.keySchema(),
                record.key(),
                transformedSchema,
                transformedValue,
                record.timestamp()
        );
    }

    @Override
    public ConfigDef config() {
        return new ConfigDef()
                .define(CONFIG_SOURCE_DB, ConfigDef.Type.STRING, null, ConfigDef.Importance.MEDIUM,
                        "Logical source database type (mysql/oracle/postgresql)")
                .define(CONFIG_ENABLE_DECIMAL_MAPPING, ConfigDef.Type.BOOLEAN, false, ConfigDef.Importance.LOW,
                        "Enable VariableScaleDecimal conversion")
                .define(CONFIG_DECIMAL_TARGET, ConfigDef.Type.STRING, "string", ConfigDef.Importance.LOW,
                        "Target representation for VariableScaleDecimal (string|decimal)");
    }

    @Override
    public void close() {
        // no-op
    }

    @Override
    public void configure(java.util.Map<String, ?> configs) {
        Object db = configs.get(CONFIG_SOURCE_DB);
        this.sourceDb = db != null ? db.toString() : null;

        Object decEnabled = configs.get(CONFIG_ENABLE_DECIMAL_MAPPING);
        if (decEnabled != null) {
            this.enableDecimalMapping = Boolean.parseBoolean(decEnabled.toString());
        }
        Object decTarget = configs.get(CONFIG_DECIMAL_TARGET);
        if (decTarget != null) {
            this.decimalTarget = decTarget.toString().toLowerCase(Locale.ROOT);
        }
    }

    private Schema transformSchema(Schema schema) {
        if (schema == null) {
            return null;
        }

        String name = schema.name();

        if (enableDecimalMapping && "io.debezium.data.VariableScaleDecimal".equals(name)) {
            if ("decimal".equals(decimalTarget)) {
                SchemaBuilder builder = Decimal.builder(0);
                if (schema.isOptional()) {
                    builder.optional();
                }
                return builder.build();
            } else {
                SchemaBuilder builder = SchemaBuilder.string();
                if (schema.isOptional()) {
                    builder.optional();
                }
                return builder.build();
            }
        }

        if (enableTimeMapping && "io.debezium.time.Date".equals(name)) {
            SchemaBuilder builder = Date.builder();
            if (schema.isOptional()) {
                builder.optional();
            }
            return builder.build();
        }
        if (enableTimeMapping && "io.debezium.time.Time".equals(name)) {
            SchemaBuilder builder = Time.builder();
            if (schema.isOptional()) {
                builder.optional();
            }
            return builder.build();
        }
        if (enableTimeMapping && ( "io.debezium.time.Timestamp".equals(name)
                || "io.debezium.time.MicroTimestamp".equals(name)
                || "io.debezium.time.NanoTimestamp".equals(name))) {
            SchemaBuilder builder = Timestamp.builder();
            if (schema.isOptional()) {
                builder.optional();
            }
            return builder.build();
        }

        if (enableJsonMapping && "io.debezium.data.Json".equals(name)) {
            SchemaBuilder builder = SchemaBuilder.string();
            if (schema.isOptional()) {
                builder.optional();
            }
            return builder.build();
        }

        return switch (schema.type()) {
            case STRUCT -> {
                SchemaBuilder builder = SchemaBuilder.struct().name(schema.name());
                if (schema.isOptional()) {
                    builder.optional();
                }
                schema.fields().forEach(f ->
                        builder.field(f.name(), transformSchema(f.schema())));
                yield builder.build();
            }
            case ARRAY -> {
                Schema valueSchema = transformSchema(schema.valueSchema());
                SchemaBuilder builder = SchemaBuilder.array(valueSchema);
                if (schema.isOptional()) {
                    builder.optional();
                }
                builder.name(schema.name());
                yield builder.build();
            }
            case MAP -> {
                Schema keySchema = transformSchema(schema.keySchema());
                Schema valueSchema = transformSchema(schema.valueSchema());
                SchemaBuilder builder = SchemaBuilder.map(keySchema, valueSchema);
                if (schema.isOptional()) {
                    builder.optional();
                }
                builder.name(schema.name());
                yield builder.build();
            }
            default -> schema;
        };
    }

    private Object transformValue(Object value, Schema sourceSchema, Schema targetSchema) {
        if (value == null || sourceSchema == null) {
            return value;
        }

        String name = sourceSchema.name();

        if (enableTimeMapping && "io.debezium.time.Date".equals(name)) {
            int days = ((Number) value).intValue();
            long millis = days * 24L * 60L * 60L * 1000L;
            return new java.util.Date(millis);
        }

        if (enableJsonMapping && "io.debezium.data.Json".equals(name)) {
            return value;
        }

        if (enableDecimalMapping && "io.debezium.data.VariableScaleDecimal".equals(name)) {
            if (!(value instanceof Struct struct)) {
                return value;
            }
            Integer scale = (Integer) struct.get("scale");
            Object bytesObj = struct.get("value");
            if (scale == null || bytesObj == null) {
                return null;
            }
            byte[] bytes = (byte[]) bytesObj;
            BigInteger unscaled = new BigInteger(bytes);
            BigDecimal decimal = new BigDecimal(unscaled, scale);
            if ("decimal".equals(decimalTarget)) {
                return decimal;
            }
            return decimal.toPlainString();
        }

        return switch (sourceSchema.type()) {
            case STRUCT -> transformStructValue(value, sourceSchema, targetSchema);
            case ARRAY -> transformArrayValue(value, sourceSchema, targetSchema);
            case MAP -> transformMapValue(value, sourceSchema, targetSchema);
            default -> value;
        };
    }

    private Object transformStructValue(Object value, Schema sourceSchema, Schema targetSchema) {
        if (!(value instanceof Struct sourceStruct) || targetSchema == null) {
            return value;
        }
        Struct targetStruct = new Struct(targetSchema);
        for (org.apache.kafka.connect.data.Field field : sourceSchema.fields()) {
            Schema srcFieldSchema = field.schema();
            org.apache.kafka.connect.data.Field targetField = targetSchema.field(field.name());
            Schema tgtFieldSchema = targetField != null ? targetField.schema() : srcFieldSchema;
            Object fieldValue = sourceStruct.get(field.name());
            Object transformed = transformValue(fieldValue, srcFieldSchema, tgtFieldSchema);
            targetStruct.put(field.name(), transformed);
        }
        return targetStruct;
    }

    private Object transformArrayValue(Object value, Schema sourceSchema, Schema targetSchema) {
        if (!(value instanceof List<?> list)) {
            return value;
        }
        Schema srcElemSchema = sourceSchema.valueSchema();
        Schema tgtElemSchema = targetSchema != null ? targetSchema.valueSchema() : srcElemSchema;
        List<Object> result = new ArrayList<>(list.size());
        for (Object elem : list) {
            result.add(transformValue(elem, srcElemSchema, tgtElemSchema));
        }
        return result;
    }

    private Object transformMapValue(Object value, Schema sourceSchema, Schema targetSchema) {
        if (!(value instanceof Map<?, ?> map)) {
            return value;
        }
        Schema srcKeySchema = sourceSchema.keySchema();
        Schema srcValSchema = sourceSchema.valueSchema();
        Schema tgtKeySchema = targetSchema != null ? targetSchema.keySchema() : srcKeySchema;
        Schema tgtValSchema = targetSchema != null ? targetSchema.valueSchema() : srcValSchema;
        Map<Object, Object> result = new java.util.LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            Object key = transformValue(entry.getKey(), srcKeySchema, tgtKeySchema);
            Object val = transformValue(entry.getValue(), srcValSchema, tgtValSchema);
            result.put(key, val);
        }
        return result;
    }
}
