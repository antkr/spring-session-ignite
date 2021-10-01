package org.apache.ignite.springsession;

import org.springframework.session.Session;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

import static org.apache.ignite.springsession.IgniteSessionRepository.DEFAULT_INACTIVE_INTERVAL;

public class IgniteSession implements Session {

    private String id;
    private String originalId;
    private long createdMs = System.currentTimeMillis();
    private long accessedMs;
    private long intervalSeconds;
    private Date expireAt;
    private Map<String, Object> attributes = new HashMap<>();

    public IgniteSession() {
        this(DEFAULT_INACTIVE_INTERVAL);
    }

    public IgniteSession(long maxInactiveIntervalInSeconds) {
        this(UUID.randomUUID().toString(), maxInactiveIntervalInSeconds);
    }

    public IgniteSession(String id, long maxInactiveIntervalInSeconds) {
        this.id = id;
        this.originalId = id;
        this.intervalSeconds = maxInactiveIntervalInSeconds;
        setLastAccessedTime(Instant.ofEpochMilli(this.createdMs));
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public String changeSessionId() {
        String sessionId = UUID.randomUUID().toString();
        this.id = sessionId;
        return sessionId;
    }

    @Override
    public <T> T getAttribute(String s) {
        return (T) attributes.get(s);
    }

    @Override
    public Set<String> getAttributeNames() {
        return new HashSet<>(this.attributes.keySet());
    }

    @Override
    public void setAttribute(String s, Object o) {
        if (o == null)
            removeAttribute(s);
        else
            this.attributes.put(s, o);
    }

    @Override
    public void removeAttribute(String s) {
        this.attributes.remove(s);
    }

    @Override
    public Instant getCreationTime() {
        return Instant.ofEpochMilli(this.createdMs);
    }

    @Override
    public void setLastAccessedTime(Instant instant) {
        this.accessedMs = instant.toEpochMilli();
        this.expireAt = Date.from(instant.plus(Duration.ofSeconds(this.intervalSeconds)));
    }

    @Override
    public Instant getLastAccessedTime() {
        return Instant.ofEpochMilli(this.accessedMs);
    }

    @Override
    public void setMaxInactiveInterval(Duration duration) {
        long difference = duration.getSeconds() - this.intervalSeconds;

        this.intervalSeconds = duration.getSeconds();

        Instant instantToExpire = expireAt.toInstant();

        if (difference < 0)
            this.expireAt = Date.from(instantToExpire.minus(Duration.ofSeconds(Math.abs(difference))));
        else
            this.expireAt = Date.from(instantToExpire.plus(Duration.ofSeconds(Math.abs(difference))));
    }

    @Override
    public Duration getMaxInactiveInterval() {
        return Duration.ofSeconds(this.intervalSeconds);
    }

    @Override
    public boolean isExpired() {
        return this.intervalSeconds >=0 && new Date().after(this.expireAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        IgniteSession another = (IgniteSession) o;
        return Objects.equals(this.id, another.id);
    }

    String getOriginalSessionId() {
        return this.originalId;
    }

    boolean sessionIdChanged() {
        return !getId().equals(this.originalId);
    }

    public Date getExpireAt() {
        return expireAt;
    }

    public void setExpireAt(Date expireAt) {
        this.expireAt = expireAt;
    }
}
