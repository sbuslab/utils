package com.sbuslab.utils.db;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sbuslab.model.Paging;
import com.sbuslab.model.Sorting;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;


@Slf4j
@Aspect
@Component
public class AopDbQueryExecutor {

    @Autowired
    private EntitiesSqlFields entitiesSqlFields;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ApplicationContext appContext;

    @Around(value = "@annotation(com.sbuslab.utils.db.WithQueryBuilder) && args()", argNames = "joinPoint")
    public Object buildQueryAndExecute(ProceedingJoinPoint joinPoint) {
        return executeRequest(joinPoint, null, null, null);
    }

    @Around(value = "@annotation(com.sbuslab.utils.db.WithQueryBuilder) && args(filters)", argNames = "joinPoint,filters")
    public Object buildQueryAndExecute(ProceedingJoinPoint joinPoint, Map<String, Object> filters) {
        return executeRequest(joinPoint, filters, null, null);
    }

    @Around(value = "@annotation(com.sbuslab.utils.db.WithQueryBuilder) && args(filters,sortings)", argNames = "joinPoint,filters,sortings")
    public Object buildQueryAndExecute(ProceedingJoinPoint joinPoint, Map<String, Object> filters, List<Sorting> sortings) {
        return executeRequest(joinPoint, filters, sortings, null);
    }

    @Around(value = "@annotation(com.sbuslab.utils.db.WithQueryBuilder) && args(filters,sortings,paging)", argNames = "joinPoint,filters,sortings,paging")
    public Object buildQueryAndExecute(ProceedingJoinPoint joinPoint, Map<String, Object> filters, List<Sorting> sortings, Paging paging) {
        return executeRequest(joinPoint, filters, sortings, paging);
    }

    private Object executeRequest(ProceedingJoinPoint joinPoint,
                                  Map<String, Object> filters,
                                  List<Sorting> sortings,
                                  Paging paging) {
        String targetClassName = joinPoint.getSignature().getDeclaringType().getCanonicalName();
        String targetMethodName = joinPoint.getSignature().getName();
        String fullMethodName = targetClassName + "." + targetMethodName;

        WithQueryBuilder ann = ((MethodSignature) joinPoint.getSignature()).getMethod().getAnnotation(WithQueryBuilder.class);

        RowMapper rowMapper = ann.rowMapper().equals(JacksonBeanRowMapper.class) ?
            new JacksonBeanRowMapper(ann.entityClass(), objectMapper) : appContext.getBean(ann.rowMapper());

        QueryBuilder qb = appContext.getBean(ann.queryBuilder());

        EntitySqlFields esf = entitiesSqlFields.getFieldsByFullMethodName(fullMethodName);

        return qb.getByFiltersAndSortingsWithPaging(filters, sortings, paging, rowMapper, esf, fullMethodName, ann.returnsCollection());
    }
}
