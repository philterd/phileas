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
import ai.philterd.phileas.model.filtering.BinaryDocumentFilterResult;
import ai.philterd.phileas.model.filtering.IncrementalRedaction;
import ai.philterd.phileas.model.filtering.FilterType;
import ai.philterd.phileas.model.filtering.MimeType;
import ai.philterd.phileas.model.filtering.Span;
import ai.philterd.phileas.model.filtering.TextFilterResult;
import ai.philterd.phileas.policy.Identifiers;
import ai.philterd.phileas.policy.Policy;
import ai.philterd.phileas.policy.filters.BitcoinAddress;
import ai.philterd.phileas.policy.filters.CreditCard;
import ai.philterd.phileas.policy.filters.CustomDictionary;
import ai.philterd.phileas.policy.filters.DriversLicense;
import ai.philterd.phileas.policy.filters.PassportNumber;
import ai.philterd.phileas.services.context.ContextService;
import ai.philterd.phileas.services.context.DefaultContextService;
import ai.philterd.phileas.services.disambiguation.vector.InMemoryVectorService;
import ai.philterd.phileas.services.disambiguation.vector.VectorService;
import ai.philterd.phileas.services.filters.filtering.PdfFilterService;
import ai.philterd.phileas.services.filters.filtering.PlainTextFilterService;
import ai.philterd.phileas.services.strategies.AbstractFilterStrategy;
import ai.philterd.phileas.services.strategies.custom.CustomDictionaryFilterStrategy;
import ai.philterd.phileas.services.strategies.rules.BitcoinAddressFilterStrategy;
import ai.philterd.phileas.services.strategies.rules.CreditCardFilterStrategy;
import ai.philterd.phileas.services.strategies.rules.DriversLicenseFilterStrategy;
import ai.philterd.phileas.services.strategies.rules.PassportNumberFilterStrategy;
import com.google.gson.GsonBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;

import static ai.philterd.phileas.services.EndToEndTestsHelper.getPolicy;
import static ai.philterd.phileas.services.EndToEndTestsHelper.getPolicyJustCreditCard;
import static ai.philterd.phileas.services.EndToEndTestsHelper.getPolicyJustCreditCardNotInUnixTimestamps;
import static ai.philterd.phileas.services.EndToEndTestsHelper.getPolicyJustPhoneNumber;
import static ai.philterd.phileas.services.EndToEndTestsHelper.getPolicyJustStreetAddress;
import static ai.philterd.phileas.services.EndToEndTestsHelper.getPolicyZipCodeWithIgnored;

public class EndToEndTests {

    private static final Logger LOGGER = LogManager.getLogger(EndToEndTests.class);

    // Use the real services rather than mocks so these end-to-end tests exercise the actual
    // replacement-context and vector wiring instead of no-op stubs.
    private final ContextService contextService = new DefaultContextService();
    private final VectorService vectorService = new InMemoryVectorService();
    
    @BeforeEach
    public void before() {
        final GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(String.class, new PlaceholderDeserializer());
    }

    @Test
    public void endToEndPdf1() throws Exception {

        final Properties properties = new Properties();
        properties.setProperty("incremental.redactions.enabled", "true");

        final PhileasConfiguration phileasConfiguration = new PhileasConfiguration(properties);

        final Policy policy = getPolicy();

        final byte[] document = Files.readAllBytes(Paths.get(this.getClass().getResource("/pdfs/12-12110 K.pdf").toURI()));

        final PdfFilterService service = new PdfFilterService(phileasConfiguration, contextService, vectorService, null);
        final BinaryDocumentFilterResult response = service.filter(policy, "context", document, MimeType.APPLICATION_PDF);

        Assertions.assertFalse(response.getIncrementalRedactions().isEmpty());

        for(final IncrementalRedaction incrementalRedaction : response.getIncrementalRedactions()) {
            LOGGER.info(incrementalRedaction.toString());
        }

    }

    @Test
    public void endToEnd2() throws Exception {

        final Properties properties = new Properties();
        final PhileasConfiguration phileasConfiguration = new PhileasConfiguration(properties);

        final Policy policy = getPolicy();

        final PlainTextFilterService service = new PlainTextFilterService(phileasConfiguration, contextService, vectorService, null);
        final TextFilterResult response = service.filter(policy, "context", "My email is test@something.com and cc is 4121742025464465");

        LOGGER.info(response.getFilteredText());

        Assertions.assertEquals("My email is {{{REDACTED-email-address}}} and cc is {{{REDACTED-credit-card}}}", response.getFilteredText());
        

    }

    @Test
    public void endToEnd3() throws Exception {

        final Properties properties = new Properties();
        final PhileasConfiguration phileasConfiguration = new PhileasConfiguration(properties);

        final Policy policy = getPolicy();

        final PlainTextFilterService service = new PlainTextFilterService(phileasConfiguration, contextService, vectorService, null);
        final TextFilterResult response = service.filter(policy, "context", "test@something.com is email and cc is 4121742025464465");

        LOGGER.info(response.getFilteredText());

        Assertions.assertEquals("{{{REDACTED-email-address}}} is email and cc is {{{REDACTED-credit-card}}}", response.getFilteredText());
        

    }

