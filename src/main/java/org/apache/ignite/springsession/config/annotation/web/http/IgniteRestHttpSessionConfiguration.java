package org.apache.ignite.springsession.config.annotation.web.http;

import java.util.Map;
import org.apache.ignite.Ignite;
import org.apache.ignite.springsession.IgniteRestSessionRepository;
import org.apache.ignite.springsession.IgniteSessionRepository;
import org.apache.ignite.springsession.config.annotation.SpringSessionIgniteInstance;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportAware;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.session.MapSession;
import org.springframework.session.config.annotation.web.http.SpringHttpSessionConfiguration;
import org.springframework.util.StringUtils;

@Configuration
public class IgniteRestHttpSessionConfiguration extends SpringHttpSessionConfiguration implements ImportAware {

    private static String sessionCacheName = IgniteRestSessionRepository.DFLT_SESSION_STORAGE_NAME;

    private static Integer maxInactiveIntervalInSeconds = MapSession.DEFAULT_MAX_INACTIVE_INTERVAL_SECONDS;

    private static String igniteAddress = IgniteRestSessionRepository.DFLT_IGNITE_ADDRESS;

    private static String ignitePort = IgniteRestSessionRepository.DFLT_IGNITE_PORT;

    @Bean
    public IgniteRestSessionRepository repository(/*@Value("$(ignite.ip.url)") final String ip, @Value("$(ignite.port") final String port*/) {
        IgniteRestSessionRepository repository = new IgniteRestSessionRepository(igniteAddress, ignitePort);
        repository.setSessionCacheName(sessionCacheName);
        repository.setDefaultMaxInactiveInterval(maxInactiveIntervalInSeconds);
        return repository;
    }

    @Override
    public void setImportMetadata(AnnotationMetadata importMetadata) {
        Map<String, Object> attributeMap = importMetadata
            .getAnnotationAttributes(EnableRestIgniteHttpSession.class.getName());
        AnnotationAttributes attributes = AnnotationAttributes.fromMap(attributeMap);
        this.maxInactiveIntervalInSeconds =
            attributes.getNumber("maxInactiveIntervalInSeconds");
        String sessionCacheNameValue = attributes.getString("sessionCacheName");
        if (StringUtils.hasText(sessionCacheNameValue)) {
            this.sessionCacheName = sessionCacheNameValue;
        }
        String igniteAddr = attributes.getString("igniteAddress");
        if (StringUtils.hasText(igniteAddr)) {
            this.igniteAddress = igniteAddr;
        }
        String ignitePort = attributes.getString("ignitePort");
        if (StringUtils.hasText(ignitePort)) {
            this.ignitePort = ignitePort;
        }
    }
}
