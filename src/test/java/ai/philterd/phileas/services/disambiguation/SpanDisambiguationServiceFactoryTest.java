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
package ai.philterd.phileas.services.disambiguation;

import ai.philterd.phileas.PhileasConfiguration;
import ai.philterd.phileas.model.filtering.FilterType;
import ai.philterd.phileas.model.filtering.Span;
import ai.philterd.phileas.services.disambiguation.vector.InMemoryVectorService;
import ai.philterd.phileas.services.disambiguation.vector.VectorBasedSpanDisambiguationService;
import ai.philterd.phileas.services.disambiguation.vector.VectorService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public class SpanDisambiguationServiceFactoryTest {

    private SpanDisambiguationService build(final boolean enabled, final VectorService vectorService) {
        final Properties properties = new Properties();
        properties.setProperty("span.disambiguation.enabled", Boolean.toString(enabled));
        return SpanDisambiguationServiceFactory.getSpanDisambiguationService(
                new PhileasConfiguration(properties), vectorService);
    }

    @Test
    public void enabledConfigurationYieldsTheVectorBasedService() {
        Assertions.assertInstanceOf(VectorBasedSpanDisambiguationService.class,
                build(true, new InMemoryVectorService()),
                "disambiguation enabled should produce the vector-based implementation");
    }

    @Test
    public void disabledConfigurationYieldsTheNoOpService() {
        Assertions.assertInstanceOf(NoOpSpanDisambiguationService.class,
                build(false, new InMemoryVectorService()),
                "disambiguation disabled should produce the no-op implementation");
    }

    @Test
    public void noOpServiceLeavesSpansUnchangedAndDoesNotTrain() {

        // The no-op service must pass spans through untouched and record nothing, so a disabled
        // configuration behaves exactly as if disambiguation never ran.
        final InMemoryVectorService vectorService = new InMemoryVectorService();
        final SpanDisambiguationService service = build(false, vectorService);

        final Span asSsn = Span.make(0, 4, FilterType.SSN, "c", 0.5, "123456789", "x", "",
                false, true, new String[]{"phone", "number"}, 0);
        final Span asPhone = Span.make(0, 4, FilterType.PHONE_NUMBER, "c", 0.5, "123456789", "x", "",
                false, true, new String[]{"phone", "number"}, 0);

        final List<Span> spans = Arrays.asList(asSsn, asPhone);
        final List<Span> result = service.disambiguate("c", spans);

        Assertions.assertEquals(spans, result, "the no-op service should return the spans unchanged");
        Assertions.assertTrue(vectorService.getVectorRepresentation("c", FilterType.PHONE_NUMBER).isEmpty(),
                "the no-op service should not record any training data");

        // The three-argument form keeps the first candidate.
        Assertions.assertEquals(FilterType.SSN,
                service.disambiguate("c", Arrays.asList(FilterType.SSN, FilterType.PHONE_NUMBER), asSsn),
                "the no-op service should keep the first candidate");
    }

}