    @Test
    public void endToEnd4() throws Exception {

        final Properties properties = new Properties();
        final PhileasConfiguration phileasConfiguration = new PhileasConfiguration(properties);

        final Policy policy = getPolicy();

        final PlainTextFilterService service = new PlainTextFilterService(phileasConfiguration, contextService, vectorService, null);
        final TextFilterResult response = service.filter(policy, "context", "test@something.com");

        LOGGER.info(response.getFilteredText());

        Assertions.assertEquals("{{{REDACTED-email-address}}}", response.getFilteredText());
        

    }

    @Test
    public void endToEnd5() throws Exception {

        final Properties properties = new Properties();
        final PhileasConfiguration phileasConfiguration = new PhileasConfiguration(properties);

        final Policy policy = getPolicy();

        final PlainTextFilterService service = new PlainTextFilterService(phileasConfiguration, contextService, vectorService, null);
        final TextFilterResult response = service.filter(policy, "context", "90210");

        LOGGER.info(response.getFilteredText());

        Assertions.assertEquals("{{{REDACTED-zip-code}}}", response.getFilteredText());
        

    }

    @Test
    public void endToEnd7() throws Exception {

        final Properties properties = new Properties();
        final PhileasConfiguration phileasConfiguration = new PhileasConfiguration(properties);

        final Policy policy = getPolicy();

        final PlainTextFilterService service = new PlainTextFilterService(phileasConfiguration, contextService, vectorService, null);
        final TextFilterResult response = service.filter(policy, "context", "he was seen on 10-19-2020.");

        LOGGER.info(response.getFilteredText());

        Assertions.assertEquals("he was seen on 12-20-2023.", response.getFilteredText());
        

    }

    @Test
    public void endToEnd8() throws Exception {

        final Properties properties = new Properties();
        final PhileasConfiguration phileasConfiguration = new PhileasConfiguration(properties);

        final Policy policy = getPolicy();

        final PlainTextFilterService service = new PlainTextFilterService(phileasConfiguration, contextService, vectorService, null);
        final TextFilterResult response = service.filter(policy, "context",
                "George Washington was president." + System.lineSeparator() + "Abraham Lincoln was president.");

        LOGGER.info(response.getFilteredText());

        // Ensure that the line separator in the input text was not removed.
        Assertions.assertTrue(response.getFilteredText().contains(System.lineSeparator()));

    }

    @Test
    public void endToEnd11() throws Exception {

        final Properties properties = new Properties();
        final PhileasConfiguration phileasConfiguration = new PhileasConfiguration(properties);

        final Policy policy = getPolicy();

        final String input = "IN THE UNITED STATES DISTRICT COURT \nEASTERN DISTRICT OF ARKANSAS \nWESTERN DIVISION \nJAMES EDWARD SMITH, \nafk/a James Edward Bridges, \nADC#103093 \nv. No. 4:14-cv-455-DPM \nPLAINTIFF \nCHARLES A. SMITH; \nMARY ANN CONLEY, \nafk/a Mary Ann Smith; and \nROBERT CASTILLOW DEFENDANTS \nORDER \nJames Smith's prose complaint must be dismissed without prejudice. \nHe hasn't paid the filing fee, moved to proceed in forma pauperis, or provided \nproof of service on any defendant. FED. R. CIV. P. 4(I); Local Rule 5.5(c)(2). \nSo Ordered. \nD.P. Marshall Jr. \nUnited States District Judge \nCase 4:14-cv-00455-DPM   Document 2   Filed 12/09/14   Page 1 of 1\n";

        final PlainTextFilterService service = new PlainTextFilterService(phileasConfiguration, contextService, vectorService, null);
        final TextFilterResult response = service.filter(policy, "context", input);

        LOGGER.info(response.getFilteredText());

        //Assertions.assertEquals("George Washington was president and his ssn was {{{REDACTED-ssn}}} and he lived at {{{REDACTED-zip-code}}}. The name 456 should be filtered. Jeff Smith should be ignored.", response.getFilteredText());
        

    }

    @Test
    public void endToEnd17() throws Exception {

        final Properties properties = new Properties();
        final PhileasConfiguration phileasConfiguration = new PhileasConfiguration(properties);

        final Policy policy = getPolicyJustStreetAddress();

        final String input = "he lived at 100 main street";

        final PlainTextFilterService service = new PlainTextFilterService(phileasConfiguration, contextService, vectorService, null);
        final TextFilterResult response = service.filter(policy, "context", input);

        LOGGER.info(response.getFilteredText());

        showSpans(response.getExplanation().appliedSpans());

        
        Assertions.assertEquals(1, response.getExplanation().appliedSpans().size());
        Assertions.assertEquals("he lived at {{{REDACTED-street-address}}}", response.getFilteredText().trim());

    }

