package org.apache.ignite.springsession.config.annotation.web.http;

import org.apache.ignite.springsession.IgniteRestSessionRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportAware;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.session.MapSession;
import org.springframework.session.config.annotation.web.http.SpringHttpSessionConfiguration;
import org.springframework.util.StringUtils;

import java.util.Map;

@Configuration
public class IgniteRestHttpSessionConfiguration extends SpringHttpSessionConfiguration implements ImportAware {

    private String sessionCacheName = IgniteRestSessionRepository.DFLT_SESSION_STORAGE_NAME;

    private Integer maxInactiveIntervalInSeconds = MapSession.DEFAULT_MAX_INACTIVE_INTERVAL_SECONDS;

    private String url;

    @Bean
    public IgniteRestSessionRepository repository() {
        IgniteRestSessionRepository repository = new IgniteRestSessionRepository();
        repository.setUrl(this.url);
        repository.setSessionCacheName(this.sessionCacheName);
        repository.setDefaultMaxInactiveInterval(maxInactiveIntervalInSeconds);
        return repository;
    }

    public void setSessionCacheName(String sessionCacheName) {
        this.sessionCacheName = sessionCacheName;
    }

    public void setMaxInactiveIntervalInSeconds(Integer maxInactiveIntervalInSeconds) {
        this.maxInactiveIntervalInSeconds = maxInactiveIntervalInSeconds;
    }

    public void setUrl(String url) {
        this.url = url;
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
        this.url = attributes.getString("url");
    }
}
