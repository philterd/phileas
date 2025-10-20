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
package ai.philterd.phileas.filters;

import ai.philterd.phileas.model.filtering.FilterType;
import ai.philterd.phileas.model.filtering.Filtered;
import ai.philterd.phileas.services.anonymization.AlphanumericAnonymizationService;
import ai.philterd.phileas.services.context.DefaultContextService;
import ai.philterd.phileas.services.filters.regex.TrackingNumberFilter;
import ai.philterd.phileas.services.strategies.rules.TrackingNumberFilterStrategy;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.List;

public class TrackingNumberFilterTest extends AbstractFilterTest {

    @Test
    public void filter0() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new TrackingNumberFilterStrategy()))
                .withAnonymizationService(new AlphanumericAnonymizationService(new DefaultContextService()))
                .withWindowSize(windowSize)
                .build();

        final TrackingNumberFilter filter = new TrackingNumberFilter(filterConfiguration, true, true, true);

        final Filtered filtered = filter.filter(getPolicy(), "context", PIECE, "the tracking number is 1Z9YF1280343418566");

        showSpans(filtered.getSpans());

        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 23, 41, FilterType.TRACKING_NUMBER));
        Assertions.assertEquals("{{{REDACTED-tracking-number}}}", filtered.getSpans().get(0).getReplacement());
        Assertions.assertEquals("1Z9YF1280343418566", filtered.getSpans().get(0).getText());
        Assertions.assertEquals("ups", filtered.getSpans().get(0).getClassification());

    }

    @Test
    public void filter1() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new TrackingNumberFilterStrategy()))
                .withAnonymizationService(new AlphanumericAnonymizationService(new DefaultContextService()))
                .withWindowSize(windowSize)
                .build();

        final TrackingNumberFilter filter = new TrackingNumberFilter(filterConfiguration, true, true, true);

        final Filtered filtered = filter.filter(getPolicy(), "context", PIECE, "the tracking number is 9400100000000000000000");

        showSpans(filtered.getSpans());

        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 23, 45, FilterType.TRACKING_NUMBER));
        Assertions.assertEquals("{{{REDACTED-tracking-number}}}", filtered.getSpans().get(0).getReplacement());
        Assertions.assertEquals("9400100000000000000000", filtered.getSpans().get(0).getText());
        Assertions.assertEquals("fedex", filtered.getSpans().get(0).getClassification());

    }

    @Test
    @Disabled
    public void filter2() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new TrackingNumberFilterStrategy()))
                .withAnonymizationService(new AlphanumericAnonymizationService(new DefaultContextService()))
                .withWindowSize(windowSize)
                .build();

        final TrackingNumberFilter filter = new TrackingNumberFilter(filterConfiguration, true, true, true);

        final Filtered filtered = filter.filter(getPolicy(), "context", PIECE, "the tracking number is 9400 1000 0000 0000 0000");

        showSpans(filtered.getSpans());

        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 23, 47, FilterType.TRACKING_NUMBER));
        Assertions.assertEquals("{{{REDACTED-tracking-number}}}", filtered.getSpans().get(0).getReplacement());
        Assertions.assertEquals("9400 1000 0000 0000 0000", filtered.getSpans().get(0).getText());
        Assertions.assertEquals("usps", filtered.getSpans().get(0).getClassification());

    }

    @Test
    public void filter3() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new TrackingNumberFilterStrategy()))
                .withAnonymizationService(new AlphanumericAnonymizationService(new DefaultContextService()))
                .withWindowSize(windowSize)
                .build();

        final TrackingNumberFilter filter = new TrackingNumberFilter(filterConfiguration, true, true, true);

        final Filtered filtered = filter.filter(getPolicy(), "context", PIECE, "the tracking number is 4204319935009201990138501144099814");

        showSpans(filtered.getSpans());

        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 23, 57, FilterType.TRACKING_NUMBER));
        Assertions.assertEquals("{{{REDACTED-tracking-number}}}", filtered.getSpans().get(0).getReplacement());
        Assertions.assertEquals("4204319935009201990138501144099814", filtered.getSpans().get(0).getText());
        Assertions.assertEquals("usps", filtered.getSpans().get(0).getClassification());

    }

    @Test
    public void filter4() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new TrackingNumberFilterStrategy()))
                .withAnonymizationService(new AlphanumericAnonymizationService(new DefaultContextService()))
                .withWindowSize(windowSize)
                .build();

        final TrackingNumberFilter filter = new TrackingNumberFilter(filterConfiguration, true, true, true);

        final Filtered filtered = filter.filter(getPolicy(), "context", PIECE, "the tracking number is 420431993500920199013850114409");

        showSpans(filtered.getSpans());

        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 23, 53, FilterType.TRACKING_NUMBER));
        Assertions.assertEquals("{{{REDACTED-tracking-number}}}", filtered.getSpans().get(0).getReplacement());
        Assertions.assertEquals("420431993500920199013850114409", filtered.getSpans().get(0).getText());
        Assertions.assertEquals("usps", filtered.getSpans().get(0).getClassification());

    }

    @Test
    public void filter5() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new TrackingNumberFilterStrategy()))
                .withAnonymizationService(new AlphanumericAnonymizationService(new DefaultContextService()))
                .withWindowSize(windowSize)
                .build();

        final TrackingNumberFilter filter = new TrackingNumberFilter(filterConfiguration, true, true, true);

        final Filtered filtered = filter.filter(getPolicy(), "context", PIECE, "the tracking number is 4204319935009201990138501144");

        showSpans(filtered.getSpans());

        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 23, 51, FilterType.TRACKING_NUMBER));
        Assertions.assertEquals("{{{REDACTED-tracking-number}}}", filtered.getSpans().get(0).getReplacement());
        Assertions.assertEquals("4204319935009201990138501144", filtered.getSpans().get(0).getText());
        Assertions.assertEquals("usps", filtered.getSpans().get(0).getClassification());

    }

    @Test
    public void filter6() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new TrackingNumberFilterStrategy()))
                .withAnonymizationService(new AlphanumericAnonymizationService(new DefaultContextService()))
                .withWindowSize(windowSize)
                .build();

        final TrackingNumberFilter filter = new TrackingNumberFilter(filterConfiguration, true, true, true);

        final Filtered filtered = filter.filter(getPolicy(), "context", PIECE, "the tracking number is 42043199350092019901385011");

        showSpans(filtered.getSpans());

        Assertions.assertEquals(1, filtered.getSpans().size());

        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 23, 49, FilterType.TRACKING_NUMBER));
        Assertions.assertEquals("{{{REDACTED-tracking-number}}}", filtered.getSpans().get(0).getReplacement());
        Assertions.assertEquals("42043199350092019901385011", filtered.getSpans().get(0).getText());
        Assertions.assertEquals("ups", filtered.getSpans().get(0).getClassification());

    }

    @Test
    public void filter7() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new TrackingNumberFilterStrategy()))
                .withAnonymizationService(new AlphanumericAnonymizationService(new DefaultContextService()))
                .withWindowSize(windowSize)
                .build();

        final TrackingNumberFilter filter = new TrackingNumberFilter(filterConfiguration, false, false, true);

        final Filtered filtered = filter.filter(getPolicy(), "context", PIECE, "the tracking number is 42043199350092019901385011");

        showSpans(filtered.getSpans());

        Assertions.assertEquals(1, filtered.getSpans().size());

        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 23, 49, FilterType.TRACKING_NUMBER));
        Assertions.assertEquals("{{{REDACTED-tracking-number}}}", filtered.getSpans().get(0).getReplacement());
        Assertions.assertEquals("42043199350092019901385011", filtered.getSpans().get(0).getText());
        Assertions.assertEquals("usps", filtered.getSpans().get(0).getClassification());

    }

    @Test
    public void filter8() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new TrackingNumberFilterStrategy()))
                .withAnonymizationService(new AlphanumericAnonymizationService(new DefaultContextService()))
                .withWindowSize(windowSize)
                .build();

        final TrackingNumberFilter filter = new TrackingNumberFilter(filterConfiguration, false, false, false);

        final Filtered filtered = filter.filter(getPolicy(), "context", PIECE, "the tracking number is 42043199350092019901385011");

        showSpans(filtered.getSpans());

        Assertions.assertEquals(0, filtered.getSpans().size());

    }

    @Test
    public void filter9() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new TrackingNumberFilterStrategy()))
                .withAnonymizationService(new AlphanumericAnonymizationService(new DefaultContextService()))
                .withWindowSize(windowSize)
                .build();

        final TrackingNumberFilter filter = new TrackingNumberFilter(filterConfiguration, true, false, true);

        final Filtered filtered = filter.filter(getPolicy(), "context", PIECE, "the tracking number is 9400100000000000000000");

        showSpans(filtered.getSpans());

        Assertions.assertEquals(1, filtered.getSpans().size());

        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 23, 45, FilterType.TRACKING_NUMBER));
        Assertions.assertEquals("{{{REDACTED-tracking-number}}}", filtered.getSpans().get(0).getReplacement());
        Assertions.assertEquals("9400100000000000000000", filtered.getSpans().get(0).getText());
        Assertions.assertEquals("usps", filtered.getSpans().get(0).getClassification());

    }

    public void filter10() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new TrackingNumberFilterStrategy()))
                .withAnonymizationService(new AlphanumericAnonymizationService(new DefaultContextService()))
                .withWindowSize(windowSize)
                .build();

        final TrackingNumberFilter filter = new TrackingNumberFilter(filterConfiguration, true, true, false);

        final Filtered filtered = filter.filter(getPolicy(), "context", PIECE, "the tracking number is 9400100000000000000000");

        showSpans(filtered.getSpans());

        Assertions.assertEquals(1, filtered.getSpans().size());

        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 23, 45, FilterType.TRACKING_NUMBER));
        Assertions.assertEquals("{{{REDACTED-tracking-number}}}", filtered.getSpans().get(0).getReplacement());
        Assertions.assertEquals("9400100000000000000000", filtered.getSpans().get(0).getText());
        Assertions.assertEquals("fedex", filtered.getSpans().get(0).getClassification());

    }

    public void filter11() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new TrackingNumberFilterStrategy()))
                .withAnonymizationService(new AlphanumericAnonymizationService(new DefaultContextService()))
                .withWindowSize(windowSize)
                .build();

        final TrackingNumberFilter filter = new TrackingNumberFilter(filterConfiguration, true, true, true);

        final Filtered filtered = filter.filter(getPolicy(), "context", PIECE, "the tracking number is 9400100000000000000000");

        showSpans(filtered.getSpans());

        Assertions.assertEquals(1, filtered.getSpans().size());

        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 23, 45, FilterType.TRACKING_NUMBER));
        Assertions.assertEquals("{{{REDACTED-tracking-number}}}", filtered.getSpans().get(0).getReplacement());
        Assertions.assertEquals("9400100000000000000000", filtered.getSpans().get(0).getText());
        Assertions.assertEquals("fedex", filtered.getSpans().get(0).getClassification());

    }

}
