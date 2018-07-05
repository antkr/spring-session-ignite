/*
 * Copyright 2014-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ignite.springsession;

import javax.annotation.PostConstruct;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.resources.IgniteInstanceResource;
import org.springframework.session.SessionRepository;
import org.springframework.stereotype.Component;

@Component
public class IgniteSessionRepository implements SessionRepository<IgniteSession> {
	public static final String DFLT_SESSION_STORAGE_NAME = "spring.session.cache";

	private Long defaultActiveTimeout;

	private IgniteCache<String, IgniteSession> sessionCache;

	@IgniteInstanceResource
	private Ignite ignite;

	private String sessionCacheName;

	private Integer defaultMaxInactiveInterval;

	public IgniteSessionRepository(Ignite ignite) {
		this.ignite = ignite;
	}

	private CacheConfiguration<String, IgniteSession> getSessionCacheConfig() {
		CacheConfiguration<String, IgniteSession> sesCacheCfg = new CacheConfiguration<String, IgniteSession>();

		sesCacheCfg.setName(this.sessionCacheName);
		sesCacheCfg.setCacheMode(CacheMode.REPLICATED);

		return sesCacheCfg;
	}

	@PostConstruct
	public void init() {
		this.sessionCache = this.ignite.getOrCreateCache(this.getSessionCacheConfig());
	}

	@Override
	public IgniteSession createSession() {
		IgniteSession session = new IgniteSession();

		return session;
	}

	@Override
	public void save(final IgniteSession session) {
		if (!session.getId().equals(session.getOriginalId())) {
			delete(session.getOriginalId());
			session.setOriginalId(session.getId());
		}
		this.sessionCache.put(session.getId(), session);
	}

	@Override
	public IgniteSession getSession(String id) {
		IgniteSession session = this.sessionCache.get(id);
		if (session == null) {
			return null;
		}
		if (session.isExpired()) {
			delete(id);
			return null;
		}
		return session;
	}

	@Override
	public void delete(String id) {
		this.sessionCache.remove(id);
	}

	public void setSessionCacheName(String name) {
		this.sessionCacheName = name;
	}

	public void setDefaultMaxInactiveInterval(Integer interval) {
		this.defaultMaxInactiveInterval = interval;
	}
}