    @Test
    public void endToEndWithOverlappingSpans() throws Exception {

        BitcoinAddressFilterStrategy bitcoinAddressFilterStrategy = new BitcoinAddressFilterStrategy();
        bitcoinAddressFilterStrategy.setStrategy(AbstractFilterStrategy.MASK);
        bitcoinAddressFilterStrategy.setMaskCharacter("*");
        bitcoinAddressFilterStrategy.setMaskLength("same");
        BitcoinAddress bitcoinAddress = new BitcoinAddress();
        bitcoinAddress.setBitcoinFilterStrategies(List.of(bitcoinAddressFilterStrategy));

        CreditCardFilterStrategy creditCardFilterStrategy = new CreditCardFilterStrategy();
        creditCardFilterStrategy.setStrategy(AbstractFilterStrategy.MASK);
        creditCardFilterStrategy.setMaskCharacter("*");
        creditCardFilterStrategy.setMaskLength("same");
        CreditCard creditCard = new CreditCard();
        creditCard.setCreditCardFilterStrategies(List.of(creditCardFilterStrategy));

        DriversLicenseFilterStrategy driversLicenseFilterStrategy = new DriversLicenseFilterStrategy();
        driversLicenseFilterStrategy.setStrategy(AbstractFilterStrategy.MASK);
        driversLicenseFilterStrategy.setMaskCharacter("*");
        driversLicenseFilterStrategy.setMaskLength("same");
        DriversLicense driversLicense = new DriversLicense();
        driversLicense.setDriversLicenseFilterStrategies(List.of(driversLicenseFilterStrategy));

        Identifiers identifiers = new Identifiers();
        identifiers.setBitcoinAddress(bitcoinAddress);
        identifiers.setCreditCard(creditCard);
        identifiers.setDriversLicense(driversLicense);

        final Policy policy = new Policy();
        policy.setIdentifiers(identifiers);
        Properties properties = new Properties();
        PhileasConfiguration configuration = new PhileasConfiguration(properties);

        final String input = "the payment method is 4532613702852251 visa or 1Lbcfr7sAHTD9CgdQo3HTMTkV8LK4ZnX71 BTC from user.";

        final PlainTextFilterService service = new PlainTextFilterService(configuration, contextService, vectorService, null);
        final TextFilterResult response = service.filter(policy, "context", input);

        LOGGER.info(response.getFilteredText());

        showSpans(response.getExplanation().appliedSpans());

        Assertions.assertEquals(2, response.getExplanation().appliedSpans().size());
        Assertions.assertEquals("the payment method is **************** visa or ********************************** BTC from user.", response.getFilteredText());

        // We do NOT want driver's license to show up as a span.
        for(final Span span : response.getExplanation().appliedSpans()) {
            Assertions.assertTrue(span.getFilterType().getType().equals("bitcoin-address") || span.getFilterType().getType().equals("credit-card"));
        }

    }

    @Test
    public void endToEndJustPhoneNumbers() throws Exception {

        final Properties properties = new Properties();
        final PhileasConfiguration phileasConfiguration = new PhileasConfiguration(properties);

        final Policy policy = getPolicyJustPhoneNumber();

        final String input = "his number is 123-456-7890. her number is 9999999999. her number is 102-304-5678.";

        final PlainTextFilterService service = new PlainTextFilterService(phileasConfiguration, contextService, vectorService, null);
        final TextFilterResult response = service.filter(policy, "context", input);

        LOGGER.info(response.getFilteredText());

        LOGGER.info("Identified spans:");
        showSpans(response.getExplanation().identifiedSpans());

        LOGGER.info("Applied spans:");
        showSpans(response.getExplanation().appliedSpans());

        
        Assertions.assertEquals(1, response.getExplanation().appliedSpans().size());
        Assertions.assertEquals(3, response.getExplanation().identifiedSpans().size());
        Assertions.assertEquals("his number is {{{REDACTED-phone-number}}}. her number is 9999999999. her number is 102-304-5678.", response.getFilteredText().trim());

    }

    @Test
    public void endToEndPhoneNumberRegionGb() throws Exception {

        final Properties properties = new Properties();
        final PhileasConfiguration phileasConfiguration = new PhileasConfiguration(properties);

        // Parse from JSON so the region deserialization, the FilterPolicyLoader wiring, and the filter
        // are all exercised together on the real load path.
        final String json = """
                {
                  "identifiers": {
                    "phoneNumber": {
                      "region": "GB",
                      "phoneNumberFilterStrategies": [ { "strategy": "REDACT" } ]
                    }
                  }
                }
                """;
        final Policy policy = new com.google.gson.Gson().fromJson(json, Policy.class);

        final String input = "his UK number is 020 7946 0958.";

        final PlainTextFilterService service = new PlainTextFilterService(phileasConfiguration, contextService, vectorService, null);
        final TextFilterResult response = service.filter(policy, "context", input);

        LOGGER.info(response.getFilteredText());

        Assertions.assertEquals("his UK number is {{{REDACTED-phone-number}}}.", response.getFilteredText().trim());

    }

    @Test
    public void endToEndEin() throws Exception {

        final Properties properties = new Properties();
        final PhileasConfiguration phileasConfiguration = new PhileasConfiguration(properties);

        // Parse from JSON so deserialization, the FilterPolicyLoader wiring, and the filter run together.
        final String json = """
                {
                  "identifiers": {
                    "ein": {
                      "einFilterStrategies": [ { "strategy": "REDACT" } ]
                    }
                  }
                }
                """;
        final Policy policy = new com.google.gson.Gson().fromJson(json, Policy.class);

        final PlainTextFilterService service = new PlainTextFilterService(phileasConfiguration, contextService, vectorService, null);
        final TextFilterResult response = service.filter(policy, "context", "the ein is 12-3456789.");

        Assertions.assertEquals("the ein is {{{REDACTED-ein}}}.", response.getFilteredText().trim());

    }

