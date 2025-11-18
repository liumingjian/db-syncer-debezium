package com.dbsyncer.metadata.entity.converter;

import com.dbsyncer.metadata.entity.ConnectorType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class ConnectorTypeConverter implements AttributeConverter<ConnectorType, Object> {

    @Override
    public Object convertToDatabaseColumn(ConnectorType attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.name();
    }

    @Override
    public ConnectorType convertToEntityAttribute(Object dbData) {
        if (dbData == null) {
            return null;
        }
        String value = dbData.toString();
        return ConnectorType.valueOf(value);
    }
}
