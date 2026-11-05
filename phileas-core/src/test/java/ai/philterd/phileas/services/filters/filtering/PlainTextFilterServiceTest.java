package ai.philterd.phileas.services.filters.filtering;

import ai.philterd.phileas.PhileasConfiguration;
import ai.philterd.phileas.model.filtering.FilterType;
import ai.philterd.phileas.model.filtering.Span;
import ai.philterd.phileas.services.context.ContextService;
import ai.philterd.phileas.services.context.DefaultContextService;
import ai.philterd.phileas.services.disambiguation.vector.InMemoryVectorService;
import ai.philterd.phileas.services.disambiguation.vector.VectorService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class PlainTextFilterServiceTest {

    private static final Logger LOGGER = LogManager.getLogger(PlainTextFilterServiceTest.class);

    @Test
    public void apply1() {

        final PhileasConfiguration phileasConfiguration = new PhileasConfiguration(new Properties());
        final ContextService contextService = new DefaultContextService();
        final VectorService vectorService = new InMemoryVectorService();

        final PlainTextFilterService service = new PlainTextFilterService(phileasConfiguration, contextService, vectorService, null);

        final String input = "George Washington whose SSN was 123-45-6789 was the first president of the United States and he lived at 90210.";

        final List<Span> spans = new ArrayList<>();
        spans.add(Span.make(0, 17, FilterType.PERSON, "context", 1.0, "George Washington", "***", "", false, true, null, 1));
        spans.add(Span.make(32, 43, FilterType.SSN, "context", 1.0, "123-45-6789", "***", "", false, true, null, 1));

        final byte[] bytes = service.apply(input.getBytes(StandardCharsets.UTF_8), spans);
        final String redacted = new String(bytes);
        LOGGER.info(redacted);

        Assertions.assertEquals("*** whose SSN was *** was the first president of the United States and he lived at 90210.", redacted);

    }

    @Test
    public void apply2() {

        final PhileasConfiguration phileasConfiguration = new PhileasConfiguration(new Properties());
        final ContextService contextService = new DefaultContextService();
        final VectorService vectorService = new InMemoryVectorService();

        final PlainTextFilterService service = new PlainTextFilterService(phileasConfiguration, contextService, vectorService, null);

        final String input = "George Washington was president and his SSN was 123-45-6789.";

        final List<Span> spans = new ArrayList<>();
        spans.add(Span.make(0, 17, FilterType.PERSON, "context", 1.0, "George Washington", "{{{REDACTED-person}}}", "", false, true, null, 1));
        spans.add(Span.make(48, 59, FilterType.SSN, "context", 1.0, "123-45-6789", "{{{REDACTED-ssn}}}", "", false, true, null, 1));

        final byte[] bytes = service.apply(input.getBytes(StandardCharsets.UTF_8), spans);
        final String redacted = new String(bytes);
        LOGGER.info(redacted);

        Assertions.assertEquals("{{{REDACTED-person}}} was president and his SSN was {{{REDACTED-ssn}}}.", redacted);

    }

    @Test
    public void apply3() {

        final PhileasConfiguration phileasConfiguration = new PhileasConfiguration(new Properties());
        final ContextService contextService = new DefaultContextService();
        final VectorService vectorService = new InMemoryVectorService();

        final PlainTextFilterService service = new PlainTextFilterService(phileasConfiguration, contextService, vectorService, null);

        final String input = "George Washington was president and his SSN was 123-45-6789.";

        final List<Span> spans = new ArrayList<>();
        spans.add(Span.make(0, 17, FilterType.PERSON, "context", 1.0, "George Washington", "***", "", false, true, null, 1));
        spans.add(Span.make(48, 59, FilterType.SSN, "context", 1.0, "123-45-6789", "***", "", false, true, null, 1));

        final byte[] bytes = service.apply(input.getBytes(StandardCharsets.UTF_8), spans);
        final String redacted = new String(bytes);
        LOGGER.info(redacted);

        Assertions.assertEquals("*** was president and his SSN was ***.", redacted);

    }

    @Test
    public void applyNoSpans() throws Exception {

        final PhileasConfiguration phileasConfiguration = new PhileasConfiguration(new Properties());
        final ContextService contextService = new DefaultContextService();
        final VectorService vectorService = new InMemoryVectorService();

        final PlainTextFilterService service = new PlainTextFilterService(phileasConfiguration, contextService, vectorService, null);

        final String input = "George Washington whose SSN was 123-45-6789";

        final List<Span> spans = new ArrayList<>();

        final byte[] bytes = service.apply(input.getBytes(StandardCharsets.UTF_8), spans);
        final String redacted = new String(bytes);
        LOGGER.info(redacted);

        Assertions.assertEquals("George Washington whose SSN was 123-45-6789", redacted);

    }

}
