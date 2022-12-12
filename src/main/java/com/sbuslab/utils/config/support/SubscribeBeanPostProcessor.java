package com.sbuslab.utils.config.support;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import scala.compat.java8.FutureConverters;
import scala.concurrent.Future;
import scala.concurrent.duration.FiniteDuration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotatedElementUtils;

import static java.util.Optional.of;

import com.sbuslab.model.BadRequestError;
import com.sbuslab.model.ErrorMessage;
import com.sbuslab.model.Message;
import com.sbuslab.model.scheduler.ScheduleCommand;
import com.sbuslab.sbus.Context;
import com.sbuslab.sbus.auth.AuthProvider;
import com.sbuslab.sbus.javadsl.Sbus;
import com.sbuslab.utils.Schedule;
import com.sbuslab.utils.Subscribe;
import com.sbuslab.utils.config.ConfigLoader;


public class SubscribeBeanPostProcessor implements BeanPostProcessor, Ordered {

    private final Sbus sbus;
    private final ObjectMapper mapper;
    private final AuthProvider authProvider;
    private final Validator validator;

    private final String routeBase;

    protected final Log logger = LogFactory.getLog(getClass());

    public SubscribeBeanPostProcessor(Sbus sbus, ObjectMapper mapper, AuthProvider authProvider) {
        this.sbus = sbus;
        this.mapper = mapper;
        this.authProvider = authProvider;
        this.routeBase = of(ConfigLoader.INSTANCE)
            .filter(f -> f.hasPath("sbus.route-base"))
            .map(m -> m.getString("sbus.route-base")).orElse(null);

        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        Class<?> targetClass = AopProxyUtils.ultimateTargetClass(bean);

        Map<Method, Subscribe> annotatedMethods = MethodIntrospector.selectMethods(targetClass,
            (MethodIntrospector.MetadataLookup<Subscribe>) method -> AnnotatedElementUtils.getMergedAnnotation(
                method, Subscribe.class));

        if (!annotatedMethods.isEmpty()) {
            annotatedMethods.forEach((method, subscribeAnnotation) ->
                processSubscribe(subscribeAnnotation, method, bean));

            if (logger.isTraceEnabled()) {
                logger.trace(annotatedMethods.size() + " @Subscribe methods processed on bean '" + beanName +
                             "': " + annotatedMethods);
            }
        }

        return bean;
    }

    private void processSubscribe(Subscribe subscribe, Method method, Object bean) {
        boolean featured = CompletableFuture.class.isAssignableFrom(method.getReturnType());
        boolean scalaFeatured = !featured && Future.class.isAssignableFrom(method.getReturnType());

        if (method.getParameterCount() != 2 || !Context.class.isAssignableFrom(method.getParameterTypes()[1])) {
            throw new RuntimeException("Method with @Subscribe must have second argument Context! " + method);
        }

        String[] routingKeys = subscribe.values().length > 0 ? subscribe.values() : new String[]{subscribe.value()};

        for (String routingKey : routingKeys) {
            String enrichedRoutingKey = routingKey;
            if (!enrichedRoutingKey.contains(".")) {
                if (routeBase == null) {
                  throw new RuntimeException(MessageFormat.format("Cannot enrich route: {0} because the sbus.default-route property is not set in application.properties", routingKey));
                }
                enrichedRoutingKey = MessageFormat.format("{0}.{1}", routeBase, routingKey);
            }
            sbus.on(enrichedRoutingKey, method.getParameterTypes()[0], (req, ctx) -> {
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

                        logger.error("Sbus validation error: " + ex.getMessage(), ex);

                        throw ex;
                    }
                }

                if (featured || scalaFeatured) {
                    try {
                        if (scalaFeatured) {
                            return FutureConverters
                                .toJava((Future<?>) method.invoke(bean, req, ctx))
                                .toCompletableFuture();
                        } else {
                            return (CompletableFuture<?>) method.invoke(bean, req, ctx);
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
                        return method.invoke(bean, req, ctx);
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
                try {
                    Schedule schedule = AnnotatedElementUtils.getMergedAnnotation(method, Schedule.class);

                    String[] split = enrichedRoutingKey.split(":", 2);
                    String realRoutingKey = split[split.length - 1];

                    byte[] cmd = mapper.writeValueAsBytes(new Message(realRoutingKey, null));

                    Context signedContext = authProvider.signCommand(Context.empty().withRoutingKey(realRoutingKey), cmd);

                    sbus.command("scheduler.schedule", ScheduleCommand.builder()
                        .period(FiniteDuration.apply(schedule.value()).toMillis())
                        .routingKey(enrichedRoutingKey)
                        .origin(signedContext.origin())
                        .signature(signedContext.signature())
                        .build());
                } catch (JsonProcessingException e) {
                    Throwable cause = e.getCause();

                    if (cause instanceof ErrorMessage) {
                        throw (ErrorMessage) e.getCause();
                    } else {
                        throw new RuntimeException(cause != null ? cause : e);
                    }
                }
            }
        }
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
