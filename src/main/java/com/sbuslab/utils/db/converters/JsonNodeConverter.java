package com.sbuslab.utils.db.converters;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.sbuslab.utils.json.JsonMapperFactory;


@Converter(autoApply = true)
public class JsonNodeConverter implements AttributeConverter<JsonNode, String> {

    private final static ObjectMapper mapper = JsonMapperFactory.mapper;

    @Override
    public String convertToDatabaseColumn(JsonNode objectValue) {
        try {
            return (objectValue != null) ? mapper.writeValueAsString(objectValue) : null;
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to serialize to json field ", e);
        }
    }

    @Override
    public JsonNode convertToEntityAttribute(String dataValue) {
        try {
            return (dataValue != null) ? mapper.readTree(dataValue) : null;
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to deserialize json field ", e);
        }
    }
}
