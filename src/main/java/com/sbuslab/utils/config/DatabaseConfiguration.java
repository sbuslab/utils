package com.sbuslab.utils.config;

import javax.sql.DataSource;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.typesafe.config.Config;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.reflections.Reflections;
import org.reflections.scanners.MethodAnnotationsScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static com.typesafe.config.ConfigValueFactory.fromAnyRef;

import com.sbuslab.utils.db.DbMigration;
import com.sbuslab.utils.db.EntitiesSqlFields;
import com.sbuslab.utils.db.JdbcUtils;
import com.sbuslab.utils.db.WithQueryBuilder;


@Configuration
public abstract class DatabaseConfiguration extends DefaultConfiguration {

    @Bean(name = {"dbConfig"})
    abstract protected Config getDbConfig();

    @Bean(initMethod = "run")
    public static DbMigration dbMigrations(@Qualifier("dbConfig") Config dbConfig) {
        return new DbMigration(dbConfig);
    }

    @Bean
    @DependsOn("dbMigrations")
    public static DataSource getDatasource(@Qualifier("dbConfig") Config dbConfig) {
        return createDatasource(dbConfig);
    }

    /**
     * @deprecated use {#createDatasource} instead
     */
    @Deprecated
    protected DataSource makeDatasource(Config conf, int port) {
        return createDatasource(conf.withValue("port", fromAnyRef(port)));
    }

    protected static DataSource createDatasource(Config conf) {
        HikariConfig hk = new HikariConfig();
        hk.setJdbcUrl(String.format("jdbc:%s://%s:%d/%s", conf.getString("driver"),
            conf.getString("host"), conf.getInt("port"), conf.getString("db")));
        hk.setDriverClassName(conf.getString("driverClassName"));
        hk.setUsername(conf.getString("username"));
        hk.setPassword(conf.getString("password"));
        hk.setConnectionTimeout(conf.getInt("connection-timeout"));
        hk.setValidationTimeout(conf.getInt("validation-timeout"));
        hk.setIdleTimeout(conf.getInt("idle-timeout"));
        hk.setMaxLifetime(conf.getInt("max-lifetime"));
        hk.setMinimumIdle(conf.getInt("initial-size"));
        hk.setMaximumPoolSize(conf.getInt("max-active"));
        hk.setAutoCommit(conf.getBoolean("auto-commit"));
        hk.addDataSourceProperty("stringtype", "unspecified"); // it is needed to be able to store string values as jsonb

        return new HikariDataSource(hk);
    }

    @Bean
    public static Reflections initDbQueryBuilders(ApplicationContext appContext, @Qualifier("dbConfig") Config dbConfig) {
        List<String> packages = dbConfig.getStringList("packages-to-scan");
        List<URL> urls = new ArrayList<>();
        packages.forEach(p -> urls.addAll(ClasspathHelper.forPackage(p)));

        Reflections reflections = new Reflections(
            new ConfigurationBuilder()
                .setUrls(urls)
                .filterInputsBy(new FilterBuilder().includePackage(packages.toArray(new String[0])))
                .setScanners(new MethodAnnotationsScanner()));

        EntitiesSqlFields esf = appContext.getBean(EntitiesSqlFields.class);
        reflections.getMethodsAnnotatedWith(WithQueryBuilder.class).forEach(method -> {
            WithQueryBuilder ann = method.getAnnotation(WithQueryBuilder.class);
            String parentName = method.getDeclaringClass().getCanonicalName();
            String fullMethodName = parentName + "." + method.getName();
            esf.addSqlFieldsForType(fullMethodName, ann.entityClass());
        });
        return reflections;
    }

    @Bean
    @Primary
    @DependsOn("dbMigrations")
    public static NamedParameterJdbcTemplate getJdbcTemplate(DataSource datasource) {
        return new NamedParameterJdbcTemplate(datasource);
    }

    @Lazy
    @Bean(name = "readOnlyJdbc")
    @DependsOn("dbMigrations")
    public static NamedParameterJdbcTemplate getReadonlyJdbcTemplate(@Qualifier("dbConfig") Config dbConfig) {
        if (dbConfig.hasPath("readonly-host")) {
            dbConfig = dbConfig.withValue("host", fromAnyRef(dbConfig.getString("readonly-host")));
        }
        dbConfig = dbConfig.withValue("port", fromAnyRef(dbConfig.getString("readonly-port")));
        return new NamedParameterJdbcTemplate(createDatasource(dbConfig));
    }

    @Bean
    @DependsOn("dbMigrations")
    public static LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource datasource, @Qualifier("dbConfig") Config dbConfig) {
        LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
        factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());

        Properties props = new Properties();
        Config hibernateConfig = dbConfig.getConfig("hibernate");
        hibernateConfig.entrySet().forEach(entry -> props.put(entry.getKey(), entry.getValue().unwrapped()));

        factory.setPackagesToScan(dbConfig.getStringList("packages-to-scan").toArray(new String[0]));
        factory.setJpaProperties(props);

        factory.setDataSource(datasource);

        return factory;
    }

    @Bean
    @DependsOn("dbMigrations")
    public static PlatformTransactionManager transactionManager(LocalContainerEntityManagerFactoryBean entityManagerFactory) {
        JpaTransactionManager txManager = new JpaTransactionManager();
        txManager.setEntityManagerFactory(entityManagerFactory.getObject());
        return txManager;
    }

    @Bean
    @DependsOn("dbMigrations")
    public static JdbcUtils jdbcUtils(DataSource datasource) {
        return new JdbcUtils(datasource);
    }

    @Bean
    public static TransactionTemplate transactionTemplate(PlatformTransactionManager ptm) {
        return new TransactionTemplate(ptm);
    }
}
