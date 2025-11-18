package com.dbsyncer.metadata.entity.converter;

import com.dbsyncer.metadata.entity.DatabaseType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class DatabaseTypeConverter implements AttributeConverter<DatabaseType, Object> {

    @Override
    public Object convertToDatabaseColumn(DatabaseType attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.name();
    }

    @Override
    public DatabaseType convertToEntityAttribute(Object dbData) {
        if (dbData == null) {
            return null;
        }
        String value = dbData.toString();
        return DatabaseType.valueOf(value);
    }
}
