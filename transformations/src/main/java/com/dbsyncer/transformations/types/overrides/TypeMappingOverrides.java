package com.dbsyncer.transformations.types.overrides;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class TypeMappingOverrides {
    private String sourceDb;
    private List<Rule> overrides = new ArrayList<>();

    @Data
    public static class Rule {
        private String name; // base type name, case-insensitive
        private Boolean unsigned; // optional
        private Integer lengthEquals; // optional
        private Integer precisionEquals; // optional
        private Integer scaleEquals; // optional
        private String target; // template, supports ${length}, ${precision}, ${scale}
    }
}

