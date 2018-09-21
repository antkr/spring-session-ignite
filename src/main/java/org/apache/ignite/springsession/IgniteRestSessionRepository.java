package org.apache.ignite.springsession;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.springframework.core.ConfigurableObjectInputStream;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.core.serializer.support.SerializationFailedException;
import org.springframework.core.serializer.support.SerializingConverter;
import org.springframework.session.SessionRepository;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

@Component
public class IgniteRestSessionRepository implements SessionRepository<IgniteSession> {

    public final static String DFLT_URL = "http://localhost:8080";

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

    private String url = DFLT_URL;

    private String sessionCacheName;

    private Integer defaultMaxInactiveInterval;

    private ObjectMapper mapper;

    private ConversionService conversionService;

    public IgniteRestSessionRepository() {

    }

    public IgniteRestSessionRepository(String url) {
        this.url = url;
    }

    private static GenericConversionService createDefaultConversionService() {
        GenericConversionService converter = new GenericConversionService();
        converter.addConverter(IgniteSession.class, byte[].class,
            new SerializingConverter());
        converter.addConverter(byte[].class, IgniteSession.class,
            new IgniteSessionDeserializingConverter());
        return converter;
    }

    private String serialize(IgniteSession attributeValue) {
        byte[] value = (byte[]) this.conversionService.convert(attributeValue,
            TypeDescriptor.valueOf(IgniteSession.class),
            TypeDescriptor.valueOf(byte[].class));
        return Hex.encodeHexString(value);
    }

    private IgniteSession deserialize(String value) throws DecoderException {
        if ("null".equals(value))
            return null;
        return (IgniteSession) this.conversionService.convert(Hex.decodeHex(value.toCharArray()),
            TypeDescriptor.valueOf(byte[].class),
            TypeDescriptor.valueOf(IgniteSession.class));
    }

    @PostConstruct
    public void init() {
        this.mapper = new ObjectMapper();
        this.mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE);
        this.mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);

        this.conversionService = createDefaultConversionService();

        executeSessionCacheCommand(CREATE_CACHE, CACHE_TEMPLATE, REPLICATED);
    }

    @Override
    public IgniteSession createSession() {
        IgniteSession session = new IgniteSession();

        if (this.defaultMaxInactiveInterval != null)
            session.setMaxInactiveIntervalInSeconds(this.defaultMaxInactiveInterval);

        return session;
    }

    @Override
    public void save(IgniteSession session) {
        executeSessionCacheCommand(PUT, KEY, session.getId(), VALUE, serialize(session));
    }

    @Override
    public void delete(String id) {
        executeSessionCacheCommand(DELETE, KEY, id);
    }

    @Override
    public IgniteSession getSession(final String id) {
        ResponseHandler<IgniteSession> hnd = new ResponseHandler<IgniteSession>() {
            @Override
            public IgniteSession handleResponse(HttpResponse res) throws IOException {
                assert res.getStatusLine().getStatusCode() == 200;

                InputStream is = null;
                try {
                    is = res.getEntity().getContent();
                    JsonNode node = mapper.readTree(is).get("response");
                    IgniteSession session = deserialize(node.asText());

                    if (session == null)
                        return null;

                    if (session.isExpired()) {
                        delete(id);
                        return null;
                    }

                    return session;
                } catch (DecoderException e) {
                    e.printStackTrace();
                } finally {
                    if (is != null)
                        is.close();
                }

                return null;
            }
        };

        return executeSessionCacheCommand(hnd, GET, KEY, id);
    }

    private CloseableHttpClient createHttpClient() {
        return HttpClients.custom().build();
    }

    private void executeSessionCacheCommand(String command, String... args) {
        executeSessionCacheCommand(null, command, args);
    }

    private <T> T executeSessionCacheCommand(ResponseHandler<? extends T> hnd, String command, String... args) {
        CloseableHttpClient client = createHttpClient();
        try {
            HttpUriRequest req = buildCacheCommandRequest(this.url, command, this.sessionCacheName, args);

            if (hnd != null)
                return client.execute(req, hnd);
            else {
                CloseableHttpResponse res = client.execute(req);
                assert res.getStatusLine().getStatusCode() == 200;
                res.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                client.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return null;
    }

    private HttpUriRequest buildCacheCommandRequest(String urlAddr, String command, String cacheName,
        String... params) throws Exception {
        if (params.length % 2 != 0)
            throw new IllegalArgumentException("Number of parameters should be even");

        List<NameValuePair> paramList = new ArrayList<NameValuePair>(params.length / 2);

        for (int i = 0; i < params.length; i += 2) {
            String key = params[i];
            String val = params[i + 1];

            paramList.add(new BasicNameValuePair(key, val));
        }

        URL url = new URL(urlAddr + "/ignite" +
            "?" + CMD + '=' + command +
            "&" + CACHE_NAME + '=' + cacheName);
        HttpPost req = new HttpPost(url.toURI());
        req.setEntity(new UrlEncodedFormEntity(paramList));

        return req;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setSessionCacheName(String sessionCacheName) {
        this.sessionCacheName = sessionCacheName;
    }

    public void setDefaultMaxInactiveInterval(Integer dfltMaxInactiveInterval) {
        defaultMaxInactiveInterval = dfltMaxInactiveInterval;
    }

    private static class IgniteSessionDeserializingConverter implements Converter<byte[], IgniteSession> {

        @Override public IgniteSession convert(byte[] bytes) {
            ByteArrayInputStream byteStream = new ByteArrayInputStream(bytes);

            try {
                ConfigurableObjectInputStream objectInputStream = new ConfigurableObjectInputStream(byteStream, null);
                return (IgniteSession)objectInputStream.readObject();
            }
            catch (Throwable e) {
                throw new SerializationFailedException("Failed to deserialize payload. Is the byte array a result of corresponding serialization for " + IgniteSessionDeserializingConverter.class.getSimpleName() + "?", e);
            }
        }
    }

    private class IgniteSessionSerializingConverter implements Converter<IgniteSession, byte[]> {

        @Override public byte[] convert(IgniteSession bytes) {
            return null;
        }
    }

}
