package com.dbsyncer.transformations.mapper;

import lombok.Builder;
import lombok.Value;

/**
 * Target database column definition produced by a type mapper.
 */
@Value
@Builder
public class TargetColumn {

    String name;
    String typeDefinition;
    boolean nullable;
}

