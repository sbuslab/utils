package com.sbuslab.utils.db;

import org.springframework.jdbc.core.namedparam.AbstractSqlParameterSource;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;


public class CombinedSqlParameterSource extends AbstractSqlParameterSource {
    private final MapSqlParameterSource mapSqlParameterSource = new MapSqlParameterSource();
    private final BeanPropertySqlParameterSource beanPropertySqlParameterSource;

    public CombinedSqlParameterSource(Object object) {
        this.beanPropertySqlParameterSource = new BeanPropertySqlParameterSource(object);
    }

    public CombinedSqlParameterSource add(String paramName, Object value) {
        return addValue(paramName, value);
    }

    public CombinedSqlParameterSource addValue(String paramName, Object value) {
        mapSqlParameterSource.addValue(paramName, value);

        return this;
    }

    @Override
    public boolean hasValue(String paramName) {
        return mapSqlParameterSource.hasValue(paramName) || beanPropertySqlParameterSource.hasValue(paramName);
    }

    @Override
    public Object getValue(String paramName) {
        return mapSqlParameterSource.hasValue(paramName) ? mapSqlParameterSource.getValue(paramName) : beanPropertySqlParameterSource.getValue(paramName);
    }

    @Override
    public int getSqlType(String paramName) {
        return mapSqlParameterSource.hasValue(paramName) ? mapSqlParameterSource.getSqlType(paramName) : beanPropertySqlParameterSource.getSqlType(paramName);
    }
}
