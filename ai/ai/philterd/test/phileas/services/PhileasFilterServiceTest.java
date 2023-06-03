package ai.philterd.test.phileas.services;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import ai.philterd.phileas.configuration.PhileasConfiguration;
import ai.philterd.phileas.model.enums.MimeType;
import ai.philterd.phileas.model.objects.Span;
import ai.philterd.phileas.model.profile.FilterProfile;
import ai.philterd.phileas.model.profile.Ignored;
import ai.philterd.phileas.model.responses.BinaryDocumentFilterResponse;
import ai.philterd.phileas.model.serializers.PlaceholderDeserializer;
import ai.philterd.phileas.services.PhileasFilterService;
import org.aeonbits.owner.ConfigFactory;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import static ai.philterd.test.phileas.services.EndToEndTestsHelper.getFilterProfile;
import static ai.philterd.test.phileas.services.EndToEndTestsHelper.getPdfFilterProfile;
import static ai.philterd.test.phileas.services.EndToEndTestsHelper.getPdfFilterWithPersonProfile;

public class PhileasFilterServiceTest {

    private static final Logger LOGGER = LogManager.getLogger(PhileasFilterServiceTest.class);

    private String INDEXES_DIRECTORY = "/mtnfog/code/philter/philter/distribution/indexes/";
    private Gson gson;

    @BeforeEach
    public void before() {
        INDEXES_DIRECTORY = System.getProperty( "os.name" ).contains( "indow" ) ? INDEXES_DIRECTORY.substring(1) : INDEXES_DIRECTORY;

        final GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(String.class, new PlaceholderDeserializer());
        gson = gsonBuilder.create();
    }

    @Test
    public void filterProfile() throws IOException, URISyntaxException {

        final FilterProfile filterProfile = getFilterProfile("default");
        final String json = gson.toJson(filterProfile);
        LOGGER.info(json);

        final FilterProfile deserialized = gson.fromJson(json, FilterProfile.class);

        Assertions.assertEquals("default", deserialized.getName());

    }

    @Test
    public void filterProfileWithPlaceholder() throws IOException, URISyntaxException {

        final Ignored ignored = new Ignored();
        ignored.setTerms(Arrays.asList("john", "jeff", "${USER}"));

        final FilterProfile filterProfile = getFilterProfile("placeholder");
        filterProfile.setIgnored(Arrays.asList(ignored));
        final String json = gson.toJson(filterProfile);
        LOGGER.info(json);

        final FilterProfile deserialized = gson.fromJson(json, FilterProfile.class);

        Assertions.assertEquals("placeholder", deserialized.getName());
        Assertions.assertEquals(3, filterProfile.getIgnored().get(0).getTerms().size());
        Assertions.assertTrue(CollectionUtils.isNotEmpty(deserialized.getIgnored().get(0).getTerms()));

    }

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
        final BinaryDocumentFilterResponse response = service.filter(Arrays.asList("pdf"), "context", "documentid", document, MimeType.APPLICATION_PDF, MimeType.APPLICATION_PDF);

        // Write the byte array to a file.
        final File outputFile = File.createTempFile("redact", ".pdf");
        //outputFile.deleteOnExit();
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
        final BinaryDocumentFilterResponse response = service.filter(Arrays.asList("pdf"), "context", "documentid", document, MimeType.APPLICATION_PDF, MimeType.APPLICATION_PDF);

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

    private void showSpans(List<Span> spans) {

        for(Span span : spans) {
            LOGGER.info(span.toString());
        }

    }

}