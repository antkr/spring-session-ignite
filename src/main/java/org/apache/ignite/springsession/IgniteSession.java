package org.apache.ignite.springsession;

import java.util.Set;
import org.springframework.session.ExpiringSession;
import org.springframework.session.MapSession;
import org.springframework.util.Assert;

public final class IgniteSession implements ExpiringSession {

    private final MapSession delegate;

    private String originalId;

    IgniteSession() {
        this(new MapSession());
    }

    IgniteSession(MapSession ses) {
        Assert.notNull(ses, "Already cached session cannot be null");
        this.delegate = ses;
        this.originalId = ses.getId();
    }

    public String getId() {
        return this.delegate.getId();
    }

    @Override
    public <T> T getAttribute(String attributeName) {
        return this.delegate.getAttribute(attributeName);
    }

    @Override
    public Set<String> getAttributeNames() {
        return this.delegate.getAttributeNames();
    }

    @Override
    public void setAttribute(String attributeName, Object attributeValue) {
        this.delegate.setAttribute(attributeName, attributeValue);
    }

    @Override
    public void removeAttribute(String attributeName) {
        this.delegate.removeAttribute(attributeName);
    }

    @Override
    public long getCreationTime() {
        return this.delegate.getCreationTime();
    }

    @Override
    public void setLastAccessedTime(long lastAccessedTime) {
        this.delegate.setLastAccessedTime(lastAccessedTime);
    }

    @Override
    public long getLastAccessedTime() {
        return this.delegate.getLastAccessedTime();
    }

    @Override
    public void setMaxInactiveIntervalInSeconds(int interval) {
        this.delegate.setMaxInactiveIntervalInSeconds(interval);
    }

    @Override
    public int getMaxInactiveIntervalInSeconds() {
        return this.delegate.getMaxInactiveIntervalInSeconds();
    }

    @Override
    public boolean isExpired() {
        return this.delegate.isExpired();
    }

    public void setOriginalId(String originalId) {
        this.originalId = originalId;
    }

    public String getOriginalId() {
        return originalId;
    }
}