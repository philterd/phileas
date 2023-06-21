/*
 *     Copyright 2023 Philerd, LLC @ https://www.philterd.ai
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
package ai.philterd.test.phileas.services.filters;

import ai.philterd.phileas.model.enums.FilterType;
import ai.philterd.phileas.model.filter.FilterConfiguration;
import ai.philterd.phileas.model.objects.FilterResult;
import ai.philterd.phileas.model.profile.filters.strategies.rules.ZipCodeFilterStrategy;
import ai.philterd.phileas.model.services.AlertService;
import ai.philterd.phileas.services.anonymization.ZipCodeAnonymizationService;
import ai.philterd.phileas.services.anonymization.cache.LocalAnonymizationCacheService;
import ai.philterd.phileas.services.filters.regex.ZipCodeFilter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;

public class ZipCodeFilterTest extends AbstractFilterTest {

    private final AlertService alertService = Mockito.mock(AlertService.class);

    @Test
    public void filterZipCode1() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(new ZipCodeFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new ZipCodeAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final ZipCodeFilter filter = new ZipCodeFilter(filterConfiguration, true);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", PIECE, "the zip is 90210.");
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 11, 16, FilterType.ZIP_CODE));
        Assertions.assertEquals("90210", filterResult.getSpans().get(0).getText());

    }

    @Test
    public void filterZipCode2() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(new ZipCodeFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new ZipCodeAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final ZipCodeFilter filter = new ZipCodeFilter(filterConfiguration, true);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", PIECE, "the zip is 90210abd.");
        Assertions.assertEquals(0, filterResult.getSpans().size());

    }

    @Test
    public void filterZipCode3() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(new ZipCodeFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new ZipCodeAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final ZipCodeFilter filter = new ZipCodeFilter(filterConfiguration, true);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", PIECE, "the zip is 90210 in california.");
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 11, 16, FilterType.ZIP_CODE));

    }

    @Test
    public void filterZipCode4() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(new ZipCodeFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new ZipCodeAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final ZipCodeFilter filter = new ZipCodeFilter(filterConfiguration, true);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", PIECE, "the zip is 85055 in california.");
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 11, 16, FilterType.ZIP_CODE));

    }

    @Test
    public void filterZipCode5() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(new ZipCodeFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new ZipCodeAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final ZipCodeFilter filter = new ZipCodeFilter(filterConfiguration, true);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", PIECE, "the zip is 90213-1544 in california.");
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 11, 21, FilterType.ZIP_CODE));

    }

    @Test
    public void filterZipCode6() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(new ZipCodeFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new ZipCodeAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final ZipCodeFilter filter = new ZipCodeFilter(filterConfiguration, true);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", PIECE, "George Washington was president and his ssn was 123-45-6789 and he lived in 90210.");
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 76, 81, FilterType.ZIP_CODE));

    }

    @Test
    public void filterZipCode7() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(new ZipCodeFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new ZipCodeAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final ZipCodeFilter filter = new ZipCodeFilter(filterConfiguration, true);

        // Tests whole word only.
        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", PIECE, "George Washington was president and his ssn was 123-45-6789 and he lived in 9021032.");
        Assertions.assertEquals(0, filterResult.getSpans().size());

    }

    @Test
    public void filterZipCode8() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(new ZipCodeFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new ZipCodeAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final ZipCodeFilter filter = new ZipCodeFilter(filterConfiguration, true);

        // Tests whole word only.
        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", PIECE, "George Washington was president and his ssn was 123-45-6789 and he lived in 90210-1234.");
        Assertions.assertEquals(1, filterResult.getSpans().size());

    }

    @Test
    public void filterZipCode9() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(new ZipCodeFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new ZipCodeAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final ZipCodeFilter filter = new ZipCodeFilter(filterConfiguration, false);

        // Tests without delimiter.
        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", PIECE, "George Washington was president and his ssn was 123-45-6789 and he lived in 902101234.");
        Assertions.assertEquals(1, filterResult.getSpans().size());

    }

    @Test
    public void filterZipCode10() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(new ZipCodeFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new ZipCodeAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final ZipCodeFilter filter = new ZipCodeFilter(filterConfiguration, true);

        // Tests without delimiter.
        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", PIECE, "George Washington was president and his ssn was 123-45-6789 and he lived in 902101234.");
        Assertions.assertEquals(0, filterResult.getSpans().size());

    }

}
