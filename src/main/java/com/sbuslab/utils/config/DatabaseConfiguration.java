package com.sbuslab.utils.config;

import javax.sql.DataSource;
import java.util.Properties;

import com.typesafe.config.Config;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

import com.sbuslab.utils.db.DbMigration;


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

        HikariConfig hk = new HikariConfig();
        hk.setJdbcUrl(String.format("jdbc:%s://%s:%d/%s", conf.getString("driver"), conf.getString("host"), conf.getInt("port"), conf.getString("db")));
        hk.setDriverClassName(conf.getString("driverClassName"));
        hk.setUsername(conf.getString("username"));
        hk.setPassword(conf.getString("password"));
        hk.setConnectionTimeout(conf.getInt("connection-timeout"));
        hk.setValidationTimeout(conf.getInt("validation-timeout"));
        hk.setMinimumIdle(conf.getInt("initial-size"));
        hk.setMaximumPoolSize(conf.getInt("max-active"));
        hk.setAutoCommit(conf.getBoolean("auto-commit"));
        hk.addDataSourceProperty("stringtype", "unspecified"); // it is needed to be able to store string values as jsonb

        return new HikariDataSource(hk);
    }

    @Bean
    @Autowired
    @DependsOn("dbMigrations")
    public NamedParameterJdbcTemplate getJdbcTemplate(DataSource datasource) {
        return new NamedParameterJdbcTemplate(datasource);
    }

    @Bean
    @Autowired
    @DependsOn("dbMigrations")
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource datasource) {
        LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
        factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        factory.setPackagesToScan("com.sbuslab");

        Properties props = new Properties();
        Config dbConfig = getDbConfig().getConfig("hibernate");
        dbConfig.entrySet().forEach(entry -> props.put(entry.getKey(), entry.getValue().unwrapped()));
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
}
