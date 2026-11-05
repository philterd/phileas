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
package ai.philterd.phileas.services;

import ai.philterd.phileas.PhileasConfiguration;
import ai.philterd.phileas.model.filtering.TextFilterResult;
import ai.philterd.phileas.policy.Policy;
import ai.philterd.phileas.services.context.ContextService;
import ai.philterd.phileas.services.context.DefaultContextService;
import ai.philterd.phileas.services.disambiguation.vector.InMemoryVectorService;
import ai.philterd.phileas.services.disambiguation.vector.VectorService;
import ai.philterd.phileas.services.filters.filtering.PlainTextFilterService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Properties;

import static ai.philterd.phileas.services.EndToEndTestsHelper.getPolicyWithCurrency;

/**
 * End-to-end opaque-box test suite for multi-currency redaction functionality.
 * Exercises PlainTextFilterService with real policy configurations covering:
 * Tier 1: Feature Coverage (Symbols €, £, ¥, ₹, CAD, AUD; ISO codes EUR, GBP, JPY, CAD, USD, INR; US & EU formats)
 * Tier 2: Boundary & Corner Cases (limits, zero, adjacent punctuation, non-currency negative checks)
 * Tier 3: Cross-Feature Combinations (pairwise mixed symbols/ISO/formats in single text)
 * Tier 4: Real-World Application Scenarios (medical invoices, receipts, prompt acceptance criteria)
 */
public class MultiCurrencyRedactionEndToEndTest {

    private static final Logger LOGGER = LogManager.getLogger(MultiCurrencyRedactionEndToEndTest.class);

    private ContextService contextService;
    private VectorService vectorService;
    private PlainTextFilterService filterService;
    private Policy currencyPolicy;

    @BeforeEach
    public void setup() throws Exception {
        contextService = new DefaultContextService();
        vectorService = new InMemoryVectorService();
        final PhileasConfiguration configuration = new PhileasConfiguration(new Properties());
        filterService = new PlainTextFilterService(configuration, contextService, vectorService, null);
        currencyPolicy = getPolicyWithCurrency();
    }

    // =========================================================================
    // TIER 1: Feature Coverage (Isolated Happy Path Operations - min 25 cases)
    // =========================================================================

    @ParameterizedTest(name = "Tier 1 Feature Test {index}: {0}")
    @CsvSource({
            "'Euro symbol US format', 'the cost is €1,450.00.', 'the cost is {{{REDACTED-currency}}}.'",
            "'Euro symbol EU format postfix', 'the cost is 1.450,00 €.', 'the cost is {{{REDACTED-currency}}}.'",
            "'Euro symbol EU format prefix', 'the cost is €1.500,00.', 'the cost is {{{REDACTED-currency}}}.'",
            "'Euro symbol integer', 'the cost is €50.', 'the cost is {{{REDACTED-currency}}}.'",
            "'Pound symbol US format', 'the fee is £250.00.', 'the fee is {{{REDACTED-currency}}}.'",
            "'Pound symbol EU format', 'the fee is 250,50 £.', 'the fee is {{{REDACTED-currency}}}.'",
            "'Pound symbol integer', 'the fee is £100.', 'the fee is {{{REDACTED-currency}}}.'",
            "'Yen symbol integer', 'price is ¥5000.', 'price is {{{REDACTED-currency}}}.'",
            "'Yen symbol comma format', 'price is ¥5,000.', 'price is {{{REDACTED-currency}}}.'",
            "'Yen symbol postfix', 'price is 5000 ¥.', 'price is {{{REDACTED-currency}}}.'",
            "'Rupee symbol US format', 'amount is ₹1,000.00.', 'amount is {{{REDACTED-currency}}}.'",
            "'Rupee symbol integer', 'amount is ₹500.', 'amount is {{{REDACTED-currency}}}.'",
            "'CAD code prefix', 'total CAD 150.00.', 'total {{{REDACTED-currency}}}.'",
            "'CAD code postfix', 'total 150.00 CAD.', 'total {{{REDACTED-currency}}}.'",
            "'AUD code prefix', 'fee is AUD 75.50.', 'fee is {{{REDACTED-currency}}}.'",
            "'AUD code postfix', 'fee is 75.50 AUD.', 'fee is {{{REDACTED-currency}}}.'",
            "'EUR code postfix', 'paid 500 EUR.', 'paid {{{REDACTED-currency}}}.'",
            "'EUR code prefix', 'paid EUR 500.00.', 'paid {{{REDACTED-currency}}}.'",
            "'GBP code prefix', 'balance is GBP 250.00.', 'balance is {{{REDACTED-currency}}}.'",
            "'GBP code postfix', 'balance is 250 GBP.', 'balance is {{{REDACTED-currency}}}.'",
            "'JPY code prefix', 'total JPY 10000.', 'total {{{REDACTED-currency}}}.'",
            "'JPY code postfix', 'total 10000 JPY.', 'total {{{REDACTED-currency}}}.'",
            "'USD code prefix', 'charge USD 1,234.56.', 'charge {{{REDACTED-currency}}}.'",
            "'USD code postfix', 'charge 1234.56 USD.', 'charge {{{REDACTED-currency}}}.'",
            "'INR code prefix', 'deposit INR 2,500.', 'deposit {{{REDACTED-currency}}}.'",
            "'INR code postfix', 'deposit 2500.00 INR.', 'deposit {{{REDACTED-currency}}}.'",
            "'USD symbol baseline', 'drug cost is $35.53.', 'drug cost is {{{REDACTED-currency}}}.'",
            "'USD symbol large amount', 'payment of $1,234,567.89.', 'payment of {{{REDACTED-currency}}}.'"
    })
    public void testTier1FeatureCoverage(String description, String input, String expected) throws Exception {
        final TextFilterResult result = filterService.filter(currencyPolicy, "context", input);
        LOGGER.info("Tier 1 [{}] Output: {}", description, result.getFilteredText());
        Assertions.assertEquals(expected, result.getFilteredText().trim());
    }

