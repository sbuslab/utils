package com.sbuslab.utils.config.logger;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigObject;
import com.typesafe.config.ConfigValue;
import com.typesafe.config.ConfigValueType;
import lombok.experimental.UtilityClass;

@UtilityClass
public class LoggerConfigurationParser {

    public static Set<LoggerConfigurationData> parseLoggersConfiguration(Config config) {
        return parseLoggersConfiguration(config.getObject("sbuslab.loggers"), "")
                .collect(Collectors.toSet());
    }

    private static Stream<LoggerConfigurationData> parseLoggersConfiguration(ConfigObject config, String currentLoggerNamePrefix) {
        return config.entrySet().stream().flatMap(entry -> {
            final String key = entry.getKey();
            final String currentLoggerName = currentLoggerNamePrefix.isEmpty() ? key : currentLoggerNamePrefix + "." + key;
            final ConfigValue value = entry.getValue();
            if (value.valueType() == ConfigValueType.STRING) {
                return Stream.of(new LoggerConfigurationData(
                        currentLoggerName,
                        value.atPath("/").getString("/")));
            } else {
                return parseLoggersConfiguration(value.atPath("/").getObject("/"), currentLoggerName);
            }
        });
    }
}
