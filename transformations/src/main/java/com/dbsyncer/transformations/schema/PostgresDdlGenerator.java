package com.dbsyncer.transformations.schema;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Generates PostgreSQL DDL from column definitions and computes simple schema diffs.
 */
public class PostgresDdlGenerator {

    /**
     * Generate a CREATE TABLE statement for the given columns.
     *
     * @param schemaName optional schema name (may be null or blank)
     * @param tableName  table name (required)
     * @param columns    non-empty list of column definitions
     * @return CREATE TABLE IF NOT EXISTS ... DDL
     */
    public String generateCreateTable(String schemaName, String tableName, List<ColumnDefinition> columns) {
        if (tableName == null || tableName.isBlank()) {
            throw new IllegalArgumentException("tableName must not be blank");
        }
        if (columns == null || columns.isEmpty()) {
            throw new IllegalArgumentException("columns must not be empty");
        }

        String qualifiedName;
        if (schemaName != null && !schemaName.isBlank()) {
            qualifiedName = quoteIdent(schemaName) + "." + quoteIdent(tableName);
        } else {
            qualifiedName = quoteIdent(tableName);
        }

        String colsSql = columns.stream()
                .map(c -> quoteIdent(c.getName()) + " " + c.getPostgresType())
                .collect(Collectors.joining(", "));

        return "CREATE TABLE IF NOT EXISTS " + qualifiedName + " (" + colsSql + ");";
    }

    /**
     * Compute a simple diff between two column lists based on name and type.
     */
    public SchemaDiff diff(List<ColumnDefinition> existing, List<ColumnDefinition> desired) {
        Map<String, ColumnDefinition> existingByName = indexByName(existing);
        Map<String, ColumnDefinition> desiredByName = indexByName(desired);

        List<ColumnDefinition> added = new ArrayList<>();
        List<ColumnDefinition> removed = new ArrayList<>();
        List<ColumnDefinition> changed = new ArrayList<>();

        for (Map.Entry<String, ColumnDefinition> e : desiredByName.entrySet()) {
            String name = e.getKey();
            ColumnDefinition target = e.getValue();
            ColumnDefinition current = existingByName.get(name);
            if (current == null) {
                added.add(target);
            } else if (!Objects.equals(normalizeType(current.getPostgresType()),
                    normalizeType(target.getPostgresType()))) {
                changed.add(target);
            }
        }

        for (Map.Entry<String, ColumnDefinition> e : existingByName.entrySet()) {
            if (!desiredByName.containsKey(e.getKey())) {
                removed.add(e.getValue());
            }
        }

        return SchemaDiff.builder()
                .addedColumns(added)
                .removedColumns(removed)
                .changedColumns(changed)
                .build();
    }

    private Map<String, ColumnDefinition> indexByName(List<ColumnDefinition> cols) {
        Map<String, ColumnDefinition> index = new LinkedHashMap<>();
        if (cols != null) {
            for (ColumnDefinition c : cols) {
                index.put(c.getName().toLowerCase(Locale.ROOT), c);
            }
        }
        return index;
    }

    private String quoteIdent(String ident) {
        return "\"" + ident.replace("\"", "\"\"") + "\"";
    }

    private String normalizeType(String type) {
        return type == null ? "" : type.trim().toLowerCase(Locale.ROOT);
    }
}

