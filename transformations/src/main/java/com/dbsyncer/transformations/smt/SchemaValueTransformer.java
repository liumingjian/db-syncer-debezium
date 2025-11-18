package com.dbsyncer.transformations.smt;

import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaAndValue;
import org.apache.kafka.connect.data.SchemaBuilder;
import org.apache.kafka.connect.data.Struct;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Rewrites Debezium logical types to Kafka Connect logicals and coerces values.
 * Focus: io.debezium.time.* and io.debezium.data.Json.
 */
class SchemaValueTransformer {

    // Debezium logical names
    private static final String DBZ_DATE = "io.debezium.time.Date";               // int32 days
    private static final String DBZ_TIME = "io.debezium.time.Time";               // int32 millis
    private static final String DBZ_TIMESTAMP = "io.debezium.time.Timestamp";     // int64 millis
    private static final String DBZ_MICRO_TS = "io.debezium.time.MicroTimestamp"; // int64 micros
    private static final String DBZ_NANO_TS = "io.debezium.time.NanoTimestamp";   // int64 nanos
    private static final String DBZ_JSON = "io.debezium.data.Json";               // string
    private static final String DBZ_VARIABLE_SCALE_DECIMAL = "io.debezium.data.VariableScaleDecimal"; // struct

    SchemaAndValue transform(Schema schema, Object value,
                             boolean enableTime, boolean enableJson,
                             boolean enableDecimal, String decimalTarget) {
        if (schema == null || value == null) {
            return null;
        }

        // Debezium VariableScaleDecimal is a STRUCT with dedicated logical name
        if (DBZ_VARIABLE_SCALE_DECIMAL.equals(schema.name())) {
            if (enableDecimal) {
                return transformVariableScaleDecimal(schema, value, decimalTarget);
            }
            return new SchemaAndValue(schema, value);
        }

        switch (schema.type()) {
            case STRUCT:
                return transformStruct(schema, value, enableTime, enableJson, enableDecimal, decimalTarget);
            case ARRAY:
                return transformArray(schema, value, enableTime, enableJson, enableDecimal, decimalTarget);
            case MAP:
                return transformMap(schema, value, enableTime, enableJson, enableDecimal, decimalTarget);
            default:
                // primitive with possible logical
                return transformPrimitive(schema, value, enableTime, enableJson);
        }
    }

    private SchemaAndValue transformStruct(Schema schema, Object value,
                                           boolean enableTime, boolean enableJson,
                                           boolean enableDecimal, String decimalTarget) {
        if (!(value instanceof Struct in)) {
            return new SchemaAndValue(schema, value);
        }

        SchemaBuilder outBuilder = SchemaBuilder.struct().name(schema.name());
        copyCommon(schema, outBuilder);

        for (org.apache.kafka.connect.data.Field f : schema.fields()) {
            Schema fieldSchema = f.schema();
            SchemaAndValue sv = transform(fieldSchema, in.get(f), enableTime, enableJson, enableDecimal, decimalTarget);

            Schema mappedSchema = sv != null ? sv.schema() : fieldSchema;
            Object mappedValue = sv != null ? sv.value() : in.get(f);

            outBuilder.field(f.name(), mappedSchema);
        }
        Schema outSchema = outBuilder.build();

        Struct outValue = new Struct(outSchema);
        for (org.apache.kafka.connect.data.Field f : outSchema.fields()) {
            Object original = ((Struct) value).get(f.name());
            Schema originalSchema = schema.field(f.name()).schema();
            SchemaAndValue sv = transform(originalSchema, original, enableTime, enableJson, enableDecimal, decimalTarget);
            outValue.put(f, sv != null ? sv.value() : original);
        }

        return new SchemaAndValue(outSchema, outValue);
    }

    private SchemaAndValue transformPrimitive(Schema schema, Object value, boolean enableTime, boolean enableJson) {
        String name = schema.name();
        if (name == null) {
            return new SchemaAndValue(schema, value);
        }

        // Time logicals
        if (enableTime) {
            if (DBZ_DATE.equals(name)) {
                Schema target = org.apache.kafka.connect.data.Date.SCHEMA;
                if (schema.isOptional()) target = org.apache.kafka.connect.data.Date.builder().optional().build();
                int days = (value instanceof Number) ? ((Number) value).intValue() : 0;
                long millis = days * 24L * 60 * 60 * 1000;
                return new SchemaAndValue(target, new Date(millis));
            }
            if (DBZ_TIME.equals(name)) {
                Schema target = org.apache.kafka.connect.data.Time.SCHEMA;
                if (schema.isOptional()) target = org.apache.kafka.connect.data.Time.builder().optional().build();
                int ms = (value instanceof Number) ? ((Number) value).intValue() : 0;
                return new SchemaAndValue(target, new Date(ms));
            }
            if (DBZ_TIMESTAMP.equals(name)) {
                Schema target = org.apache.kafka.connect.data.Timestamp.SCHEMA;
                if (schema.isOptional()) target = org.apache.kafka.connect.data.Timestamp.builder().optional().build();
                long ms = (value instanceof Number) ? ((Number) value).longValue() : 0L;
                return new SchemaAndValue(target, new Date(ms));
            }
            if (DBZ_MICRO_TS.equals(name)) {
                Schema target = org.apache.kafka.connect.data.Timestamp.SCHEMA;
                if (schema.isOptional()) target = org.apache.kafka.connect.data.Timestamp.builder().optional().build();
                long micros = (value instanceof Number) ? ((Number) value).longValue() : 0L;
                return new SchemaAndValue(target, new Date(micros / 1000));
            }
            if (DBZ_NANO_TS.equals(name)) {
                Schema target = org.apache.kafka.connect.data.Timestamp.SCHEMA;
                if (schema.isOptional()) target = org.apache.kafka.connect.data.Timestamp.builder().optional().build();
                long nanos = (value instanceof Number) ? ((Number) value).longValue() : 0L;
                return new SchemaAndValue(target, new Date(nanos / 1_000_000));
            }
        }

        // Json logical
        if (enableJson && DBZ_JSON.equals(name)) {
            Schema target = Schema.OPTIONAL_STRING_SCHEMA;
            if (!schema.isOptional()) target = Schema.STRING_SCHEMA;
            // Debezium Json is a string payload; pass-through
            return new SchemaAndValue(target, value);
        }

        return new SchemaAndValue(schema, value);
    }

