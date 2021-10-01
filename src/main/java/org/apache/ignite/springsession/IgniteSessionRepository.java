package org.apache.ignite.springsession;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.cache.CacheAtomicityMode;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.cache.CacheWriteSynchronizationMode;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.resources.IgniteInstanceResource;
import org.springframework.session.FindByIndexNameSessionRepository;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.util.Map;

public class IgniteSessionRepository implements FindByIndexNameSessionRepository<IgniteSession> {

    public static final String DFLT_SESSION_STORAGE_NAME = "spring.session.cache";
    public static final int DEFAULT_INACTIVE_INTERVAL = 1800;

    @IgniteInstanceResource
    private Ignite ignite;

    private IgniteCache<String, IgniteSession> sessionCache;

    private String sessionCacheName = DFLT_SESSION_STORAGE_NAME;

    private Integer defaultMaxInactiveInterval = DEFAULT_INACTIVE_INTERVAL;

    public IgniteSessionRepository(Ignite ignite) {
        this.ignite = ignite;
    }

    private CacheConfiguration<String, IgniteSession> sessionCacheConfiguration() {
        CacheConfiguration<String, IgniteSession> ccfg = new CacheConfiguration<>();

        ccfg.setAtomicityMode(CacheAtomicityMode.TRANSACTIONAL);
        ccfg.setWriteSynchronizationMode(CacheWriteSynchronizationMode.FULL_SYNC);
        ccfg.setCacheMode(CacheMode.REPLICATED);
        ccfg.setName(sessionCacheName);

        return ccfg;
    }

    @PostConstruct
    public void init() {
        this.sessionCache = this.ignite.getOrCreateCache(this.sessionCacheConfiguration());
    }

    @Override
    public Map<String, IgniteSession> findByIndexNameAndIndexValue(String s, String s1) {
        return null;
    }

    @Override
    public IgniteSession createSession() {
        IgniteSession session = new IgniteSession();

        if (this.defaultMaxInactiveInterval != null)
            session.setMaxInactiveInterval(Duration.ofSeconds(this.defaultMaxInactiveInterval));

        return session;
    }

    @Override
    public void save(IgniteSession igniteSession) {
        if(igniteSession.sessionIdChanged())
            this.sessionCache.remove(igniteSession.getOriginalSessionId());

        this.sessionCache.put(igniteSession.getId(), igniteSession);
    }

    @Override
    public IgniteSession findById(String key) {
        IgniteSession storedSession = this.sessionCache.get(key);

        if (storedSession == null)
            return null;

        if (storedSession.isExpired()) {
            deleteById(key);

            return null;
        }

        return storedSession;
    }

    @Override
    public void deleteById(String key) {
        this.sessionCache.remove(key);
    }

    public void setSessionCacheName(String name) {
        this.sessionCacheName = name;
    }

    public void setDefaultMaxInactiveInterval(Integer interval) {
        this.defaultMaxInactiveInterval = interval;
    }
}
