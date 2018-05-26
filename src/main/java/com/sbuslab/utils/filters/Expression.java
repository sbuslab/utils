package com.sbuslab.utils.filters;

import java.util.Map;


public interface Expression {

    String buildSql(Map<String, Object> params);

    boolean applied(Object target);
}
