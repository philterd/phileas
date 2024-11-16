package ai.philterd.phileas.benchmarks.tests;

import ai.philterd.phileas.benchmarks.Documents;
import ai.philterd.phileas.benchmarks.Redactor;
import com.fasterxml.jackson.core.JsonParser;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.opensearch.action.bulk.BulkRequest;
import org.opensearch.action.index.IndexRequest;
import org.opensearch.client.RequestOptions;
import org.opensearch.client.RestClient;
import org.opensearch.client.RestClientBuilder;
import org.opensearch.client.RestHighLevelClient;
import org.opensearch.client.indices.CreateIndexRequest;
import org.opensearch.client.indices.GetIndexRequest;
import org.opensearch.core.xcontent.MediaType;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509ExtendedTrustManager;
import java.io.InputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Tag("benchmarks")
@EnabledIf("benchmarksEnabled")
public class BenchmarksTest {

    static boolean benchmarksEnabled() {
        return System.getenv("BENCHMARKS_ENABLED") != null;
    }

    @Test
    public void runBenchmarks() throws Exception {

        final String branch = getGitBranch();
        final String runId = UUID.randomUUID().toString();

        final BulkRequest bulkRequest = new BulkRequest();

        // read arguments
        final String arg_document = "all";
        final String arg_redactor = "mask_all";
        final int repetitions = 1;
        final int workload_millis = 100;

        // create redactor based on Phileas PII engine
        final Redactor redactor = new Redactor(arg_redactor);

        // repeatedly redact documents and print results
        final List<String> documents = "all".equals(arg_document) ? Documents.keys : List.of(arg_document);
        final int[] value_lengths = {0, 1, 2, 4, 8, 16, 32, 64, 128, 256, 512, 768, 1024, 1280, 1536, 1792, 2048, 3072, 4096};

        for (int i = 0; i < repetitions; i++) {

            for (final String document : documents) {

                //if (!arg_format.equals("json")) {
                    System.out.println("\n------------------------------------------------------------------------------------------");
                    System.out.println("Using document: " + document);
                    System.out.println("Using redactor: " + arg_redactor);
                    System.out.println("Using workload_millis: " + workload_millis);
                    System.out.println("\nstring_length,calls_per_sec");
                //}

                final Map<String, Long> calls = new HashMap<>();

                for (int value_length : value_lengths) {

                    if(Documents.get(document).length() >= value_length) {

                        final String value = Documents.get(document).substring(0, value_length);
                        final long calls_per_sec = run_workload(workload_millis, redactor, value);
                        System.out.println(value.length() + "," + calls_per_sec);

                        calls.put(String.valueOf(value_length), calls_per_sec);

                    } else {
                        break;
                    }

                }

                final Map<String, Object> run = new HashMap<>();
                run.put("document", document);
                run.put("workload_mills", workload_millis);
                run.put("redactor", arg_redactor);
                run.put("timestamp", System.currentTimeMillis());
                run.put("phileas_version", System.getProperty("phileasVersion"));
                run.put("branch", branch);
                run.put("calls_per_second", calls);
                run.put("run_id", runId);

                final IndexRequest indexRequest = new IndexRequest("phileas_benchmarks");
                indexRequest.id(UUID.randomUUID().toString()).source(run);

                bulkRequest.add(indexRequest);

            }

        }

        if(System.getenv("PHILEAS_BENCHMARKS_OPENSEARCH_URL") != null) {

            System.out.println("Indexing results...");

            final String phileasBenchMarksOpenSearchUrl = System.getenv("PHILEAS_BENCHMARKS_OPENSEARCH_URL");
            final String phileasBenchmarksOpenSearchUser = System.getenv("PHILEAS_BENCHMARKS_OPENSEARCH_USER");
            final String phileasBenchmarksOpenSearchPassword = System.getenv("PHILEAS_BENCHMARKS_OPENSEARCH_PASSWORD");

            final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(phileasBenchmarksOpenSearchUser, phileasBenchmarksOpenSearchPassword));

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{UnsafeX509ExtendedTrustManager.INSTANCE}, null);

            final RestClientBuilder builder = RestClient.builder(new HttpHost(phileasBenchMarksOpenSearchUrl, 9200, "https"));
            builder.setHttpClientConfigCallback(httpClientBuilder ->
                    httpClientBuilder
                            .setSSLHostnameVerifier((s, sslSession) -> true)
                            .setSSLContext(sslContext)
                            .setDefaultCredentialsProvider(credentialsProvider));

            final RestHighLevelClient openSearchClient = new RestHighLevelClient(builder);

            if(!openSearchClient.indices().exists(new GetIndexRequest("phileas_benchmarks"), RequestOptions.DEFAULT)) {

                final InputStream inputStream = getClass().getClassLoader().getResourceAsStream("mapping.json");
                final String mapping = IOUtils.toString(inputStream, StandardCharsets.UTF_8.name());

                final CreateIndexRequest createIndexRequest = new CreateIndexRequest("phileas_benchmarks");
                createIndexRequest.mapping(mapping, MediaType.fromMediaType("application/json; charset=UTF-8"));
                openSearchClient.indices().create(createIndexRequest, RequestOptions.DEFAULT);

            }

            openSearchClient.bulk(bulkRequest, RequestOptions.DEFAULT);

        }

    }

    private static long run_workload(final int millis, final Redactor redactor, final String value) throws Exception {

        final long start = System.currentTimeMillis();
        long calls = -1;
        while ((++calls % 100 != 0) || (System.currentTimeMillis() - start < millis)) redactor.filter(value);

        return calls * 1000 / (System.currentTimeMillis() - start);

    }

    private static String getGitBranch() throws Exception {

        Process process = Runtime.getRuntime().exec("git rev-parse --abbrev-ref HEAD");
        process.waitFor();

        byte[] output = process.getInputStream().readAllBytes();

        return new String(output).trim();

    }

    public static class UnsafeX509ExtendedTrustManager extends X509ExtendedTrustManager {

        private static final X509ExtendedTrustManager INSTANCE = new UnsafeX509ExtendedTrustManager();
        private static final X509Certificate[] EMPTY_CERTIFICATES = new X509Certificate[0];

        private UnsafeX509ExtendedTrustManager() {}

        @Override
        public void checkClientTrusted(X509Certificate[] certificates, String authType) {

        }

        @Override
        public void checkClientTrusted(X509Certificate[] certificates, String authType, Socket socket) {

        }

        @Override
        public void checkClientTrusted(X509Certificate[] certificates, String authType, SSLEngine sslEngine) {

        }

        @Override
        public void checkServerTrusted(X509Certificate[] certificates, String authType) {

        }

        @Override
        public void checkServerTrusted(X509Certificate[] certificates, String authType, Socket socket) {

        }

        @Override
        public void checkServerTrusted(X509Certificate[] certificates, String authType, SSLEngine sslEngine) {

        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return EMPTY_CERTIFICATES;
        }

    }

}
