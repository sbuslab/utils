package com.sbuslab.utils.db.converters;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import scala.math.BigDecimal;


@Converter(autoApply = true)
public class ScalaBigDecimalConverter implements AttributeConverter<BigDecimal, String> {

    @Override
    public String convertToDatabaseColumn(BigDecimal objectValue) {
        return (objectValue != null) ? objectValue.toString() : null;
    }

    @Override
    public BigDecimal convertToEntityAttribute(String dataValue) {
        try {
            return (dataValue != null) ? BigDecimal.exact(dataValue) : null;
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to deserialize BigDecimal field ", e);
        }
    }
}
