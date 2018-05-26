package com.sbuslab.utils.filters;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Value;
import lombok.experimental.Wither;
import org.springframework.util.CollectionUtils;

import com.sbuslab.model.UnrecoverableError;


@Value
@Wither
@AllArgsConstructor
public class Group implements Expression {

    @NotNull
    private final String operand;

    private final boolean negative;

    @NotNull
    private final List<Expression> inner;

    public Group(String operand, List<Expression> inner) {
        this.operand = operand;
        this.inner = inner;
        this.negative = false;
    }

    public Group negative() {
        return new Group(operand, true, inner);
    }

    public String buildSql(Map<String, Object> params) {
        if (CollectionUtils.isEmpty(inner)) {
            return "";
        }

        if (!operand.equalsIgnoreCase("or") && !operand.equalsIgnoreCase("and")) {
            throw new UnrecoverableError("Unsupported sql operand: " + operand);
        }

        List<String> out = new ArrayList<>();
        for (Expression ex : inner) {
            out.add(ex.buildSql(params));
        }

        return (isNegative() ? " NOT " : "") +
               "(" + String.join(" " + operand.toUpperCase() + " ", out) + ")";
    }

    public boolean applied(Object target) {
        return false;
    }
}
