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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.sbuslab.utils.db.DbMigration;
import com.sbuslab.utils.db.EntitiesSqlFields;
import com.sbuslab.utils.db.JdbcUtils;
import com.sbuslab.utils.db.WithQueryBuilder;


@Configuration
public abstract class DatabaseConfiguration extends DefaultConfiguration {

    abstract protected Config getDbConfig();

    @Bean(initMethod = "run")
    public DbMigration dbMigrations() {
        return new DbMigration(getDbConfig());
    }

    @Bean
    @DependsOn("dbMigrations")
    public DataSource getDatasource() {
        Config conf = getDbConfig();
        return makeDatasource(conf, conf.getInt("port"));
    }

    protected DataSource makeDatasource(Config conf, int port) {
        HikariConfig hk = new HikariConfig();
        hk.setJdbcUrl(String.format("jdbc:%s://%s:%d/%s", conf.getString("driver"), conf.getString("host"), port, conf.getString("db")));
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
    @Autowired
    public Reflections initDbQueryBuilders(ApplicationContext appContext) {
        List<String> packages = getDbConfig().getStringList("packages-to-scan");
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
    @Autowired
    @DependsOn("dbMigrations")
    public NamedParameterJdbcTemplate getJdbcTemplate(DataSource datasource) {
        return new NamedParameterJdbcTemplate(datasource);
    }

    @Lazy
    @Bean(name = "readOnlyJdbc")
    @DependsOn("dbMigrations")
    public NamedParameterJdbcTemplate getReadonlyJdbcTemplate() {
        Config conf = getDbConfig();
        return new NamedParameterJdbcTemplate(makeDatasource(conf, conf.getInt("readonly-port")));
    }

    @Bean
    @Autowired
    @DependsOn("dbMigrations")
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource datasource) {
        LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
        factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());

        Properties props = new Properties();
        Config dbConfig = getDbConfig().getConfig("hibernate");
        dbConfig.entrySet().forEach(entry -> props.put(entry.getKey(), entry.getValue().unwrapped()));

        factory.setPackagesToScan(getDbConfig().getStringList("packages-to-scan").toArray(new String[0]));
        factory.setJpaProperties(props);

        factory.setDataSource(datasource);

        return factory;
    }

    @Bean
    @Autowired
    @DependsOn("dbMigrations")
    public PlatformTransactionManager transactionManager(DataSource datasource) {
        JpaTransactionManager txManager = new JpaTransactionManager();
        txManager.setEntityManagerFactory(entityManagerFactory(datasource).getObject());
        return txManager;
    }

    @Bean
    @Autowired
    @DependsOn("dbMigrations")
    public JdbcUtils jdbcUtils(DataSource datasource) {
        return new JdbcUtils(datasource);
    }

    @Bean
    @Autowired
    public TransactionTemplate transactionTemplate(PlatformTransactionManager ptm) {
        return new TransactionTemplate(ptm);
    }
}