    // =========================================================================
    // TIER 2: Boundary & Corner Cases (min 25 cases)
    // =========================================================================

    @ParameterizedTest(name = "Tier 2 Boundary Test {index}: {0}")
    @CsvSource({
            "'Zero amount Euro symbol', 'balance is €0.00.', 'balance is {{{REDACTED-currency}}}.'",
            "'Zero amount EUR code', 'balance is 0 EUR.', 'balance is {{{REDACTED-currency}}}.'",
            "'Multi-million EU format', 'amount is 1.000.000,00 €.', 'amount is {{{REDACTED-currency}}}.'",
            "'Multi-million US format', 'amount is $10,000,000.00.', 'amount is {{{REDACTED-currency}}}.'",
            "'Single digit Pound', 'cost is £5.', 'cost is {{{REDACTED-currency}}}.'",
            "'Decimal only Euro', 'cost is €.50.', 'cost is {{{REDACTED-currency}}}.'",
            "'Trailing period boundary', 'The price is €45.00.', 'The price is {{{REDACTED-currency}}}.'",
            "'Trailing comma boundary', 'Total was 500 EUR, paid.', 'Total was {{{REDACTED-currency}}}, paid.'",
            "'Parentheses boundary', '(fee of £12.50)', '(fee of {{{REDACTED-currency}}})'",
            "'Brackets boundary', '[cost: ¥1000]', '[cost: {{{REDACTED-currency}}}]'",
            "'Leading space boundary', '  €99.99', '  {{{REDACTED-currency}}}'",
            "'Space between symbol and digits US', 'cost is € 150.00.', 'cost is {{{REDACTED-currency}}}.'",
            "'Space between symbol and digits EU', 'cost is £ 250,00.', 'cost is {{{REDACTED-currency}}}.'",
            "'Calendar year negative check', 'In the year 2026 events occurred.', 'In the year 2026 events occurred.'",
            "'Room number negative check', 'Patient moved to room 500.', 'Patient moved to room 500.'",
            "'Plain integer negative check', 'Counted 42 items today.', 'Counted 42 items today.'",
            "'Decimal quantity negative check', 'The weight is 15.5 kg.', 'The weight is 15.5 kg.'",
            "'Phone number negative check', 'Call 800-555-0199 for info.', 'Call 800-555-0199 for info.'",
            "'Zip code negative check', 'Zip code is 90210.', 'Zip code is 90210.'",
            "'Word starting with EUR negative check', 'EUROPE is a large continent.', 'EUROPE is a large continent.'",
            "'Case insensitive eur code', 'fee is eur 500.', 'fee is {{{REDACTED-currency}}}.'",
            "'Quote marks boundary', '\"€100.00\"', '\"{{{REDACTED-currency}}}\"'",
            "'Thousand EU integer boundary', 'amount 1.500 €.', 'amount {{{REDACTED-currency}}}.'"
    })
    public void testTier2BoundaryCases(String description, String input, String expected) throws Exception {
        final TextFilterResult result = filterService.filter(currencyPolicy, "context", input);
        LOGGER.info("Tier 2 [{}] Output: {}", description, result.getFilteredText());
        Assertions.assertEquals(expected, result.getFilteredText());
    }

