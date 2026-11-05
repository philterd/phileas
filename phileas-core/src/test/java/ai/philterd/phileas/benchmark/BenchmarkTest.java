/*
 *     Copyright 2024 Philterd, LLC @ https://www.philterd.ai
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ai.philterd.phileas.benchmark;

import com.google.gson.Gson;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Single-threaded redaction benchmark for the Phileas PII engine. This is tagged {@code benchmark}
 * and is excluded from the normal test run (it runs timed workloads, not assertions). Run it
 * explicitly with the {@code benchmark} profile, for example:
 *
 * <pre>
 *   mvn test -Pbenchmark
 *   mvn test -Pbenchmark -Dbenchmark.redactor=mask_credit_cards -Dbenchmark.millis=2000
 *   mvn test -Pbenchmark -Dbenchmark.document=i_have_a_dream -Dbenchmark.format=json
 * </pre>
 *
 * Parameters (all optional, passed as system properties):
 * <ul>
 *   <li>{@code benchmark.document} - a document name or {@code all} (default {@code all})</li>
 *   <li>{@code benchmark.redactor} - a redactor name (default {@code mask_all})</li>
 *   <li>{@code benchmark.repetitions} - number of times to repeat the workloads (default {@code 1})</li>
 *   <li>{@code benchmark.millis} - duration of each workload in milliseconds (default {@code 1000})</li>
 *   <li>{@code benchmark.format} - {@code sysout} or {@code json} (default {@code sysout})</li>
 * </ul>
 */
@Tag("benchmark")
public class BenchmarkTest {

    private static final int[] VALUE_LENGTHS =
            {0, 1, 2, 4, 8, 16, 32, 64, 128, 256, 512, 768, 1024, 1280, 1536, 1792, 2048, 3072, 4096};

    @Test
    public void benchmark() throws Exception {

        final String argDocument = System.getProperty("benchmark.document", "all");
        final String argRedactor = System.getProperty("benchmark.redactor", "mask_all");
        final int repetitions = Integer.parseInt(System.getProperty("benchmark.repetitions", "1"));
        final int workloadMillis = Integer.parseInt(System.getProperty("benchmark.millis", "1000"));
        final String argFormat = System.getProperty("benchmark.format", "sysout");

        final Redactor redactor = new Redactor(argRedactor);

        final List<String> documents = "all".equals(argDocument) ? Documents.keys : List.of(argDocument);

        for (int i = 0; i < repetitions; i++) {

            for (final String document : documents) {

                if (!argFormat.equals("json")) {
                    System.out.println("\n------------------------------------------------------------------------------------------");
                    System.out.println("Using document: " + document);
                    System.out.println("Using redactor: " + argRedactor);
                    System.out.println("Using workload_millis: " + workloadMillis);
                    System.out.println("\nstring_length,calls_per_sec");
                }

                final Map<Integer, Long> calls = new HashMap<>();

                for (final int valueLength : VALUE_LENGTHS) {

                    if (Documents.get(document).length() >= valueLength) {

                        final String value = Documents.get(document).substring(0, valueLength);
                        final long callsPerSec = runWorkload(workloadMillis, redactor, value);

                        if (!argFormat.equals("json")) {
                            System.out.println(value.length() + "," + callsPerSec);
                        }

                        calls.put(valueLength, callsPerSec);

                    } else {
                        break;
                    }

                }

                if (argFormat.equals("json")) {

                    final Result result = new Result();
                    result.setWorkloadMillis(workloadMillis);
                    result.setRedactor(argRedactor);
                    result.setDocument(document);
                    result.setCallsPerSecond(calls);

                    System.out.println(new Gson().toJson(result));
                }

            }

        }

    }

    private static long runWorkload(int millis, Redactor redactor, String value) throws Exception {

        final long start = System.currentTimeMillis();
        long calls = -1;
        while ((++calls % 100 != 0) || (System.currentTimeMillis() - start < millis)) {
            redactor.filter(value);
        }

        return calls * 1000 / (System.currentTimeMillis() - start);

    }

}
