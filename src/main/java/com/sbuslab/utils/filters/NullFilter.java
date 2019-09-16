package com.sbuslab.utils.filters;

import javax.validation.constraints.NotNull;
import java.util.Map;

import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.With;


@With
@Value
@EqualsAndHashCode(callSuper = true)
public class NullFilter extends Filter {

    @NotNull
    private final Field field;

    private final boolean negative;

    public NullFilter(@NotNull Field field) {
        this(field, false);
    }

    public NullFilter(@NotNull Field field, boolean negative) {
        this.field = field;
        this.negative = negative;
    }

    public String buildSql(Map<String, Object> params) {
        return field.getFilteringSqlColumn() + (isNegative() ? " IS NOT NULL" : " IS NULL");
    }

    @Override
    public boolean applied(Object target) {
        return field.invoke(target) == null;
    }
}
