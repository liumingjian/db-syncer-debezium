package com.dbsyncer.transformations.types.mysql;

import com.dbsyncer.transformations.types.ColumnSpec;
import com.dbsyncer.transformations.types.TypeMapper;

import java.util.Locale;

public class MySqlToPostgresTypeMapper implements TypeMapper {

    @Override
    public String mapToPostgres(ColumnSpec column) {
        String type = column.getTypeName();
        if (type == null) {
            throw new IllegalArgumentException("MySQL typeName is required");
        }
        String t = type.trim().toUpperCase(Locale.ROOT);

        if (t.startsWith("TINYINT")) {
            Integer len = column.getLength();
            if (len != null && len == 1) {
                return "boolean";
            }
            return column.isUnsigned() ? "smallint" : "smallint";
        }
        if (t.startsWith("SMALLINT")) {
            return "smallint";
        }
        if (t.startsWith("MEDIUMINT")) {
            return "integer";
        }
        if (t.equals("INT") || t.equals("INTEGER") || t.startsWith("INT(")) {
            return "integer";
        }
        if (t.startsWith("BIGINT")) {
            return column.isUnsigned() ? "numeric(20,0)" : "bigint";
        }
        if (t.startsWith("DECIMAL") || t.startsWith("NUMERIC")) {
            Integer p = column.getPrecision();
            Integer s = column.getScale();
            if (p != null && s != null) {
                return "numeric(" + p + "," + s + ")";
            }
            if (p != null) {
                return "numeric(" + p + ")";
            }
            return "numeric";
        }
        if (t.equals("FLOAT")) {
            return "real";
        }
        if (t.equals("DOUBLE") || t.equals("DOUBLE PRECISION")) {
            return "double precision";
        }
        if (t.startsWith("BIT")) {
            Integer len = column.getLength();
            if (len != null && len == 1) {
                return "boolean";
            }
            if (len != null) {
                return "bit varying(" + len + ")";
            }
            return "bit varying";
        }
        if (t.startsWith("CHAR")) {
            Integer len = column.getLength();
            if (len != null) {
                return "char(" + len + ")";
            }
            return "char";
        }
        if (t.startsWith("VARCHAR") || t.equals("TEXT") || t.endsWith("TEXT")
                || t.equals("TINYTEXT") || t.equals("MEDIUMTEXT") || t.equals("LONGTEXT")) {
            if (t.startsWith("VARCHAR")) {
                Integer len = column.getLength();
                if (len != null) {
                    return "varchar(" + len + ")";
                }
                return "varchar";
            }
            return "text";
        }
        if (t.endsWith("BLOB") || t.equals("BLOB") || t.equals("TINYBLOB") || t.equals("MEDIUMBLOB") || t.equals("LONGBLOB")
                || t.equals("BINARY") || t.equals("VARBINARY")) {
            return "bytea";
        }
        if (t.equals("DATE")) {
            return "date";
        }
        if (t.equals("DATETIME") || t.startsWith("TIMESTAMP")) {
            return "timestamp without time zone";
        }
        if (t.equals("TIME")) {
            return "time without time zone";
        }
        if (t.equals("JSON")) {
            return "jsonb";
        }
        if (t.startsWith("ENUM") || t.startsWith("SET")) {
            return "text";
        }
        if (t.equals("YEAR")) {
            return "integer";
        }
        return fallback(t);
    }

    private String fallback(String t) {
        return "text";
    }
}
