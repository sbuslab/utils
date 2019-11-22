package com.sbuslab.utils.db.converters;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import com.sbuslab.model.SecureString;


@Converter(autoApply = true)
public class SecureStringConverter implements AttributeConverter<SecureString, String> {

    @Override
    public String convertToDatabaseColumn(SecureString v) {
        return v == null ? null : v.original();
    }

    @Override
    public SecureString convertToEntityAttribute(String v) {
        return v == null ? null : new SecureString(v);
    }
}
