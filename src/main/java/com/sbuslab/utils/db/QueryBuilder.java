package com.sbuslab.utils.db;

import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.RowMapper;

import com.sbuslab.model.Paging;
import com.sbuslab.model.Sorting;

public interface QueryBuilder {

    Object getByFiltersAndSortingsWithPaging(Map<String, Object> filters,
                                             List<Sorting> sortings,
                                             Paging paging,
                                             RowMapper rowMapper,
                                             EntitySqlFields esf,
                                             String methodName,
                                             boolean returnsCollection);
}
