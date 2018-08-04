package com.sbuslab.utils.db;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.sbuslab.model.Paging;
import com.sbuslab.model.Sorting;
import com.sbuslab.utils.filters.Filter;
import com.sbuslab.utils.filters.Group;

@Component
public class DefaultQueryBuilder extends QueryLogging implements QueryBuilder {

    @Autowired
    @Lazy
    private EntitiesSqlFields esfs;

    @Autowired
    @Lazy
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Autowired
    private FiltersRegistry filtersRegistry;

    @Override
    public Object getByFiltersAndSortingsWithPaging(Map<String, Object> filters,
                                                           List<Sorting> sortings,
                                                           Paging paging,
                                                           RowMapper rowMapper,
                                                           EntitySqlFields esf,
                                                           String methodName,
                                                           boolean returnsCollection) {
        Map<String, Object> params = new HashMap<>();

        Long offset = paging != null && paging.getOffset() != null ? paging.getOffset() : 0;
        Long limit = paging != null ? paging.getLimit() : null;

        String sqlPrefix = "";
        String sqlSuffix = "";

        if (limit != null) {
            sqlSuffix = "\nOFFSET " + offset + " LIMIT " + limit;
        }

        String filtersSql = filtersSql(filters, params, esf, "\n WHERE");
        String sortingsSql = sortingSql(sortings, "\n ORDER BY", esf);

        return logged(methodName,
            sqlPrefix + "SELECT " + esf.getAllSqlColumns() + " \n" +
                "FROM " + esf.getTableName() + " " +
                filtersSql +
                sortingsSql +
                sqlSuffix,

            sql -> returnsCollection ?
                jdbcTemplate.query(sql, params, rowMapper)
                : jdbcTemplate.queryForObject(sql, params, rowMapper)
        );
    }

    private String filtersSql(Map<String, Object> filters, Map<String, Object> params, EntitySqlFields esf, String prefix) {
        return filtersSql(filtersRegistry.getFilters(filters, esf), params, prefix);
    }

    private String filtersSql(List<Filter> filters, Map<String, Object> params, String prefix) {
        String res = new Group("AND", filters).buildSql(params);
        return res.isEmpty() ? res : (prefix != null && !prefix.isEmpty() ? prefix + " " : "") + res;
    }

    private String sortingSql(List<Sorting> sortings, String prefix, EntitySqlFields esf) {
        if (CollectionUtils.isEmpty(sortings)) {
            return "";
        }

        List<String> out = new ArrayList<>();
        for (Sorting s : sortings) {
            if (s.getRawSql() == null) {
                out.add(esf.byName(s.getField()).getSelectableSqlColumn() + " " + s.getDirection().value());
            } else {
                out.add(s.getRawSql() + " " + s.getDirection().value());
            }
        }

        String res = String.join(", ", out);
        return res.isEmpty() ? res : (prefix != null && !prefix.isEmpty() ? prefix + " " : "") + res;
    }
}
