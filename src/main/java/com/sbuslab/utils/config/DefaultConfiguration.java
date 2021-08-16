package com.sbuslab.utils.config;

import javax.annotation.PostConstruct;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import scala.compat.java8.FutureConverters;
import scala.concurrent.ExecutionContext;
import scala.concurrent.Future;

import akka.actor.ActorSystem;
import ch.qos.logback.classic.Level;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.typesafe.config.Config;
import io.prometheus.client.exporter.HTTPServer;
import io.prometheus.client.hotspot.DefaultExports;
import net.spy.memcached.*;
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
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

import com.sbuslab.model.BadRequestError;
import com.sbuslab.model.ErrorMessage;
import com.sbuslab.model.scheduler.ScheduleCommand;
import com.sbuslab.sbus.Context;
import com.sbuslab.sbus.Transport;
import com.sbuslab.sbus.TransportDispatcher;
import com.sbuslab.sbus.javadsl.Sbus;
import com.sbuslab.sbus.kafka.KafkaTransport;
import com.sbuslab.sbus.rabbitmq.RabbitMqTransport;
import com.sbuslab.utils.Schedule;
import com.sbuslab.utils.Subscribe;
import com.sbuslab.utils.json.JsonMapperFactory;


@ComponentScan("com.sbuslab")
@EnableAspectJAutoProxy
public abstract class DefaultConfiguration {

    protected static final org.slf4j.Logger log = LoggerFactory.getLogger(DefaultConfiguration.class);

    private final Config config = ConfigLoader.load();

    @Bean
    public Config getConfig() {
        return config;
    }

    @Bean
    public ObjectMapper getObjectMapper() {
        return JsonMapperFactory.mapper;
    }

    @EventListener({ContextRefreshedEvent.class})
    public void initPrometheusExporter() {
        Config conf = getConfig().getConfig("prometheus.exporter");

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

    @PostConstruct
    public void reconfigureLoggers() {
        getConfig().getObject("sbuslab.loggers").forEach((key, value) -> {
            org.slf4j.Logger logger = LoggerFactory.getLogger(key);

            if (logger instanceof ch.qos.logback.classic.Logger) {
                ((ch.qos.logback.classic.Logger) logger).setLevel(Level.toLevel(value.atPath("/").getString("/"), Level.INFO));
            }
        });
    }

    @Bean
    @Lazy
    @Autowired
    public AsyncHttpClient getAsyncHttpClient(Config config) {
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
            bldr.setProxyServer(new ProxyServer.Builder(conf.getString("proxy.host"), conf.getInt("proxy.port"))
                .setProxyType(ProxyType.HTTP));
        }

        return Dsl.asyncHttpClient(bldr);
    }

    @Bean
    @Lazy
    @Autowired
    public MemcachedClient getMemcachedClient(Config config) throws IOException {
        Config conf = config.getConfig("sbuslab.memcache");

        ConnectionFactoryBuilder builder = new ConnectionFactoryBuilder()
            .setDaemon(true)
            .setShouldOptimize(true)
            .setFailureMode(FailureMode.Redistribute)
            .setHashAlg(DefaultHashAlgorithm.KETAMA_HASH)
            .setLocatorType(ConnectionFactoryBuilder.Locator.CONSISTENT)
            .setOpTimeout(conf.getDuration("timeout", TimeUnit.MILLISECONDS))
            .setMaxReconnectDelay(conf.getDuration("max-reconnect-delay", TimeUnit.SECONDS))
            .setProtocol(ConnectionFactoryBuilder.Protocol.BINARY);

        return new MemcachedClient(builder.build(), AddrUtil.getAddresses(conf.getStringList("hosts")));
    }

    @Bean
    @Lazy
    @Autowired
    public Transport getSbusTransport(Config config, ObjectMapper mapper) {
        ActorSystem actorSystem = ActorSystem.create("sbus", config);

        return new TransportDispatcher(
            config.getConfig("sbus.transports.dispatcher"),
            Map.of(
                "rabbitmq", new RabbitMqTransport(
                    config.getConfig("sbus.transports.rabbitmq"),
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
    @Autowired
    public Sbus getJavaSbus(Transport transport) {
        return new Sbus(transport);
    }

    @Bean
    @Lazy
    @Autowired
    public com.sbuslab.sbus.Sbus getScalaSbus(Transport transport, ExecutionContext ec) {
        return new com.sbuslab.sbus.Sbus(transport, ec);
    }

    @Bean
    @Autowired
    public Reflections initSbusSubscriptions(ApplicationContext appContext, Config config) {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

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

            Sbus sbus     = appContext.getBean(Sbus.class);
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
                        } catch (ArrayIndexOutOfBoundsException ignored) {}

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

                    sbus.command("scheduler.schedule", ScheduleCommand.builder()
                      .period(scala.concurrent.duration.FiniteDuration.apply(schedule.value()).toMillis())
                      .routingKey(routingKey)
                      .build());
                }
            }
        });

        return reflections;
    }
}