    @Test
    public void endToEndEinOnlyValidPrefixes() throws Exception {

        final Properties properties = new Properties();
        final PhileasConfiguration phileasConfiguration = new PhileasConfiguration(properties);

        final String json = """
                {
                  "identifiers": {
                    "ein": {
                      "onlyValidPrefixes": true,
                      "einFilterStrategies": [ { "strategy": "REDACT" } ]
                    }
                  }
                }
                """;
        final Policy policy = new com.google.gson.Gson().fromJson(json, Policy.class);

        final PlainTextFilterService service = new PlainTextFilterService(phileasConfiguration, contextService, vectorService, null);

        // 07 is not an IRS-issued prefix, so with onlyValidPrefixes on it is left in the clear.
        final TextFilterResult invalid = service.filter(policy, "context", "the ein is 07-1234567.");
        Assertions.assertEquals("the ein is 07-1234567.", invalid.getFilteredText().trim());

        // 12 is a valid prefix, so it is redacted.
        final TextFilterResult valid = service.filter(policy, "context", "the ein is 12-3456789.");
        Assertions.assertEquals("the ein is {{{REDACTED-ein}}}.", valid.getFilteredText().trim());

    }

    @Test
    public void endToEndItinAndSsn() throws Exception {

        final Properties properties = new Properties();
        final PhileasConfiguration phileasConfiguration = new PhileasConfiguration(properties);

        // With ssn, ein, and itin all enabled, an ITIN is reported only as itin and an SSN only as ssn.
        final String json = """
                {
                  "identifiers": {
                    "ssn": {
                      "ssnFilterStrategies": [ { "strategy": "REDACT" } ]
                    },
                    "ein": {
                      "einFilterStrategies": [ { "strategy": "REDACT" } ]
                    },
                    "itin": {
                      "itinFilterStrategies": [ { "strategy": "REDACT" } ]
                    }
                  }
                }
                """;
        final Policy policy = new com.google.gson.Gson().fromJson(json, Policy.class);

        final PlainTextFilterService service = new PlainTextFilterService(phileasConfiguration, contextService, vectorService, null);

        for (final String itin : List.of("912-70-1234", "912 70 1234", "912701234")) {
            final TextFilterResult response = service.filter(policy, "context", "the itin is " + itin + ".");
            Assertions.assertEquals("the itin is {{{REDACTED-itin}}}.", response.getFilteredText().trim(), itin);
        }

        final TextFilterResult ssn = service.filter(policy, "context", "the ssn is 123-45-6789.");
        Assertions.assertEquals("the ssn is {{{REDACTED-ssn}}}.", ssn.getFilteredText().trim());

    }

    @Test
    public void endToEndItinOnlyValidRanges() throws Exception {

        final Properties properties = new Properties();
        final PhileasConfiguration phileasConfiguration = new PhileasConfiguration(properties);

        final String json = """
                {
                  "identifiers": {
                    "itin": {
                      "onlyValidRanges": true,
                      "itinFilterStrategies": [ { "strategy": "REDACT" } ]
                    }
                  }
                }
                """;
        final Policy policy = new com.google.gson.Gson().fromJson(json, Policy.class);

        final PlainTextFilterService service = new PlainTextFilterService(phileasConfiguration, contextService, vectorService, null);

        // An ATIN (93) is outside the ITIN ranges, so with onlyValidRanges on it is left in the clear.
        final TextFilterResult atin = service.filter(policy, "context", "the number is 912-93-1234.");
        Assertions.assertEquals("the number is 912-93-1234.", atin.getFilteredText().trim());

        final TextFilterResult itin = service.filter(policy, "context", "the number is 912-70-1234.");
        Assertions.assertEquals("the number is {{{REDACTED-itin}}}.", itin.getFilteredText().trim());

    }

    @Test
    public void endToEndCanadaSin() throws Exception {

        final Properties properties = new Properties();
        final PhileasConfiguration phileasConfiguration = new PhileasConfiguration(properties);

        final String json = """
                {
                  "identifiers": {
                    "canadaSin": {
                      "canadaSinFilterStrategies": [ { "strategy": "REDACT" } ]
                    }
                  }
                }
                """;
        final Policy policy = new com.google.gson.Gson().fromJson(json, Policy.class);

        final PlainTextFilterService service = new PlainTextFilterService(phileasConfiguration, contextService, vectorService, null);

        for (final String sin : List.of("046454286", "046 454 286", "046-454-286", "912-345-675")) {
            final TextFilterResult response = service.filter(policy, "context", "the sin is " + sin + ".");
            Assertions.assertEquals("the sin is {{{REDACTED-canada-sin}}}.", response.getFilteredText().trim(), sin);
        }

        // A Luhn-invalid value is left in the clear.
        final TextFilterResult invalid = service.filter(policy, "context", "the number 123 456 789 is not a sin.");
        Assertions.assertEquals("the number 123 456 789 is not a sin.", invalid.getFilteredText().trim());

    }

