package com.sbuslab.utils.db;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.sbuslab.model.BadRequestError;
import com.sbuslab.model.ErrorMessage;
import com.sbuslab.model.Range;
import com.sbuslab.utils.filters.*;


@Component
public class FiltersRegistry {

    @Autowired
    private ObjectMapper objectMapper;

    private final Map<String, CustomFilterBuilder> customFilterBuilders = new ConcurrentHashMap<>();

    public List<Filter> getFilters(Map<String, Object> filters, EntitySqlFields esf) {
        if (CollectionUtils.isEmpty(filters)) {
            return Collections.emptyList();
        }

        List<Filter> exps = new ArrayList<>();
        for (Map.Entry<String, Object> entry : filters.entrySet()) {
            try {
                exps.add(getFilter(entry.getKey(), entry.getValue(), esf));
            } catch (ErrorMessage e) {
                throw e;
            } catch (Exception e) {
                throw new BadRequestError("Error on parse order filter: '" + entry.getKey() + "' -> '" + entry.getValue() + "'", e);
            }
        }
        return exps;
    }

    public void registerCustomFilterBuilder(String name, CustomFilterBuilder filterBuilder) {
        customFilterBuilders.put(name, filterBuilder);
    }

    private Filter getFilter(String filterName, Object value, EntitySqlFields esf) {
        Field field = esf.byName(filterName);


        if (customFilterBuilders.containsKey(filterName)) {
            return customFilterBuilders.get(filterName).createFilter(filterName, value, esf, objectMapper);
        }

        if (value == null) {
            return new NullFilter(field);
        }

        if (value instanceof Map || value instanceof scala.collection.Map || value instanceof Range) {
            return new RangeFilter(field, objectMapper.convertValue(value, Range.class));
        }

        return new ArrayFilter(
            field,
            forceList(value).stream().map(field::parseValue).collect(Collectors.toList()),
            false
        );
    }

    private Collection<?> forceList(Object value) {
        Collection<?> list;
        Class cls = value.getClass();

        if (value instanceof Collection<?>) {
            list = (Collection<?>) value;
        } else if (cls.isArray()) {
            list = Arrays.asList((Object[]) value);
        } else if (cls.isPrimitive()) {
            list = Collections.singletonList(value);
        } else if (cls.getName().contains("scala.collection.")) {
            list = objectMapper.convertValue(value, ArrayList.class);
        } else {
            list = Collections.singletonList(value);
        }
        return list;
    }
}
