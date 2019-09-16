package com.sbuslab.utils.filters;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.With;

import com.sbuslab.model.Range;


@With
@Value
@EqualsAndHashCode(callSuper = true)
public class RangeFilter extends Filter {

    @NotNull
    private final Field field;

    @NotNull
    private final Range value;

    private final boolean negative;

    public RangeFilter(@NotNull Field field, @NotNull Range value) {
        this(field, value, false);
    }

    public RangeFilter(@NotNull Field field, @NotNull Range value, boolean negative) {
        this.field = field;
        this.value = value;
        this.negative = negative;
    }

    public String buildSql(Map<String, Object> params) {
        String fromName = field.getName() + "From";
        String toName = field.getName() + "To";

        List<String> out = new ArrayList<>();

        if (value.getFrom() != null) {
            out.add(field.getFilteringSqlColumn() + " >= :" + fromName);
            params.put(fromName, value.getFrom());
        }

        if (value.getTo() != null) {
            out.add(field.getFilteringSqlColumn() + " <= :" + toName);
            params.put(toName, value.getTo());
        }

        if (out.isEmpty()) return "";

        return (isNegative() ? "NOT " : "") +
               "(" + String.join(" AND ", out) + ")";
    }

    @Override
    public boolean applied(Object target) {
        Object res = field.invoke(target);
        BigDecimal result = new BigDecimal(res != null ? res.toString() : "0");

        return (value.getFrom() == null || result.compareTo(value.getFrom()) >= 0) &&
               (value.getTo() == null || result.compareTo(value.getTo()) <= 0);
    }
}
