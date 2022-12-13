package com.sbuslab.utils.config;

import com.typesafe.config.Config;
import org.springframework.core.env.PropertySource;

public class ConfigPropertySource extends PropertySource<Config> {

    private final Config config;

    public ConfigPropertySource(String name, Config config) {
        super(name, config);
        this.config = config;
    }

    public Object getProperty(String name) {
        if (config.hasPath(name))
            return config.getAnyRef(name);
        else
            return null;
    }
}
