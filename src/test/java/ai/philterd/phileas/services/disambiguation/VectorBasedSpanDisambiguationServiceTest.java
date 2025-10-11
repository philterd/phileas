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
package ai.philterd.phileas.services.disambiguation;

import ai.philterd.phileas.PhileasConfiguration;
import ai.philterd.phileas.model.enums.FilterType;
import ai.philterd.phileas.model.objects.Span;
import ai.philterd.phileas.services.disambiguation.vector.InMemoryVectorService;
import ai.philterd.phileas.services.disambiguation.vector.VectorBasedSpanDisambiguationService;
import ai.philterd.phileas.services.disambiguation.vector.VectorService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public class VectorBasedSpanDisambiguationServiceTest {

    private static final Logger LOGGER = LogManager.getLogger(VectorBasedSpanDisambiguationServiceTest.class);

    @Test
    public void disambiguateLocal1() throws IOException {

        final VectorService vectorService = new InMemoryVectorService();

        final Properties properties = new Properties();

        properties.setProperty("span.disambiguation.enabled", "true");
        properties.setProperty("span.disambiguation.ignore.stopwords", "false");
        properties.setProperty("span.disambiguation.vector.size", "32");

        final PhileasConfiguration phileasConfiguration = new PhileasConfiguration(properties);

        final String context = "c";

        final VectorBasedSpanDisambiguationService vectorBasedSpanDisambiguationService = new VectorBasedSpanDisambiguationService(phileasConfiguration, vectorService);

        final Span span1 = Span.make(0, 4, FilterType.SSN, context, 0.00, "123-45-6789", "000-00-0000", "", false, true, new String[]{"ssn", "was", "he", "id"}, 0);
        vectorBasedSpanDisambiguationService.hashAndInsert(context, span1);

        final Span span = Span.make(0, 4, FilterType.SSN, context, 0.00, "123-45-6789", "000-00-0000",  "", false, true, new String[]{"ssn", "asdf", "he", "was"}, 0);
        vectorBasedSpanDisambiguationService.hashAndInsert(context, span);

        final Span span2 = Span.make(0, 4, FilterType.PHONE_NUMBER, "c", 0.00, "123-45-6789", "000-00-0000",  "", false, true, new String[]{"phone", "number", "she", "had"}, 0);
        vectorBasedSpanDisambiguationService.hashAndInsert(context, span2);

        final List<FilterType> filterTypes = Arrays.asList(span1.getFilterType(), span2.getFilterType());

        final Span ambiguousSpan = Span.make(0, 4, FilterType.PHONE_NUMBER, "c", 0.00, "123-45-6789", "000-00-0000",  "", false, true, new String[]{"phone", "number", "called", "is"}, 0);
        final FilterType filterType = vectorBasedSpanDisambiguationService.disambiguate(context, filterTypes, ambiguousSpan);

        Assertions.assertEquals(FilterType.PHONE_NUMBER, filterType);

    }

    @Test
    public void disambiguateLocal2() throws IOException {

        final VectorService vectorService = new InMemoryVectorService();

        final Properties properties = new Properties();

        properties.setProperty("span.disambiguation.enabled", "true");
        properties.setProperty("span.disambiguation.ignore.stopwords", "false");
        properties.setProperty("span.disambiguation.vector.size", "32");

        final PhileasConfiguration phileasConfiguration = new PhileasConfiguration(properties);

        final String context = "c";

        final VectorBasedSpanDisambiguationService vectorBasedSpanDisambiguationService = new VectorBasedSpanDisambiguationService(phileasConfiguration, vectorService);

        final Span span1 = Span.make(0, 4, FilterType.SSN, context, 0.00, "123-45-6789", "000-00-0000",  "", false, true, new String[]{"ssn", "was", "he", "id"}, 0);
        vectorBasedSpanDisambiguationService.hashAndInsert(context, span1);

        final Span span = Span.make(0, 4, FilterType.SSN, context, 0.00, "123-45-6789", "000-00-0000",  "", false, true, new String[]{"ssn", "asdf", "he", "was"}, 0);
        vectorBasedSpanDisambiguationService.hashAndInsert(context, span);

        final Span span2 = Span.make(0, 4, FilterType.PHONE_NUMBER, "c", 0.00, "123-45-6789", "000-00-0000",  "", false, true, new String[]{"phone", "number", "she", "had"}, 0);
        vectorBasedSpanDisambiguationService.hashAndInsert(context, span2);

        final Span ambiguousSpan = Span.make(0, 4, FilterType.PHONE_NUMBER, "c", 0.00, "123-45-6789", "000-00-0000", "",  false, true, new String[]{"phone", "number", "called", "is"}, 0);

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
