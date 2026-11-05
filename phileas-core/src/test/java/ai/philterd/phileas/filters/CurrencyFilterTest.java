/*
 *     Copyright 2025 Philterd, LLC @ https://www.philterd.ai
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

import ai.philterd.phileas.model.filtering.FilterType;
import ai.philterd.phileas.model.filtering.Filtered;
import ai.philterd.phileas.services.filters.regex.CurrencyFilter;
import ai.philterd.phileas.services.strategies.AbstractFilterStrategy;
import ai.philterd.phileas.services.strategies.rules.CurrencyFilterStrategy;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

import static ai.philterd.phileas.services.strategies.AbstractFilterStrategy.RANDOM_REPLACE;

public class CurrencyFilterTest extends AbstractFilterTest {

    @Test
    public void filter1() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new CurrencyFilterStrategy()))
                .withWindowSize(windowSize)
                .build();

        final CurrencyFilter filter = new CurrencyFilter(filterConfiguration);

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "the drug cost is $35.53 .");

        showSpans(filtered.getSpans());

        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 17, 23, FilterType.CURRENCY));
        Assertions.assertNotEquals(filtered.getSpans().get(0).getText(), filtered.getSpans().get(0).getReplacement());

    }

    @Test
    public void filter2() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new CurrencyFilterStrategy()))
                .withWindowSize(windowSize)
                .build();

        final CurrencyFilter filter = new CurrencyFilter(filterConfiguration);

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "the drug cost is $35.53.");

        showSpans(filtered.getSpans());

        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 17, 23, FilterType.CURRENCY));
        Assertions.assertNotEquals(filtered.getSpans().get(0).getText(), filtered.getSpans().get(0).getReplacement());

    }

    @Test
    public void filter3() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new CurrencyFilterStrategy()))
                .withWindowSize(windowSize)
                .build();

        final CurrencyFilter filter = new CurrencyFilter(filterConfiguration);

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "the drug cost is $35.00.");

        showSpans(filtered.getSpans());

        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 17, 23, FilterType.CURRENCY));
        Assertions.assertNotEquals(filtered.getSpans().get(0).getText(), filtered.getSpans().get(0).getReplacement());

    }

    @Test
    public void filter4() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new CurrencyFilterStrategy()))
                .withWindowSize(windowSize)
                .build();

        final CurrencyFilter filter = new CurrencyFilter(filterConfiguration);

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "the drug cost is $3.00.");

        showSpans(filtered.getSpans());

        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 17, 22, FilterType.CURRENCY));
        Assertions.assertNotEquals(filtered.getSpans().get(0).getText(), filtered.getSpans().get(0).getReplacement());

    }

    @Test
    public void filter5() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new CurrencyFilterStrategy()))
                .withWindowSize(windowSize)
                .build();

        final CurrencyFilter filter = new CurrencyFilter(filterConfiguration);

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "the drug cost is $.50.");

        showSpans(filtered.getSpans());

        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 17, 21, FilterType.CURRENCY));
        Assertions.assertNotEquals(filtered.getSpans().get(0).getText(), filtered.getSpans().get(0).getReplacement());

    }

    @Test
    public void filter6() throws Exception {

        final CurrencyFilterStrategy currencyFilterStrategy = new CurrencyFilterStrategy();
        currencyFilterStrategy.setStrategy(AbstractFilterStrategy.RANDOM_REPLACE);

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(currencyFilterStrategy))
                .withWindowSize(windowSize)
                .build();

        final CurrencyFilter filter = new CurrencyFilter(filterConfiguration);

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "the drug cost is $50.");

        showSpans(filtered.getSpans());

        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 17, 20, FilterType.CURRENCY));
        Assertions.assertNotEquals(filtered.getSpans().get(0).getText(), filtered.getSpans().get(0).getReplacement());

    }

    @Test
    public void filterWithCandidates1() throws Exception {

        final List<String> candidates = List.of("candidate1", "candidate2");

        final CurrencyFilterStrategy currencyFilterStrategy = new CurrencyFilterStrategy();
        currencyFilterStrategy.setStrategy(RANDOM_REPLACE);
        currencyFilterStrategy.setAnonymizationCandidates(candidates);

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(currencyFilterStrategy))
                .withWindowSize(windowSize)
                .build();

        final CurrencyFilter filter = new CurrencyFilter(filterConfiguration);

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "the drug cost is $35.53 .");
        showSpans(filtered.getSpans());
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(candidates.contains(filtered.getSpans().get(0).getReplacement()));

    }

    @Test
    public void filterEuroUSFormat() throws Exception {
        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new CurrencyFilterStrategy()))
                .withWindowSize(windowSize)
                .build();
        final CurrencyFilter filter = new CurrencyFilter(filterConfiguration);

        final String input = "the cost is €1,450.00.";
        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, input);

        showSpans(filtered.getSpans());
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertEquals("€1,450.00", filtered.getSpans().get(0).getText());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 12, 21, FilterType.CURRENCY));
    }

    @Test
    public void filterEuroEUFormat() throws Exception {
        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new CurrencyFilterStrategy()))
                .withWindowSize(windowSize)
                .build();
        final CurrencyFilter filter = new CurrencyFilter(filterConfiguration);

        final String input = "the cost is 1.450,00 €.";
        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, input);

        showSpans(filtered.getSpans());
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertEquals("1.450,00 €", filtered.getSpans().get(0).getText());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 12, 22, FilterType.CURRENCY));
    }

    @Test
    public void filterPoundSymbol() throws Exception {
        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new CurrencyFilterStrategy()))
                .withWindowSize(windowSize)
                .build();
        final CurrencyFilter filter = new CurrencyFilter(filterConfiguration);

        final String input = "the fee is £250.00.";
        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, input);

        showSpans(filtered.getSpans());
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertEquals("£250.00", filtered.getSpans().get(0).getText());
    }

    @Test
    public void filterYenSymbol() throws Exception {
        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new CurrencyFilterStrategy()))
                .withWindowSize(windowSize)
                .build();
        final CurrencyFilter filter = new CurrencyFilter(filterConfiguration);

        final String input = "the price is ¥5,000.";
        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, input);

        showSpans(filtered.getSpans());
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertEquals("¥5,000", filtered.getSpans().get(0).getText());
    }

    @Test
    public void filterRupeeSymbol() throws Exception {
        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new CurrencyFilterStrategy()))
                .withWindowSize(windowSize)
                .build();
        final CurrencyFilter filter = new CurrencyFilter(filterConfiguration);

        final String input = "the total is ₹1,000.00.";
        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, input);

        showSpans(filtered.getSpans());
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertEquals("₹1,000.00", filtered.getSpans().get(0).getText());
    }

    @Test
    public void filterIsoCodeGbpPostfix() throws Exception {
        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new CurrencyFilterStrategy()))
                .withWindowSize(windowSize)
                .build();
        final CurrencyFilter filter = new CurrencyFilter(filterConfiguration);

        final String input = "fee is 250 GBP.";
        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, input);

        showSpans(filtered.getSpans());
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertEquals("250 GBP", filtered.getSpans().get(0).getText());
    }

    @Test
    public void filterIsoCodeEurPrefix() throws Exception {
        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new CurrencyFilterStrategy()))
                .withWindowSize(windowSize)
                .build();
        final CurrencyFilter filter = new CurrencyFilter(filterConfiguration);

        final String input = "paid EUR 500.00.";
        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, input);

        showSpans(filtered.getSpans());
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertEquals("EUR 500.00", filtered.getSpans().get(0).getText());
    }

    @Test
    public void filterNegativeNonCurrencyNumbers() throws Exception {
        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new CurrencyFilterStrategy()))
                .withWindowSize(windowSize)
                .build();
        final CurrencyFilter filter = new CurrencyFilter(filterConfiguration);

        final String input = "In year 2026 patient was in room 500.";
        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, input);

        showSpans(filtered.getSpans());
        Assertions.assertEquals(0, filtered.getSpans().size());
    }

    @Test
    public void testConsecutiveCurrencies() throws Exception {
        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new CurrencyFilterStrategy()))
                .withWindowSize(windowSize)
                .build();
        final CurrencyFilter filter = new CurrencyFilter(filterConfiguration);

        final String input = "€50 £100 $150";
        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, input);

        showSpans(filtered.getSpans());
        Assertions.assertEquals(3, filtered.getSpans().size());
        Assertions.assertEquals("€50", filtered.getSpans().get(0).getText());
        Assertions.assertEquals("£100", filtered.getSpans().get(1).getText());
        Assertions.assertEquals("$150", filtered.getSpans().get(2).getText());
    }

}
