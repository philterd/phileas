package ai.philterd.phileas.services.filters.filtering;

import ai.philterd.phileas.PhileasConfiguration;
import ai.philterd.phileas.model.filtering.BinaryDocumentFilterResult;
import ai.philterd.phileas.model.filtering.MimeType;
import ai.philterd.phileas.model.filtering.Span;
import ai.philterd.phileas.policy.Identifiers;
import ai.philterd.phileas.policy.Policy;
import ai.philterd.phileas.policy.filters.Date;
import ai.philterd.phileas.policy.filters.EmailAddress;
import ai.philterd.phileas.policy.filters.ZipCode;
import ai.philterd.phileas.services.context.ContextService;
import ai.philterd.phileas.services.context.DefaultContextService;
import ai.philterd.phileas.services.disambiguation.vector.InMemoryVectorService;
import ai.philterd.phileas.services.disambiguation.vector.VectorService;
import ai.philterd.phileas.services.strategies.AbstractFilterStrategy;
import ai.philterd.phileas.services.strategies.rules.DateFilterStrategy;
import ai.philterd.phileas.services.strategies.rules.EmailAddressFilterStrategy;
import ai.philterd.phileas.services.strategies.rules.ZipCodeFilterStrategy;
import org.apache.pdfbox.io.IOUtils;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Properties;

public class PdfFilterServiceTest {

    @Test
    public void testFilter1() throws Exception {

        final Properties properties = new Properties();
        final PhileasConfiguration phileasConfiguration = new PhileasConfiguration(properties);

        final ContextService contextService = new DefaultContextService();
        final VectorService vectorService = new InMemoryVectorService();

        final PdfFilterService pdfFilterService = new PdfFilterService(phileasConfiguration, contextService, vectorService, null);

        final Policy policy = getPolicy("test");

        final InputStream is = getClass().getClassLoader().getResourceAsStream("12-12110 K.pdf");
        final byte[] file = IOUtils.toByteArray(is);
        is.close();

        final BinaryDocumentFilterResult binaryDocumentFilterResult = pdfFilterService.filter(policy, "context", file, MimeType.APPLICATION_PDF);

//        final File outputFile = new File("/tmp/output.pdf");// File.createTempFile("output", ".pdf");
//        try (FileOutputStream outputStream = new FileOutputStream(outputFile)) {
//            outputStream.write(binaryDocumentFilterResult.getDocument());
//        }
//
//        System.out.println("Output written to " + outputFile.getAbsolutePath());

    }

    @Test
    public void testApply1() throws Exception {

        final Properties properties = new Properties();
        final PhileasConfiguration phileasConfiguration = new PhileasConfiguration(properties);

        final ContextService contextService = new DefaultContextService();
        final VectorService vectorService = new InMemoryVectorService();

        final PdfFilterService pdfFilterService = new PdfFilterService(phileasConfiguration, contextService, vectorService, null);

        final Policy policy = new Policy();

        final InputStream is = getClass().getClassLoader().getResourceAsStream("12-12110 K.pdf");
        final byte[] file = IOUtils.toByteArray(is);
        is.close();

        // characterStart: 21798;  characterEnd: 21807;  filterType: date;  context: context;  confidence: 75.0;  text: 1-20-0102;  replacement: {{{REDACTED-date}}};  salt: ;  ignored: false;  applied: true;  classification: null;  priority: 0;  line number: 416; x: 120.39199; y: 764.47; page number: 6
        final Span span1 = new Span();
        span1.setCharacterStart(21798);
        span1.setCharacterEnd(21807);
        span1.setReplacement("{{{REDACTED-date}}}");
        span1.setText("1-20-0102");
        span1.setPageNumber(7);
        span1.setLineHash("1f962c9fc626ad3fdf374aba55ac4231");

        // span = characterStart: 8683;  characterEnd: 8699;  filterType: date;  context: context;  confidence: 0.75;  text: November 5, 2018;  replacement: {{{REDACTED-date}}};  salt: ;  ignored: false;  applied: true;  classification: null;  priority: 0;  line number: 145; x: 168.956; y: 634.9; page number: 2; line based character start: 13; line based character end: 29; line hash: e80797576f7f24e9d729e42fe1a4bc75
        final Span span2 = new Span();
        span2.setCharacterStart(21798);
        span2.setCharacterEnd(21807);
        span2.setReplacement("{{{REDACTED-date}}}");
        span2.setText("November 5, 2018");
        span2.setPageNumber(7);
        span2.setLineHash("e80797576f7f24e9d729e42fe1a4bc75");

        final List<Span> spans = List.of(span1, span2);

        final byte[] bytes = pdfFilterService.apply(policy, file,  spans,  MimeType.APPLICATION_PDF);

        final File outputFile = new File("/tmp/output.pdf");// File.createTempFile("output", ".pdf");
        try (FileOutputStream outputStream = new FileOutputStream(outputFile)) {
            outputStream.write(bytes);
        }

        System.out.println("Output written to " + outputFile.getAbsolutePath());

    }

    public static Policy getPolicy(String policyName) throws IOException, URISyntaxException {

        DateFilterStrategy dateFilterStrategy = new DateFilterStrategy();
        dateFilterStrategy.setStrategy(AbstractFilterStrategy.REDACT);

        Date date = new Date();
        date.setDateFilterStrategies(List.of(dateFilterStrategy));

        EmailAddressFilterStrategy emailAddressFilterStrategy = new EmailAddressFilterStrategy();

        EmailAddress emailAddress = new EmailAddress();
        emailAddress.setEmailAddressFilterStrategies(List.of(emailAddressFilterStrategy));

        ZipCodeFilterStrategy zipCodeFilterStrategy = new ZipCodeFilterStrategy();
        zipCodeFilterStrategy.setTruncateDigits(2);

        ZipCode zipCode = new ZipCode();
        zipCode.setZipCodeFilterStrategies(List.of(zipCodeFilterStrategy));

        Identifiers identifiers = new Identifiers();

        identifiers.setDate(date);
        identifiers.setEmailAddress(emailAddress);
        identifiers.setZipCode(zipCode);

        Policy policy = new Policy();
        policy.setIdentifiers(identifiers);

        return policy;

    }

}
