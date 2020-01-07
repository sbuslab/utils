package com.sbuslab.utils.config;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URISyntaxException;
import java.net.URL;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import lombok.extern.slf4j.Slf4j;

import com.sbuslab.utils.FileUtils;

/**
 * config.localfile - позволяет задать локальную конфигурацию для каждого проекта через VM options
 * (-Dconfig.localfile=путь_к_настройкам)
 * <p>
 * LOCALFILE_SBUS_CONFIG - позволяет задать локальную конфигурацию для всех проектов разом на уровне окружения,
 * при этом сохраняется приоритет индвидуальной конфигурации над конфигурацией окружения ОС.
 */
@Slf4j
public class ConfigLoader {

    public static Config load() {
        String localfileSbusConfig = System.getenv("LOCALFILE_SBUS_CONFIG"); //to be able to overwrite the configuration in docker
        String configPaths = System.getProperty("config.localfile",
                localfileSbusConfig == null
                        ? "" : localfileSbusConfig)
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
