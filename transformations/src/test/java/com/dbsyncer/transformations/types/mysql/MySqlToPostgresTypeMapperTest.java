package com.dbsyncer.transformations.types.mysql;

import com.dbsyncer.transformations.types.ColumnSpec;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class MySqlToPostgresTypeMapperTest {
    private final MySqlToPostgresTypeMapper mapper = new MySqlToPostgresTypeMapper();

    @Test
    void mapsTinyintOneToBoolean() {
        String t = mapper.mapToPostgres(ColumnSpec.builder().typeName("tinyint").length(1).build());
        assertThat(t).isEqualTo("boolean");
    }

    @Test
    void mapsBigintUnsignedToNumeric() {
        String t = mapper.mapToPostgres(ColumnSpec.builder().typeName("bigint").unsigned(true).build());
        assertThat(t).isEqualTo("numeric(20,0)");
    }

    @Test
    void mapsVarcharWithLength() {
        String t = mapper.mapToPostgres(ColumnSpec.builder().typeName("varchar").length(255).build());
        assertThat(t).isEqualTo("varchar(255)");
    }

    @Test
    void mapsDecimalWithPrecisionScale() {
        String t = mapper.mapToPostgres(ColumnSpec.builder().typeName("decimal").precision(18).scale(4).build());
        assertThat(t).isEqualTo("numeric(18,4)");
    }

    @Test
    void mapsJsonToJsonb() {
        String t = mapper.mapToPostgres(ColumnSpec.builder().typeName("json").build());
        assertThat(t).isEqualTo("jsonb");
    }
}

