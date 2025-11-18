package com.dbsyncer.transformations.types;

import com.dbsyncer.transformations.types.mysql.MySqlToPostgresTypeMapper;
import com.dbsyncer.transformations.types.oracle.OracleToPostgresTypeMapper;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TypeMappingRegistry {
    private final Map<String, TypeMapper> mappers = new ConcurrentHashMap<>();

    public TypeMappingRegistry() {
        register("mysql", new MySqlToPostgresTypeMapper());
        register("oracle", new OracleToPostgresTypeMapper());
    }

    public void register(String sourceDb, TypeMapper mapper) {
        mappers.put(sourceDb.toLowerCase(Locale.ROOT), mapper);
    }

    public TypeMapper get(String sourceDb) {
        TypeMapper mapper = mappers.get(sourceDb.toLowerCase(Locale.ROOT));
        if (mapper == null) {
            throw new IllegalArgumentException("No TypeMapper registered for: " + sourceDb);
        }
        return mapper;
    }
}

