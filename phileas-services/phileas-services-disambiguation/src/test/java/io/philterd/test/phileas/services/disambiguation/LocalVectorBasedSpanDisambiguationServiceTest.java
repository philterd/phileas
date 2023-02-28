package io.philterd.test.phileas.services.disambiguation;

import io.philterd.phileas.configuration.PhileasConfiguration;
import io.philterd.phileas.model.enums.FilterType;
import io.philterd.phileas.model.objects.Span;
import io.philterd.phileas.services.disambiguation.VectorBasedSpanDisambiguationService;
import org.aeonbits.owner.ConfigFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public class LocalVectorBasedSpanDisambiguationServiceTest {

    private static final Logger LOGGER = LogManager.getLogger(LocalVectorBasedSpanDisambiguationServiceTest.class);

    @Test
    public void disambiguateLocal1() throws IOException {

        final Properties properties = new Properties();

        properties.setProperty("span.disambiguation.enabled", "true");
        properties.setProperty("span.disambiguation.ignore.stopwords", "false");
        properties.setProperty("span.disambiguation.vector.size", "32");
        properties.setProperty("cache.redis.enabled", "false");

        final PhileasConfiguration phileasConfiguration = ConfigFactory.create(PhileasConfiguration.class, properties);

        final String context = "c";

        final VectorBasedSpanDisambiguationService vectorBasedSpanDisambiguationService = new VectorBasedSpanDisambiguationService(phileasConfiguration);

        final Span span1 = Span.make(0, 4, FilterType.SSN, context, "d", 0.00, "123-45-6789", "000-00-0000", "", false, new String[]{"ssn", "was", "he", "id"});
        vectorBasedSpanDisambiguationService.hashAndInsert(context, span1);

        final Span span = Span.make(0, 4, FilterType.SSN, context, "d", 0.00, "123-45-6789", "000-00-0000",  "", false, new String[]{"ssn", "asdf", "he", "was"});
        vectorBasedSpanDisambiguationService.hashAndInsert(context, span);

        final Span span2 = Span.make(0, 4, FilterType.PHONE_NUMBER, "c", "d", 0.00, "123-45-6789", "000-00-0000",  "", false, new String[]{"phone", "number", "she", "had"});
        vectorBasedSpanDisambiguationService.hashAndInsert(context, span2);

        final List<FilterType> filterTypes = Arrays.asList(span1.getFilterType(), span2.getFilterType());

        final Span ambiguousSpan = Span.make(0, 4, FilterType.PHONE_NUMBER, "c", "d", 0.00, "123-45-6789", "000-00-0000",  "", false, new String[]{"phone", "number", "called", "is"});
        final FilterType filterType = vectorBasedSpanDisambiguationService.disambiguate(context, filterTypes, ambiguousSpan);

        Assertions.assertEquals(FilterType.PHONE_NUMBER, filterType);

    }

    @Test
    public void disambiguateLocal2() throws IOException {

        final Properties properties = new Properties();

        properties.setProperty("span.disambiguation.enabled", "true");
        properties.setProperty("span.disambiguation.ignore.stopwords", "false");
        properties.setProperty("span.disambiguation.vector.size", "32");
        properties.setProperty("cache.redis.enabled", "false");

        final PhileasConfiguration phileasConfiguration = ConfigFactory.create(PhileasConfiguration.class, properties);

        final String context = "c";

        final VectorBasedSpanDisambiguationService vectorBasedSpanDisambiguationService = new VectorBasedSpanDisambiguationService(phileasConfiguration);

        final Span span1 = Span.make(0, 4, FilterType.SSN, context, "d", 0.00, "123-45-6789", "000-00-0000",  "", false, new String[]{"ssn", "was", "he", "id"});
        vectorBasedSpanDisambiguationService.hashAndInsert(context, span1);

        final Span span = Span.make(0, 4, FilterType.SSN, context, "d", 0.00, "123-45-6789", "000-00-0000",  "", false, new String[]{"ssn", "asdf", "he", "was"});
        vectorBasedSpanDisambiguationService.hashAndInsert(context, span);

        final Span span2 = Span.make(0, 4, FilterType.PHONE_NUMBER, "c", "d", 0.00, "123-45-6789", "000-00-0000",  "", false, new String[]{"phone", "number", "she", "had"});
        vectorBasedSpanDisambiguationService.hashAndInsert(context, span2);

        final Span ambiguousSpan = Span.make(0, 4, FilterType.PHONE_NUMBER, "c", "d", 0.00, "123-45-6789", "000-00-0000", "",  false, new String[]{"phone", "number", "called", "is"});

        final List<Span> spans = Arrays.asList(span, span1, span2, ambiguousSpan);

        final List<Span> disambiguatedSpans = vectorBasedSpanDisambiguationService.disambiguate(context, spans);

        showSpans(disambiguatedSpans);

        Assertions.assertEquals(1, disambiguatedSpans.size());
        Assertions.assertEquals(FilterType.PHONE_NUMBER, disambiguatedSpans.get(0).getFilterType());

    }

    public void showSpans(List<Span> spans) {

        for(Span span : spans) {
            LOGGER.info(span.toString());
        }

    }

}
