package com.dbsyncer.transformations.types.oracle;

import com.dbsyncer.transformations.types.ColumnSpec;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class OracleIntervalMappingTest {

    private final OracleToPostgresTypeMapper mapper = new OracleToPostgresTypeMapper();

    @Test
    void mapsIntervalYearToMonth() {
        String t = mapper.mapToPostgres(ColumnSpec.builder().typeName("INTERVAL YEAR TO MONTH").build());
        assertThat(t).isEqualTo("interval year to month");
    }

    @Test
    void mapsIntervalDayToSecondWithScale() {
        String t = mapper.mapToPostgres(ColumnSpec.builder().typeName("INTERVAL DAY TO SECOND").scale(3).build());
        assertThat(t).isEqualTo("interval day to second(3)");
    }
}

