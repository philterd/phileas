package ai.philterd.phileas.benchmarks.tests;

import ai.philterd.phileas.benchmarks.Documents;
import ai.philterd.phileas.benchmarks.Redactor;
import ai.philterd.phileas.benchmarks.Result;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Tag("benchmarks")
@EnabledIf("benchmarksEnabled")
public class BenchmarksTest {

    static boolean benchmarksEnabled() {
        return System.getenv("BENCHMARKS_ENABLED") != null;
    }

    @Test
    public void runBenchmarks() throws Exception {

        // java -server -Xmx512M -XX:+AlwaysPreTouch -XX:PerBytecodeRecompilationCutoff=10000 -XX:PerMethodRecompilationCutoff=10000 -jar target/phileas-benchmark-cmd.jar all mask_all 1 15000

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

                final Result result = new Result();
                result.setWorkloadMillis(workload_millis);
                result.setRedactor(arg_redactor);
                result.setDocument(document);
                result.setCallsPerSecond(calls);

                // TODO: Persist the result somewhere.

            }

        }

    }

    private static long run_workload(final int millis, final Redactor redactor, final String value) throws Exception {

        final long start = System.currentTimeMillis();
        long calls = -1;
        while ((++calls % 100 != 0) || (System.currentTimeMillis() - start < millis)) redactor.filter(value);

        return calls * 1000 / (System.currentTimeMillis() - start);

    }

}
