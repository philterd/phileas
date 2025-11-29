package ai.philterd.phileas.services;

import ai.philterd.phileas.model.filtering.ApplyResult;
import ai.philterd.phileas.model.filtering.FilterType;
import ai.philterd.phileas.model.filtering.MimeType;
import ai.philterd.phileas.model.filtering.Span;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class PhileasApplyServiceTest {

    private static final Logger LOGGER = LogManager.getLogger(PhileasApplyServiceTest.class);

    @Test
    public void apply1() {

        final PhileasApplyService service = new PhileasApplyService();

        final String input = "George Washington whose SSN was 123-45-6789 was the first president of the United States and he lived at 90210.";

        final List<Span> spans = new ArrayList<>();
        spans.add(Span.make(0, 17, FilterType.PERSON, "context", 1.0, "George Washington", "***", "", false, true, null, 1));
        spans.add(Span.make(18, 29, FilterType.SSN, "context", 1.0, "123-45-6789", "***", "", false, true, null, 1));

        final ApplyResult applyResult = service.apply(spans, input.getBytes(StandardCharsets.UTF_8), MimeType.TEXT_PLAIN);
        LOGGER.info(applyResult.toString());

        Assertions.assertEquals("*** whose SSN was *** was the first president of the United States and he lived at 90210.", applyResult.getFilteredText());

    }

    @Test
    public void applyNoSpans() {

        final PhileasApplyService service = new PhileasApplyService();

        final String input = "George Washington whose SSN was 123-45-6789";

        final List<Span> spans = new ArrayList<>();

        final ApplyResult applyResult = service.apply(spans, input.getBytes(StandardCharsets.UTF_8), MimeType.TEXT_PLAIN);
        LOGGER.info(applyResult.toString());

        Assertions.assertEquals("George Washington whose SSN was 123-45-6789", applyResult.getFilteredText());

    }

}
