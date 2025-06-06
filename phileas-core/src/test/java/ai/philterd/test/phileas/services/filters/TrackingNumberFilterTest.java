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
package ai.philterd.test.phileas.services.filters;

import ai.philterd.phileas.model.cache.InMemoryCache;
import ai.philterd.phileas.model.enums.FilterType;
import ai.philterd.phileas.model.filter.FilterConfiguration;
import ai.philterd.phileas.model.objects.FilterResult;
import ai.philterd.phileas.model.policy.filters.strategies.rules.TrackingNumberFilterStrategy;
import ai.philterd.phileas.model.services.AlertService;
import ai.philterd.phileas.services.anonymization.AlphanumericAnonymizationService;

import ai.philterd.phileas.services.filters.regex.TrackingNumberFilter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

public class TrackingNumberFilterTest extends AbstractFilterTest {

    private final AlertService alertService = Mockito.mock(AlertService.class);

    @Test
    public void filter0() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new TrackingNumberFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new AlphanumericAnonymizationService(new InMemoryCache()))
                .withWindowSize(windowSize)
                .build();

        final TrackingNumberFilter filter = new TrackingNumberFilter(filterConfiguration, true, true, true);

        final FilterResult filterResult = filter.filter(getPolicy(), "context", "documentid", PIECE, "the tracking number is 1Z9YF1280343418566", attributes);

        showSpans(filterResult.getSpans());

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 23, 41, FilterType.TRACKING_NUMBER));
        Assertions.assertEquals("{{{REDACTED-tracking-number}}}", filterResult.getSpans().get(0).getReplacement());
        Assertions.assertEquals("1Z9YF1280343418566", filterResult.getSpans().get(0).getText());
        Assertions.assertEquals("ups", filterResult.getSpans().get(0).getClassification());

    }

    @Test
    public void filter1() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new TrackingNumberFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new AlphanumericAnonymizationService(new InMemoryCache()))
                .withWindowSize(windowSize)
                .build();

        final TrackingNumberFilter filter = new TrackingNumberFilter(filterConfiguration, true, true, true);

        final FilterResult filterResult = filter.filter(getPolicy(), "context", "documentid", PIECE, "the tracking number is 9400100000000000000000", attributes);

        showSpans(filterResult.getSpans());

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 23, 45, FilterType.TRACKING_NUMBER));
        Assertions.assertEquals("{{{REDACTED-tracking-number}}}", filterResult.getSpans().get(0).getReplacement());
        Assertions.assertEquals("9400100000000000000000", filterResult.getSpans().get(0).getText());
        Assertions.assertEquals("fedex", filterResult.getSpans().get(0).getClassification());

    }

    @Test
    @Disabled
    public void filter2() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new TrackingNumberFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new AlphanumericAnonymizationService(new InMemoryCache()))
                .withWindowSize(windowSize)
                .build();

        final TrackingNumberFilter filter = new TrackingNumberFilter(filterConfiguration, true, true, true);

        final FilterResult filterResult = filter.filter(getPolicy(), "context", "documentid", PIECE, "the tracking number is 9400 1000 0000 0000 0000", attributes);

        showSpans(filterResult.getSpans());

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 23, 47, FilterType.TRACKING_NUMBER));
        Assertions.assertEquals("{{{REDACTED-tracking-number}}}", filterResult.getSpans().get(0).getReplacement());
        Assertions.assertEquals("9400 1000 0000 0000 0000", filterResult.getSpans().get(0).getText());
        Assertions.assertEquals("usps", filterResult.getSpans().get(0).getClassification());

    }

    @Test
    public void filter3() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new TrackingNumberFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new AlphanumericAnonymizationService(new InMemoryCache()))
                .withWindowSize(windowSize)
                .build();

        final TrackingNumberFilter filter = new TrackingNumberFilter(filterConfiguration, true, true, true);

        final FilterResult filterResult = filter.filter(getPolicy(), "context", "documentid", PIECE, "the tracking number is 4204319935009201990138501144099814", attributes);

        showSpans(filterResult.getSpans());

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 23, 57, FilterType.TRACKING_NUMBER));
        Assertions.assertEquals("{{{REDACTED-tracking-number}}}", filterResult.getSpans().get(0).getReplacement());
        Assertions.assertEquals("4204319935009201990138501144099814", filterResult.getSpans().get(0).getText());
        Assertions.assertEquals("usps", filterResult.getSpans().get(0).getClassification());

    }

    @Test
    public void filter4() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new TrackingNumberFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new AlphanumericAnonymizationService(new InMemoryCache()))
                .withWindowSize(windowSize)
                .build();

        final TrackingNumberFilter filter = new TrackingNumberFilter(filterConfiguration, true, true, true);

        final FilterResult filterResult = filter.filter(getPolicy(), "context", "documentid", PIECE, "the tracking number is 420431993500920199013850114409", attributes);

        showSpans(filterResult.getSpans());

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 23, 53, FilterType.TRACKING_NUMBER));
        Assertions.assertEquals("{{{REDACTED-tracking-number}}}", filterResult.getSpans().get(0).getReplacement());
        Assertions.assertEquals("420431993500920199013850114409", filterResult.getSpans().get(0).getText());
        Assertions.assertEquals("usps", filterResult.getSpans().get(0).getClassification());

    }

    @Test
    public void filter5() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new TrackingNumberFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new AlphanumericAnonymizationService(new InMemoryCache()))
                .withWindowSize(windowSize)
                .build();

        final TrackingNumberFilter filter = new TrackingNumberFilter(filterConfiguration, true, true, true);

        final FilterResult filterResult = filter.filter(getPolicy(), "context", "documentid", PIECE, "the tracking number is 4204319935009201990138501144", attributes);

        showSpans(filterResult.getSpans());

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 23, 51, FilterType.TRACKING_NUMBER));
        Assertions.assertEquals("{{{REDACTED-tracking-number}}}", filterResult.getSpans().get(0).getReplacement());
        Assertions.assertEquals("4204319935009201990138501144", filterResult.getSpans().get(0).getText());
        Assertions.assertEquals("usps", filterResult.getSpans().get(0).getClassification());

    }

    @Test
    public void filter6() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new TrackingNumberFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new AlphanumericAnonymizationService(new InMemoryCache()))
                .withWindowSize(windowSize)
                .build();

        final TrackingNumberFilter filter = new TrackingNumberFilter(filterConfiguration, true, true, true);

        final FilterResult filterResult = filter.filter(getPolicy(), "context", "documentid", PIECE, "the tracking number is 42043199350092019901385011", attributes);

        showSpans(filterResult.getSpans());

        Assertions.assertEquals(1, filterResult.getSpans().size());

        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 23, 49, FilterType.TRACKING_NUMBER));
        Assertions.assertEquals("{{{REDACTED-tracking-number}}}", filterResult.getSpans().get(0).getReplacement());
        Assertions.assertEquals("42043199350092019901385011", filterResult.getSpans().get(0).getText());
        Assertions.assertEquals("ups", filterResult.getSpans().get(0).getClassification());

    }

    @Test
    public void filter7() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new TrackingNumberFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new AlphanumericAnonymizationService(new InMemoryCache()))
                .withWindowSize(windowSize)
                .build();

        final TrackingNumberFilter filter = new TrackingNumberFilter(filterConfiguration, false, false, true);

        final FilterResult filterResult = filter.filter(getPolicy(), "context", "documentid", PIECE, "the tracking number is 42043199350092019901385011", attributes);

        showSpans(filterResult.getSpans());

        Assertions.assertEquals(1, filterResult.getSpans().size());

        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 23, 49, FilterType.TRACKING_NUMBER));
        Assertions.assertEquals("{{{REDACTED-tracking-number}}}", filterResult.getSpans().get(0).getReplacement());
        Assertions.assertEquals("42043199350092019901385011", filterResult.getSpans().get(0).getText());
        Assertions.assertEquals("usps", filterResult.getSpans().get(0).getClassification());

    }

    @Test
    public void filter8() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new TrackingNumberFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new AlphanumericAnonymizationService(new InMemoryCache()))
                .withWindowSize(windowSize)
                .build();

        final TrackingNumberFilter filter = new TrackingNumberFilter(filterConfiguration, false, false, false);

        final FilterResult filterResult = filter.filter(getPolicy(), "context", "documentid", PIECE, "the tracking number is 42043199350092019901385011", attributes);

        showSpans(filterResult.getSpans());

        Assertions.assertEquals(0, filterResult.getSpans().size());

    }

    @Test
    public void filter9() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new TrackingNumberFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new AlphanumericAnonymizationService(new InMemoryCache()))
                .withWindowSize(windowSize)
                .build();

        final TrackingNumberFilter filter = new TrackingNumberFilter(filterConfiguration, true, false, true);

        final FilterResult filterResult = filter.filter(getPolicy(), "context", "documentid", PIECE, "the tracking number is 9400100000000000000000", attributes);

        showSpans(filterResult.getSpans());

        Assertions.assertEquals(1, filterResult.getSpans().size());

        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 23, 45, FilterType.TRACKING_NUMBER));
        Assertions.assertEquals("{{{REDACTED-tracking-number}}}", filterResult.getSpans().get(0).getReplacement());
        Assertions.assertEquals("9400100000000000000000", filterResult.getSpans().get(0).getText());
        Assertions.assertEquals("usps", filterResult.getSpans().get(0).getClassification());

    }

    public void filter10() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new TrackingNumberFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new AlphanumericAnonymizationService(new InMemoryCache()))
                .withWindowSize(windowSize)
                .build();

        final TrackingNumberFilter filter = new TrackingNumberFilter(filterConfiguration, true, true, false);

        final FilterResult filterResult = filter.filter(getPolicy(), "context", "documentid", PIECE, "the tracking number is 9400100000000000000000", attributes);

        showSpans(filterResult.getSpans());

        Assertions.assertEquals(1, filterResult.getSpans().size());

        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 23, 45, FilterType.TRACKING_NUMBER));
        Assertions.assertEquals("{{{REDACTED-tracking-number}}}", filterResult.getSpans().get(0).getReplacement());
        Assertions.assertEquals("9400100000000000000000", filterResult.getSpans().get(0).getText());
        Assertions.assertEquals("fedex", filterResult.getSpans().get(0).getClassification());

    }

    public void filter11() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new TrackingNumberFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new AlphanumericAnonymizationService(new InMemoryCache()))
                .withWindowSize(windowSize)
                .build();

        final TrackingNumberFilter filter = new TrackingNumberFilter(filterConfiguration, true, true, true);

        final FilterResult filterResult = filter.filter(getPolicy(), "context", "documentid", PIECE, "the tracking number is 9400100000000000000000", attributes);

        showSpans(filterResult.getSpans());

        Assertions.assertEquals(1, filterResult.getSpans().size());

        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 23, 45, FilterType.TRACKING_NUMBER));
        Assertions.assertEquals("{{{REDACTED-tracking-number}}}", filterResult.getSpans().get(0).getReplacement());
        Assertions.assertEquals("9400100000000000000000", filterResult.getSpans().get(0).getText());
        Assertions.assertEquals("fedex", filterResult.getSpans().get(0).getClassification());

    }

}
