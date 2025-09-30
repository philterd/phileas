/*
 *     Copyright 2025 Philterd, LLC @ https://www.philterd.ai
 *
 * Licensed under the Apache License, Version 2.0 (the "License", attributes);
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
package ai.philterd.phileas.filters;

import ai.philterd.phileas.model.enums.FilterType;
import ai.philterd.phileas.model.objects.FilterResult;
import ai.philterd.phileas.services.context.DefaultContextService;
import ai.philterd.phileas.services.anonymization.AlphanumericAnonymizationService;
import ai.philterd.phileas.services.filters.regex.SectionFilter;
import ai.philterd.phileas.services.strategies.rules.SectionFilterStrategy;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

public class SectionFilterTest extends AbstractFilterTest {

    @Test
    public void filterSection1() throws Exception {

        final String startPattern = "BEGIN-REDACT";
        final String endPattern = "END-REDACT";

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new SectionFilterStrategy()))
                .withAnonymizationService(new AlphanumericAnonymizationService(new DefaultContextService()))
                .withWindowSize(windowSize)
                .build();

        final SectionFilter filter = new SectionFilter(filterConfiguration, startPattern, endPattern);

        final FilterResult filterResult = filter.filter(getPolicy(), "context",  "documentid", PIECE, "This is some test. BEGIN-REDACT This text should be redacted. END-REDACT This is outside the text.", attributes);

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 19, 72, FilterType.SECTION));
        Assertions.assertEquals("BEGIN-REDACT This text should be redacted. END-REDACT", filterResult.getSpans().get(0).getText());

    }

    @Test
    public void filterSection2() throws Exception {

        final String startPattern = "BEGIN-REDACT";
        final String endPattern = "END-REDACT";

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new SectionFilterStrategy()))
                .withAnonymizationService(new AlphanumericAnonymizationService(new DefaultContextService()))
                .withWindowSize(windowSize)
                .build();

        final SectionFilter filter = new SectionFilter(filterConfiguration, startPattern, endPattern);

        final FilterResult filterResult = filter.filter(getPolicy(), "context",  "documentid", PIECE, "This is some test. BEGIN-REDACT This text should be redacted. This is outside the text.", attributes);

        Assertions.assertEquals(0, filterResult.getSpans().size());

    }

    @Test
    public void filterSection3() throws Exception {

        final String startPattern = "BEGIN-REDACT";
        final String endPattern = "END-REDACT";

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new SectionFilterStrategy()))
                .withAnonymizationService(new AlphanumericAnonymizationService(new DefaultContextService()))
                .withWindowSize(windowSize)
                .build();

        final SectionFilter filter = new SectionFilter(filterConfiguration, startPattern, endPattern);

        final FilterResult filterResult = filter.filter(getPolicy(), "context",  "documentid", PIECE, "BEGIN-REDACT This text should be redacted. END-REDACT This is outside the text.", attributes);

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 0, 53, FilterType.SECTION));
        Assertions.assertEquals("BEGIN-REDACT This text should be redacted. END-REDACT", filterResult.getSpans().get(0).getText());

    }

}
