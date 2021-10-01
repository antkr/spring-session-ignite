package springsession;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.springsession.IgniteSession;
import org.apache.ignite.springsession.IgniteSessionRepository;
import org.apache.ignite.springsession.config.annotation.SpringSessionIgniteInstance;
import org.apache.ignite.springsession.config.annotation.web.http.EnableIgniteHttpSession;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.MapSession;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

import static org.apache.ignite.springsession.IgniteSessionRepository.DFLT_SESSION_STORAGE_NAME;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@WebAppConfiguration
public class IgniteClientRepositoryIntegrationTests {

	private static Ignite ignite;

	@Autowired
	private IgniteSessionRepository repository;

	@EnableIgniteHttpSession
	@Configuration
	static class SessionConfig {

		@Bean
		@SpringSessionIgniteInstance
		public Ignite igniteNode() {
			return IgniteTestUtils.getIgniteClientInstance();
		}

	}

	@BeforeClass
	public static void setup() {
		ignite = IgniteTestUtils.getIgniteServerInstance();
	}

	@AfterClass
	public static void teardown() {
		if (ignite != null) {
			ignite.close();
		}
	}

	@Test
	public void createSessionTest() {
		IgniteCache<String, MapSession> sessionCache = ignite.getOrCreateCache(DFLT_SESSION_STORAGE_NAME);

        final IgniteSession session = this.repository.createSession();

		assertThat(sessionCache.size()).isEqualTo(0);

		this.repository.save(session);

		assertThat(sessionCache.size()).isEqualTo(1);

        final IgniteSession fetched = this.repository.findById(session.getId());

		assertThat(fetched.getId()).isEqualTo(session.getId());

		this.repository.deleteById(session.getId());

		assertThat(sessionCache.size()).isEqualTo(0);
	}

	@Test
	public void saveSessionWithAttributes() {
		IgniteCache<String, MapSession> sessionCache = ignite.getOrCreateCache(DFLT_SESSION_STORAGE_NAME);

		final String attrName = "attribute";
		final String attrValue = "value";

        final IgniteSession session = this.repository.createSession();
		session.setAttribute(attrName, attrValue);

		this.repository.save(session);

        final IgniteSession fetchedSession = this.repository.findById(session.getId());
		assertThat(fetchedSession.getAttribute(attrName).toString()).isEqualTo(attrValue);

		this.repository.deleteById(session.getId());

		assertThat(sessionCache.size()).isEqualTo(0);
	}

	@Test
	public void changeSessionIdOnAttributeUpdate() {
		final IgniteCache<String, MapSession> sessionCache = ignite.getOrCreateCache(DFLT_SESSION_STORAGE_NAME);

		final String attrName = "attribute";
		final String attrValue = "value";

        final IgniteSession session = this.repository.createSession();

		this.repository.save(session);

        final IgniteSession fetchedSession = this.repository.findById(session.getId());

        assertThat(fetchedSession.getAttributeNames()).isEmpty();

		fetchedSession.setAttribute(attrName, attrValue);

		this.repository.save(fetchedSession);

        final IgniteSession changedSession = this.repository.findById(session.getId());

		assertThat(changedSession.getAttribute(attrName).toString()).isEqualTo(attrValue);

		this.repository.deleteById(session.getId());

		assertThat(sessionCache.size()).isEqualTo(0);
	}

	@Test
    public void igniteSessionsApiOperationsNotCachedTest() {

	    final IgniteSession session = this.repository.createSession();
	    final IgniteSession secondSession = this.repository.createSession();

        assertThat(session.getId()).isNotEqualTo(secondSession.getId());

	    assertThat(session.isExpired()).isFalse();

		Instant instant = Instant.ofEpochMilli(System.currentTimeMillis()).minus(Duration.ofMinutes(40));

		session.setLastAccessedTime(instant);

	    assertThat(session.isExpired()).isTrue();

	    session.setMaxInactiveInterval(Duration.ofMinutes(100));

	    assertThat(session.isExpired()).isFalse();

		session.setLastAccessedTime(Instant.ofEpochMilli(System.currentTimeMillis()).minus(Duration.ofMinutes(30)));

        assertThat(session.isExpired()).isFalse();
    }

    @Test
    public void testExpiredSessionRemovalOnGet() {
        IgniteCache<String, MapSession> sessionCache = ignite.getOrCreateCache(DFLT_SESSION_STORAGE_NAME);

	    final IgniteSession session = this.repository.createSession();

	    session.setMaxInactiveInterval(Duration.ofSeconds(30));
	    session.setLastAccessedTime(session.getLastAccessedTime().minus(Duration.ofMinutes(1)));

	    repository.save(session);

	    final IgniteSession fetchedSession = this.repository.findById(session.getId());

	    assertThat(fetchedSession).isNull();

        assertThat(sessionCache.size()).isEqualTo(0);
    }
}
