/*
 *     Copyright 2024 Philterd, LLC @ https://www.philterd.ai
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

import ai.philterd.phileas.model.enums.FilterType;
import ai.philterd.phileas.model.enums.SensitivityLevel;
import ai.philterd.phileas.model.filter.FilterConfiguration;
import ai.philterd.phileas.model.filter.rules.dictionary.FuzzyDictionaryFilter;
import ai.philterd.phileas.model.objects.FilterResult;
import ai.philterd.phileas.model.policy.filters.strategies.dynamic.FirstNameFilterStrategy;
import ai.philterd.phileas.model.services.AlertService;
import ai.philterd.phileas.services.anonymization.PersonsAnonymizationService;
import ai.philterd.phileas.services.anonymization.cache.LocalAnonymizationCacheService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

public class FirstNameFilterTest extends AbstractFilterTest {

    private static final Logger LOGGER = LogManager.getLogger(FirstNameFilterTest.class);

    private final AlertService alertService = Mockito.mock(AlertService.class);

    @Test
    public void filterLow() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new FirstNameFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new PersonsAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final FuzzyDictionaryFilter filter = new FuzzyDictionaryFilter(FilterType.FIRST_NAME, filterConfiguration, SensitivityLevel.LOW);

        final FilterResult filterResult = filter.filter(getPolicy(), "context", "documentid", PIECE,"John", attributes);
        showSpans(filterResult.getSpans());
        Assertions.assertEquals(50, filterResult.getSpans().size());

    }

    @Test
    public void filterMedium1() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new FirstNameFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new PersonsAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final FuzzyDictionaryFilter filter = new FuzzyDictionaryFilter(FilterType.FIRST_NAME, filterConfiguration, SensitivityLevel.MEDIUM);

        final FilterResult filterResult = filter.filter(getPolicy(), "context", "documentid", PIECE, "Michel had eye cancer", attributes);
        showSpans(filterResult.getSpans());
        Assertions.assertEquals(20, filterResult.getSpans().size());

    }

    @Test
    public void filterMedium2() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new FirstNameFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new PersonsAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final FuzzyDictionaryFilter filter = new FuzzyDictionaryFilter(FilterType.FIRST_NAME, filterConfiguration, SensitivityLevel.LOW);

        final FilterResult filterResult = filter.filter(getPolicy(), "context", "documentid", PIECE, "Jennifer had eye cancer", attributes);
        showSpans(filterResult.getSpans());
        Assertions.assertEquals(4, filterResult.getSpans().size());

    }

    @Test
    public void filterHigh() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new FirstNameFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new PersonsAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final FuzzyDictionaryFilter filter = new FuzzyDictionaryFilter(FilterType.FIRST_NAME, filterConfiguration, SensitivityLevel.HIGH);

        final FilterResult filterResult = filter.filter(getPolicy(), "context", "documentid", PIECE, "Sandra in Washington", attributes);
        showSpans(filterResult.getSpans());
        Assertions.assertEquals(2, filterResult.getSpans().size());

    }

    @Test
    public void filter1() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new FirstNameFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new PersonsAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final FuzzyDictionaryFilter filter = new FuzzyDictionaryFilter(FilterType.FIRST_NAME, filterConfiguration, SensitivityLevel.MEDIUM);

        final FilterResult filterResult = filter.filter(getPolicy(), "context", "documentid", PIECE, "Melissa", attributes);

        showSpans(filterResult.getSpans());
        Assertions.assertEquals(33, filterResult.getSpans().size());

    }

    @Test
    public void filter2() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new FirstNameFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new PersonsAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final FuzzyDictionaryFilter filter = new FuzzyDictionaryFilter(FilterType.FIRST_NAME, filterConfiguration, SensitivityLevel.LOW);

        final FilterResult filterResult = filter.filter(getPolicy(), "context", "documentid", PIECE,"thomas", attributes);
        showSpans(filterResult.getSpans());
        Assertions.assertEquals(1, filterResult.getSpans().size());

    }

    @Test
    public void filter3() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new FirstNameFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new PersonsAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final FuzzyDictionaryFilter filter = new FuzzyDictionaryFilter(FilterType.FIRST_NAME, filterConfiguration, SensitivityLevel.LOW);

        final FilterResult filterResult = filter.filter(getPolicy(), "context", "documentid", PIECE,"dat", attributes);
        showSpans(filterResult.getSpans());
        Assertions.assertEquals(1, filterResult.getSpans().size());

    }

    @Test
    public void filter4() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new FirstNameFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new PersonsAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final FuzzyDictionaryFilter filter = new FuzzyDictionaryFilter(FilterType.FIRST_NAME, filterConfiguration, SensitivityLevel.LOW);

        final FilterResult filterResult = filter.filter(getPolicy(), "context", "documentid", PIECE,"joie", attributes);
        showSpans(filterResult.getSpans());
        Assertions.assertEquals(1, filterResult.getSpans().size());

    }

    @Test
    public void filter5() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new FirstNameFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new PersonsAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final FuzzyDictionaryFilter filter = new FuzzyDictionaryFilter(FilterType.FIRST_NAME, filterConfiguration, SensitivityLevel.LOW);

        final FilterResult filterResult = filter.filter(getPolicy(), "context", "documentid", PIECE,"John", attributes);
        showSpans(filterResult.getSpans());
        Assertions.assertEquals(50, filterResult.getSpans().size());

    }

    @Test
    public void filter6() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new FirstNameFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new PersonsAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final FuzzyDictionaryFilter filter = new FuzzyDictionaryFilter(FilterType.FIRST_NAME, filterConfiguration, SensitivityLevel.LOW);

        final FilterResult filterResult = filter.filter(getPolicy(), "context", "documentid", PIECE,"Smith,Melissa A,MD", attributes);
        showSpans(filterResult.getSpans());
        Assertions.assertEquals(3, filterResult.getSpans().size());

    }

}