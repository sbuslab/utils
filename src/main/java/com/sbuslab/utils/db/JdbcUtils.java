package com.sbuslab.utils.db;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class JdbcUtils {

    private static final Logger log = LoggerFactory.getLogger(JdbcUtils.class);

    private final DataSource connectionPool;

    public JdbcUtils(DataSource connectionPool) {
        this.connectionPool = connectionPool;
    }

    public <T> T withTransaction(Function<Connection, T> f) throws Throwable {
        Connection connection = connectionPool.getConnection();

        try {
            connection.setAutoCommit(false);

            T result = f.apply(connection);

            connection.commit();

            return result;
        } catch (Throwable e) {
            try {
                connection.rollback();
            } catch (Throwable ex) {
                log.error("Error occured during transaction rollback", ex);
            }
            throw e;
        } finally {
            try {
                connection.close();
            } catch (Throwable e) {
                log.error("Error occurred during returning connection back to the pool", e);
            }
        }
    }
}
