package com.sbuslab.utils.db;

import javax.persistence.Table;
import javax.persistence.Transient;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import lombok.Data;
import org.springframework.stereotype.Component;

import com.sbuslab.model.Searchable;
import com.sbuslab.utils.StringUtils;
import com.sbuslab.utils.filters.Field;


@Data
@Component
public class EntitiesSqlFields {
    private Map<String, EntitySqlFields> sqlFieldsByCallerMethods = new ConcurrentHashMap<>();

    public void addSqlFieldsForType(String fullMethodName, Class<?> cl) {
        sqlFieldsByCallerMethods.putIfAbsent(fullMethodName, createEntitySqlFields(cl));
    }

    public EntitySqlFields getFieldsByFullMethodName(String methodName) {
        return sqlFieldsByCallerMethods.get(methodName);
    }

    private EntitySqlFields createEntitySqlFields(Class<?> cl) {
        Map<String, Field> fieldsByName = new HashMap<>();
        Map<String, Field> fieldsBySqlColumn = new HashMap<>();

        String tableName = cl.getAnnotation(Table.class).name();

        for (java.lang.reflect.Field f : cl.getDeclaredFields()) {
            boolean createSqlField = (!f.isAnnotationPresent(Transient.class) || f.isAnnotationPresent(Searchable.class)) && !f.getName().equals("embedded");
            Searchable searchableAnn = f.getAnnotation(Searchable.class);
            boolean selectable = searchableAnn == null || searchableAnn.selectable();

            if (createSqlField) {

                Method method = null;
                try {
                    method = cl.getDeclaredMethod("get" + f.getName().substring(0, 1).toUpperCase() + f.getName().substring(1));
                } catch (NoSuchMethodException e) {
                    // skip...
                }

                String selectableSqlColumnName = StringUtils.toUnderscore(f.getName());
                String filteringSqlColumnName = selectableSqlColumnName;

                if (searchableAnn != null && !searchableAnn.matchAgainstColumn().isEmpty()) {
                    filteringSqlColumnName = searchableAnn.matchAgainstColumn();
                }

                String filteringExpression = searchableAnn == null ? ":" + f.getName() : searchableAnn.query();

                Field field = new Field(
                    f.getName(),
                    tableName + "." + selectableSqlColumnName,
                    filteringSqlColumnName,
                    f.getType(),
                    method,
                    filteringExpression,
                    selectable);

                fieldsByName.put(field.getName(), field);

                if (selectable) {
                    fieldsBySqlColumn.put(field.getSelectableSqlColumn(), field);
                }
            }
        }

        String allSqlColumns = String.join(", ", fieldsBySqlColumn.keySet());

        return new EntitySqlFields(fieldsByName, fieldsBySqlColumn, allSqlColumns, tableName, cl.getSimpleName());
    }
}
