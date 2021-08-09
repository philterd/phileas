package com.mtnfog.test.phileas.services;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mtnfog.phileas.configuration.PhileasConfiguration;
import com.mtnfog.phileas.model.enums.MimeType;
import com.mtnfog.phileas.model.objects.Span;
import com.mtnfog.phileas.model.profile.FilterProfile;
import com.mtnfog.phileas.model.profile.Identifiers;
import com.mtnfog.phileas.model.profile.Ignored;
import com.mtnfog.phileas.model.profile.filters.*;
import com.mtnfog.phileas.model.profile.filters.Date;
import com.mtnfog.phileas.model.profile.filters.strategies.AbstractFilterStrategy;
import com.mtnfog.phileas.model.profile.filters.strategies.ai.NerFilterStrategy;
import com.mtnfog.phileas.model.profile.filters.strategies.custom.CustomDictionaryFilterStrategy;
import com.mtnfog.phileas.model.profile.filters.strategies.dynamic.*;
import com.mtnfog.phileas.model.profile.filters.strategies.rules.*;
import com.mtnfog.phileas.model.responses.BinaryDocumentFilterResponse;
import com.mtnfog.phileas.model.responses.FilterResponse;
import com.mtnfog.phileas.model.serializers.PlaceholderDeserializer;
import com.mtnfog.phileas.services.PhileasFilterService;
import org.aeonbits.owner.ConfigFactory;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class PhileasFilterServiceTest {

    private static final Logger LOGGER = LogManager.getLogger(PhileasFilterServiceTest.class);

    private String INDEXES_DIRECTORY = "/mtnfog/code/bitbucket/philter/philter/distribution/indexes/";
    private Gson gson;

    @BeforeEach
    public void before() {
        INDEXES_DIRECTORY = System.getProperty( "os.name" ).contains( "indow" ) ? INDEXES_DIRECTORY.substring(1) : INDEXES_DIRECTORY;

        final GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(String.class, new PlaceholderDeserializer());
        gson = gsonBuilder.create();
    }

    @Test
    public void filterProfile() throws IOException {

        final FilterProfile filterProfile = getFilterProfile("default");
        final String json = gson.toJson(filterProfile);
        LOGGER.info(json);

        final FilterProfile deserialized = gson.fromJson(json, FilterProfile.class);

        Assertions.assertEquals("default", deserialized.getName());

    }

    @Test
    public void filterProfileWithPlaceholder() throws IOException {

        final Ignored ignored = new Ignored();
        ignored.setTerms(Arrays.asList("john", "jeff", "${WINDOWPATH}"));

        final FilterProfile filterProfile = getFilterProfile("placeholder");
        filterProfile.setIgnored(Arrays.asList(ignored));
        final String json = gson.toJson(filterProfile);
        LOGGER.info(json);

        final FilterProfile deserialized = gson.fromJson(json, FilterProfile.class);

        Assertions.assertEquals("placeholder", deserialized.getName());
        Assertions.assertEquals(3, filterProfile.getIgnored().get(0).getTerms().size());
        Assertions.assertTrue(deserialized.getIgnored().get(0).getTerms().contains("2"));

    }

    @Test
    public void endToEnd1() throws Exception {

        final Path temp = Files.createTempDirectory("philter");
        final File file = Paths.get(temp.toFile().getAbsolutePath(), "default.json").toFile();
        LOGGER.info("Writing profile to {}", file.getAbsolutePath());
        FileUtils.writeStringToFile(file, gson.toJson(getFilterProfile("default")), Charset.defaultCharset());

        Properties properties = new Properties();
        properties.setProperty("indexes.directory", INDEXES_DIRECTORY);
        properties.setProperty("store.enabled", "false");
        properties.setProperty("filter.profiles.directory", temp.toFile().getAbsolutePath());

        final PhileasConfiguration phileasConfiguration = ConfigFactory.create(PhileasConfiguration.class, properties);

        final PhileasFilterService service = new PhileasFilterService(phileasConfiguration);
        final FilterResponse response = service.filter("default", "context", "documentid", "George Washington was president and his ssn was 123-45-6789 and he lived at 90210.", MimeType.TEXT_PLAIN);

        LOGGER.info(response.getFilteredText());

        Assertions.assertEquals("George Washington was president and his ssn was {{{REDACTED-ssn}}} and he lived at {{{REDACTED-zip-code}}}.", response.getFilteredText());
        Assertions.assertEquals("documentid", response.getDocumentId());

    }

    @Test
    public void endToEnd2() throws Exception {

        final Path temp = Files.createTempDirectory("philter");
        final File file = Paths.get(temp.toFile().getAbsolutePath(), "default.json").toFile();
        LOGGER.info("Writing profile to {}", file.getAbsolutePath());
        FileUtils.writeStringToFile(file, gson.toJson(getFilterProfile("default")), Charset.defaultCharset());

        Properties properties = new Properties();
        properties.setProperty("indexes.directory", INDEXES_DIRECTORY);
        properties.setProperty("store.enabled", "false");
        properties.setProperty("filter.profiles.directory", temp.toFile().getAbsolutePath());

        final PhileasConfiguration phileasConfiguration = ConfigFactory.create(PhileasConfiguration.class, properties);

        PhileasFilterService service = new PhileasFilterService(phileasConfiguration);
        final FilterResponse response = service.filter("default", "context", "documentid", "My email is test@something.com and cc is 4121742025464465", MimeType.TEXT_PLAIN);

        LOGGER.info(response.getFilteredText());

        Assertions.assertEquals("My email is {{{REDACTED-email-address}}} and cc is {{{REDACTED-credit-card}}}", response.getFilteredText());
        Assertions.assertEquals("documentid", response.getDocumentId());

    }

    @Test
    public void endToEnd3() throws Exception {

        final Path temp = Files.createTempDirectory("philter");
        final File file = Paths.get(temp.toFile().getAbsolutePath(), "default.json").toFile();
        LOGGER.info("Writing profile to {}", file.getAbsolutePath());
        FileUtils.writeStringToFile(file, gson.toJson(getFilterProfile("default")), Charset.defaultCharset());

        Properties properties = new Properties();
        properties.setProperty("indexes.directory", INDEXES_DIRECTORY);
        properties.setProperty("store.enabled", "false");
        properties.setProperty("filter.profiles.directory", temp.toFile().getAbsolutePath());

        final PhileasConfiguration phileasConfiguration = ConfigFactory.create(PhileasConfiguration.class, properties);

        PhileasFilterService service = new PhileasFilterService(phileasConfiguration);
        final FilterResponse response = service.filter("default", "context", "documentid", "test@something.com is email and cc is 4121742025464465", MimeType.TEXT_PLAIN);

        LOGGER.info(response.getFilteredText());

        Assertions.assertEquals("{{{REDACTED-email-address}}} is email and cc is {{{REDACTED-credit-card}}}", response.getFilteredText());
        Assertions.assertEquals("documentid", response.getDocumentId());

    }

    @Test
    public void endToEnd4() throws Exception {

        final Path temp = Files.createTempDirectory("philter");
        final File file = Paths.get(temp.toFile().getAbsolutePath(), "default.json").toFile();
        LOGGER.info("Writing profile to {}", file.getAbsolutePath());
        FileUtils.writeStringToFile(file, gson.toJson(getFilterProfile("default")), Charset.defaultCharset());

        Properties properties = new Properties();
        properties.setProperty("indexes.directory", INDEXES_DIRECTORY);
        properties.setProperty("store.enabled", "false");
        properties.setProperty("filter.profiles.directory", temp.toFile().getAbsolutePath());

        final PhileasConfiguration phileasConfiguration = ConfigFactory.create(PhileasConfiguration.class, properties);

        PhileasFilterService service = new PhileasFilterService(phileasConfiguration);
        final FilterResponse response = service.filter("default", "context", "documentid", "test@something.com", MimeType.TEXT_PLAIN);

        LOGGER.info(response.getFilteredText());

        Assertions.assertEquals("{{{REDACTED-email-address}}}", response.getFilteredText());
        Assertions.assertEquals("documentid", response.getDocumentId());

    }

    @Test
    public void endToEnd5() throws Exception {

        final Path temp = Files.createTempDirectory("philter");
        final File file = Paths.get(temp.toFile().getAbsolutePath(), "default.json").toFile();
        LOGGER.info("Writing profile to {}", file.getAbsolutePath());
        FileUtils.writeStringToFile(file, gson.toJson(getFilterProfile("default")), Charset.defaultCharset());

        Properties properties = new Properties();
        properties.setProperty("indexes.directory", INDEXES_DIRECTORY);
        properties.setProperty("store.enabled", "false");
        properties.setProperty("filter.profiles.directory", temp.toFile().getAbsolutePath());

        final PhileasConfiguration phileasConfiguration = ConfigFactory.create(PhileasConfiguration.class, properties);

        PhileasFilterService service = new PhileasFilterService(phileasConfiguration);
        final FilterResponse response = service.filter("default", "context", "documentid", "90210", MimeType.TEXT_PLAIN);

        LOGGER.info(response.getFilteredText());

        Assertions.assertEquals("{{{REDACTED-zip-code}}}", response.getFilteredText());
        Assertions.assertEquals("documentid", response.getDocumentId());

    }

    @Test
    public void endToEnd6() throws Exception {

        final Path temp = Files.createTempDirectory("philter");
        final File file = Paths.get(temp.toFile().getAbsolutePath(), "default.json").toFile();
        LOGGER.info("Writing profile to {}", file.getAbsolutePath());
        FileUtils.writeStringToFile(file, gson.toJson(getFilterProfile("default")), Charset.defaultCharset());

        Properties properties = new Properties();
        properties.setProperty("indexes.directory", INDEXES_DIRECTORY);
        properties.setProperty("store.enabled", "false");
        properties.setProperty("filter.profiles.directory", temp.toFile().getAbsolutePath());

        final PhileasConfiguration phileasConfiguration = ConfigFactory.create(PhileasConfiguration.class, properties);

        PhileasFilterService service = new PhileasFilterService(phileasConfiguration);
        final FilterResponse response = service.filter("default", "context", "documentid", "his name was JEFF.", MimeType.TEXT_PLAIN);

        LOGGER.info(response.getFilteredText());

        Assertions.assertEquals("his name was STATIC-REPLACEMENT.", response.getFilteredText());
        Assertions.assertEquals("documentid", response.getDocumentId());

    }

    @Test
    public void endToEnd7() throws Exception {

        final Path temp = Files.createTempDirectory("philter");
        final File file = Paths.get(temp.toFile().getAbsolutePath(), "default.json").toFile();
        LOGGER.info("Writing profile to {}", file.getAbsolutePath());
        FileUtils.writeStringToFile(file, gson.toJson(getFilterProfile("default")), Charset.defaultCharset());

        Properties properties = new Properties();
        properties.setProperty("indexes.directory", INDEXES_DIRECTORY);
        properties.setProperty("store.enabled", "false");
        properties.setProperty("filter.profiles.directory", temp.toFile().getAbsolutePath());

        final PhileasConfiguration phileasConfiguration = ConfigFactory.create(PhileasConfiguration.class, properties);

        PhileasFilterService service = new PhileasFilterService(phileasConfiguration);
        final FilterResponse response = service.filter("default", "context", "documentid", "he was seen on 10-19-2020.", MimeType.TEXT_PLAIN);

        LOGGER.info(response.getFilteredText());

        Assertions.assertEquals("he was seen on 12-20-2023.", response.getFilteredText());
        Assertions.assertEquals("documentid", response.getDocumentId());

    }

    @Test
    public void endToEnd8() throws Exception {

        final Path temp = Files.createTempDirectory("philter");
        final File file = Paths.get(temp.toFile().getAbsolutePath(), "default.json").toFile();
        LOGGER.info("Writing profile to {}", file.getAbsolutePath());
        FileUtils.writeStringToFile(file, gson.toJson(getFilterProfile("default")), Charset.defaultCharset());

        Properties properties = new Properties();
        properties.setProperty("indexes.directory", INDEXES_DIRECTORY);
        properties.setProperty("store.enabled", "false");
        properties.setProperty("filter.profiles.directory", temp.toFile().getAbsolutePath());

        final PhileasConfiguration phileasConfiguration = ConfigFactory.create(PhileasConfiguration.class, properties);

        PhileasFilterService service = new PhileasFilterService(phileasConfiguration);
        final FilterResponse response = service.filter("default", "context", "documentid",
                "George Washington was president." + System.lineSeparator() + "Abraham Lincoln was president.", MimeType.TEXT_PLAIN);

        LOGGER.info(response.getFilteredText());

        // Ensure that the line separator in the input text was not removed.
        Assertions.assertTrue(response.getFilteredText().contains(System.lineSeparator()));

    }

    @Test
    public void endToEndUsingCustomDictionary() throws Exception {

        final CustomDictionary customDictionary = new CustomDictionary();
        customDictionary.setCustomDictionaryFilterStrategies(Arrays.asList(new CustomDictionaryFilterStrategy()));
        customDictionary.setTerms(Arrays.asList("george", "samuel"));
        customDictionary.setFuzzy(false);

        final FilterProfile filterProfile = new FilterProfile();
        filterProfile.setName("custom-dictionary");
        filterProfile.getIdentifiers().setCustomDictionaries(Arrays.asList(customDictionary));

        final Path temp = Files.createTempDirectory("philter");
        final File file = Paths.get(temp.toFile().getAbsolutePath(), "default.json").toFile();
        FileUtils.writeStringToFile(file, gson.toJson(filterProfile), Charset.defaultCharset());
        LOGGER.info("Filter profile written to {}", file.getAbsolutePath());

        final Properties properties = new Properties();
        properties.setProperty("indexes.directory", INDEXES_DIRECTORY);
        properties.setProperty("store.enabled", "false");
        properties.setProperty("filter.profiles.directory", temp.toFile().getAbsolutePath());

        final PhileasConfiguration phileasConfiguration = ConfigFactory.create(PhileasConfiguration.class, properties);

        final PhileasFilterService service = new PhileasFilterService(phileasConfiguration);
        final FilterResponse response = service.filter("default", "context", "documentid", "his name was samuel and george.", MimeType.TEXT_PLAIN);

        LOGGER.info(response.getFilteredText());

        Assertions.assertEquals("his name was {{{REDACTED-custom-dictionary}}} and {{{REDACTED-custom-dictionary}}}.", response.getFilteredText());
        Assertions.assertEquals("documentid", response.getDocumentId());

    }

    @Test
    public void endToEndUsingCustomDictionaryFileLuceneFilter() throws Exception {

        final Path temp = Files.createTempDirectory("philter");

        final String terms = IOUtils.toString(this.getClass().getResourceAsStream("/customdictionaries/terms1.txt"), Charset.defaultCharset());
        final File termsFile = Paths.get(temp.toFile().getAbsolutePath(), "terms1.txt").toFile();
        FileUtils.writeStringToFile(termsFile, terms, Charset.defaultCharset());
        LOGGER.info("Terms file written to {}", termsFile.getAbsolutePath());

        final CustomDictionary customDictionary = new CustomDictionary();
        customDictionary.setFiles(Arrays.asList(termsFile.getAbsolutePath()));
        customDictionary.setCustomDictionaryFilterStrategies(Arrays.asList(new CustomDictionaryFilterStrategy()));
        customDictionary.setClassification("names");
        customDictionary.setTerms(Arrays.asList("george"));
        customDictionary.setFuzzy(false);
        customDictionary.setSensitivity("low");

        final FilterProfile filterProfile = new FilterProfile();
        filterProfile.setName("custom-dictionary");
        filterProfile.getIdentifiers().setCustomDictionaries(Arrays.asList(customDictionary));

        final File file = Paths.get(temp.toFile().getAbsolutePath(), "default.json").toFile();
        FileUtils.writeStringToFile(file, gson.toJson(filterProfile), Charset.defaultCharset());
        LOGGER.info("Filter profile written to {}", file.getAbsolutePath());

        final Properties properties = new Properties();
        properties.setProperty("indexes.directory", INDEXES_DIRECTORY);
        properties.setProperty("store.enabled", "false");
        properties.setProperty("filter.profiles.directory", temp.toFile().getAbsolutePath());

        final PhileasConfiguration phileasConfiguration = ConfigFactory.create(PhileasConfiguration.class, properties);

        final PhileasFilterService service = new PhileasFilterService(phileasConfiguration);
        final FilterResponse response = service.filter("default", "context", "documentid", "his name was samuel and george.", MimeType.TEXT_PLAIN);

        LOGGER.info(response.getFilteredText());

        Assertions.assertEquals("his name was {{{REDACTED-custom-dictionary}}} and {{{REDACTED-custom-dictionary}}}.", response.getFilteredText());
        Assertions.assertEquals("documentid", response.getDocumentId());

    }

    @Test
    public void endToEndUsingCustomDictionaryFileBloomFilter() throws Exception {

        final Path temp = Files.createTempDirectory("philter");

        final String terms = IOUtils.toString(this.getClass().getResourceAsStream("/customdictionaries/terms1.txt"), Charset.defaultCharset());
        final File termsFile = Paths.get(temp.toFile().getAbsolutePath(), "terms1.txt").toFile();
        FileUtils.writeStringToFile(termsFile, terms, Charset.defaultCharset());
        LOGGER.info("Terms file written to {}", termsFile.getAbsolutePath());

        final CustomDictionary customDictionary = new CustomDictionary();
        customDictionary.setFiles(Arrays.asList(termsFile.getAbsolutePath()));
        customDictionary.setCustomDictionaryFilterStrategies(Arrays.asList(new CustomDictionaryFilterStrategy()));
        customDictionary.setClassification("names");
        customDictionary.setFuzzy(false);

        final FilterProfile filterProfile = new FilterProfile();
        filterProfile.setName("custom-dictionary-bloom");
        filterProfile.getIdentifiers().setCustomDictionaries(Arrays.asList(customDictionary));

        final File file = Paths.get(temp.toFile().getAbsolutePath(), "default.json").toFile();
        FileUtils.writeStringToFile(file, gson.toJson(filterProfile), Charset.defaultCharset());
        LOGGER.info("Filter profile written to {}", file.getAbsolutePath());

        final Properties properties = new Properties();
        properties.setProperty("indexes.directory", INDEXES_DIRECTORY);
        properties.setProperty("store.enabled", "false");
        properties.setProperty("filter.profiles.directory", temp.toFile().getAbsolutePath());

        final PhileasConfiguration phileasConfiguration = ConfigFactory.create(PhileasConfiguration.class, properties);

        final PhileasFilterService service = new PhileasFilterService(phileasConfiguration);
        final FilterResponse response = service.filter("default", "context", "documentid", "his name was samuel.", MimeType.TEXT_PLAIN);

        LOGGER.info(response.getFilteredText());

        Assertions.assertEquals("his name was {{{REDACTED-custom-dictionary}}}.", response.getFilteredText());
        Assertions.assertEquals("documentid", response.getDocumentId());

    }

    @Test
    public void endToEndWithoutDocumentId() throws Exception {

        final Path temp = Files.createTempDirectory("philter");
        final File file = Paths.get(temp.toFile().getAbsolutePath(), "default.json").toFile();
        LOGGER.info("Writing profile to {}", file.getAbsolutePath());
        FileUtils.writeStringToFile(file, gson.toJson(getFilterProfile("default")), Charset.defaultCharset());

        Properties properties = new Properties();
        properties.setProperty("indexes.directory", INDEXES_DIRECTORY);
        properties.setProperty("store.enabled", "false");
        properties.setProperty("filter.profiles.directory", temp.toFile().getAbsolutePath());

        final PhileasConfiguration phileasConfiguration = ConfigFactory.create(PhileasConfiguration.class, properties);

        final PhileasFilterService service = new PhileasFilterService(phileasConfiguration);
        final FilterResponse response = service.filter("default", "context", null, "his name was JEFF.", MimeType.TEXT_PLAIN);

        LOGGER.info("Generated document ID: " + response.getDocumentId());
        LOGGER.info(response.getFilteredText());

        Assertions.assertEquals("his name was STATIC-REPLACEMENT.", response.getFilteredText());
        Assertions.assertNotNull(response.getDocumentId());

    }

    @Test
    public void endToEndMultipleFilterProfiles() throws Exception {

        final Path temp = Files.createTempDirectory("philter");

        final File file1 = Paths.get(temp.toFile().getAbsolutePath(), "default.json").toFile();
        LOGGER.info("Writing profile to {}", file1.getAbsolutePath());
        FileUtils.writeStringToFile(file1, gson.toJson(getFilterProfile("default")), Charset.defaultCharset());

        final File file2 = Paths.get(temp.toFile().getAbsolutePath(), "justcreditcard.json").toFile();
        LOGGER.info("Writing profile to {}", file2.getAbsolutePath());
        FileUtils.writeStringToFile(file2, gson.toJson(getFilterProfileJustCreditCard("justcreditcard")), Charset.defaultCharset());

        Properties properties = new Properties();
        properties.setProperty("indexes.directory", INDEXES_DIRECTORY);
        properties.setProperty("store.enabled", "false");
        properties.setProperty("filter.profiles.directory", temp.toFile().getAbsolutePath());

        final PhileasConfiguration phileasConfiguration = ConfigFactory.create(PhileasConfiguration.class, properties);

        final PhileasFilterService service = new PhileasFilterService(phileasConfiguration);
        final FilterResponse response = service.filter("justcreditcard", "context", "documentid", "My email is test@something.com", MimeType.TEXT_PLAIN);

        LOGGER.info(response.getFilteredText());

        Assertions.assertEquals("My email is test@something.com", response.getFilteredText());

    }

    @Test
    public void endToEndJustCreditCard() throws Exception {

        final Path temp = Files.createTempDirectory("philter");

        final File file2 = Paths.get(temp.toFile().getAbsolutePath(), "justcreditcard.json").toFile();
        LOGGER.info("Writing profile to {}", file2.getAbsolutePath());
        FileUtils.writeStringToFile(file2, gson.toJson(getFilterProfileJustCreditCard("justcreditcard")), Charset.defaultCharset());

        Properties properties = new Properties();
        properties.setProperty("indexes.directory", INDEXES_DIRECTORY);
        properties.setProperty("store.enabled", "false");
        properties.setProperty("filter.profiles.directory", temp.toFile().getAbsolutePath());

        final PhileasConfiguration phileasConfiguration = ConfigFactory.create(PhileasConfiguration.class, properties);

        final PhileasFilterService service = new PhileasFilterService(phileasConfiguration);
        final FilterResponse response = service.filter("justcreditcard", "context", "documentid", "My cc is 4121742025464465", MimeType.TEXT_PLAIN);

        LOGGER.info(response.getFilteredText());

        Assertions.assertEquals("My cc is {{{REDACTED-credit-card}}}", response.getFilteredText());

    }

    @Test
    public void endToEndJustCreditCardWithIgnoredTerms() throws Exception {

        final Path temp = Files.createTempDirectory("philter");

        final File file2 = Paths.get(temp.toFile().getAbsolutePath(), "justcreditcard.json").toFile();
        LOGGER.info("Writing profile to {}", file2.getAbsolutePath());
        FileUtils.writeStringToFile(file2, gson.toJson(getFilterProfileJustCreditCard("justcreditcard")), Charset.defaultCharset());

        Properties properties = new Properties();
        properties.setProperty("indexes.directory", INDEXES_DIRECTORY);
        properties.setProperty("store.enabled", "false");
        properties.setProperty("filter.profiles.directory", temp.toFile().getAbsolutePath());

        final PhileasConfiguration phileasConfiguration = ConfigFactory.create(PhileasConfiguration.class, properties);

        final PhileasFilterService service = new PhileasFilterService(phileasConfiguration);
        final FilterResponse response = service.filter("justcreditcard", "context", "documentid", "My cc is 4121742025464400", MimeType.TEXT_PLAIN);

        LOGGER.info(response.getFilteredText());

        Assertions.assertEquals("My cc is 4121742025464400", response.getFilteredText());

    }

    @Test
    public void endToEndWithFilterSpecificIgnoredTerms() throws Exception {

        final Path temp = Files.createTempDirectory("philter");
        final File file = Paths.get(temp.toFile().getAbsolutePath(), "default.json").toFile();
        LOGGER.info("Writing profile to {}", file.getAbsolutePath());
        FileUtils.writeStringToFile(file, gson.toJson(getFilterProfileZipCodeWithIgnored("default")), Charset.defaultCharset());

        Properties properties = new Properties();
        properties.setProperty("indexes.directory", INDEXES_DIRECTORY);
        properties.setProperty("store.enabled", "false");
        properties.setProperty("filter.profiles.directory", temp.toFile().getAbsolutePath());

        final PhileasConfiguration phileasConfiguration = ConfigFactory.create(PhileasConfiguration.class, properties);

        final PhileasFilterService service = new PhileasFilterService(phileasConfiguration);
        final FilterResponse response = service.filter("default", "context", "documentid", "George Washington was president and his ssn was 123-45-6789 and he lived at 90210.", MimeType.TEXT_PLAIN);

        LOGGER.info(response.getFilteredText());

        Assertions.assertEquals("George Washington was president and his ssn was {{{REDACTED-ssn}}} and he lived at 90210.", response.getFilteredText());

    }

    @Test
    public void endToEndWithFilterIgnoredTermsFromFile() throws Exception {

        final Path temp = Files.createTempDirectory("philter");
        final File file = Paths.get(temp.toFile().getAbsolutePath(), "default.json").toFile();
        LOGGER.info("Writing profile to {}", file.getAbsolutePath());
        FileUtils.writeStringToFile(file, gson.toJson(getFilterProfileZipCodeWithIgnoredFromFile("default")), Charset.defaultCharset());

        Properties properties = new Properties();
        properties.setProperty("indexes.directory", INDEXES_DIRECTORY);
        properties.setProperty("store.enabled", "false");
        properties.setProperty("filter.profiles.directory", temp.toFile().getAbsolutePath());

        final PhileasConfiguration phileasConfiguration = ConfigFactory.create(PhileasConfiguration.class, properties);

        final PhileasFilterService service = new PhileasFilterService(phileasConfiguration);
        final FilterResponse response = service.filter("default", "context", "documentid", "George Washington was president and his ssn was 123-45-6789 and he lived at 90210.", MimeType.TEXT_PLAIN);

        LOGGER.info(response.getFilteredText());

        Assertions.assertEquals("George Washington was president and his ssn was {{{REDACTED-ssn}}} and he lived at 90210.", response.getFilteredText());

    }

    @Test
    public void endToEndNonexistentFilterProfile() throws Exception {

        final Path temp = Files.createTempDirectory("philter");

        final File file1 = Paths.get(temp.toFile().getAbsolutePath(), "default.json").toFile();
        LOGGER.info("Writing profile to {}", file1.getAbsolutePath());
        FileUtils.writeStringToFile(file1, gson.toJson(getFilterProfile("default")), Charset.defaultCharset());

        Properties properties = new Properties();
        properties.setProperty("indexes.directory", INDEXES_DIRECTORY);
        properties.setProperty("store.enabled", "false");
        properties.setProperty("filter.profiles.directory", temp.toFile().getAbsolutePath());

        final PhileasConfiguration phileasConfiguration = ConfigFactory.create(PhileasConfiguration.class, properties);

        Assertions.assertThrows(FileNotFoundException.class, () -> {

            PhileasFilterService service = new PhileasFilterService(phileasConfiguration);
            final FilterResponse response = service.filter("custom1", "context", "documentid", "My email is test@something.com", MimeType.TEXT_PLAIN);

        });

    }

    // PHL-223: Face recognition
    /*@Test
    public void imageFaces() throws Exception {

        final String filename = "05-29-Small-crowd.jpg";

        final InputStream is = this.getClass().getResourceAsStream("/images/" + filename);
        final byte[] document = IOUtils.toByteArray(is);
        is.close();

        final Path temp = Files.createTempDirectory("philter");

        final File file1 = Paths.get(temp.toFile().getAbsolutePath(), "pdf.json").toFile();
        LOGGER.info("Writing profile to {}", file1.getAbsolutePath());
        FileUtils.writeStringToFile(file1, gson.toJson(getPdfFilterProfile("pdf")), Charset.defaultCharset());

        Properties properties = new Properties();
        properties.setProperty("indexes.directory", INDEXES_DIRECTORY);
        properties.setProperty("store.enabled", "false");
        properties.setProperty("filter.profiles.directory", temp.toFile().getAbsolutePath());

        final PhileasConfiguration phileasConfiguration = ConfigFactory.create(PhileasConfiguration.class, properties);

        final PhileasFilterService service = new PhileasFilterService(phileasConfiguration);
        final BinaryDocumentFilterResponse response = service.filter("pdf", "context", "documentid", document, MimeType.IMAGE_JPEG, MimeType.IMAGE_JPEG);

        // Write the byte array to a file.
        final File outputFile = File.createTempFile("redact", ".jpg");
        //outputFile.deleteOnExit();
        final String output = outputFile.getAbsolutePath();
        LOGGER.info("Writing redacted PDF to {}", output);
        FileUtils.writeByteArrayToFile(new File(output), response.getDocument());

        LOGGER.info("Spans: {}", response.getExplanation().getAppliedSpans().size());
        showSpans(response.getExplanation().getAppliedSpans());

        // TODO: How to assert? MD5 gives a different value each time.

    }*/

    @Test
    public void pdf1() throws Exception {

        final String filename = "12-12110 K.pdf";

        final InputStream is = this.getClass().getResourceAsStream("/pdfs/" + filename);
        final byte[] document = IOUtils.toByteArray(is);
        is.close();

        final Path temp = Files.createTempDirectory("philter");

        final File file1 = Paths.get(temp.toFile().getAbsolutePath(), "pdf.json").toFile();
        LOGGER.info("Writing profile to {}", file1.getAbsolutePath());
        FileUtils.writeStringToFile(file1, gson.toJson(getPdfFilterProfile("pdf")), Charset.defaultCharset());

        Properties properties = new Properties();
        properties.setProperty("indexes.directory", INDEXES_DIRECTORY);
        properties.setProperty("store.enabled", "false");
        properties.setProperty("filter.profiles.directory", temp.toFile().getAbsolutePath());

        final PhileasConfiguration phileasConfiguration = ConfigFactory.create(PhileasConfiguration.class, properties);

        PhileasFilterService service = new PhileasFilterService(phileasConfiguration);
        final BinaryDocumentFilterResponse response = service.filter("pdf", "context", "documentid", document, MimeType.APPLICATION_PDF, MimeType.APPLICATION_PDF);

        // Write the byte array to a file.
        final File outputFile = File.createTempFile("redact", ".pdf");
        outputFile.deleteOnExit();
        final String output = outputFile.getAbsolutePath();
        LOGGER.info("Writing redacted PDF to {}", output);
        FileUtils.writeByteArrayToFile(new File(output), response.getDocument());

        LOGGER.info("Spans: {}", response.getExplanation().getAppliedSpans().size());
        showSpans(response.getExplanation().getAppliedSpans());

        // TODO: How to assert? MD5 gives a different value each time.

    }

    @Test
    public void pdf2() throws Exception {

        final InputStream is = this.getClass().getResourceAsStream("/pdfs/new-lines.pdf");
        final byte[] document = IOUtils.toByteArray(is);
        is.close();

        final Path temp = Files.createTempDirectory("philter");

        final File file1 = Paths.get(temp.toFile().getAbsolutePath(), "pdf.json").toFile();
        LOGGER.info("Writing profile to {}", file1.getAbsolutePath());
        FileUtils.writeStringToFile(file1, gson.toJson(getPdfFilterProfile("pdf")), Charset.defaultCharset());

        Properties properties = new Properties();
        properties.setProperty("indexes.directory", INDEXES_DIRECTORY);
        properties.setProperty("store.enabled", "false");
        properties.setProperty("filter.profiles.directory", temp.toFile().getAbsolutePath());

        final PhileasConfiguration phileasConfiguration = ConfigFactory.create(PhileasConfiguration.class, properties);

        PhileasFilterService service = new PhileasFilterService(phileasConfiguration);
        final BinaryDocumentFilterResponse response = service.filter("pdf", "context", "documentid", document, MimeType.APPLICATION_PDF, MimeType.APPLICATION_PDF);

        // Write the byte array to a file.
        final File outputFile = File.createTempFile("redact", ".pdf");
        outputFile.deleteOnExit();
        final String output = outputFile.getAbsolutePath();
        LOGGER.info("Writing redacted PDF to {}", output);
        FileUtils.writeByteArrayToFile(new File(output), response.getDocument());

        LOGGER.info("Spans: {}", response.getExplanation().getAppliedSpans().size());
        showSpans(response.getExplanation().getAppliedSpans());

        // output:
        // characterStart: 35;  characterEnd: 40;  filterType: zip-code;  context: context;  documentId: documentid;  confidence: 0.9;  text: 90210;  replacement: {{{REDACTED-zip-code}}};  salt: ;  ignored: false;  classification: null;

        // TODO: How to assert? MD5 gives a different value each time.

    }

    private FilterProfile getFilterProfileZipCodeWithIgnored(String filterProfileName) throws IOException {

        Set<String> ignored = new HashSet<>();
        ignored.add("90210");

        SsnFilterStrategy ssnFilterStrategy = new SsnFilterStrategy();

        Ssn ssn = new Ssn();
        ssn.setSsnFilterStrategies(Arrays.asList(ssnFilterStrategy));

        ZipCodeFilterStrategy zipCodeFilterStrategy = new ZipCodeFilterStrategy();
        zipCodeFilterStrategy.setTruncateDigits(2);

        ZipCode zipCode = new ZipCode();
        zipCode.setZipCodeFilterStrategies(Arrays.asList(zipCodeFilterStrategy));
        zipCode.setIgnored(ignored);

        Identifiers identifiers = new Identifiers();
        identifiers.setSsn(ssn);
        identifiers.setZipCode(zipCode);

        FilterProfile filterProfile = new FilterProfile();
        filterProfile.setName(filterProfileName);
        filterProfile.setIdentifiers(identifiers);

        return filterProfile;

    }

    private FilterProfile getFilterProfileZipCodeWithIgnoredFromFile(String filterProfileName) throws IOException {

        // Copy file to temp directory.
        final File file = File.createTempFile("philter", "ignore");
        FileUtils.writeLines(file, Arrays.asList("90210", "John Smith"));

        Set<String> ignoredFiles = new HashSet<>();
        ignoredFiles.add(file.getAbsolutePath());

        SsnFilterStrategy ssnFilterStrategy = new SsnFilterStrategy();

        Ssn ssn = new Ssn();
        ssn.setSsnFilterStrategies(Arrays.asList(ssnFilterStrategy));

        ZipCodeFilterStrategy zipCodeFilterStrategy = new ZipCodeFilterStrategy();
        zipCodeFilterStrategy.setTruncateDigits(2);

        ZipCode zipCode = new ZipCode();
        zipCode.setZipCodeFilterStrategies(Arrays.asList(zipCodeFilterStrategy));
        zipCode.setIgnoredFiles(ignoredFiles);

        Identifiers identifiers = new Identifiers();
        identifiers.setSsn(ssn);
        identifiers.setZipCode(zipCode);

        FilterProfile filterProfile = new FilterProfile();
        filterProfile.setName(filterProfileName);
        filterProfile.setIdentifiers(identifiers);

        return filterProfile;

    }

    private FilterProfile getFilterProfile(String filterProfileName) throws IOException {

        AgeFilterStrategy ageFilterStrategy = new AgeFilterStrategy();

        Age age = new Age();
        age.setAgeFilterStrategies(Arrays.asList(ageFilterStrategy));

        CreditCardFilterStrategy creditCardFilterStrategy = new CreditCardFilterStrategy();

        CreditCard creditCard = new CreditCard();
        creditCard.setCreditCardFilterStrategies(Arrays.asList(creditCardFilterStrategy));

        DateFilterStrategy dateFilterStrategy = new DateFilterStrategy();
        dateFilterStrategy.setStrategy(AbstractFilterStrategy.SHIFT);
        dateFilterStrategy.setShiftYears(3);
        dateFilterStrategy.setShiftMonths(2);
        dateFilterStrategy.setShiftDays(1);

        Date date = new Date();
        date.setDateFilterStrategies(Arrays.asList(dateFilterStrategy));

        EmailAddressFilterStrategy emailAddressFilterStrategy = new EmailAddressFilterStrategy();

        EmailAddress emailAddress = new EmailAddress();
        emailAddress.setEmailAddressFilterStrategies(Arrays.asList(emailAddressFilterStrategy));

        Identifier identifier1 = new Identifier();
        identifier1.setIdentifierFilterStrategies(Arrays.asList(new IdentifierFilterStrategy()));
        identifier1.setPattern("asdfasdfasdf");
        identifier1.setCaseSensitive(true);

        IdentifierFilterStrategy identifier2FilterStrategy = new IdentifierFilterStrategy();
        identifier2FilterStrategy.setStrategy(AbstractFilterStrategy.STATIC_REPLACE);
        identifier2FilterStrategy.setStaticReplacement("STATIC-REPLACEMENT");
        Identifier identifier2 = new Identifier();
        identifier2.setPattern("JEFF");
        identifier2.setCaseSensitive(true);
        identifier2.setIdentifierFilterStrategies(Arrays.asList(identifier2FilterStrategy));

        IpAddressFilterStrategy ipAddressFilterStrategy = new IpAddressFilterStrategy();

        IpAddress ipAddress = new IpAddress();
        ipAddress.setIpAddressFilterStrategies(Arrays.asList(ipAddressFilterStrategy));

        PhoneNumberFilterStrategy phoneNumberFilterStrategy = new PhoneNumberFilterStrategy();

        PhoneNumber phoneNumber = new PhoneNumber();
        phoneNumber.setPhoneNumberFilterStrategies(Arrays.asList(phoneNumberFilterStrategy));

        SsnFilterStrategy ssnFilterStrategy = new SsnFilterStrategy();

        Ssn ssn = new Ssn();
        ssn.setSsnFilterStrategies(Arrays.asList(ssnFilterStrategy));

        StateAbbreviationFilterStrategy stateAbbreviationFilterStrategy = new StateAbbreviationFilterStrategy();

        StateAbbreviation stateAbbreviation = new StateAbbreviation();
        stateAbbreviation.setStateAbbreviationsFilterStrategies(Arrays.asList(stateAbbreviationFilterStrategy));

        UrlFilterStrategy urlFilterStrategy = new UrlFilterStrategy();

        Url url = new Url();
        url.setUrlFilterStrategies(Arrays.asList(urlFilterStrategy));

        VinFilterStrategy vinFilterStrategy = new VinFilterStrategy();

        Vin vin = new Vin();
        vin.setVinFilterStrategies(Arrays.asList(vinFilterStrategy));

        ZipCodeFilterStrategy zipCodeFilterStrategy = new ZipCodeFilterStrategy();
        zipCodeFilterStrategy.setTruncateDigits(2);

        ZipCode zipCode = new ZipCode();
        zipCode.setZipCodeFilterStrategies(Arrays.asList(zipCodeFilterStrategy));

        NerFilterStrategy nerFilterStrategy = new NerFilterStrategy();

        Ner ner = new Ner();
        ner.setNerStrategies(Arrays.asList(nerFilterStrategy));

        // ----------------------------------------------------------------------------------

        CityFilterStrategy cityFilterStrategy = new CityFilterStrategy();

        City city = new City();
        city.setCityFilterStrategies(Arrays.asList(cityFilterStrategy));

        CountyFilterStrategy countyFilterStrategy = new CountyFilterStrategy();

        County county = new County();
        county.setCountyFilterStrategies(Arrays.asList(countyFilterStrategy));

        FirstNameFilterStrategy firstNameFilterStrategy = new FirstNameFilterStrategy();

        FirstName firstName = new FirstName();
        firstName.setFirstNameFilterStrategies(Arrays.asList(firstNameFilterStrategy));

        HospitalAbbreviationFilterStrategy hospitalAbbreviationFilterStrategy = new HospitalAbbreviationFilterStrategy();

        HospitalAbbreviation hospitalAbbreviation = new HospitalAbbreviation();
        hospitalAbbreviation.setHospitalAbbreviationFilterStrategies(Arrays.asList(hospitalAbbreviationFilterStrategy));

        HospitalFilterStrategy hospitalFilterStrategy = new HospitalFilterStrategy();

        Hospital hospital = new Hospital();
        hospital.setHospitalFilterStrategies(Arrays.asList(hospitalFilterStrategy));

        StateFilterStrategy stateFilterStrategy = new StateFilterStrategy();

        State state = new State();
        state.setStateFilterStrategies(Arrays.asList(stateFilterStrategy));

        SurnameFilterStrategy surnameFilterStrategy = new SurnameFilterStrategy();

        Surname surname = new Surname();
        surname.setSurnameFilterStrategies(Arrays.asList(surnameFilterStrategy));

        // ----------------------------------------------------------------------------------

        Identifiers identifiers = new Identifiers();

        identifiers.setAge(age);
        identifiers.setCreditCard(creditCard);
        identifiers.setDate(date);
        identifiers.setEmailAddress(emailAddress);
        identifiers.setIdentifiers(Arrays.asList(identifier1, identifier2));
        identifiers.setIpAddress(ipAddress);
        identifiers.setPhoneNumber(phoneNumber);
        identifiers.setSsn(ssn);
        identifiers.setStateAbbreviation(stateAbbreviation);
        identifiers.setUrl(url);
        identifiers.setVin(vin);
        identifiers.setZipCode(zipCode);
        //identifiers.setNer(ner);

        /*identifiers.setCity(city);
        identifiers.setCounty(county);
        identifiers.setFirstName(firstName);
        identifiers.setHospital(hospital);
        identifiers.setHospitalAbbreviation(hospitalAbbreviation);
        identifiers.setState(state);
        identifiers.setSurname(surname);*/

        FilterProfile filterProfile = new FilterProfile();
        filterProfile.setName(filterProfileName);
        filterProfile.setIdentifiers(identifiers);

        return filterProfile;

    }

    private FilterProfile getPdfFilterProfile(String filterProfileName) throws IOException {

        ZipCodeFilterStrategy zipCodeFilterStrategy = new ZipCodeFilterStrategy();
        zipCodeFilterStrategy.setTruncateDigits(2);

        ZipCode zipCode = new ZipCode();
        zipCode.setZipCodeFilterStrategies(Arrays.asList(zipCodeFilterStrategy));

        CustomDictionaryFilterStrategy customDictionaryFilterStrategy = new CustomDictionaryFilterStrategy();
        customDictionaryFilterStrategy.setStrategy("REDACT");

        CustomDictionary customDictionary = new CustomDictionary();
        customDictionary.setCustomDictionaryFilterStrategies(Arrays.asList(customDictionaryFilterStrategy));
        customDictionary.setTerms(Arrays.asList("Wendy"));

        Identifiers identifiers = new Identifiers();

        identifiers.setCustomDictionaries(Arrays.asList(customDictionary));
        identifiers.setZipCode(zipCode);

        FilterProfile filterProfile = new FilterProfile();
        filterProfile.setName(filterProfileName);
        filterProfile.setIdentifiers(identifiers);

        return filterProfile;

    }

    private FilterProfile getFilterProfileJustCreditCard(String filterProfileName) throws IOException {

        CreditCardFilterStrategy creditCardFilterStrategy = new CreditCardFilterStrategy();

        CreditCard creditCard = new CreditCard();
        creditCard.setCreditCardFilterStrategies(Arrays.asList(creditCardFilterStrategy));

        Identifiers identifiers = new Identifiers();
        identifiers.setCreditCard(creditCard);

        Ignored ignored = new Ignored();
        ignored.setTerms(Arrays.asList("4121742025464400", "12341341234", "2423543545"));

        FilterProfile filterProfile = new FilterProfile();
        filterProfile.setName(filterProfileName);
        filterProfile.setIdentifiers(identifiers);
        filterProfile.setIgnored(Arrays.asList(ignored));

        return filterProfile;

    }

    private void showSpans(List<Span> spans) {

        for(Span span : spans) {
            LOGGER.info(span.toString());
        }

    }

}