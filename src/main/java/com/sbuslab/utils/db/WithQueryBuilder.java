package com.sbuslab.utils.db;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface WithQueryBuilder {
    String query() default "";
    boolean returnsCollection() default true;
    Class<?> entityClass();
    Class<? extends QueryBuilder> queryBuilder() default DefaultQueryBuilder.class;
    Class<? extends JacksonBeanRowMapper> rowMapper() default JacksonBeanRowMapper.class;
}
