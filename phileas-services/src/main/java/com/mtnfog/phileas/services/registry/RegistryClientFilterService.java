package com.mtnfog.phileas.services.registry;

import com.google.gson.Gson;
import com.mtnfog.phileas.model.profile.FilterProfile;
import com.mtnfog.phileas.model.services.FilterProfileService;
import okhttp3.OkHttpClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Path;

import javax.net.ssl.*;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RegistryClientFilterService implements FilterProfileService {

    private static final Logger LOGGER = LogManager.getLogger(RegistryClientFilterService.class);

    private FilterProfileRegistryService service;
    private Gson gson;

    public RegistryClientFilterService(String endpoint, boolean verifySslCertificate) {

        this.gson = new Gson();

        OkHttpClient okHttpClient = new OkHttpClient();

        if(!verifySslCertificate) {

            try {

                LOGGER.warn("Allowing all SSL certificates is not recommended.");
                okHttpClient = UnsafeOkHttpClient.getUnsafeOkHttpClient();

            } catch (NoSuchAlgorithmException | KeyManagementException ex) {

                LOGGER.error("Cannot create unsafe HTTP client.", ex);
                throw new RuntimeException("Cannot create unsafe HTTP client.", ex);

            }

        }

        final Retrofit.Builder builder = new Retrofit.Builder()
                .baseUrl(endpoint)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create());

        final Retrofit retrofit = builder.build();

        service = retrofit.create(FilterProfileRegistryService.class);

    }

    @Override
    public String getFilterProfile(String filterProfileName) throws IOException {
        return service.getFilterProfile(filterProfileName).execute().body();
    }

    @Override
    public Map<String, FilterProfile> getAll() throws IOException {

        final Map<String, FilterProfile> filterProfiles = new HashMap<>();

        final List<String> filterProfileNames = service.getFilterProfiles().execute().body();

        for(String filterProfileName : filterProfileNames) {

            final String filterProfileJson = getFilterProfile(filterProfileName);
            final FilterProfile filterProfile = gson.fromJson(filterProfileJson, FilterProfile.class);

            filterProfiles.put(filterProfileName, filterProfile);

        }

        return filterProfiles;

    }

    interface FilterProfileRegistryService {

        @GET("/api/profiles/{name}")
        Call<String> getFilterProfile(@Path("name") String filterProfileName);

        @GET("/api/profiles")
        Call<List<String>> getFilterProfiles();

    }

    public static class UnsafeOkHttpClient {

        public static OkHttpClient getUnsafeOkHttpClient() throws NoSuchAlgorithmException, KeyManagementException {

            final TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {

                @Override
                public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType)
                        throws CertificateException {
                }

                @Override
                public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType)
                        throws CertificateException {
                }

                @Override
                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return new java.security.cert.X509Certificate[]{};
                }

            }};

            final SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());

            OkHttpClient.Builder builder = new OkHttpClient.Builder();
            builder.sslSocketFactory(sslContext.getSocketFactory(), (X509TrustManager) trustAllCerts[0]);
            builder.hostnameVerifier(new HostnameVerifier() {

                @Override
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }

            });

            return builder.build();

        }

    }

}


