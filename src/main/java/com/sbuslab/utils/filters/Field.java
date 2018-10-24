package com.sbuslab.utils.filters;

import javax.validation.constraints.NotNull;
import java.lang.reflect.Method;

import lombok.Value;
import org.springframework.jdbc.core.SqlTypeValue;

import com.sbuslab.model.BadRequestError;
import com.sbuslab.model.ErrorMessage;


@Value
final public class Field {

    @NotNull
    private final String name;

    private final String selectableSqlColumn;

    @NotNull
    private final String filteringSqlColumn;

    @NotNull
    private final Class<?> filterType;

    private final boolean isLong;

    private final boolean isString;

    private final Method method;

    private Method enumParserMethod;

    private String filteringExpression;

    private Boolean isSelectable;

    private CustomFilterBuilder customFilterBuilder;

    public Field(@NotNull String name,
                 @NotNull String selectableSqlColumn,
                 String filteringSqlColumn,
                 @NotNull Class<?> filterType,
                 Method method,
                 String filteringExpression,
                 @NotNull Boolean isSelectable,
                 CustomFilterBuilder customFilterBuilder) {
        this.name = name;
        this.selectableSqlColumn = selectableSqlColumn;
        this.filteringSqlColumn = filteringSqlColumn;
        this.filterType = filterType;
        this.method = method;
        this.isLong = filterType.equals(Long.TYPE) || filterType.equals(Integer.TYPE);
        this.isString = filterType.equals(String.class);
        this.filteringExpression = filteringExpression;
        this.isSelectable = isSelectable;
        this.customFilterBuilder = customFilterBuilder;

        Method enumParserMethod = null;
        if (SqlTypeValue.class.isAssignableFrom(filterType)) {
            try {
                enumParserMethod = filterType.getMethod("fromValue", String.class);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        }

        this.enumParserMethod = enumParserMethod;
    }

    public Object invoke(Object obj) {
        try {
            return method.invoke(obj);
        } catch (ErrorMessage e) {
            throw e;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public Object parseValue(Object value) {
        if (value == null) return null;

        if (enumParserMethod != null) {
            try {
                return enumParserMethod.invoke(null, value.toString());
            } catch (Throwable e) {
                throw new BadRequestError("Error parse enum value for " + name + ": " + value, e.getCause() != null ? e.getCause() : e, "filters");
            }
        }

        if (isLong) {
            return Long.parseLong(value.toString());
        }

        if (isString) {
            return value.toString();
        }

        return value;
    }
}