    @Test
    public void endToEndCanadaSinOnlyValidPrefixes() throws Exception {

        final Properties properties = new Properties();
        final PhileasConfiguration phileasConfiguration = new PhileasConfiguration(properties);

        final String json = """
                {
                  "identifiers": {
                    "canadaSin": {
                      "onlyValidPrefixes": true,
                      "canadaSinFilterStrategies": [ { "strategy": "REDACT" } ]
                    }
                  }
                }
                """;
        final Policy policy = new com.google.gson.Gson().fromJson(json, Policy.class);

        final PlainTextFilterService service = new PlainTextFilterService(phileasConfiguration, contextService, vectorService, null);

        // A value beginning with 8 (the business number range) is left in the clear.
        final TextFilterResult business = service.filter(policy, "context", "the number is 812 345 676.");
        Assertions.assertEquals("the number is 812 345 676.", business.getFilteredText().trim());

        final TextFilterResult sin = service.filter(policy, "context", "the sin is 912 345 675.");
        Assertions.assertEquals("the sin is {{{REDACTED-canada-sin}}}.", sin.getFilteredText().trim());

    }

    @Test
    public void endToEndCanadaSinAndSsn() throws Exception {

        final Properties properties = new Properties();
        final PhileasConfiguration phileasConfiguration = new PhileasConfiguration(properties);

        // With both enabled the existing span disambiguation applies; no SIN-specific rule.
        final String json = """
                {
                  "identifiers": {
                    "ssn": {
                      "ssnFilterStrategies": [ { "strategy": "REDACT" } ]
                    },
                    "canadaSin": {
                      "canadaSinFilterStrategies": [ { "strategy": "REDACT" } ]
                    }
                  }
                }
                """;
        final Policy policy = new com.google.gson.Gson().fromJson(json, Policy.class);

        final PlainTextFilterService service = new PlainTextFilterService(phileasConfiguration, contextService, vectorService, null);

        // Hyphenated forms never collide.
        Assertions.assertEquals("the sin is {{{REDACTED-canada-sin}}}.", service.filter(policy, "context", "the sin is 046-454-286.").getFilteredText().trim());
        Assertions.assertEquals("the ssn is {{{REDACTED-ssn}}}.", service.filter(policy, "context", "the ssn is 123-45-6789.").getFilteredText().trim());

        // An unformatted value matching both is redacted as one of them.
        final String both = service.filter(policy, "context", "the number is 046454286.").getFilteredText().trim();
        Assertions.assertTrue(both.equals("the number is {{{REDACTED-canada-sin}}}.") || both.equals("the number is {{{REDACTED-ssn}}}."), both);

    }

    /** Filters the text with a policy that applies RANDOM_REPLACE, with the given extra properties, to one filter. */
    private String randomReplace(final String filter, final String strategyProperties, final String text) throws Exception {

        final String json = "{ \"identifiers\": { \"" + filter + "\": { \"" + filter + "FilterStrategies\": [ "
                + "{ \"strategy\": \"RANDOM_REPLACE\"" + strategyProperties + " } ] } } }";
        final Policy policy = new com.google.gson.Gson().fromJson(json, Policy.class);

        final PlainTextFilterService service = new PlainTextFilterService(new PhileasConfiguration(new Properties()), contextService, vectorService, null);

        return service.filter(policy, "context", text).getFilteredText().trim();

    }

    private static final String UUID_PATTERN = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";

    @Test
    public void endToEndRandomReplaceRealisticByDefault() throws Exception {

        for (final String method : List.of("", ", \"anonymizationMethod\": \"REALISTIC\"")) {

            final String ssn = randomReplace("ssn", method, "the ssn is 123-45-6789.");
            Assertions.assertTrue(ssn.matches("the ssn is \\d{3}-\\d{2}-\\d{4}\\."), ssn);
            Assertions.assertNotEquals("the ssn is 123-45-6789.", ssn);

            final String email = randomReplace("emailAddress", method, "write to jane@example.com today.");
            Assertions.assertTrue(email.matches("write to [^\\s@]+@[^\\s@]+\\.[a-z]+ today\\."), email);
            Assertions.assertFalse(email.contains("jane@example.com"), email);

            final String itin = randomReplace("itin", method, "the itin is 912-70-1234.");
            Assertions.assertTrue(itin.matches("the itin is 9\\d{2}-\\d{2}-\\d{4}\\."), itin);
            Assertions.assertNotEquals("the itin is 912-70-1234.", itin);

        }

    }

    @Test
    public void endToEndRandomReplaceTokenItCannotChange() {

        // An age in words has no digits to replace, and an all-zero IP address was the only value the IP
        // generator produced, so the realistic value could only equal the token: this used to loop
        // forever. It now falls back to a UUID.
        Assertions.assertTimeoutPreemptively(java.time.Duration.ofSeconds(30), () -> {

            final String age = randomReplace("age", "", "she is thirty-five years old.");
            Assertions.assertTrue(age.matches("she is " + UUID_PATTERN + "\\."), age);

            final String ipv4 = randomReplace("ipAddress", "", "the ip is 000.000.000.000 here.");
            Assertions.assertTrue(ipv4.matches("the ip is \\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3} here\\."), ipv4);
            Assertions.assertFalse(ipv4.contains("000.000.000.000"), ipv4);

            final String ipv6 = randomReplace("ipAddress", "", "the ip is 0000:0000:0000:0000:0000:0000:0000:0000 here.");
            Assertions.assertFalse(ipv6.contains("0000:0000:0000:0000:0000:0000:0000:0000"), ipv6);

        });

    }

