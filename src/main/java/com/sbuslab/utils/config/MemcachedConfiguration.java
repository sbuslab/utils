package com.sbuslab.utils.config;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.typesafe.config.Config;
import net.spy.memcached.AddrUtil;
import net.spy.memcached.ConnectionFactory;
import net.spy.memcached.ConnectionFactoryBuilder;
import net.spy.memcached.DefaultHashAlgorithm;
import net.spy.memcached.FailureMode;
import net.spy.memcached.MemcachedClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import com.sbuslab.utils.config.memcached.NoopConnectionFactory;


@Configuration
public abstract class MemcachedConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(MemcachedConfiguration.class);

    @Bean
    @Lazy
    public MemcachedClient getMemcachedClient(Config config) throws IOException {
        final Config memcacheConfig = config.getConfig("sbuslab.memcache");
        final ConnectionFactory cf;
        final List<String> hosts;
        if (DefaultConfiguration.DISABLED_MEMOIZE_CACHE) {
            LOG.info("Caching is disabled.");
            cf = new NoopConnectionFactory();
            hosts = Collections.singletonList("127.0.0.1:11211");
        } else {
            LOG.info("Caching is enabled.");
            cf = new ConnectionFactoryBuilder()
                    .setDaemon(true)
                    .setShouldOptimize(true)
                    .setFailureMode(FailureMode.Redistribute)
                    .setHashAlg(DefaultHashAlgorithm.KETAMA_HASH)
                    .setLocatorType(ConnectionFactoryBuilder.Locator.CONSISTENT)
                    .setOpTimeout(memcacheConfig.getDuration("timeout", TimeUnit.MILLISECONDS))
                    .setMaxReconnectDelay(memcacheConfig.getDuration("max-reconnect-delay", TimeUnit.SECONDS))
                    .setProtocol(ConnectionFactoryBuilder.Protocol.BINARY)
                    .build();
            hosts = memcacheConfig.getStringList("hosts");
        }

        return new MemcachedClient(cf, AddrUtil.getAddresses(hosts));
    }

}
