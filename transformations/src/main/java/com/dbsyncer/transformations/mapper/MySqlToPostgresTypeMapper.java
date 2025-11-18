package com.dbsyncer.transformations.mapper;

import java.util.Locale;

/**
 * Basic MySQL → PostgreSQL column type mapping.
 * Covers the most common numeric, string, datetime, binary and JSON types.
 */
public class MySqlToPostgresTypeMapper implements DatabaseTypeMapper {

    @Override
    public TargetColumn map(SourceColumn column) {
        String typeName = column.getTypeName() != null
                ? column.getTypeName().toLowerCase(Locale.ROOT)
                : "";
        String mappedType = switch (typeName) {
            case "tinyint" -> mapTinyInt(column);
            case "smallint" -> "smallint";
            case "mediumint" -> "integer";
            case "int", "integer" -> "integer";
            case "bigint" -> column.isUnsigned() ? "numeric(20,0)" : "bigint";
            case "decimal", "numeric" -> mapDecimal(column);
            case "float" -> "real";
            case "double" -> "double precision";

            case "char" -> buildCharType("char", column.getLength());
            case "varchar" -> buildCharType("varchar", column.getLength());
            case "text", "tinytext", "mediumtext", "longtext" -> "text";

            case "date" -> "date";
            case "datetime", "timestamp" -> "timestamp";
            case "time" -> "time";

            case "blob", "tinyblob", "mediumblob", "longblob", "varbinary", "binary" -> "bytea";
            case "json" -> "jsonb";

            default -> "text";
        };

        return TargetColumn.builder()
                .name(column.getName())
                .typeDefinition(mappedType)
                .nullable(column.isNullable())
                .build();
    }

    private String mapTinyInt(SourceColumn column) {
        if (column.getLength() != null && column.getLength() == 1) {
            return "boolean";
        }
        return "smallint";
    }

    private String mapDecimal(SourceColumn column) {
        Integer length = column.getLength();
        Integer scale = column.getScale();
        if (length != null && scale != null) {
            return "numeric(" + length + "," + scale + ")";
        }
        if (length != null) {
            return "numeric(" + length + ",0)";
        }
        return "numeric";
    }

    private String buildCharType(String base, Integer length) {
        if (length == null || length <= 0) {
            return base;
        }
        return base + "(" + length + ")";
    }
}

