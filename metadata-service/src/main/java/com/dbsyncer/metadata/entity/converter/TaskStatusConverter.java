package com.dbsyncer.metadata.entity.converter;

import com.dbsyncer.metadata.entity.TaskStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class TaskStatusConverter implements AttributeConverter<TaskStatus, Object> {

    @Override
    public Object convertToDatabaseColumn(TaskStatus attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.name();
    }

    @Override
    public TaskStatus convertToEntityAttribute(Object dbData) {
        if (dbData == null) {
            return null;
        }
        String value = dbData.toString();
        return TaskStatus.valueOf(value);
    }
}
