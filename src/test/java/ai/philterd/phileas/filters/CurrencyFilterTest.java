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

import ai.philterd.phileas.PhileasConfiguration;
import ai.philterd.phileas.model.filtering.FilterType;
import ai.philterd.phileas.model.filtering.Filtered;
import ai.philterd.phileas.policy.Policy;
import ai.philterd.phileas.services.disambiguation.vector.InMemoryVectorService;
import ai.philterd.phileas.services.filters.filtering.PlainTextFilterService;
import ai.philterd.phileas.services.filters.regex.CurrencyFilter;
import ai.philterd.phileas.services.strategies.AbstractFilterStrategy;
import ai.philterd.phileas.services.strategies.rules.CurrencyFilterStrategy;
import com.google.gson.Gson;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Properties;

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

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "Paid 5,000 euros today. | 5,000 euros",
            "The fee is 100 dollars. | 100 dollars",
            "He owed 20 British pounds. | 20 British pounds",
            "He owed 20 pounds sterling. | 20 pounds sterling",
            "He paid 235,000 Swedish crowns. | 235,000 Swedish crowns",
            "He paid 235,000 kronor. | 235,000 kronor",
            "Fined 50,000 Austrian schillings. | 50,000 Austrian schillings",
            "Damages of 10,000 French francs. | 10,000 French francs",
            "Settled for 20,000 Polish złotys. | 20,000 Polish złotys",
            "Awarded 50,000 Russian rubles. | 50,000 Russian rubles",
            "Deal valued at USD 1.5M total. | USD 1.5M",
            "Deal valued at 1.5M USD total. | 1.5M USD",
            "Deal valued at $1.5M total. | $1.5M",
            "Grant of EUR 250k approved. | EUR 250k",
            "Fund reached GBP 2bn last year. | GBP 2bn",
            "Adjustment of .50 USD applied. | .50 USD",
            "Adjustment of TRY .50 applied. | TRY .50",
            "Invoice for USD .50 balance. | USD .50",
            "Paid USD.1,000 today. | USD.1,000",
            "Paid EUR.2,500.00 today. | EUR.2,500.00",
            "Paid USD 1,,000 today. | USD 1,,000",
            "Paid 1,,000 USD today. | 1,,000 USD",
            "Paid TRY.3,000 today. | TRY.3,000",
            "Paid RUB,3,000.50 today. | RUB,3,000.50",
            "Paid CHF 1,,000 today. | CHF 1,,000",
            "Paid 1,,000 CHF today. | 1,,000 CHF",
            "Paid .3,000 CHF today. | .3,000 CHF"
    })
    void filterCurrencyNamesSingleSpan(final String input, final String expectedSpan) throws Exception {
        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new CurrencyFilterStrategy()))
                .withWindowSize(windowSize)
                .build();
        final CurrencyFilter filter = new CurrencyFilter(filterConfiguration);

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, input);
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertEquals(expectedSpan, filtered.getSpans().get(0).getText());
        final int start = input.indexOf(expectedSpan);
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), start, start + expectedSpan.length(), FilterType.CURRENCY));
    }

    @Test
    void filterIsoCodeOnNextLine() throws Exception {
        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new CurrencyFilterStrategy()))
                .withWindowSize(windowSize)
                .build();
        final CurrencyFilter filter = new CurrencyFilter(filterConfiguration);

        final String input = "Total due is 250.00\nUSD upon receipt.";
        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, input);
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertEquals("250.00\nUSD", filtered.getSpans().get(0).getText());
    }

    @ParameterizedTest
    @ValueSource(strings = {"CHF", "SEK", "NOK", "DKK", "PLN", "CZK", "HUF", "RUB", "TRY", "RON"})
    void filterExpandedIsoCodesPrefixAndSuffix(final String code) throws Exception {
        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new CurrencyFilterStrategy()))
                .withWindowSize(windowSize)
                .build();
        final CurrencyFilter filter = new CurrencyFilter(filterConfiguration);

        final Filtered prefix = filter.filter(contextService, getPolicy(), "context", PIECE, "Awarded " + code + " 3,000.");
        Assertions.assertEquals(1, prefix.getSpans().size());
        Assertions.assertEquals(code + " 3,000", prefix.getSpans().get(0).getText());

        final Filtered suffix = filter.filter(contextService, getPolicy(), "context", PIECE, "Awarded 3,000 " + code + ".");
        Assertions.assertEquals(1, suffix.getSpans().size());
        Assertions.assertEquals("3,000 " + code, suffix.getSpans().get(0).getText());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "In 2026 patient in room 500 took 2 tablets.",
            "Patient weighs 180 pounds.",
            "Dentist placed 2 crowns on the molars.",
            "PATIENT TO TRY 2 TABLETS DAILY.",
            "PATIENT TO TRY 2.5 MG DAILY.",
            "PATIENT TO TRY 2.55MG DAILY.",
            "PATIENT TO TRY 2.5 G DAILY.",
            "PATIENT TO TRY 1.5 L FLUIDS DAILY.",
            "APPLY AND RUB 0.5 ML TWICE DAILY.",
            "235,000 Swedish citizens voted in 2024.",
            "The student scored 80 marks on the test.",
            "Please try 2 tablets and rub 5 ml onto the skin.",
            "Please try 3,000 steps daily.",
            "PATIENT TO TRY 2ND DOSE.",
            "History of CHF 3 years ago.",
            "2 CHF admissions this year.",
            "Dyspnea 2/2 CHF exacerbation.",
            "Age 72\nCHF stable.",
            "Failed 2 chf clinics.",
            "TAKE 2 TRY AGAIN TOMORROW.",
            "APPLY 2 RUB GENTLY.",
            "SEEN 3 RON SMITH MD."
    })
    void filterCurrencyNamesNoFalsePositives(final String input) throws Exception {
        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new CurrencyFilterStrategy()))
                .withWindowSize(windowSize)
                .build();
        final CurrencyFilter filter = new CurrencyFilter(filterConfiguration);

        Assertions.assertEquals(0, filter.filter(contextService, getPolicy(), "context", PIECE, input).getSpans().size());
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "DOB 1/2/20 CHF stable. | DOB {{{REDACTED-date}}} CHF stable.",
            "DOB 1/2/20 TRY again. | DOB {{{REDACTED-date}}} TRY again.",
            "Call 555-0187 CHF stable. | Call {{{REDACTED-phone-number}}} CHF stable."
    })
    void anAmbiguousCodeDoesNotTakePartOfADateOrPhoneNumber(final String input, final String expected) throws Exception {
        final String policyJson = "{\"identifiers\": {\"date\": {}, \"phoneNumber\": {}, \"currency\": {}}}";
        final Policy policy = new Gson().fromJson(policyJson, Policy.class);
        final PlainTextFilterService service = new PlainTextFilterService(
                new PhileasConfiguration(new Properties()), contextService, new InMemoryVectorService(), null);

        Assertions.assertEquals(expected, service.filter(policy, "context", input).getFilteredText());
    }

    @Test
    void filterIssue482Policy() throws Exception {
        final String policyJson = "{\"identifiers\": {\"currency\": {\"currencyFilterStrategies\": [{\"strategy\": \"REDACT\"}]}}}";
        final Policy policy = new Gson().fromJson(policyJson, Policy.class);
        final PlainTextFilterService service = new PlainTextFilterService(
                new PhileasConfiguration(new Properties()), contextService, new InMemoryVectorService(), null);

        final String filtered = service.filter(policy, "context", "He paid 235,000 Swedish crowns and 5,000 euros.").getFilteredText();
        Assertions.assertEquals("He paid {{{REDACTED-currency}}} and {{{REDACTED-currency}}}.", filtered);
    }

}
