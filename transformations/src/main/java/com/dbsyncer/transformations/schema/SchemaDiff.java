package com.dbsyncer.transformations.schema;

import lombok.Builder;
import lombok.Value;

import java.util.List;

/**
 * Simple representation of schema differences between two versions of a table.
 */
@Value
@Builder
public class SchemaDiff {

    List<ColumnDefinition> addedColumns;
    List<ColumnDefinition> removedColumns;
    List<ColumnDefinition> changedColumns;
}

