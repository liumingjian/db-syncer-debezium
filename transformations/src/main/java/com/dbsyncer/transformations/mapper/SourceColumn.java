package com.dbsyncer.transformations.mapper;

import lombok.Builder;
import lombok.Value;

/**
 * Simplified representation of a source database column for type mapping.
 */
@Value
@Builder
public class SourceColumn {

    String name;
    String typeName;
    Integer length;
    Integer scale;
    boolean nullable;
    boolean unsigned;
}

