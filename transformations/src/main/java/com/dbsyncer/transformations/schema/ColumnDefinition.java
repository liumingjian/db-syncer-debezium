package com.dbsyncer.transformations.schema;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ColumnDefinition {
    String name;
    String postgresType;
}

