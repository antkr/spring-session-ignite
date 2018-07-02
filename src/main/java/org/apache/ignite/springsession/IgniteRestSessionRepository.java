package org.apache.ignite.springsession;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.PostConstruct;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.ignite.internal.util.typedef.internal.SB;
import org.springframework.session.SessionRepository;
import org.springframework.stereotype.Component;

@Component
public class IgniteRestSessionRepository implements SessionRepository<IgniteSession> {

    public final static String DFLT_IGNITE_ADDRESS = "localhost";

    public final static String DFLT_IGNITE_PORT = "8080";

    public static final String DFLT_SESSION_STORAGE_NAME = "spring.session.cache";

    private final static String CMD = "cmd";

    private final static String GET = "get";

    private final static String PUT = "put";

    private final static String DELETE = "rmv";

    private final static String CREATE_CACHE = "getOrCreate";

    private static final String CACHE_NAME = "cacheName";

    private static final String KEY = "key";

    private static final String VALUE = "val";

    private static final String CACHE_TEMPLATE = "templateName";

    private static final String REPLICATED = "REPLICATED";

    private static final String PARTITIONED = "PARTITIONED";

    private static String ip;

    private static String port;

    private String sessionCacheName;

    private Integer defaultMaxInactiveInterval;

    public IgniteRestSessionRepository(String ip, String port) {
        this.ip = ip;
        this.port = port;
    }

    @PostConstruct
    public void init() {
        CloseableHttpClient client = getHttpClient(this.ip, this.port);

        HttpResponse res;

        try {
            Map<String, String> ss = new HashMap<String, String>();
            ss.put(CMD, CREATE_CACHE);
            ss.put(CACHE_NAME, sessionCacheName);
            ss.put(CACHE_TEMPLATE, REPLICATED);
            res = client.execute(buildCacheRequest("localhost", "8080", ss));

            try {
                assert res.getStatusLine().getStatusCode() == 200;
            }
            finally {
                ((CloseableHttpResponse)res).close();
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            try {
                client.close();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private CloseableHttpClient getHttpClient(String ip, String port) {
        CloseableHttpClient client = HttpClients.custom().build();
        return client;
    }

    private HttpGet buildCacheRequest(String host, String port, Map<String, String> params) throws Exception {
        SB sb = new SB("http://" + host + ":" + port + "/ignite?");

        for (Map.Entry<String, String> e : params.entrySet())
            sb.a(e.getKey()).a('=').a(e.getValue()).a('&');

        sb.d(sb.length() - 1, sb.length());
        URL url = new URL(sb.toString());

        return new HttpGet(url.toURI());
    }

    public void setSessionCacheName(String sessionCacheName) {
        this.sessionCacheName = sessionCacheName;
    }

    @Override public IgniteSession createSession() {
        IgniteSession session = new IgniteSession();

        return session;
    }

    @Override public void save(IgniteSession session) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE);
        mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);

        CloseableHttpClient client = getHttpClient(this.ip, this.port);

        HttpResponse res;

        try {
            Map<String, String> ss = new HashMap<String, String>();
            ss.put(CMD, PUT);
            ss.put(CACHE_NAME, sessionCacheName);
            ss.put(KEY, session.getId());
            ss.put(VALUE, URLEncoder.encode(mapper.writeValueAsString(session), "UTF-8"));
            res = client.execute(buildCacheRequest(this.ip, this.port, ss));

            try {
                assert res.getStatusLine().getStatusCode() == 200;
            }
            finally {
                ((CloseableHttpResponse)res).close();
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            try {
                client.close();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override public IgniteSession getSession(String id) {

        CloseableHttpClient client = getHttpClient(this.ip, this.port);

        Map<String, String> ss = new HashMap<String, String>();
        ss.put(CMD, GET);
        ss.put(CACHE_NAME, sessionCacheName);
        ss.put(KEY, id);

        HttpResponse res;

        IgniteSession session = null;
        try {
            res = client.execute(buildCacheRequest("localhost", "8080", ss));
            try {
                assert res.getStatusLine().getStatusCode() == 200;

                InputStream is = null;

                try {
                    is = res.getEntity().getContent();
                    ObjectMapper mapper = new ObjectMapper();
                    mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE);
                    mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
                    JsonNode node = mapper.readTree(is).get("response");
                    session = mapper.readValue(node.asText(), IgniteSession.class);
                }
                finally {
                    if (is != null )
                        is.close();
                }
            }
            finally {
                ((CloseableHttpResponse)res).close();
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        if (session == null) {
            return null;
        }
        if (session.isExpired()) {
            delete(id);
            return null;
        }

        return session;
    }

    @Override public void delete(String id) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE);
        mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);

        CloseableHttpClient client = getHttpClient(this.ip, this.port);

        HttpResponse res;

        try {
            Map<String, String> ss = new HashMap<String, String>();
            ss.put(CMD, DELETE);
            ss.put(CACHE_NAME, sessionCacheName);
            ss.put(KEY, id);
            res = client.execute(buildCacheRequest("localhost", "8080", ss));

            try {
                assert res.getStatusLine().getStatusCode() == 200;
            }
            finally {
                ((CloseableHttpResponse)res).close();
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            try {
                client.close();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void setDefaultMaxInactiveInterval(Integer dfltMaxInactiveInterval) {
        defaultMaxInactiveInterval = dfltMaxInactiveInterval;
    }
}
