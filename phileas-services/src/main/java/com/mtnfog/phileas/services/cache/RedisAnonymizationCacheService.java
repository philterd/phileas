package com.mtnfog.phileas.services.cache;

import com.mtnfog.phileas.model.services.AnonymizationCacheService;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import redis.clients.jedis.Jedis;

import javax.net.ssl.*;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.SecureRandom;

public class RedisAnonymizationCacheService implements AnonymizationCacheService {

    private Jedis jedis;

    public RedisAnonymizationCacheService(String host, int port, String authToken, String trustStoreJks) throws Exception {

        final SSLSocketFactory sslSocketFactory = createTrustStoreSslSocketFactory(trustStoreJks);

        final SSLParameters sslParameters = new SSLParameters();
        sslParameters.setEndpointIdentificationAlgorithm("HTTPS");

        final HostnameVerifier hostnameVerifier = HttpsURLConnection.getDefaultHostnameVerifier();

        this.jedis = new Jedis(host, port, true, sslSocketFactory, sslParameters, hostnameVerifier);

        this.jedis.connect();
        this.jedis.auth(authToken);
        this.jedis.flushAll();

    }

    public RedisAnonymizationCacheService(String host, int port, String authToken) {

        this.jedis = new Jedis(host, port, true);

        this.jedis.connect();
        this.jedis.auth(authToken);
        this.jedis.flushAll();

    }

    /**
     * Only used for testing. A production connection to Redis must use SSL.
     * @param host The hostname  of the Redis server.
     * @param port The port of the Redis server.
     */
    public RedisAnonymizationCacheService(String host, int port) {

        this.jedis = new Jedis(host, port, false);

    }

    @Override
    public String generateKey(String context, String token) {

        return DigestUtils.md5Hex(context + "|" + token);

    }

    @Override
    public void put(String context, String token, String replacement) {

        jedis.set(generateKey(context, token), replacement);
        jedis.hset(context, token, replacement);

    }

    @Override
    public String get(String context, String token) {

        return jedis.get(generateKey(context, token));

    }

    @Override
    public void remove(String context, String token) {

        jedis.del(generateKey(context, token));

    }

    @Override
    public boolean contains(String context, String token) {

        return jedis.exists(generateKey(context, token));

    }

    @Override
    public boolean containsValue(String context, String replacement) {

        return jedis.hexists(context, replacement);

    }

    private static SSLSocketFactory createTrustStoreSslSocketFactory(String trustStoreJks) throws Exception {

        final KeyStore trustStore = KeyStore.getInstance("jceks");

        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(trustStoreJks);
            trustStore.load(inputStream, null);
        } finally {
            inputStream.close();
        }

        final TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance("PKIX");
        trustManagerFactory.init(trustStore);

        final TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();

        final SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, trustManagers, new SecureRandom());

        return sslContext.getSocketFactory();

    }

}
