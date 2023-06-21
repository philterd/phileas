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
import ai.philterd.phileas.model.profile.filters.strategies.rules.SsnFilterStrategy;
import ai.philterd.phileas.model.services.AlertService;
import ai.philterd.phileas.services.anonymization.AlphanumericAnonymizationService;
import ai.philterd.phileas.services.anonymization.cache.LocalAnonymizationCacheService;
import ai.philterd.phileas.services.filters.regex.SsnFilter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;

public class SsnFilterTest extends AbstractFilterTest {

    private AlertService alertService = Mockito.mock(AlertService.class);

    @Test
    public void filterSsn1() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(new SsnFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new AlphanumericAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final SsnFilter filter = new SsnFilter(filterConfiguration);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", PIECE, "the ssn is 123-45-6789.");
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 11, 22, FilterType.SSN));
        Assertions.assertEquals("123-45-6789", filterResult.getSpans().get(0).getText());

    }

    @Test
    public void filterSsn2() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(new SsnFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new AlphanumericAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final SsnFilter filter = new SsnFilter(filterConfiguration);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", PIECE, "the ssn is 123456789.");
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 11, 20, FilterType.SSN));

    }

    @Test
    public void filterSsn3() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(new SsnFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new AlphanumericAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final SsnFilter filter = new SsnFilter(filterConfiguration);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", PIECE, "the ssn is 123 45 6789.");
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 11, 22, FilterType.SSN));

    }

    @Test
    public void filterSsn4() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(new SsnFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new AlphanumericAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final SsnFilter filter = new SsnFilter(filterConfiguration);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", PIECE, "the ssn is 123 45 6789.");
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 11, 22, FilterType.SSN));

    }

    @Test
    public void filterSsn5() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(new SsnFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new AlphanumericAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final SsnFilter filter = new SsnFilter(filterConfiguration);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", PIECE, "the ssn is 123 454 6789.");
        Assertions.assertEquals(0, filterResult.getSpans().size());

    }

    @Test
    public void filterSsn6() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(new SsnFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new AlphanumericAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final SsnFilter filter = new SsnFilter(filterConfiguration);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", PIECE, "the ssn is 123 4f 6789.");
        Assertions.assertEquals(0, filterResult.getSpans().size());

    }

    @Test
    public void filterSsn7() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(new SsnFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new AlphanumericAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final SsnFilter filter = new SsnFilter(filterConfiguration);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", PIECE, "the ssn is 11-1234567.");
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 11, 21, FilterType.SSN));

    }

}
