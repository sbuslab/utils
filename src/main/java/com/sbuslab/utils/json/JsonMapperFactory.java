package com.sbuslab.utils.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.joda.JodaModule;


public class JsonMapperFactory {

    public static final ObjectMapper mapper = JsonMapperFactory.createMapper();

    public static ObjectMapper createMapper() {
        ObjectMapper m = new ObjectMapper();
        m.registerModule(new JodaModule());
        m.registerModule(new Jdk8Module());
        m.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        m.configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false);
        m.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
        m.configure(Feature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER, true);
        m.configure(Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
        m.configure(Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true);
        m.configure(Feature.ALLOW_SINGLE_QUOTES, true);
        m.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, true);
        m.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        m.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
        m.configure(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN, true);
        m.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        m.setSerializationInclusion(JsonInclude.Include.NON_ABSENT);
        m.setPropertyNamingStrategy(new MapperNamingStrategy());
        return m;
    }
}
