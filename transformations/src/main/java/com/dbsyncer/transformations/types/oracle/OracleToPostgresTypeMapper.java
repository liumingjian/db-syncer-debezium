package com.dbsyncer.transformations.types.oracle;

import com.dbsyncer.transformations.types.ColumnSpec;
import com.dbsyncer.transformations.types.TypeMapper;

import java.util.Locale;

public class OracleToPostgresTypeMapper implements TypeMapper {
    @Override
    public String mapToPostgres(ColumnSpec column) {
        String type = column.getTypeName();
        if (type == null) {
            throw new IllegalArgumentException("Oracle typeName is required");
        }
        String t = type.trim().toUpperCase(Locale.ROOT);

        if (t.startsWith("NUMBER")) {
            Integer p = column.getPrecision();
            Integer s = column.getScale();
            if (p == null && s == null) {
                return "numeric";
            }
            if (s != null && s > 0) {
                return "numeric(" + nullToMax(p) + "," + s + ")";
            }
            int prec = p != null ? p : 0;
            if (prec <= 9) {
                return "integer";
            }
            if (prec <= 18) {
                return "bigint";
            }
            return "numeric(" + prec + ",0)";
        }
        if (t.startsWith("VARCHAR2") || t.startsWith("NVARCHAR2")) {
            Integer len = column.getLength();
            if (len != null) {
                return "varchar(" + len + ")";
            }
            return "varchar";
        }
        if (t.startsWith("CHAR") || t.startsWith("NCHAR")) {
            Integer len = column.getLength();
            if (len != null) {
                return "char(" + len + ")";
            }
            return "char";
        }
        if (t.equals("CLOB") || t.equals("NCLOB")) {
            return "text";
        }
        if (t.equals("BLOB") || t.equals("RAW")) {
            return "bytea";
        }
        if (t.startsWith("RAW")) {
            return "bytea";
        }
        if (t.startsWith("INTERVAL")) {
            // Map to PostgreSQL interval with field qualifiers when possible
            String tl = t.toUpperCase(java.util.Locale.ROOT);
            if (tl.startsWith("INTERVAL YEAR")) {
                return "interval year to month";
            }
            if (tl.startsWith("INTERVAL DAY")) {
                Integer secPrec = column.getScale();
                if (secPrec == null) {
                    // DDL preview parser may store single param in length
                    secPrec = column.getLength();
                }
                if (secPrec != null) {
                    return "interval day to second(" + secPrec + ")";
                }
                return "interval day to second";
            }
            return "interval";
        }
        if (t.equals("DATE")) {
            return "timestamp without time zone";
        }
        if (t.startsWith("TIMESTAMP WITH TIME ZONE")) {
            return "timestamp with time zone";
        }
        if (t.startsWith("TIMESTAMP")) {
            return "timestamp without time zone";
        }
        if (t.equals("FLOAT") || t.equals("BINARY_DOUBLE")) {
            return "double precision";
        }
        if (t.equals("BINARY_FLOAT")) {
            return "real";
        }
        return fallback(t);
    }

    private int nullToMax(Integer p) {
        return p != null ? p : 38;
    }

    private String fallback(String t) {
        return "text";
    }
}
