package org.apache.ignite.springsession.config.annotation.web.http;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.apache.ignite.springsession.IgniteRestSessionRepository;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.session.MapSession;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Import(IgniteRestHttpSessionConfiguration.class)
@Configuration
public @interface EnableRestIgniteHttpSession {

    int maxInactiveIntervalInSeconds() default MapSession.DEFAULT_MAX_INACTIVE_INTERVAL_SECONDS;

    String sessionCacheName() default IgniteRestSessionRepository.DFLT_SESSION_STORAGE_NAME;

    String url() default IgniteRestSessionRepository.DFLT_URL;
}
