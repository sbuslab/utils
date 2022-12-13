package com.sbuslab.utils.config;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.Map;
import scala.concurrent.ExecutionContext;

import akka.actor.ActorSystem;
import ch.qos.logback.classic.Level;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.typesafe.config.Config;
import io.lettuce.core.RedisURI;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.prometheus.client.exporter.HTTPServer;
import io.prometheus.client.hotspot.DefaultExports;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;
import org.asynchttpclient.Dsl;
import org.asynchttpclient.proxy.ProxyServer;
import org.asynchttpclient.proxy.ProxyType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.EmbeddedValueResolverAware;
import org.springframework.context.annotation.*;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.util.StringValueResolver;

import com.sbuslab.sbus.Transport;
import com.sbuslab.sbus.TransportDispatcher;
import com.sbuslab.sbus.auth.AuthProvider;
import com.sbuslab.sbus.auth.AuthProviderImpl;
import com.sbuslab.sbus.auth.DynamicAuthConfigProvider;
import com.sbuslab.sbus.auth.NoopAuthProvider;
import com.sbuslab.sbus.auth.providers.ConsulAuthConfigProvider;
import com.sbuslab.sbus.auth.providers.NoopDynamicProvider;
import com.sbuslab.sbus.javadsl.Sbus;
import com.sbuslab.sbus.kafka.KafkaTransport;
import com.sbuslab.sbus.rabbitmq.RabbitMqTransport;
import com.sbuslab.utils.config.logger.LoggerConfigurationParser;
import com.sbuslab.utils.config.support.SubscribeBeanPostProcessor;
import com.sbuslab.utils.json.JsonMapperFactory;


@ComponentScan("com.sbuslab")
@Import(MemcachedConfiguration.class)
@EnableAspectJAutoProxy
public abstract class DefaultConfiguration implements ApplicationContextAware, EmbeddedValueResolverAware {

    public static final boolean DISABLED_MEMOIZE_CACHE = "true".equals(System.getenv("DISABLED_MEMOIZE_CACHE"));
    protected static final Logger log = LoggerFactory.getLogger(DefaultConfiguration.class);
    private static ApplicationContext context;

    private StringValueResolver resolver;

    @Bean(name = "config")
    public Config getConfigBean() {
        return ConfigLoader.INSTANCE;
    }

    public Config getConfig() {
        ApplicationContext ctx = context;
        if (ctx == null) {
            throw new IllegalStateException("Context was not set");
        }
        return ctx.getBean(Config.class);
    }

    @Bean
    public ObjectMapper getObjectMapper() {
        return JsonMapperFactory.mapper;
    }

    @EventListener({ContextRefreshedEvent.class})
    public void initPrometheusExporter(ContextRefreshedEvent event) {
        Config baseConfig = event.getApplicationContext().getBean(Config.class);
        Config conf = baseConfig.getConfig("prometheus.exporter");
        if (conf.getBoolean("enabled")) {
            log.info("Start prometheus HTTPServer on {}", conf.getInt("port"));

            DefaultExports.initialize();

            try {
                new HTTPServer(conf.getInt("port"));
            } catch (IOException e) {
                log.error("Error on start prometheus HTTPServer: " + e.getMessage(), e);
            }
        }
    }

    @EventListener({ContextRefreshedEvent.class})
    public void reconfigureLoggers(ContextRefreshedEvent event) {
        reconfigureLoggers(event.getApplicationContext().getBean(Config.class));
    }

    @PostConstruct
    public void init() {
        reconfigureLoggers(getConfig());
    }

    @Bean
    @Lazy
    public static AsyncHttpClient getAsyncHttpClient(Config config) {
        Config conf = config.getConfig("sbuslab.http-client");

        DefaultAsyncHttpClientConfig.Builder bldr = Dsl.config()
            .setUserAgent(conf.getString("user-agent"))
            .setMaxConnections(conf.getInt("max-connections"))
            .setMaxConnectionsPerHost(conf.getInt("max-connections-per-host"))
            .setConnectTimeout(conf.getInt("connect-timeout"))
            .setRequestTimeout(conf.getInt("request-timeout"))
            .setReadTimeout(conf.getInt("read-timeout"))
            .setHttpClientCodecMaxHeaderSize(16384)
            .setCookieStore(null) // don't save cookies between requests
            .setFollowRedirect(conf.getBoolean("follow-redirect"));

        if (!conf.getString("proxy.host").isEmpty()) {
            bldr.setProxyServer(
                new ProxyServer.Builder(conf.getString("proxy.host"), conf.getInt("proxy.port"))
                    .setProxyType(ProxyType.HTTP)
                    .setNonProxyHosts(conf.getStringList("proxy.non-proxy-hosts"))
                    .build()
            );
        }

        return Dsl.asyncHttpClient(bldr);
    }

