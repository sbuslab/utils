package com.sbuslab.common.config;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URISyntaxException;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import lombok.extern.slf4j.Slf4j;

import com.sbuslab.common.FileUtils;


@Slf4j
public class ConfigLoader {

    public static Config load() {
        String configPaths = System.getProperty("config.localfile", "")
            .replace("/", File.separator)
            .replace("\\", File.separator);

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
