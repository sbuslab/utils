package com.sbuslab.utils.filters;

import javax.validation.constraints.NotNull;
import java.util.Collection;
import java.util.Map;

import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.With;


@With
@Value
@EqualsAndHashCode(callSuper = true)
public class ArrayFilter extends Filter {

    @NotNull
    private final Field field;

    @NotNull
    private final Collection<?> value;

    private final boolean negative;

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
            nullable = field.getFilteringSqlColumn() + (isNegative() ? " IS NOT NULL AND " : " IS NULL OR ");
        }

        return nullable + field.getFilteringSqlColumn() +
               (isNegative() ? " NOT" : "") +
               " IN (" + field.getFilteringExpression() + ") ";
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
