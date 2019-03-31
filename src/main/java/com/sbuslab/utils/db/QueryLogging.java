package com.sbuslab.utils.db;

import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

import io.prometheus.client.Histogram;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;


public abstract class QueryLogging {

    private static final Histogram sqlQueriesStat = Histogram.build()
        .name("sql_queries")
        .help("Execute SQL queries")
        .labelNames("query")
        .register();

    protected static Logger log = LoggerFactory.getLogger("sql.query.logging");

    protected <T> T logged(String name, String sql, Function<String, T> func) {
        return logged(name, sql, null, func);
    }

    protected <T> T logged(String name, String sql, Map<String, Object> params, Function<String, T> func) {
        return logged(name, sql, params, (sql2, ignored) -> func.apply(sql2));
    }

    protected <T> T logged(String name, String sql, Map<String, ?> params, BiFunction<String, Map<String, ?>, T> func) {
        int space = name.indexOf(" ");
        Histogram.Timer timer = sqlQueriesStat.labels(space == -1 ? name : name.substring(0, space)).startTimer();

        try {
            T result = func.apply(sql, params);

            double spent = timer.observeDuration();

            if (log.isTraceEnabled()) {
                log.trace("SQL {} ({}ms): {}{}", name, Math.round(spent * 1000), sql.trim(), CollectionUtils.isEmpty(params) ? "" : "\n" + params);
            }

            return result;
        } catch (Throwable e) {
            double spent = timer.observeDuration();
            log.warn("SQL {} ({}ms): {}{}", name, Math.round(spent * 1000), sql.trim(), CollectionUtils.isEmpty(params) ? "" : "\n" + params);
            throw e;
        }
    }
}