    @Test
    public void testTier2EmptyStringBoundary() throws Exception {
        final TextFilterResult result = filterService.filter(currencyPolicy, "context", "");
        Assertions.assertEquals("", result.getFilteredText());
    }

    @Test
    public void testTier2TrailingSpaceBoundary() throws Exception {
        final TextFilterResult result = filterService.filter(currencyPolicy, "context", "50 USD  ");
        Assertions.assertEquals("{{{REDACTED-currency}}}  ", result.getFilteredText());
    }

    @Test
    public void testTier2ConsecutiveSymbolsBoundary() throws Exception {
        final TextFilterResult result = filterService.filter(currencyPolicy, "context", "€50 £100 $150");
        Assertions.assertEquals("{{{REDACTED-currency}}} {{{REDACTED-currency}}} {{{REDACTED-currency}}}", result.getFilteredText().trim());
    }

    // =========================================================================
    // TIER 3: Cross-Feature Combinations (min 10 cases)
    // =========================================================================

    @ParameterizedTest(name = "Tier 3 Cross-Feature Test {index}: {0}")
    @CsvSource({
            "'Multi-currency symbols in sentence', 'Paid €100 and £50.', 'Paid {{{REDACTED-currency}}} and {{{REDACTED-currency}}}.'",
            "'Symbol and ISO code mixed', 'Cost was $50.00 or 45.00 EUR.', 'Cost was {{{REDACTED-currency}}} or {{{REDACTED-currency}}}.'",
            "'Mixed US and EU formatting', 'Paid $1,200.50 while EU paid 1.200,50 €.', 'Paid {{{REDACTED-currency}}} while EU paid {{{REDACTED-currency}}}.'",
            "'US Symbol + EU ISO Code', 'Paid €1,450.00 and 500,00 GBP.', 'Paid {{{REDACTED-currency}}} and {{{REDACTED-currency}}}.'",
            "'Triple ISO codes in sequence', 'Convert 1000 JPY to 7.50 GBP or 8.50 EUR.', 'Convert {{{REDACTED-currency}}} to {{{REDACTED-currency}}} or {{{REDACTED-currency}}}.'",
            "'Rupee Dollar and EU Euro', 'Charges: ₹5,000, $100.00, and 90,00 €.', 'Charges: {{{REDACTED-currency}}}, {{{REDACTED-currency}}}, and {{{REDACTED-currency}}}.'",
            "'Currency with Date in text', 'On 10-19-2020 paid £150.00.', 'On 10-19-2020 paid {{{REDACTED-currency}}}.'",
            "'Baseline USD in complex context', 'Refund of $50.00 sent.', 'Refund of {{{REDACTED-currency}}} sent.'",
            "'Postfix symbol and prefix ISO code', 'Amount 100 € or GBP 85.', 'Amount {{{REDACTED-currency}}} or {{{REDACTED-currency}}}.'",
            "'Triple symbols with punctuation', 'Totals: €1.500,00, £250.00, $50.', 'Totals: {{{REDACTED-currency}}}, {{{REDACTED-currency}}}, {{{REDACTED-currency}}}.'"
    })
    public void testTier3CrossFeatureCombinations(String description, String input, String expected) throws Exception {
        final TextFilterResult result = filterService.filter(currencyPolicy, "context", input);
        LOGGER.info("Tier 3 [{}] Output: {}", description, result.getFilteredText());
        Assertions.assertEquals(expected, result.getFilteredText().trim());
    }

