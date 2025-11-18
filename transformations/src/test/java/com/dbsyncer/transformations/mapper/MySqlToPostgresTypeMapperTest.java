package com.dbsyncer.transformations.mapper;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MySqlToPostgresTypeMapperTest {

    private final MySqlToPostgresTypeMapper mapper = new MySqlToPostgresTypeMapper();

    @Test
    void mapsTinyIntOneToBoolean() {
        SourceColumn column = SourceColumn.builder()
                .name("flag")
                .typeName("tinyint")
                .length(1)
                .nullable(false)
                .build();

        TargetColumn target = mapper.map(column);

        assertThat(target.getTypeDefinition()).isEqualTo("boolean");
        assertThat(target.isNullable()).isFalse();
    }

    @Test
    void mapsUnsignedBigintToNumeric20() {
        SourceColumn column = SourceColumn.builder()
                .name("id")
                .typeName("bigint")
                .unsigned(true)
                .nullable(false)
                .build();

        TargetColumn target = mapper.map(column);

        assertThat(target.getTypeDefinition()).isEqualTo("numeric(20,0)");
    }

    @Test
    void mapsVarcharWithLength() {
        SourceColumn column = SourceColumn.builder()
                .name("name")
                .typeName("varchar")
                .length(255)
                .nullable(true)
                .build();

        TargetColumn target = mapper.map(column);

        assertThat(target.getTypeDefinition()).isEqualTo("varchar(255)");
    }

    @Test
    void mapsJsonToJsonb() {
        SourceColumn column = SourceColumn.builder()
                .name("payload")
                .typeName("json")
                .nullable(true)
                .build();

        TargetColumn target = mapper.map(column);

        assertThat(target.getTypeDefinition()).isEqualTo("jsonb");
    }
}

