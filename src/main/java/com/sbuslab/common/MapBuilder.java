package com.sbuslab.common;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;


public class MapBuilder {

    private static final TypeReference<Map<String, Object>> mapRef = new TypeReference<Map<String, Object>>() {};

    public static Builder with(String key, Object value) {
        return new Builder().with(key, value);
    }

    public static class Builder {

        private final Map<String, Object> map = new HashMap<>();

        public Builder with(String key, Object value) {
            map.put(key, value);
            return this;
        }

        public Builder withAll(Map<String, ?> values) {
            map.putAll(values);
            return this;
        }

        public Builder withAll(Object valueAsMap, ObjectMapper mapper) {
            return withAll(mapper.convertValue(valueAsMap, mapRef));
        }

        @JsonValue
        public Map<String, Object> get() {
            return map;
        }
    }
}