    @Test
    public void endToEndRandomReplaceUuid() throws Exception {

        for (final String method : List.of(", \"anonymizationMethod\": \"UUID\"", ", \"anonymizationMethod\": \"UUID\", \"anonymizationCandidates\": [ \"000-00-0001\" ]")) {
            final String ssn = randomReplace("ssn", method, "the ssn is 123-45-6789.");
            Assertions.assertTrue(ssn.matches("the ssn is " + UUID_PATTERN + "\\."), ssn);
        }

    }

    @Test
    public void endToEndRandomReplaceFromList() throws Exception {

        // Candidates are used with FROM_LIST, and also when no method is given.
        for (final String method : List.of(", \"anonymizationMethod\": \"FROM_LIST\"", "")) {

            final String ssn = randomReplace("ssn", method + ", \"anonymizationCandidates\": [ \"000-00-0001\", \"000-00-0002\" ]", "the ssn is 123-45-6789.");
            Assertions.assertTrue(ssn.equals("the ssn is 000-00-0001.") || ssn.equals("the ssn is 000-00-0002."), ssn);

        }

    }

    @Test
    public void endToEndRandomReplaceFromEmptyListIsUuid() throws Exception {

        for (final String candidates : List.of("", ", \"anonymizationCandidates\": [ ]")) {
            final String ssn = randomReplace("ssn", ", \"anonymizationMethod\": \"FROM_LIST\"" + candidates, "the ssn is 123-45-6789.");
            Assertions.assertTrue(ssn.matches("the ssn is " + UUID_PATTERN + "\\."), ssn);
        }

    }

    @Test
    public void endToEndRandomReplaceUnrecognizedMethodIsUuid() throws Exception {

        final String ssn = randomReplace("ssn", ", \"anonymizationMethod\": \"BOGUS\"", "the ssn is 123-45-6789.");
        Assertions.assertTrue(ssn.matches("the ssn is " + UUID_PATTERN + "\\."), ssn);

    }

    @Test
    public void endToEndEinSsnDisambiguation() throws Exception {

        final Properties properties = new Properties();
        final PhileasConfiguration phileasConfiguration = new PhileasConfiguration(properties);

        // Both ein and ssn are enabled. The canonical EIN form (NN-NNNNNNN) resolves to EIN, while the
        // canonical SSN form (NNN-NN-NNNN) and bare nine-digit runs resolve to SSN.
        final String json = """
                {
                  "identifiers": {
                    "ein": { "einFilterStrategies": [ { "strategy": "REDACT" } ] },
                    "ssn": { "ssnFilterStrategies": [ { "strategy": "REDACT" } ] }
                  }
                }
                """;
        final Policy policy = new com.google.gson.Gson().fromJson(json, Policy.class);

        final PlainTextFilterService service = new PlainTextFilterService(phileasConfiguration, contextService, vectorService, null);
        final TextFilterResult response = service.filter(policy, "context", "ein 12-3456789 and ssn 123-45-6789.");

        Assertions.assertEquals("ein {{{REDACTED-ein}}} and ssn {{{REDACTED-ssn}}}.", response.getFilteredText().trim());

    }

    @Test
    public void endToEndAdjacentSsnsWithNoSeparator() throws Exception {

        // https://github.com/philterd/phileas/issues/343
        // Two SSNs written with no separator between them used to defeat detection, leaving
        // "123-" and "-45-6789" in the output.

        final Properties properties = new Properties();
        final PhileasConfiguration phileasConfiguration = new PhileasConfiguration(properties);

        final String json = """
                {
                  "identifiers": {
                    "ssn": { "ssnFilterStrategies": [ { "strategy": "REDACT" } ] }
                  }
                }
                """;
        final Policy policy = new com.google.gson.Gson().fromJson(json, Policy.class);

        final PlainTextFilterService service = new PlainTextFilterService(phileasConfiguration, contextService, vectorService, null);
        final TextFilterResult response = service.filter(policy, "context", "the ssn is 123-45-6789123-45-6789.");

        Assertions.assertEquals("the ssn is {{{REDACTED-ssn}}}.", response.getFilteredText().trim());

    }

