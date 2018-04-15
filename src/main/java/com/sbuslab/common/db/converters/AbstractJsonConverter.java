package com.sbuslab.common.db.converters;

import javax.persistence.AttributeConverter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.sbuslab.common.json.JsonMapperFactory;


abstract public class AbstractJsonConverter<T> implements AttributeConverter<T, String> {

    private final static ObjectMapper mapper = JsonMapperFactory.mapper;

    // Should be overrided. Jackson can not resolve classes throw generics https://github.com/FasterXML/jackson-databind/issues/1490
    private final TypeReference<T> typeRef = new TypeReference<T>() {
    };

    // Should be overrided. Jackson can not resolve classes throw generics https://github.com/FasterXML/jackson-databind/issues/1490
    protected TypeReference<T> getTypeRef() {
        return typeRef;
    }

    @Override
    public String convertToDatabaseColumn(T objectValue) {
        try {
            return (objectValue != null) ? mapper.writeValueAsString(objectValue) : null;
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to serialize to json field ", e);
        }
    }

    @Override
    public T convertToEntityAttribute(String dataValue) {
        try {
            return (dataValue != null) ? mapper.readValue(dataValue, getTypeRef()) : null;
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to deserialize json field ", e);
        }
    }
}
