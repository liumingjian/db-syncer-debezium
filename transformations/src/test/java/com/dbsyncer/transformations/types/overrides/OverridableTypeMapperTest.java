package com.dbsyncer.transformations.types.overrides;

import com.dbsyncer.transformations.types.ColumnSpec;
import com.dbsyncer.transformations.types.mysql.MySqlToPostgresTypeMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class OverridableTypeMapperTest {
    @Test
    void appliesOverrideBeforeDelegate() {
        var base = new MySqlToPostgresTypeMapper();
        var rule = new TypeMappingOverrides.Rule();
        rule.setName("tinyint");
        rule.setLengthEquals(1);
        rule.setTarget("boolean");
        var mapper = new OverridableTypeMapper(base, List.of(rule));

        String t = mapper.mapToPostgres(ColumnSpec.builder().name("flag").typeName("tinyint").length(1).build());
        assertThat(t).isEqualTo("boolean");
    }
}