    @Test
    public void endToEndWithPolicyAsObject() throws Exception {

        final Policy policy = getPolicyJustStreetAddress();

        final Properties properties = new Properties();
        final PhileasConfiguration phileasConfiguration = new PhileasConfiguration(properties);

        final String input = "he lived at 100 main street";

        final PlainTextFilterService service = new PlainTextFilterService(phileasConfiguration, contextService, vectorService, null);
        final TextFilterResult response = service.filter(policy, "context", input);

        LOGGER.info(response.getFilteredText());

        showSpans(response.getExplanation().appliedSpans());

        
        Assertions.assertEquals(1, response.getExplanation().appliedSpans().size());
        Assertions.assertEquals("he lived at {{{REDACTED-street-address}}}", response.getFilteredText().trim());

    }
    @Test
    public void endToEndUsingCustomDictionary() throws Exception {

        final CustomDictionary customDictionary = new CustomDictionary();
        customDictionary.setCustomDictionaryFilterStrategies(List.of(new CustomDictionaryFilterStrategy()));
        customDictionary.setTerms(List.of("george", "samuel"));

        final Policy policy = new Policy();
        policy.getIdentifiers().setCustomDictionaries(List.of(customDictionary));

        final Properties properties = new Properties();
        final PhileasConfiguration phileasConfiguration = new PhileasConfiguration(properties);

        final PlainTextFilterService service = new PlainTextFilterService(phileasConfiguration, contextService, vectorService, null);
        // The non-fuzzy (bloom) dictionary filter matches whole whitespace-delimited tokens, so
        // the terms are kept clear of trailing punctuation here.
        final TextFilterResult response = service.filter(policy, "context", "his name was samuel and george");

        LOGGER.info(response.getFilteredText());

        Assertions.assertEquals("his name was {{{REDACTED-custom-dictionary}}} and {{{REDACTED-custom-dictionary}}}", response.getFilteredText());


    }

    @Test
    public void endToEndUsingCustomDictionaryFileFuzzyDictionaryFilter() throws Exception {

        final Path temp = Files.createTempDirectory("philter");

        final String terms = new String(this.getClass().getResourceAsStream("/customdictionaries/terms1.txt").readAllBytes(), Charset.defaultCharset());
        final File termsFile = Paths.get(temp.toFile().getAbsolutePath(), "terms1.txt").toFile();
        Files.writeString(termsFile.toPath(), terms, Charset.defaultCharset());
        LOGGER.info("Terms file written to {}", termsFile.getAbsolutePath());

        final CustomDictionary customDictionary = new CustomDictionary();
        customDictionary.setFiles(List.of(termsFile.getAbsolutePath()));
        customDictionary.setCustomDictionaryFilterStrategies(List.of(new CustomDictionaryFilterStrategy()));
        customDictionary.setClassification("names");
        customDictionary.setTerms(List.of("george"));
        // The fuzzy filter matches on word boundaries, so it tolerates punctuation adjacent to a term.
        customDictionary.setFuzzy(true);

        final Policy policy = new Policy();
        policy.getIdentifiers().setCustomDictionaries(List.of(customDictionary));

        final Properties properties = new Properties();
        final PhileasConfiguration phileasConfiguration = new PhileasConfiguration(properties);

        final PlainTextFilterService service = new PlainTextFilterService(phileasConfiguration, contextService, vectorService, null);
        final TextFilterResult response = service.filter(policy, "context", "his name was samuel and george.");

        LOGGER.info(response.getFilteredText());

        Assertions.assertEquals("his name was {{{REDACTED-custom-dictionary}}} and {{{REDACTED-custom-dictionary}}}.", response.getFilteredText());
        

    }

    @Test
    public void endToEndUsingCustomDictionaryFileBloomFilter() throws Exception {

        final Path temp = Files.createTempDirectory("philter");

        final String terms = new String(this.getClass().getResourceAsStream("/customdictionaries/terms1.txt").readAllBytes(), Charset.defaultCharset());
        final File termsFile = Paths.get(temp.toFile().getAbsolutePath(), "terms1.txt").toFile();
        Files.writeString(termsFile.toPath(), terms, Charset.defaultCharset());
        LOGGER.info("Terms file written to {}", termsFile.getAbsolutePath());

        final CustomDictionary customDictionary = new CustomDictionary();
        customDictionary.setFiles(List.of(termsFile.getAbsolutePath()));
        customDictionary.setCustomDictionaryFilterStrategies(List.of(new CustomDictionaryFilterStrategy()));
        customDictionary.setClassification("names");

        final Policy policy = new Policy();
        policy.getIdentifiers().setCustomDictionaries(List.of(customDictionary));

        final Properties properties = new Properties();
        final PhileasConfiguration phileasConfiguration = new PhileasConfiguration(properties);

        final PlainTextFilterService service = new PlainTextFilterService(phileasConfiguration, contextService, vectorService, null);
        // The non-fuzzy (bloom) dictionary filter matches whole whitespace-delimited tokens, so
        // the term is kept clear of trailing punctuation here.
        final TextFilterResult response = service.filter(policy, "context", "his name was samuel");

        LOGGER.info(response.getFilteredText());

        Assertions.assertEquals("his name was {{{REDACTED-custom-dictionary}}}", response.getFilteredText());
        

    }

    @Test
    public void endToEndMultiplePolicies() throws Exception {

        final Properties properties = new Properties();
        final PhileasConfiguration phileasConfiguration = new PhileasConfiguration(properties);

        final Policy policy = getPolicyJustCreditCard();

        final PlainTextFilterService service = new PlainTextFilterService(phileasConfiguration, contextService, vectorService, null);
        final TextFilterResult response = service.filter(policy, "context", "My email is test@something.com");

        LOGGER.info(response.getFilteredText());

        Assertions.assertEquals("My email is test@something.com", response.getFilteredText());

    }

