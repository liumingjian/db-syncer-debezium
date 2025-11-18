package com.dbsyncer.transformations.types.oracle;

import com.dbsyncer.transformations.types.ColumnSpec;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class OracleToPostgresTypeMapperTest {
    private final OracleToPostgresTypeMapper mapper = new OracleToPostgresTypeMapper();

    @Test
    void mapsNumberIntegerRange() {
        String i = mapper.mapToPostgres(ColumnSpec.builder().typeName("NUMBER").precision(9).scale(0).build());
        String b = mapper.mapToPostgres(ColumnSpec.builder().typeName("NUMBER").precision(18).scale(0).build());
        String n = mapper.mapToPostgres(ColumnSpec.builder().typeName("NUMBER").precision(30).scale(0).build());
        assertThat(i).isEqualTo("integer");
        assertThat(b).isEqualTo("bigint");
        assertThat(n).isEqualTo("numeric(30,0)");
    }

    @Test
    void mapsVarchar2ToVarchar() {
        String t = mapper.mapToPostgres(ColumnSpec.builder().typeName("VARCHAR2").length(100).build());
        assertThat(t).isEqualTo("varchar(100)");
    }

    @Test
    void mapsDateToTimestamp() {
        String t = mapper.mapToPostgres(ColumnSpec.builder().typeName("DATE").build());
        assertThat(t).isEqualTo("timestamp without time zone");
    }
}

