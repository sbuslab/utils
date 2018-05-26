package com.sbuslab.utils.filters;

import javax.validation.constraints.NotNull;
import java.util.Collection;
import java.util.Map;

import lombok.Value;
import lombok.experimental.Wither;


@Value
@Wither
public class ArrayFilter implements Expression {

    @NotNull
    private final Field field;

    @NotNull
    private final Collection<?> value;

    private final boolean negative;

    public <T> ArrayFilter(@NotNull String fieldName, @NotNull Collection<?> value, @NotNull Class<T> cl, String tableName) {
        this(new FieldFilters(cl, tableName).byName(fieldName), value, false);
    }

    public ArrayFilter(@NotNull Field field, @NotNull Collection<?> value) {
        this(field, value, false);
    }

    public ArrayFilter(@NotNull Field field, @NotNull Collection<?> value, boolean negative) {
        this.field = field;
        this.value = value;
        this.negative = negative;
    }

    public ArrayFilter negative() {
        return new ArrayFilter(field, value, true);
    }

    public String buildSql(Map<String, Object> params) {
        params.put(field.getName(), value);

        String nullable = "";
        if (value.contains(null)) {
            nullable = field.getSqlColumn() + (isNegative() ? " IS NOT NULL AND " : " IS NULL OR ");
        }

        return nullable + field.getSqlColumn() +
               (isNegative() ? " NOT" : "") +
               " IN (:" + field.getName() + ") ";
    }

    @Override
    public boolean applied(Object target) {
        Object rawResult = field.invoke(target);

        if (value.contains(rawResult)) {
            return true;
        } else if (rawResult == null) {
            return false;
        } else {
            String result = rawResult.toString();
            for (Object v : value) {
                if (v.toString().equalsIgnoreCase(result)) {
                    return true;
                }
            }

            return false;
        }
    }
}
