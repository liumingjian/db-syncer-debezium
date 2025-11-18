package com.dbsyncer.transformations.types;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ColumnSpec {
    String name;
    String typeName;
    Integer length;
    Integer precision;
    Integer scale;
    boolean unsigned;
    boolean nullable;
}

