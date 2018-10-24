package com.sbuslab.utils.filters;

import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.sbuslab.utils.db.EntitySqlFields;


public class RawSqlFilter extends Filter {

    private String sql;
    private String filterName;
    private Object value;

    private RawSqlFilter(String sql, String filterName, Object value) {
        this.sql = sql;
        this.filterName = filterName;
        this.value = value;
    }

    @Override
    public String buildSql(Map<String, Object> params) {
        params.put(filterName, value);
        return sql;
    }

    @Override
    public boolean applied(Object target) {
        return false;
    }

    @Override
    public boolean requiresFieldDefinition() {
        return false;
    }

    public static class Builder implements CustomFilterBuilder {

        private String sql;

        public Builder(String sql) {
            this.sql = sql;
        }

        @Override
        public Filter createFilter(String filterName, Object value, EntitySqlFields esf, ObjectMapper objectMapper) {
            return new RawSqlFilter(sql, filterName, value);
        }
    }
}
