package com.sbuslab.utils.filters;

import javax.persistence.Transient;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.util.CollectionUtils;

import com.sbuslab.model.Range;
import com.sbuslab.model.Sorting;
import com.sbuslab.utils.StringUtils;
import com.sbuslab.model.BadRequestError;
import com.sbuslab.model.ErrorMessage;


public class FieldFilters<T> {

    private Map<String, Field> fieldsByName = new HashMap<>();
    private Map<String, Field> fieldsBySqlColumn = new HashMap<>();

    private String allSqlColumns;

    public FieldFilters(Class<T> cl, String tableName) {
        for (java.lang.reflect.Field f : cl.getDeclaredFields()) {
            if (!f.isAnnotationPresent(Transient.class) && !f.getName().equals("embedded")) {

                Method method = null;
                try {
                    method = cl.getDeclaredMethod("get" + f.getName().substring(0, 1).toUpperCase() + f.getName().substring(1));
                } catch (NoSuchMethodException e) {
                    // skip...
                }

                Field field = new Field(
                    f.getName(),
                    tableName + "." + StringUtils.toUnderscore(f.getName()),
                    f.getType(),
                    method
                );

                fieldsByName.put(field.getName(), field);
                fieldsBySqlColumn.put(field.getSqlColumn(), field);
            }
        }

        allSqlColumns = String.join(", ", fieldsBySqlColumn.keySet());
    }

    public Field byName(String name) {
        Field f = fieldsByName.get(name);

        if (f == null) {
            throw new BadRequestError("Order field `" + name + "` doesn't exist!", null, "filters");
        }

        return f;
    }

    private Expression getFilter(String filterName, Object value, ObjectMapper mapper) {
        Field field = byName(filterName);

        if (value == null) {
            return new NullFilter(field);
        } else if (value instanceof Map || value instanceof scala.collection.Map || value instanceof Range) {
            return new RangeFilter(field, mapper.convertValue(value, Range.class));
        } else {
            return new ArrayFilter(
                field,
                forceList(value, mapper).stream().map(field::parseValue).collect(Collectors.toList()),
                false
            );
        }
    }

    private Collection<?> forceList(Object value, ObjectMapper mapper) {
        Collection<?> list;
        Class cls = value.getClass();

        if (value instanceof Collection<?>) {
            list = (Collection<?>) value;
        } else if (cls.isArray()) {
            list = Arrays.asList((Object[]) value);
        } else if (cls.isPrimitive()) {
            list = Collections.singletonList(value);
        } else if (cls.getName().contains("scala.collection.")) {
            list = mapper.convertValue(value, ArrayList.class);
        } else {
            list = Collections.singletonList(value);
        }
        return list;
    }

    public List<Expression> getFilters(Map<String, Object> filters, ObjectMapper mapper) {
        if (CollectionUtils.isEmpty(filters)) {
            return Collections.emptyList();
        }

        List<Expression> exps = new ArrayList<>();
        for (Map.Entry<String, Object> entry : filters.entrySet()) {
            try {
                exps.add(getFilter(entry.getKey(), entry.getValue(), mapper));
            } catch (ErrorMessage e) {
                throw e;
            } catch (Exception e) {
                throw new BadRequestError("Error on parse order filter: '" + entry.getKey() + "' -> '" + entry.getValue() + "'", e);
            }
        }
        return exps;
    }

    public String filtersSql(Map<String, Object> filters, Map<String, Object> params, ObjectMapper mapper, String prefix) {
        return filtersSql(getFilters(filters, mapper), params, prefix);
    }

    public String filtersSql(Map<String, Object> filters, Map<String, Object> params, ObjectMapper mapper) {
        return filtersSql(getFilters(filters, mapper), params);
    }

    public String filtersSql(List<Expression> filters, Map<String, Object> params) {
        return filtersSql(filters, params, "");
    }

    public String filtersSql(List<Expression> filters, Map<String, Object> params, String prefix) {
        String res = new Group("AND", filters).buildSql(params);
        return res.isEmpty() ? res : (prefix != null && !prefix.isEmpty() ? prefix + " " : "") + res;
    }

    public String sortingSql(List<Sorting> sortings) {
        return sortingSql(sortings, "");
    }

    public String sortingSql(List<Sorting> sortings, String prefix) {
        if (CollectionUtils.isEmpty(sortings)) {
            return "";
        }

        List<String> out = new ArrayList<>();
        for (Sorting s : sortings) {
            out.add(byName(s.getField()).getSqlColumn() + " " + s.getDirection().value());
        }

        String res = String.join(", ", out);
        return res.isEmpty() ? res : (prefix != null && !prefix.isEmpty() ? prefix + " " : "") + res;
    }

    public String getAllSqlColumns() {
        return allSqlColumns;
    }
}