    @Test
    public void endToEndJustCreditCard() throws Exception {

        final Properties properties = new Properties();
        final PhileasConfiguration phileasConfiguration = new PhileasConfiguration(properties);

        final Policy policy = getPolicyJustCreditCard();

        final PlainTextFilterService service = new PlainTextFilterService(phileasConfiguration, contextService, vectorService, null);
        final TextFilterResult response = service.filter(policy, "context", "My cc is 4121742025464465");

        LOGGER.info(response.getFilteredText());

        Assertions.assertEquals("My cc is {{{REDACTED-credit-card}}}", response.getFilteredText());

    }

    @Test
    public void endToEndJustCreditCardInUnixTimestamp() throws Exception {

        final Properties properties = new Properties();
        final PhileasConfiguration phileasConfiguration = new PhileasConfiguration(properties);

        final Policy policy = getPolicyJustCreditCardNotInUnixTimestamps();

        final PlainTextFilterService service = new PlainTextFilterService(phileasConfiguration, contextService, vectorService, null);
        final TextFilterResult response = service.filter(policy, "context", "My cc is 1647725122227");

        LOGGER.info(response.getFilteredText());
        showSpans(response.getExplanation().identifiedSpans());
        Assertions.assertEquals("My cc is 1647725122227", response.getFilteredText());

    }

    @Test
    public void endToEndJustCreditCardWithIgnoredTerms() throws Exception {

        final Properties properties = new Properties();
        final PhileasConfiguration phileasConfiguration = new PhileasConfiguration(properties);

        final Policy policy = getPolicyJustCreditCard();

        final PlainTextFilterService service = new PlainTextFilterService(phileasConfiguration, contextService, vectorService, null);
        final TextFilterResult response = service.filter(policy, "context", "My cc is 4121742025464400");

        LOGGER.info(response.getFilteredText());

        Assertions.assertEquals("My cc is 4121742025464400", response.getFilteredText());

    }

    @Test
    public void endToEndWithFilterSpecificIgnoredTerms() throws Exception {

        final Properties properties = new Properties();
        final PhileasConfiguration phileasConfiguration = new PhileasConfiguration(properties);

        final Policy policy = getPolicyZipCodeWithIgnored();

        final PlainTextFilterService service = new PlainTextFilterService(phileasConfiguration, contextService, vectorService, null);
        final TextFilterResult response = service.filter(policy, "context", "George Washington was president and his ssn was 123-45-6789 and he lived at 90210.");

        LOGGER.info(response.getFilteredText());

        Assertions.assertEquals("George Washington was president and his ssn was {{{REDACTED-ssn}}} and he lived at 90210.", response.getFilteredText());

    }

    @Test
    public void endToEndWithSSNAndZipCode() throws Exception {

        final Properties properties = new Properties();
        final PhileasConfiguration phileasConfiguration = new PhileasConfiguration(properties);

        final Policy policy = getPolicy();

        final PlainTextFilterService service = new PlainTextFilterService(phileasConfiguration, contextService, vectorService, null);
        final TextFilterResult response = service.filter(policy, "context", "George Washington was president and his ssn was 123-45-6789 and he lived at 90210.");

        LOGGER.info(response.getFilteredText());

        Assertions.assertEquals("George Washington was president and his ssn was {{{REDACTED-ssn}}} and he lived at {{{REDACTED-zip-code}}}.", response.getFilteredText());

    }

    @Test
    public void endToEndPassportWinsOverDriversLicenseForNineDigits() throws Exception {

        // With both the passport and driver's-license filters enabled, a bare 9-digit number resolves
        // to a single passport span (confidence 0.55) rather than a driver's-license span (0.50).

        final PassportNumberFilterStrategy passportNumberFilterStrategy = new PassportNumberFilterStrategy();
        passportNumberFilterStrategy.setStrategy(AbstractFilterStrategy.REDACT);
        final PassportNumber passportNumber = new PassportNumber();
        passportNumber.setPassportNumberFilterStrategies(List.of(passportNumberFilterStrategy));

        final DriversLicenseFilterStrategy driversLicenseFilterStrategy = new DriversLicenseFilterStrategy();
        driversLicenseFilterStrategy.setStrategy(AbstractFilterStrategy.REDACT);
        final DriversLicense driversLicense = new DriversLicense();
        driversLicense.setDriversLicenseFilterStrategies(List.of(driversLicenseFilterStrategy));

        final Identifiers identifiers = new Identifiers();
        identifiers.setPassportNumber(passportNumber);
        identifiers.setDriversLicense(driversLicense);

        final Policy policy = new Policy();
        policy.setIdentifiers(identifiers);

        final PhileasConfiguration configuration = new PhileasConfiguration(new Properties());

        final PlainTextFilterService service = new PlainTextFilterService(configuration, contextService, vectorService, null);
        final TextFilterResult response = service.filter(policy, "context", "the number is 223456789 today.");

        showSpans(response.getExplanation().appliedSpans());

        Assertions.assertEquals(1, response.getExplanation().appliedSpans().size());
        Assertions.assertEquals(FilterType.PASSPORT_NUMBER, response.getExplanation().appliedSpans().get(0).getFilterType());

    }

    private void showSpans(List<Span> spans) {

        for(Span span : spans) {
            LOGGER.info(span.toString());
        }

    }

}
