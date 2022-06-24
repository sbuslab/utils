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
 * -Dconfig.localfile=/path/to/local-config-file.conf
 * or
 * export SBUS_CONFIG_LOCALFILE=/path/to/local-config-file.conf
 */
@Slf4j
public class ConfigLoader {

    public final static Config INSTANCE = load();

    private static Config load() {
        String localConfigFiles = System.getProperty("config.localfile",
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

        for (String filePath : localConfigFiles.split(":")) {
            try {
                File file = new File(FileUtils.getFileUrl(filePath).toURI());

                if (!file.exists()) {
                    log.warn(file.getAbsolutePath() + " is not found, skip");
                } else {
                    resultConfig = ConfigFactory.parseFile(file).withFallback(resultConfig);
                }
            } catch (FileNotFoundException | URISyntaxException e) {
                log.warn(filePath + " is not found, skip");
            }
        }

        resultConfig = ConfigFactory.defaultOverrides().withFallback(resultConfig);

        String extraConfigUrl = resultConfig.getString("sbuslab.config.external-url");

        if (!extraConfigUrl.isEmpty()) {
            try {
                resultConfig = ConfigFactory.parseURL(new URL(extraConfigUrl)).withFallback(resultConfig);
            } catch (Exception e) {
                log.warn("Error on load external config from url: " + extraConfigUrl + ". " + e.getMessage(), e);
            }
        }

        return resultConfig.resolve();
    }
}
