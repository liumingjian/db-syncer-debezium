package com.dbsyncer.transformations.types.overrides;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class TypeMappingLoader {
    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
    private final ObjectMapper jsonMapper = new ObjectMapper();

    public TypeMappingOverrides load(Path path) throws IOException {
        String content = Files.readString(path);
        String lc = path.toString().toLowerCase();
        if (lc.endsWith(".yaml") || lc.endsWith(".yml")) {
            return yamlMapper.readValue(content, TypeMappingOverrides.class);
        }
        return jsonMapper.readValue(content, TypeMappingOverrides.class);
    }
}

