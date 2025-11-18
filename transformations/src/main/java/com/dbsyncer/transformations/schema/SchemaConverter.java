package com.dbsyncer.transformations.schema;

import com.dbsyncer.transformations.types.ColumnSpec;
import com.dbsyncer.transformations.types.TypeMapper;
import com.dbsyncer.transformations.types.TypeMappingRegistry;
import org.apache.kafka.connect.data.Field;
import org.apache.kafka.connect.data.Schema;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SchemaConverter {
    private final TypeMappingRegistry registry = new TypeMappingRegistry();

    public List<ColumnDefinition> toPostgresColumns(String sourceDb, Schema rowSchema) {
        TypeMapper mapper = registry.get(sourceDb);
        return toPostgresColumns(mapper, rowSchema);
    }

    public List<ColumnDefinition> toPostgresColumns(TypeMapper mapper, Schema rowSchema) {
        if (rowSchema == null || rowSchema.type() != Schema.Type.STRUCT) {
            throw new IllegalArgumentException("rowSchema must be STRUCT");
        }
        List<ColumnDefinition> cols = new ArrayList<>();
        for (Field f : rowSchema.fields()) {
            Schema s = f.schema();
            ColumnSpec spec = toSpec(s, f.name());
            String pgType = mapper.mapToPostgres(spec);
            cols.add(ColumnDefinition.builder().name(f.name()).postgresType(pgType).build());
        }
        return cols;
    }

    private static ColumnSpec toSpec(Schema s, String name) {
        ColumnSpec.ColumnSpecBuilder b = ColumnSpec.builder().name(name);

        // Best-effort extraction
        String typeName = inferTypeName(s);
        b.typeName(typeName);

        Map<String, String> params = s.parameters();
        if (params != null) {
            b.length(parseInt(params.get("length")));
            Integer precision = parseInt(params.get("precision"));
            if (precision == null) precision = parseInt(params.get("connect.decimal.precision"));
            b.precision(precision);
            Integer scale = parseInt(params.get("scale"));
            if (scale == null) scale = parseInt(params.get("connect.decimal.scale"));
            b.scale(scale);
            b.unsigned(Boolean.parseBoolean(params.getOrDefault("unsigned", "false")));
        }

        b.nullable(s.isOptional());
        return b.build();
    }

    private static String inferTypeName(Schema s) {
        Map<String, String> p = s.parameters();
        if (p != null) {
            String srcType = p.get("debezium.source.column.type");
            if (srcType != null && !srcType.isBlank()) return srcType;
        }
        String name = s.name();
        if (name != null) {
            String n = name.toLowerCase(Locale.ROOT);
            if (n.contains("json")) return "json";
            if (n.contains("time")) return "timestamp";
            if (n.contains("decimal")) return "decimal";
        }
        // fallback based on primitive type
        return switch (s.type()) {
            case INT8 -> "tinyint";
            case INT16 -> "smallint";
            case INT32 -> "int";
            case INT64 -> "bigint";
            case FLOAT32 -> "float";
            case FLOAT64 -> "double";
            case BOOLEAN -> "boolean";
            case STRING -> "varchar";
            case BYTES -> "blob";
            case ARRAY, MAP, STRUCT -> "json";
            default -> "varchar";
        };
    }

    private static Integer parseInt(String v) {
        if (v == null) return null;
        try { return Integer.parseInt(v); } catch (NumberFormatException ignore) { return null; }
    }
}
