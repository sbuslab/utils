package com.sbuslab.utils.db;

import com.sbuslab.model.Paging;
import com.sbuslab.model.Sorting;
import org.springframework.jdbc.core.RowMapper;

import java.util.List;
import java.util.Map;

public interface QueryBuilder {

    Object getByFiltersAndSortingsWithPaging(Map<String, Object> filters,
                                             List<Sorting> sortings,
                                             Paging paging,
                                             RowMapper rowMapper,
                                             EntitySqlFields esf,
                                             String methodName,
                                             boolean returnsCollection);
}
