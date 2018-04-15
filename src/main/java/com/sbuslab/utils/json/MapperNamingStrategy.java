package com.sbuslab.utils.json;

import java.lang.reflect.Field;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.introspect.AnnotatedField;
import com.fasterxml.jackson.databind.introspect.AnnotatedMethod;


public class MapperNamingStrategy extends PropertyNamingStrategy {

    @Override
    public String nameForField(MapperConfig<?> config, AnnotatedField field, String defaultName) {
        return field.getName();
    }

    @Override
    public String nameForGetterMethod(MapperConfig<?> config, AnnotatedMethod method, String defaultName) {
        return convert(method, defaultName);
    }

    @Override
    public String nameForSetterMethod(MapperConfig<?> config, AnnotatedMethod method, String defaultName) {
        return convert(method, defaultName);
    }

    private String convert(AnnotatedMethod method, String defaultName) {
        for (Field f : method.getDeclaringClass().getDeclaredFields()) {
            if (f.getName().equalsIgnoreCase(defaultName)) {
                return f.getName();
            }
        }

        return defaultName;
    }
}
