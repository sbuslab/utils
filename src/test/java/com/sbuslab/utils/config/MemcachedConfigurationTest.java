package com.sbuslab.utils.config;

import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.util.List;
import java.util.Map;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import net.spy.memcached.MemcachedClient;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.junit4.SpringRunner;

@SpringBootTest(classes = { MemcachedConfigurationTest.TestConfig.class, MemcachedConfiguration.class })
@RunWith(SpringRunner.class)
public class MemcachedConfigurationTest {

    @TestConfiguration
    public static class TestConfig {

        @Bean
        @Primary
        public Config config() {
            return ConfigFactory.parseMap(
                    Map.of("sbuslab", Map.of(
                            "memcache", Map.of(
                                    "timeout", 1000000,
                                    "max-reconnect-delay", 1000000,
                                    "hosts", List.of("1.2.3.4:11211")))));
        }
    }

    @Autowired
    private MemcachedClient memcachedClient;

    @Test
    public void doesNotConnectToMemcachedWhenCachingDisabled() {
        assumeTrue(DefaultConfiguration.DISABLED_MEMOIZE_CACHE);
        assertTrue(memcachedClient.getUnavailableServers().isEmpty());
    }

}
