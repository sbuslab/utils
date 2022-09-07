package com.sbuslab.utils.config;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import scala.Option;
import scala.compat.java8.FutureConverters;
import scala.concurrent.ExecutionContext;
import scala.concurrent.Future;
import scala.concurrent.duration.FiniteDuration;

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
import org.reflections.Reflections;
import org.reflections.scanners.MethodAnnotationsScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

import com.sbuslab.model.BadRequestError;
import com.sbuslab.model.ErrorMessage;
import com.sbuslab.model.Message;
import com.sbuslab.model.scheduler.ScheduleCommand;
import com.sbuslab.sbus.Context;
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
import com.sbuslab.utils.Schedule;
import com.sbuslab.utils.Subscribe;
import com.sbuslab.utils.json.JsonMapperFactory;


@ComponentScan("com.sbuslab")
@Import(MemcachedConfiguration.class)
@EnableAspectJAutoProxy
public abstract class DefaultConfiguration implements ApplicationContextAware {

    public static final boolean DISABLED_MEMOIZE_CACHE = "true".equals(System.getenv("DISABLED_MEMOIZE_CACHE"));
    protected static final Logger log = LoggerFactory.getLogger(DefaultConfiguration.class);
    private static ApplicationContext context;

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
        Config config = event.getApplicationContext().getBean(Config.class);
        config.getObject("sbuslab.loggers").forEach((key, value) -> {
            Logger logger = LoggerFactory.getLogger(key);

            if (logger instanceof ch.qos.logback.classic.Logger) {
                ((ch.qos.logback.classic.Logger) logger).setLevel(Level.toLevel(value.atPath("/").getString("/"), Level.INFO));
            }
        });
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

        final int port = conf.hasPath("port") ? conf.getInt("port") : 6379;
        RedisURI redisURI = RedisURI.Builder.redis(conf.getString("host"))
            .withPort(port)
            .withSsl(conf.getBoolean("ssl"))
            .withVerifyPeer(false)
            .build();

        if (conf.hasPath("password")) {
            redisURI.setPassword(conf.getString("password").toCharArray());
        }
        if (conf.hasPath("user")) {
            redisURI.setUsername(conf.getString("user"));
        }

        RedisClusterClient clusterClient = RedisClusterClient.create(redisURI);
        StatefulRedisClusterConnection<String, String> connection = clusterClient.connect();
        return connection;
    }

    @Bean
    @Lazy
    public DynamicAuthConfigProvider dynamicAuthConfigProvider(Config config, ObjectMapper objectMapper) {
        return config.getBoolean("sbus.auth.consul.enabled")
            ? new ConsulAuthConfigProvider(config.getConfig("sbus.auth.consul"), objectMapper)
            : new NoopDynamicProvider();
    }

    @Bean
    @Lazy
    public AuthProvider authProvider(Config config, ObjectMapper objectMapper, DynamicAuthConfigProvider dynamicProvider) {
        return config.getBoolean("sbus.auth.enabled")
               && !config.getString("sbus.auth.name").isBlank()
               && !config.getString("sbus.auth.private-key").isBlank()
            ? new AuthProviderImpl(config.getConfig("sbus.auth"), objectMapper, dynamicProvider)
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
    public Sbus getJavaSbus(Transport transport, AuthProvider authProvider) {
        return new Sbus(transport, authProvider);
    }

    @Bean
    @Lazy
    public com.sbuslab.sbus.Sbus getScalaSbus(Transport transport, AuthProvider authProvider, ExecutionContext ec) {
        return new com.sbuslab.sbus.Sbus(transport, authProvider, ec);
    }

    @Bean
    public static Reflections initSbusSubscriptions(ApplicationContext appContext, Config config) {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();

            String packageToScan = config.getString("sbus.package-to-scan");

            Reflections reflections = new Reflections(
                new ConfigurationBuilder()
                    .setUrls(ClasspathHelper.forPackage(packageToScan))
                    .filterInputsBy(new FilterBuilder().includePackage(packageToScan))
                    .setScanners(new MethodAnnotationsScanner()));

            reflections.getMethodsAnnotatedWith(Subscribe.class).forEach(method -> {
                if (method.getDeclaringClass().isInterface()) {
                    return; // skip interfaces without implementations
                }

                Sbus sbus = appContext.getBean(Sbus.class);
                Object parent = appContext.getBean(method.getDeclaringClass());
                Subscribe ann = method.getAnnotation(Subscribe.class);

                boolean featured = CompletableFuture.class.isAssignableFrom(method.getReturnType());
                boolean scalaFeatured = !featured && Future.class.isAssignableFrom(method.getReturnType());

                if (method.getParameterCount() != 2 || !Context.class.isAssignableFrom(method.getParameterTypes()[1])) {
                    throw new RuntimeException("Method with @Subscribe must have second argument Context! " + method);
                }

                String[] routingKeys = ann.values().length > 0 ? ann.values() : new String[]{ann.value()};

                for (String routingKey : routingKeys) {
                    sbus.on(routingKey, method.getParameterTypes()[0], (req, ctx) -> {
                        if (req != null) {
                            Set<? extends ConstraintViolation<?>> errors = new HashSet<>();

                            try {
                                errors = validator.validate(req);
                            } catch (ArrayIndexOutOfBoundsException ignored) {
                            }

                            if (!errors.isEmpty()) {
                                BadRequestError ex = new BadRequestError(errors.stream().map(e ->
                                    e.getPropertyPath() + " in " + e.getRootBeanClass().getSimpleName() + " " + e.getMessage()
                                ).collect(Collectors.joining("; \n")), null, "validation-error");

                                log.error("Sbus validation error: " + ex.getMessage(), ex);

                                throw ex;
                            }
                        }

                        if (featured || scalaFeatured) {
                            try {
                                if (scalaFeatured) {
                                    return FutureConverters
                                        .toJava((Future<?>) method.invoke(parent, req, ctx))
                                        .toCompletableFuture();
                                } else {
                                    return (CompletableFuture<?>) method.invoke(parent, req, ctx);
                                }
                            } catch (IllegalAccessException | InvocationTargetException e) {
                                Throwable cause = e.getCause();

                                if (cause instanceof ErrorMessage) {
                                    throw (ErrorMessage) e.getCause();
                                } else {
                                    throw new RuntimeException(cause != null ? cause : e);
                                }
                            }
                        }

                        return CompletableFuture.supplyAsync(() -> {
                            try {
                                return method.invoke(parent, req, ctx);
                            } catch (IllegalAccessException | InvocationTargetException e) {
                                Throwable cause = e.getCause();

                                if (cause instanceof ErrorMessage) {
                                    throw (ErrorMessage) e.getCause();
                                } else {
                                    throw new RuntimeException(cause != null ? cause : e);
                                }
                            }
                        });
                    });

                    if (method.isAnnotationPresent(Schedule.class)) {
                        Schedule schedule = method.getAnnotation(Schedule.class);

                        AuthProvider authProvider = appContext.getBean(AuthProvider.class);

                        String[] split = routingKey.split(":", 2);
                        String realRoutingKey = split[split.length - 1];

                        Context signedContext = authProvider.signCommand(Context.empty().withRoutingKey(realRoutingKey), new Message(realRoutingKey, null));

                        sbus.command("scheduler.schedule", ScheduleCommand.builder()
                            .period(FiniteDuration.apply(schedule.value()).toMillis())
                            .routingKey(routingKey)
                            .origin(signedContext.origin())
                            .signature(signedContext.signature())
                            .build());
                    }
                }
            });

            return reflections;
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext ac) throws BeansException {
        context = ac;
    }

}