    @Bean
    @Lazy
    public static StatefulRedisClusterConnection<String, String> getRedisClient(Config config) {
        Config conf = config.getConfig("sbuslab.redis");

        RedisURI redisURI = RedisURI.Builder.redis(conf.getString("host"))
            .withPort(conf.getInt("port"))
            .withSsl(conf.getBoolean("ssl"))
            .withVerifyPeer(false)
            .build();

        final String password = conf.getString("password");
        if (!password.isEmpty()) {
            redisURI.setPassword(password.toCharArray());
        }

        final String user = conf.getString("user");
        if (!user.isEmpty()) {
            redisURI.setUsername(user);
        }

        RedisClusterClient clusterClient = RedisClusterClient.create(redisURI);
        return clusterClient.connect();
    }

    @Bean
    @Lazy
    public DynamicAuthConfigProvider dynamicAuthConfigProvider(Config config) {
        return config.getBoolean("sbus.auth.consul.enabled")
            ? new ConsulAuthConfigProvider(config.getConfig("sbus.auth.consul"))
            : new NoopDynamicProvider();
    }

    @Bean
    @Lazy
    public AuthProvider authProvider(Config config, DynamicAuthConfigProvider dynamicProvider) {
        return config.getBoolean("sbus.auth.enabled")
               && !config.getString("sbus.auth.name").isBlank()
               && !config.getString("sbus.auth.private-key").isBlank()
            ? new AuthProviderImpl(config.getConfig("sbus.auth"), dynamicProvider)
            : new NoopAuthProvider();
    }

    @Bean
    @Lazy
    public Transport getSbusTransport(Config config, ObjectMapper mapper, AuthProvider authProvider) {
        ActorSystem actorSystem = ActorSystem.create("sbus", config);

        return new TransportDispatcher(
            config.getConfig("sbus.transports.dispatcher"),
            Map.of(
                "rabbitmq", new RabbitMqTransport(
                    config.getConfig("sbus.transports.rabbitmq"),
                    authProvider,
                    actorSystem,
                    mapper
                ),

                "kafka", new KafkaTransport(
                    config.getConfig("sbus.transports.kafka"),
                    actorSystem,
                    mapper
                )
            )
        );
    }

    @Bean
    @Lazy
    public Sbus getJavaSbus(Transport transport, AuthProvider authProvider, ObjectMapper objectMapper) {
        return new Sbus(transport, authProvider, objectMapper);
    }

    @Bean
    @Lazy
    public com.sbuslab.sbus.Sbus getScalaSbus(Transport transport, AuthProvider authProvider, ExecutionContext ec, ObjectMapper objectMapper) {
        return new com.sbuslab.sbus.Sbus(transport, authProvider, objectMapper, ec);
    }

    @Bean
    public PropertySource<Config> propertySource(Config config) {
        return new ConfigPropertySource("scalaConfig", config);
    }

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer(PropertySource<Config> propertySource) {
        PropertySourcesPlaceholderConfigurer ps = new PropertySourcesPlaceholderConfigurer();
        MutablePropertySources sources = new MutablePropertySources();
        sources.addFirst(propertySource);
        ps.setPropertySources(sources);
        return ps;
    }

    @Bean
    public SubscribeBeanPostProcessor getSubscribeBeanPostProcessor(Sbus sbus, ObjectMapper objectMapper, AuthProvider authProvider) {
        return new SubscribeBeanPostProcessor(sbus, objectMapper, authProvider, resolver);
    }

    @Override
    public void setApplicationContext(ApplicationContext ac) throws BeansException {
        context = ac;
    }

    private void reconfigureLoggers(Config config) {
        LoggerConfigurationParser.parseLoggersConfiguration(config).forEach(loggerConfigData -> {
            Logger logger = LoggerFactory.getLogger(loggerConfigData.getLoggerName());
            if (logger instanceof ch.qos.logback.classic.Logger) {
                ((ch.qos.logback.classic.Logger) logger).setLevel(Level.toLevel(loggerConfigData.getLogLevel(), Level.INFO));
            }
        });
    }

    @Override
    public void setEmbeddedValueResolver(StringValueResolver resolver) {
        this.resolver = resolver;
    }
}
