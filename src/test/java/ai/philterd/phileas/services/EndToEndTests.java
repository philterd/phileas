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
package ai.philterd.phileas.services;

import ai.philterd.phileas.PhileasConfiguration;
import ai.philterd.phileas.model.filtering.BinaryDocumentFilterResult;
import ai.philterd.phileas.model.filtering.IncrementalRedaction;
import ai.philterd.phileas.model.filtering.MimeType;
import ai.philterd.phileas.model.filtering.Span;
import ai.philterd.phileas.model.filtering.TextFilterResult;
import ai.philterd.phileas.policy.Identifiers;
import ai.philterd.phileas.policy.Policy;
import ai.philterd.phileas.policy.filters.BitcoinAddress;
import ai.philterd.phileas.policy.filters.CreditCard;
import ai.philterd.phileas.policy.filters.CustomDictionary;
import ai.philterd.phileas.policy.filters.DriversLicense;
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
import com.google.gson.GsonBuilder;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
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

        final String terms = IOUtils.toString(this.getClass().getResourceAsStream("/customdictionaries/terms1.txt"), Charset.defaultCharset());
        final File termsFile = Paths.get(temp.toFile().getAbsolutePath(), "terms1.txt").toFile();
        FileUtils.writeStringToFile(termsFile, terms, Charset.defaultCharset());
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

        final String terms = IOUtils.toString(this.getClass().getResourceAsStream("/customdictionaries/terms1.txt"), Charset.defaultCharset());
        final File termsFile = Paths.get(temp.toFile().getAbsolutePath(), "terms1.txt").toFile();
        FileUtils.writeStringToFile(termsFile, terms, Charset.defaultCharset());
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

    private void showSpans(List<Span> spans) {

        for(Span span : spans) {
            LOGGER.info(span.toString());
        }

    }

}
