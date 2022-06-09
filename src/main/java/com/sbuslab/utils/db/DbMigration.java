package com.sbuslab.utils.db;

import com.typesafe.config.Config;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.configuration.FluentConfiguration;
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

            FluentConfiguration flywayConf = Flyway.configure()
                .dataSource(
                    String.format("jdbc:%s://%s:%d/%s", conf.getString("driver"), conf.getString("host"), conf.getInt("port"), conf.getString("db")),
                    conf.getString("username"),
                    conf.getString("password")
                )
                .ignoreFutureMigrations(true)
                .ignoreMissingMigrations(true)
                .baselineOnMigrate(true)
                .outOfOrder(true);

            if (conf.hasPath("locations")) {
                String[] locations = null;
                try {
                    locations = conf.getStringList("locations").stream().toArray(String[]::new);
                } catch(ConfigException.WrongType e) {
                   locations = new String[]{conf.getString("locations")};
                }

                flywayConf.locations(locations);
            }

            if (conf.hasPath("table")) {
                flywayConf.table(conf.getString("table"));
            }

            Flyway flyway = flywayConf.load();

            log.info("[" + flyway.migrate() + "] migrations are applied");

        } catch (FlywayException ex) {
            log.error("Migrations error: " + ex.getMessage());
            throw new ExceptionInInitializerError("Exception during database migrations: " + ex.getMessage()) {{
                initCause(ex);
            }};
        }
    }
}
