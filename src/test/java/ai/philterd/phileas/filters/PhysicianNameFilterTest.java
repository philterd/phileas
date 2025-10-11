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

import ai.philterd.phileas.model.enums.FilterType;
import ai.philterd.phileas.model.objects.FilterResult;
import ai.philterd.phileas.services.context.DefaultContextService;
import ai.philterd.phileas.services.anonymization.PersonsAnonymizationService;
import ai.philterd.phileas.services.filters.regex.PhysicianNameFilter;
import ai.philterd.phileas.services.strategies.rules.PhysicianNameFilterStrategy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

public class PhysicianNameFilterTest extends AbstractFilterTest {

    private static final Logger LOGGER = LogManager.getLogger(PhysicianNameFilterTest.class);

    @Test
    public void physicianNameTestPreNominal1() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new PhysicianNameFilterStrategy()))
                .withAnonymizationService(new PersonsAnonymizationService(new DefaultContextService()))
                .withWindowSize(windowSize)
                .build();

        final PhysicianNameFilter filter = new PhysicianNameFilter(filterConfiguration);

          final FilterResult filterResult = filter.filter(getPolicy(), "context", PIECE, "Doctor Smith was the attending physician.", attributes);
        showSpans(filterResult.getSpans());

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertEquals(FilterType.PHYSICIAN_NAME, filterResult.getSpans().get(0).getFilterType());
        Assertions.assertTrue(filterResult.getSpans().get(0).getText().contains("Doctor Smith"));

    }

    @Test
    public void physicianNameTestPreNominal2() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new PhysicianNameFilterStrategy()))
                .withAnonymizationService(new PersonsAnonymizationService(new DefaultContextService()))
                .withWindowSize(windowSize)
                .build();

        final PhysicianNameFilter filter = new PhysicianNameFilter(filterConfiguration);

        final FilterResult filterResult = filter.filter(getPolicy(), "context", PIECE, "Doctor James Smith", attributes);
        showSpans(filterResult.getSpans());

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 0, 18, FilterType.PHYSICIAN_NAME));
        Assertions.assertEquals("Doctor James Smith", filterResult.getSpans().get(0).getText());

    }

    @Test
    public void physicianNameTestPostNominal1() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new PhysicianNameFilterStrategy()))
                .withAnonymizationService(new PersonsAnonymizationService(new DefaultContextService()))
                .withWindowSize(windowSize)
                .build();

        final PhysicianNameFilter filter = new PhysicianNameFilter(filterConfiguration);

        final FilterResult filterResult = filter.filter(getPolicy(), "context", PIECE, "John Smith, MD", attributes);
        showSpans(filterResult.getSpans());

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 0, 14, FilterType.PHYSICIAN_NAME));
        Assertions.assertEquals("John Smith, MD", filterResult.getSpans().get(0).getText());

    }

    @Test
    public void physicianNameTestPostNominal2() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new PhysicianNameFilterStrategy()))
                .withAnonymizationService(new PersonsAnonymizationService(new DefaultContextService()))
                .withWindowSize(windowSize)
                .build();

        final PhysicianNameFilter filter = new PhysicianNameFilter(filterConfiguration);

        final FilterResult filterResult = filter.filter(getPolicy(), "context", PIECE, "attending physician was John Smith, MD", attributes);
        showSpans(filterResult.getSpans());

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 10, 38, FilterType.PHYSICIAN_NAME));
        Assertions.assertEquals("physician was John Smith, MD", filterResult.getSpans().get(0).getText());

    }

    @Test
    public void physicianNameTestPostNominal3() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new PhysicianNameFilterStrategy()))
                .withAnonymizationService(new PersonsAnonymizationService(new DefaultContextService()))
                .withWindowSize(windowSize)
                .build();

        final PhysicianNameFilter filter = new PhysicianNameFilter(filterConfiguration);

        final FilterResult filterResult = filter.filter(getPolicy(), "context", PIECE, "was John J. van Smith, MD", attributes);
        showSpans(filterResult.getSpans());

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 4, 25, FilterType.PHYSICIAN_NAME));
        Assertions.assertEquals("John J. van Smith, MD", filterResult.getSpans().get(0).getText());

    }

    @Test
    public void physicianNameTestPostNominal4() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new PhysicianNameFilterStrategy()))
                .withAnonymizationService(new PersonsAnonymizationService(new DefaultContextService()))
                .withWindowSize(windowSize)
                .build();

        final PhysicianNameFilter filter = new PhysicianNameFilter(filterConfiguration);

        final FilterResult filterResult = filter.filter(getPolicy(), "context", PIECE, "Smith,James D,MD -General Surgery", attributes);
        showSpans(filterResult.getSpans());

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 0, 16, FilterType.PHYSICIAN_NAME));
        Assertions.assertEquals("Smith,James D,MD", filterResult.getSpans().get(0).getText());

    }

    @Test
    public void physicianNameTestPostNominal5() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new PhysicianNameFilterStrategy()))
                .withAnonymizationService(new PersonsAnonymizationService(new DefaultContextService()))
                .withWindowSize(windowSize)
                .build();

        final PhysicianNameFilter filter = new PhysicianNameFilter(filterConfiguration);

        final FilterResult filterResult = filter.filter(getPolicy(), "context", PIECE, "Smith,James )D,MD -General Surgery", attributes);
        showSpans(filterResult.getSpans());

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 0, 17, FilterType.PHYSICIAN_NAME));
        Assertions.assertEquals("Smith,James )D,MD", filterResult.getSpans().get(0).getText());

    }

    @Test
    public void physicianNameTestPostNominal6() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new PhysicianNameFilterStrategy()))
                .withAnonymizationService(new PersonsAnonymizationService(new DefaultContextService()))
                .withWindowSize(windowSize)
                .build();

        final PhysicianNameFilter filter = new PhysicianNameFilter(filterConfiguration);

        final FilterResult filterResult = filter.filter(getPolicy(), "context", PIECE, "1.0 cm in outside diameter pink tan everted", attributes);
        showSpans(filterResult.getSpans());

        Assertions.assertEquals(0, filterResult.getSpans().size());

    }

    @Test
    public void physicianNameTestPostNominal7() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new PhysicianNameFilterStrategy()))
                .withAnonymizationService(new PersonsAnonymizationService(new DefaultContextService()))
                .withWindowSize(windowSize)
                .build();

        final PhysicianNameFilter filter = new PhysicianNameFilter(filterConfiguration);

        final FilterResult filterResult = filter.filter(getPolicy(), "context", PIECE, "1.0 cm", attributes);
        showSpans(filterResult.getSpans());

        Assertions.assertEquals(0, filterResult.getSpans().size());

    }

    @Test
    public void physicianNameTestPostNominal8() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new PhysicianNameFilterStrategy()))
                .withAnonymizationService(new PersonsAnonymizationService(new DefaultContextService()))
                .withWindowSize(windowSize)
                .build();

        final PhysicianNameFilter filter = new PhysicianNameFilter(filterConfiguration);

        final FilterResult filterResult = filter.filter(getPolicy(), "context", PIECE, "Ahu,Amanda D,MD -General Surgery", attributes);
        showSpans(filterResult.getSpans());

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 0, 15, FilterType.PHYSICIAN_NAME));
        Assertions.assertEquals("Ahu,Amanda D,MD", filterResult.getSpans().get(0).getText());

    }

    @Test
    public void physicianNameTestPostNominal9() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new PhysicianNameFilterStrategy()))
                .withAnonymizationService(new PersonsAnonymizationService(new DefaultContextService()))
                .withWindowSize(windowSize)
                .build();

        final PhysicianNameFilter filter = new PhysicianNameFilter(filterConfiguration);

        final FilterResult filterResult = filter.filter(getPolicy(), "context", PIECE, "Johnns,Melinda S,MD - 1/2/2018 11:54 CST 1/2/2018 12:46 CST", attributes);
        showSpans(filterResult.getSpans());

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 0, 19, FilterType.PHYSICIAN_NAME));
        Assertions.assertEquals("Johnns,Melinda S,MD", filterResult.getSpans().get(0).getText());

    }

}
