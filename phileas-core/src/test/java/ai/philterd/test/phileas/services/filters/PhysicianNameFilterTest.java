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
import ai.philterd.phileas.model.profile.filters.strategies.rules.PhysicianNameFilterStrategy;
import ai.philterd.phileas.model.services.AlertService;
import ai.philterd.phileas.services.anonymization.PersonsAnonymizationService;
import ai.philterd.phileas.services.anonymization.cache.LocalAnonymizationCacheService;
import ai.philterd.phileas.services.filters.regex.PhysicianNameFilter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;

public class PhysicianNameFilterTest extends AbstractFilterTest {

    private static final Logger LOGGER = LogManager.getLogger(PhysicianNameFilterTest.class);

    private final AlertService alertService = Mockito.mock(AlertService.class);

    @Test
    public void physicianNameTestPreNominal1() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(new PhysicianNameFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new PersonsAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final PhysicianNameFilter filter = new PhysicianNameFilter(filterConfiguration);

          final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", PIECE, "Doctor Smith was the attending physician.");
        showSpans(filterResult.getSpans());

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 0, 30, FilterType.PHYSICIAN_NAME));
        Assertions.assertEquals("Doctor Smith was the attending", filterResult.getSpans().get(0).getText());

    }

    @Test
    public void physicianNameTestPreNominal2() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(new PhysicianNameFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new PersonsAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final PhysicianNameFilter filter = new PhysicianNameFilter(filterConfiguration);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", PIECE, "Doctor James Smith");
        showSpans(filterResult.getSpans());

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 0, 18, FilterType.PHYSICIAN_NAME));
        Assertions.assertEquals("Doctor James Smith", filterResult.getSpans().get(0).getText());

    }

    @Test
    public void physicianNameTestPostNominal1() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(new PhysicianNameFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new PersonsAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final PhysicianNameFilter filter = new PhysicianNameFilter(filterConfiguration);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", PIECE, "John Smith, MD");
        showSpans(filterResult.getSpans());

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 0, 14, FilterType.PHYSICIAN_NAME));
        Assertions.assertEquals("John Smith, MD", filterResult.getSpans().get(0).getText());

    }

    @Test
    public void physicianNameTestPostNominal2() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(new PhysicianNameFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new PersonsAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final PhysicianNameFilter filter = new PhysicianNameFilter(filterConfiguration);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", PIECE, "attending physician was John Smith, MD");
        showSpans(filterResult.getSpans());

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 10, 38, FilterType.PHYSICIAN_NAME));
        Assertions.assertEquals("physician was John Smith, MD", filterResult.getSpans().get(0).getText());

    }

    @Test
    public void physicianNameTestPostNominal3() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(new PhysicianNameFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new PersonsAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final PhysicianNameFilter filter = new PhysicianNameFilter(filterConfiguration);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", PIECE, "was John J. van Smith, MD");
        showSpans(filterResult.getSpans());

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 4, 25, FilterType.PHYSICIAN_NAME));
        Assertions.assertEquals("John J. van Smith, MD", filterResult.getSpans().get(0).getText());

    }

    @Test
    public void physicianNameTestPostNominal4() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(new PhysicianNameFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new PersonsAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final PhysicianNameFilter filter = new PhysicianNameFilter(filterConfiguration);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", PIECE, "Smith,James D,MD -General Surgery");
        showSpans(filterResult.getSpans());

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 0, 16, FilterType.PHYSICIAN_NAME));
        Assertions.assertEquals("Smith,James D,MD", filterResult.getSpans().get(0).getText());

    }

    @Test
    public void physicianNameTestPostNominal5() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(new PhysicianNameFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new PersonsAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final PhysicianNameFilter filter = new PhysicianNameFilter(filterConfiguration);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", PIECE, "Smith,James )D,MD -General Surgery");
        showSpans(filterResult.getSpans());

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 0, 17, FilterType.PHYSICIAN_NAME));
        Assertions.assertEquals("Smith,James )D,MD", filterResult.getSpans().get(0).getText());

    }

    @Test
    public void physicianNameTestPostNominal6() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(new PhysicianNameFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new PersonsAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final PhysicianNameFilter filter = new PhysicianNameFilter(filterConfiguration);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", PIECE, "1.0 cm in outside diameter pink tan everted");
        showSpans(filterResult.getSpans());

        Assertions.assertEquals(0, filterResult.getSpans().size());

    }

    @Test
    public void physicianNameTestPostNominal7() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(new PhysicianNameFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new PersonsAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final PhysicianNameFilter filter = new PhysicianNameFilter(filterConfiguration);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", PIECE, "1.0 cm");
        showSpans(filterResult.getSpans());

        Assertions.assertEquals(0, filterResult.getSpans().size());

    }

    @Test
    public void physicianNameTestPostNominal8() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(new PhysicianNameFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new PersonsAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final PhysicianNameFilter filter = new PhysicianNameFilter(filterConfiguration);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", PIECE, "Ahu,Amanda D,MD -General Surgery");
        showSpans(filterResult.getSpans());

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 0, 15, FilterType.PHYSICIAN_NAME));
        Assertions.assertEquals("Ahu,Amanda D,MD", filterResult.getSpans().get(0).getText());

    }

    @Test
    public void physicianNameTestPostNominal9() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(new PhysicianNameFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new PersonsAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final PhysicianNameFilter filter = new PhysicianNameFilter(filterConfiguration);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", PIECE, "Johnns,Melinda S,MD - 1/2/2018 11:54 CST 1/2/2018 12:46 CST");
        showSpans(filterResult.getSpans());

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 0, 19, FilterType.PHYSICIAN_NAME));
        Assertions.assertEquals("Johnns,Melinda S,MD", filterResult.getSpans().get(0).getText());

    }

}
