package com.sbuslab.utils.filters;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sbuslab.utils.db.EntitySqlFields;

public interface CustomFilterBuilder {

    Filter createFilter(String filterName, Object value, EntitySqlFields esf, ObjectMapper objectMapper);
}
