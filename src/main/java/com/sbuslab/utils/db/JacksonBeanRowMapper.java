package com.sbuslab.utils.db;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.postgresql.util.PGobject;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.JdbcUtils;

import com.sbuslab.utils.StringUtils;


public class JacksonBeanRowMapper<T> implements RowMapper<T> {

    private final JavaType mappedType;
    private final ObjectMapper objectMapper;
    private final BiFunction<Map<String, Object>, T, T> extraFieldsMapper;

    public JacksonBeanRowMapper(Class<T> mappedClass, ObjectMapper objectMapper) {
        this(mappedClass, objectMapper, null);
    }

    public JacksonBeanRowMapper(Class<T> mappedClass, ObjectMapper objectMapper, BiFunction<Map<String, Object>, T, T> extraFieldsMapper) {
        this.mappedType = objectMapper.getTypeFactory().constructType(mappedClass);
        this.objectMapper = objectMapper;
        this.extraFieldsMapper = extraFieldsMapper;
    }

    @Override
    public T mapRow(ResultSet rs, int rowNum) {
        try {
            ResultSetMetaData rsmd = rs.getMetaData();
            int columnCount = rsmd.getColumnCount();
            Map<String, Object> map = new HashMap<>(columnCount);

            for (int i = 1; i <= columnCount; i++) {
                String column = JdbcUtils.lookupColumnName(rsmd, i);
                Object value = JdbcUtils.getResultSetValue(rs, i);

                map.put(StringUtils.toCamelCase(column),
                    value instanceof PGobject ? objectMapper.readTree(((PGobject) value).getValue()) : value);
            }

            T bean = objectMapper.convertValue(map, mappedType);

            if (extraFieldsMapper != null) {
                return extraFieldsMapper.apply(map, bean);
            }

            return bean;
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }
}