    private SchemaAndValue transformArray(Schema schema, Object value,
                                          boolean enableTime, boolean enableJson,
                                          boolean enableDecimal, String decimalTarget) {
        Schema elemSchema = schema.valueSchema();
        if (!(value instanceof java.util.Collection<?> in)) {
            return new SchemaAndValue(schema, value);
        }
        java.util.List<Object> outList = new java.util.ArrayList<>(in.size());
        Schema mappedElemSchema = elemSchema;
        for (Object v : in) {
            SchemaAndValue sv = transform(elemSchema, v, enableTime, enableJson, enableDecimal, decimalTarget);
            if (sv != null) {
                mappedElemSchema = sv.schema();
                outList.add(sv.value());
            } else {
                outList.add(v);
            }
        }
        SchemaBuilder builder = SchemaBuilder.array(mappedElemSchema);
        if (schema.isOptional()) builder = builder.optional();
        Schema outSchema = builder.build();
        return new SchemaAndValue(outSchema, outList);
    }

    private SchemaAndValue transformMap(Schema schema, Object value,
                                        boolean enableTime, boolean enableJson,
                                        boolean enableDecimal, String decimalTarget) {
        Schema keySchema = schema.keySchema();
        Schema valSchema = schema.valueSchema();
        if (!(value instanceof java.util.Map<?, ?> map)) {
            return new SchemaAndValue(schema, value);
        }
        java.util.Map<Object, Object> out = new java.util.LinkedHashMap<>();
        Schema mappedKeySchema = keySchema;
        Schema mappedValSchema = valSchema;
        for (Map.Entry<?, ?> e : ((java.util.Map<?, ?>) map).entrySet()) {
            SchemaAndValue ksv = transform(keySchema, e.getKey(), enableTime, enableJson, enableDecimal, decimalTarget);
            Object outKey = ksv != null ? ksv.value() : e.getKey();
            if (ksv != null) mappedKeySchema = ksv.schema();
            SchemaAndValue vsv = transform(valSchema, e.getValue(), enableTime, enableJson, enableDecimal, decimalTarget);
            Object outVal = vsv != null ? vsv.value() : e.getValue();
            if (vsv != null) mappedValSchema = vsv.schema();
            out.put(outKey, outVal);
        }
        SchemaBuilder builder = SchemaBuilder.map(mappedKeySchema, mappedValSchema);
        if (schema.isOptional()) builder = builder.optional();
        Schema outSchema = builder.build();
        return new SchemaAndValue(outSchema, out);
    }

    private SchemaAndValue transformVariableScaleDecimal(Schema schema, Object value, String decimalTarget) {
        if (!(value instanceof Struct s)) {
            return new SchemaAndValue(schema, value);
        }
        Integer scale = (Integer) s.get("scale");
        byte[] bytes = (byte[]) s.get("value");
        if (scale == null || bytes == null) {
            return new SchemaAndValue(Schema.OPTIONAL_STRING_SCHEMA, null);
        }
        java.math.BigInteger bi = new java.math.BigInteger(bytes);
        java.math.BigDecimal bd = new java.math.BigDecimal(bi, scale);
        if ("decimal".equalsIgnoreCase(decimalTarget)) {
            Schema target = org.apache.kafka.connect.data.Decimal.builder(scale).build();
            if (schema.isOptional()) {
                target = org.apache.kafka.connect.data.Decimal.builder(scale).optional().build();
            }
            return new SchemaAndValue(target, bd);
        }
        Schema target = Schema.OPTIONAL_STRING_SCHEMA;
        if (!schema.isOptional()) target = Schema.STRING_SCHEMA;
        return new SchemaAndValue(target, bd.toPlainString());
    }

    private static void copyCommon(Schema source, SchemaBuilder target) {
        if (source.isOptional()) target.optional();
        if (source.defaultValue() != null) target.defaultValue(source.defaultValue());
        if (source.doc() != null) target.doc(source.doc());
        Map<String, String> params = source.parameters();
        if (params != null && !params.isEmpty()) target.parameters(params);
    }
}
