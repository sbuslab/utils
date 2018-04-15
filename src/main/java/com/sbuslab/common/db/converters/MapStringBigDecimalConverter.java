package com.sbuslab.common.db.converters;

import javax.persistence.Converter;
import java.math.BigDecimal;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;


@Converter(autoApply = true)
public class MapStringBigDecimalConverter extends AbstractJsonConverter<Map<String, BigDecimal>> {
    private final TypeReference<Map<String, BigDecimal>> typeRef = new TypeReference<Map<String, BigDecimal>>() {};

    protected TypeReference<Map<String, BigDecimal>> getTypeRef() {
        return typeRef;
    }
}
