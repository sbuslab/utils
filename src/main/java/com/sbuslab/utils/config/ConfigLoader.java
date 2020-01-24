package com.sbuslab.utils.config;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Optional;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import lombok.extern.slf4j.Slf4j;

import com.sbuslab.utils.FileUtils;


/**
 * -Dconfig.localfile=/path/to/local-config-fiel.conf
 * or
 * export CONFIG_LOCALFILE=/path/to/local-config-fiel.conf
 */
@Slf4j
public class ConfigLoader {

    public static Config load() {
        String configPaths = System.getProperty("config.localfile",
            Optional.ofNullable(System.getenv("SBUS_CONFIG_LOCALFILE")).orElse(""))
                .replace("/", File.separator)
                .replace("\\", File.separator);

        try {
            String secretsUrl = System.getenv("SECRET_CONFIG_URL");

            if (secretsUrl != null && !secretsUrl.isEmpty()) {
                Config secrets = ConfigFactory.parseURL(new URL(secretsUrl));

                secrets.entrySet().forEach(entry ->
                    System.setProperty("SECRET_" + entry.getKey().toUpperCase().replaceAll("[^A-Z0-9]+", "_"), entry.getValue().unwrapped().toString())
                );
            }
        } catch (Exception e) {
            log.warn("Error on load configs from url " + System.getenv("SECRET_CONFIG_URL") + ": " + e.getMessage(), e);
        }

        Config resultConfig = ConfigFactory.load();

        for (String path : configPaths.split(":")) {
            try {
                File file = new File(FileUtils.getFileUrl(path).toURI());

                if (!file.exists()) {
                    log.warn(file.getAbsolutePath() + " is not found, skip");
                } else {
                    resultConfig = ConfigFactory.parseFile(file).withFallback(resultConfig).resolve();
                }
            } catch (FileNotFoundException | URISyntaxException e) {
                log.warn(path + " is not found, skip");
            }
        }

        return resultConfig;
    }
}
