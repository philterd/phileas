/*
 *     Copyright 2026 Philterd, LLC @ https://www.philterd.ai
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
package ai.philterd.phileas.filters;

import ai.philterd.phileas.filters.FilterConfiguration;
import ai.philterd.phileas.model.filtering.FilterType;
import ai.philterd.phileas.model.filtering.Filtered;
import ai.philterd.phileas.services.anonymization.CurrencyAnonymizationService;
import ai.philterd.phileas.services.filters.regex.CurrencyFilter;
import ai.philterd.phileas.services.strategies.rules.CurrencyFilterStrategy;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

public class AdversarialCurrencyFilterTest extends AbstractFilterTest {

    private CurrencyFilter getFilter() throws Exception {
        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new CurrencyFilterStrategy()))
                .withWindowSize(windowSize)
                .build();
        return new CurrencyFilter(filterConfiguration);
    }

    @Test
    public void testAdversarialEdgeCasesSymbolsAndLocales() throws Exception {
        final CurrencyFilter filter = getFilter();

        // 1. Euro with spacing and comma decimal (European format)
        Filtered f1 = filter.filter(contextService, getPolicy(), "context", PIECE, "Patient copay is € 1.500,00 for procedure.");
        Assertions.assertEquals(1, f1.getSpans().size());
        Assertions.assertEquals("€ 1.500,00", f1.getSpans().get(0).getText());

        // 2. Euro suffix with European format
        Filtered f2 = filter.filter(contextService, getPolicy(), "context", PIECE, "Total bill came to 1.450,00 € in total.");
        Assertions.assertEquals(1, f2.getSpans().size());
        Assertions.assertEquals("1.450,00 €", f2.getSpans().get(0).getText());

        // 3. British Pound with spaces and US decimal format
        Filtered f3 = filter.filter(contextService, getPolicy(), "context", PIECE, "Consultation fee is £ 250.00.");
        Assertions.assertEquals(1, f3.getSpans().size());
        Assertions.assertEquals("£ 250.00", f3.getSpans().get(0).getText());

        // 4. Japanese Yen integer with prefix space
        Filtered f4 = filter.filter(contextService, getPolicy(), "context", PIECE, "Medication cost is ¥ 15000.");
        Assertions.assertEquals(1, f4.getSpans().size());
        Assertions.assertEquals("¥ 15000", f4.getSpans().get(0).getText());

        // 5. Indian Rupee with comma thousands and decimal
        Filtered f5 = filter.filter(contextService, getPolicy(), "context", PIECE, "Hospital charge is ₹ 75,000.50 today.");
        Assertions.assertEquals(1, f5.getSpans().size());
        Assertions.assertEquals("₹ 75,000.50", f5.getSpans().get(0).getText());
    }

    @Test
    public void testAdversarialIsoCodeFormats() throws Exception {
        final CurrencyFilter filter = getFilter();

        // 1. Prefix ISO Code CAD
        Filtered f1 = filter.filter(contextService, getPolicy(), "context", PIECE, "Lab fee CAD 150.75 paid.");
        Assertions.assertEquals(1, f1.getSpans().size());
        Assertions.assertEquals("CAD 150.75", f1.getSpans().get(0).getText());

        // 2. Suffix ISO Code AUD
        Filtered f2 = filter.filter(contextService, getPolicy(), "context", PIECE, "Pharmacy balance 75.50 AUD total.");
        Assertions.assertEquals(1, f2.getSpans().size());
        Assertions.assertEquals("75.50 AUD", f2.getSpans().get(0).getText());

        // 3. Lowercase ISO code in sentence
        Filtered f3 = filter.filter(contextService, getPolicy(), "context", PIECE, "Payment received: 500 eur on file.");
        Assertions.assertEquals(1, f3.getSpans().size());
        Assertions.assertEquals("500 eur", f3.getSpans().get(0).getText());

        // 4. Case insensitive GBP prefix
        Filtered f4 = filter.filter(contextService, getPolicy(), "context", PIECE, "Invoice gbp 1250.00 issued.");
        Assertions.assertEquals(1, f4.getSpans().size());
        Assertions.assertEquals("gbp 1250.00", f4.getSpans().get(0).getText());
    }

    @Test
    public void testAdversarialPunctuationAndBoundaries() throws Exception {
        final CurrencyFilter filter = getFilter();

        // 1. Currency inside parentheses
        Filtered f1 = filter.filter(contextService, getPolicy(), "context", PIECE, "The balance (including €1.500,00 deposit) is settled.");
        Assertions.assertEquals(1, f1.getSpans().size());
        Assertions.assertEquals("€1.500,00", f1.getSpans().get(0).getText());

        // 2. Currency with trailing sentence punctuation
        Filtered f2 = filter.filter(contextService, getPolicy(), "context", PIECE, "Was the total amount 250 GBP?");
        Assertions.assertEquals(1, f2.getSpans().size());
        Assertions.assertEquals("250 GBP", f2.getSpans().get(0).getText());

        // 3. Currency with quote marks
        Filtered f3 = filter.filter(contextService, getPolicy(), "context", PIECE, "Itemized as \"$3,450.75 USD\" in bill.");
        Assertions.assertEquals(1, f3.getSpans().size());
        Assertions.assertEquals("$3,450.75 USD", f3.getSpans().get(0).getText());

        // 4. Fractional decimal amount
        Filtered f4 = filter.filter(contextService, getPolicy(), "context", PIECE, "Discount of €.50 applied.");
        Assertions.assertEquals(1, f4.getSpans().size());
        Assertions.assertEquals("€.50", f4.getSpans().get(0).getText());
    }

    @Test
    public void testAdversarialNegativeCasesNoFalsePositives() throws Exception {
        final CurrencyFilter filter = getFilter();

        // 1. Plain numbers / dates / rooms
        Filtered f1 = filter.filter(contextService, getPolicy(), "context", PIECE, "In 2026, patient in room 402 had 3 tests.");
        Assertions.assertEquals(0, f1.getSpans().size());

        // 2. Words starting with ISO codes
        Filtered f2 = filter.filter(contextService, getPolicy(), "context", PIECE, "EUROPE and CANADA are regions.");
        Assertions.assertEquals(0, f2.getSpans().size());

        // 3. Code-like strings
        Filtered f3 = filter.filter(contextService, getPolicy(), "context", PIECE, "Model CAD100 or USD500 without space.");
        // Should not match CAD100 or USD500 as currency due to word boundaries
        Assertions.assertEquals(0, f3.getSpans().size());
    }

    @Test
    public void testAdversarialAnonymizationService() {
        CurrencyAnonymizationService anonymizationService = new CurrencyAnonymizationService();

        // Euro symbol preservation
        String eurAnon = anonymizationService.anonymize("€1.500,00");
        Assertions.assertTrue(eurAnon.startsWith("€"));
        Assertions.assertFalse(eurAnon.startsWith("$"));

        // GBP ISO preservation
        String gbpAnon = anonymizationService.anonymize("250 GBP");
        Assertions.assertTrue(gbpAnon.endsWith("GBP"));

        // Yen symbol preservation
        String yenAnon = anonymizationService.anonymize("¥5000");
        Assertions.assertTrue(yenAnon.startsWith("¥"));

        // Rupee symbol preservation
        String inrAnon = anonymizationService.anonymize("₹1,000.00");
        Assertions.assertTrue(inrAnon.startsWith("₹"));
    }

    @Test
    public void testPerformanceAndThroughput() throws Exception {
        final CurrencyFilter filter = getFilter();
        final String input = "The surgery balance is €1.500,00 and consultation fee is 250 GBP. Additional charges: $50.00, ¥5000, ₹1,000.00, and 150.00 CAD.";

        // Warmup
        for (int i = 0; i < 1000; i++) {
            filter.filter(contextService, getPolicy(), "context", PIECE, input);
        }

        // Benchmark
        int iterations = 10000;
        long startTime = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            filter.filter(contextService, getPolicy(), "context", PIECE, input);
        }
        long durationNs = System.nanoTime() - startTime;
        double durationMs = durationNs / 1_000_000.0;
        double opsPerSec = (iterations * 1000.0) / durationMs;

        System.out.printf("[BENCHMARK] Executed %d document filter operations in %.2f ms (%.2f ops/sec, %.4f ms/op)%n",
                iterations, durationMs, opsPerSec, durationMs / iterations);

        // Throughput assertion: Must execute at least 100 ops/sec
        Assertions.assertTrue(opsPerSec > 100, "Throughput below minimum threshold: " + opsPerSec);
    }
}


