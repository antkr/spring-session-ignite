package org.apache.ignite.springsession.config.annotation.web.http;

import org.apache.ignite.springsession.IgniteSessionRepository;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.session.MapSession;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Import(IgniteHttpSessionConfiguration.class)
@Configuration(proxyBeanMethods = false)
public @interface EnableIgniteHttpSession {

    int maxInactiveIntervalInSeconds() default MapSession.DEFAULT_MAX_INACTIVE_INTERVAL_SECONDS;

    String sessionCacheName() default IgniteSessionRepository.DFLT_SESSION_STORAGE_NAME;

}
