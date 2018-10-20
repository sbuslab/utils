package com.sbuslab.utils.db;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.postgresql.util.PGobject;
import org.springframework.jdbc.core.ColumnMapRowMapper;
import org.springframework.jdbc.core.RowMapper;

import com.sbuslab.utils.StringUtils;


public class JacksonBeanRowMapper<T> implements RowMapper<T> {

    private final Class<T> mappedClass;
    private final ObjectMapper objectMapper;
    private final BiFunction<Map<String, Object>, T, T> extraFieldsMapper;

    private final ColumnMapRowMapper columnMapper = new ColumnMapRowMapper();

    public JacksonBeanRowMapper(Class<T> mappedClass, ObjectMapper objectMapper) {
        this(mappedClass, objectMapper, null);
    }

    public JacksonBeanRowMapper(Class<T> mappedClass, ObjectMapper objectMapper, BiFunction<Map<String, Object>, T, T> extraFieldsMapper) {
        this.mappedClass = mappedClass;
        this.objectMapper = objectMapper;
        this.extraFieldsMapper = extraFieldsMapper;
    }

    @Override
    public T mapRow(ResultSet rs, int rowNum) throws SQLException {
        try {
            Map<String, Object> row = columnMapper.mapRow(rs, rowNum);

            Map<String, Object> map = new HashMap<>(row.size());
            for (Map.Entry<String, Object> entry : row.entrySet()) {
                Object value = entry.getValue() instanceof PGobject ?
                    objectMapper.readTree(((PGobject) entry.getValue()).getValue())
                    : entry.getValue();
                map.put(StringUtils.toCamelCase(entry.getKey()), value);
            }

            T bean = objectMapper.convertValue(map, mappedClass);

            if (extraFieldsMapper != null) {
                return extraFieldsMapper.apply(row, bean);
            }

            return bean;
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }
}