    // =========================================================================
    // TIER 4: Real-World Application Scenarios (min 5 cases)
    // =========================================================================

    @Test
    public void testTier4AcceptanceCriteriaPromptExample() throws Exception {
        // Acceptance Criteria Example from ORIGINAL_REQUEST.md:
        // Input: "The surgery balance is €1.500,00 and consultation fee is 250 GBP."
        // Expected: "The surgery balance is {{{REDACTED-currency}}} and consultation fee is {{{REDACTED-currency}}}."
        final String input = "The surgery balance is €1.500,00 and consultation fee is 250 GBP.";
        final TextFilterResult result = filterService.filter(currencyPolicy, "context", input);
        LOGGER.info("Tier 4 Acceptance Criteria Example Output: {}", result.getFilteredText());
        Assertions.assertEquals("The surgery balance is {{{REDACTED-currency}}} and consultation fee is {{{REDACTED-currency}}}.", result.getFilteredText().trim());
    }

    @Test
    public void testTier4MedicalBillingStatement() throws Exception {
        final String input = "Patient John Doe has an outstanding hospital bill of $3,450.75 USD. Insurance covered €2.000,00 and remaining copay is £250.00.";
        final TextFilterResult result = filterService.filter(currencyPolicy, "context", input);
        LOGGER.info("Tier 4 Medical Billing Output: {}", result.getFilteredText());
        Assertions.assertEquals("Patient John Doe has an outstanding hospital bill of {{{REDACTED-currency}}}. Insurance covered {{{REDACTED-currency}}} and remaining copay is {{{REDACTED-currency}}}.", result.getFilteredText().trim());
    }

    @Test
    public void testTier4InternationalTravelReceipt() throws Exception {
        final String input = "Hotel room: 15,000 JPY per night. Dining: €85.50. Taxi fare: 45.00 CAD. Total expense: $320.00.";
        final TextFilterResult result = filterService.filter(currencyPolicy, "context", input);
        LOGGER.info("Tier 4 Travel Receipt Output: {}", result.getFilteredText());
        Assertions.assertEquals("Hotel room: {{{REDACTED-currency}}} per night. Dining: {{{REDACTED-currency}}}. Taxi fare: {{{REDACTED-currency}}}. Total expense: {{{REDACTED-currency}}}.", result.getFilteredText().trim());
    }

    @Test
    public void testTier4ClinicalTrialInvoice() throws Exception {
        final String input = "Trial Site EU-01 invoice: €12.500,00. Site UK-02 invoice: £8,900.00. Site IN-03 invoice: ₹75,000.00. Total budget allocation: USD 35,000.00.";
        final TextFilterResult result = filterService.filter(currencyPolicy, "context", input);
        LOGGER.info("Tier 4 Clinical Trial Invoice Output: {}", result.getFilteredText());
        Assertions.assertEquals("Trial Site EU-01 invoice: {{{REDACTED-currency}}}. Site UK-02 invoice: {{{REDACTED-currency}}}. Site IN-03 invoice: {{{REDACTED-currency}}}. Total budget allocation: {{{REDACTED-currency}}}.", result.getFilteredText().trim());
    }

    @Test
    public void testTier4ECommerceOrderSummary() throws Exception {
        final String input = "Subtotal: 120,00 € | Shipping: 15.00 AUD | Tax: £10.50 | Grand Total: €145,50.";
        final TextFilterResult result = filterService.filter(currencyPolicy, "context", input);
        LOGGER.info("Tier 4 E-Commerce Summary Output: {}", result.getFilteredText());
        Assertions.assertEquals("Subtotal: {{{REDACTED-currency}}} | Shipping: {{{REDACTED-currency}}} | Tax: {{{REDACTED-currency}}} | Grand Total: {{{REDACTED-currency}}}.", result.getFilteredText().trim());
    }
}
