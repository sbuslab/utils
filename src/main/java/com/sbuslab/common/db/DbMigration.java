package com.sbuslab.common.db;

import java.util.Properties;

import com.typesafe.config.Config;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class DbMigration {

    private static final Logger log = LoggerFactory.getLogger(DbMigration.class);

    private final Config config;

    public DbMigration(Config config) {
        this.config = config;
    }

    public void run() {
        try {
            log.info("Migrations starting...");

            Config conf = config.hasPath("migrations") ? config.getConfig("migrations").withFallback(config) : config;

            Properties props = new Properties();
            props.put("flyway.driver", conf.getString("driverClassName"));
            props.put("flyway.url", String.format("jdbc:%s://%s:%d/%s", conf.getString("driver"), conf.getString("host"), conf.getInt("port"), conf.getString("db")));
            props.put("flyway.user", conf.getString("username"));
            props.put("flyway.password", conf.getString("password"));
            props.put("flyway.ignoreFutureMigrations", "true");
            props.put("flyway.baselineOnMigrate", "true");
            props.put("flyway.outOfOrder", "true");

            if (conf.hasPath("locations")) {
                props.put("flyway.locations", conf.getString("locations"));
            }

            if (conf.hasPath("table")) {
                props.put("flyway.table", conf.getString("table"));
            }

            Flyway flyway = new Flyway();
            flyway.configure(props);

            log.info("[" + flyway.migrate() + "] migrations are applied");

        } catch (FlywayException ex) {
            throw new RuntimeException("Exception during database migrations: ", ex);
        }
    }
}
