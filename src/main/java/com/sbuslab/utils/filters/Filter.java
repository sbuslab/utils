package com.sbuslab.utils.filters;

import java.util.Map;


public abstract class Filter {

    public abstract String buildSql(Map<String, Object> params);

    public boolean requiresFieldDefinition() {
        return true;
    }

    public abstract boolean applied(Object target);
}
