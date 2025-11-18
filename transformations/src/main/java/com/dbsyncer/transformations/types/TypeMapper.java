package com.dbsyncer.transformations.types;

public interface TypeMapper {
    String mapToPostgres(ColumnSpec column);
}

