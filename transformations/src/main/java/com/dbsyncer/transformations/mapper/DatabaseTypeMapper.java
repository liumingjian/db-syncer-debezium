package com.dbsyncer.transformations.mapper;

/**
 * Maps a source column type (name + optional attributes) to a target database column definition.
 */
public interface DatabaseTypeMapper {

    /**
     * Map a source column to target database column definition.
     *
     * @param column source column metadata
     * @return mapped target column definition
     */
    TargetColumn map(SourceColumn column);
}

