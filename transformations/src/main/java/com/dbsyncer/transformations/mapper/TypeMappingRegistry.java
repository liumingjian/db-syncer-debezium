package com.dbsyncer.transformations.mapper;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Registry for database type mappers keyed by logical source database name.
 */
public class TypeMappingRegistry {

    private final Map<String, DatabaseTypeMapper> mappers = new HashMap<>();

    public void register(String sourceDb, DatabaseTypeMapper mapper) {
        mappers.put(normalizeKey(sourceDb), mapper);
    }

    public Optional<DatabaseTypeMapper> get(String sourceDb) {
        return Optional.ofNullable(mappers.get(normalizeKey(sourceDb)));
    }

    private String normalizeKey(String key) {
        return key == null ? "" : key.toLowerCase(Locale.ROOT);
    }
}

