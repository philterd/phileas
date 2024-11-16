package ai.philterd.phileas.benchmarks.tests;

import ai.philterd.phileas.benchmarks.Documents;
import ai.philterd.phileas.benchmarks.Redactor;
import ai.philterd.phileas.benchmarks.Result;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.X509ExtendedTrustManager;
import java.net.Socket;
import java.security.cert.X509Certificate;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
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

        final Collection<Result> results = new LinkedList<>();

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

                final Map<Integer, Long> calls = new HashMap<>();

                for (int value_length : value_lengths) {

                    if(Documents.get(document).length() >= value_length) {

                        final String value = Documents.get(document).substring(0, value_length);
                        final long calls_per_sec = run_workload(workload_millis, redactor, value);
                        System.out.println(value.length() + "," + calls_per_sec);

                        calls.put(value_length, calls_per_sec);

                    } else {
                        break;
                    }

                }

                // calls_per_second
                final Result result = new Result();
                result.setCallsPerSecond(calls);
                result.setDocument(document);
                result.setWorkloadMillis(workload_millis);
                result.setBranch(branch);
                result.setRedactor(arg_redactor);
                result.setPhileasVersion(System.getProperty("phileasVersion"));
                result.setTimestamp(System.currentTimeMillis());

                results.add(result);

            }

        }

        if(System.getenv("BENCHMARKS_ENABLED") != null) {

            System.out.println("Writing results to the database...");

            final String connectionString = System.getenv("BENCHMARKS_CONNECTION_STRING");
            final String user = System.getenv("BENCHMARKS_USER");
            final String password = System.getenv("BENCHMARKS_PASSWORD");

            Connection connection = DriverManager.getConnection(connectionString, user, password);

            for (final Result result : results) {

                try (PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO benchmarks(document, workload_mills, redactor, timestamp, phileas_version, branch, run_id)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                  """)) {
                    statement.setString(1, result.getDocument());
                    statement.setLong(2, result.getWorkloadMillis());
                    statement.setString(3, result.getRedactor());
                    statement.setLong(4, result.getTimestamp());
                    statement.setString(5, result.getPhileasVersion());
                    statement.setString(6, result.getBranch());
                    statement.setString(7, runId);
                    statement.executeUpdate();

                }

            }

            connection.close();

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
