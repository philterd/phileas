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
import ai.philterd.phileas.model.objects.DocumentAnalysis;
import ai.philterd.phileas.model.objects.DocumentType;
import ai.philterd.phileas.model.objects.FilterResult;
import ai.philterd.phileas.model.profile.IgnoredPattern;
import ai.philterd.phileas.model.profile.filters.strategies.rules.AgeFilterStrategy;
import ai.philterd.phileas.model.services.AlertService;
import ai.philterd.phileas.services.anonymization.AgeAnonymizationService;
import ai.philterd.phileas.services.anonymization.cache.LocalAnonymizationCacheService;
import ai.philterd.phileas.services.filters.regex.AgeFilter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class AgeFilterTest extends AbstractFilterTest {

    private AlertService alertService = Mockito.mock(AlertService.class);

    @Test
    public void filterIgnoredPattern1() throws Exception {

        final AlertService alertService = Mockito.mock(AlertService.class);

        final IgnoredPattern ignoredPattern = new IgnoredPattern("[0-9]+(years old)");
        final List<IgnoredPattern> ignoredPatterns = Arrays.asList(ignoredPattern);

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withAlertService(alertService)
                .withAnonymizationService(new AgeAnonymizationService(new LocalAnonymizationCacheService()))
                .withIgnoredPatterns(ignoredPatterns)
                .withWindowSize(windowSize)
                .build();

        final AgeFilter filter = new AgeFilter(filterConfiguration);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", PIECE, "the patient is 35years old.");

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(filterResult.getSpans().get(0).isIgnored());
        Assertions.assertNotEquals(filterResult.getSpans().get(0).getText(), filterResult.getSpans().get(0).getReplacement());

    }

    @Test
    public void filterIgnoredCaseSensitive() throws Exception {

        final AlertService alertService = Mockito.mock(AlertService.class);

        final Set<String> ignore = new LinkedHashSet<>();
        ignore.add("35yEaRs old");

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withAlertService(alertService)
                .withAnonymizationService(new AgeAnonymizationService(new LocalAnonymizationCacheService()))
                .withIgnored(ignore)
                .withWindowSize(windowSize)
                .build();

        final AgeFilter filter = new AgeFilter(filterConfiguration);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", PIECE, "the patient is 35yEaRs old.");

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(filterResult.getSpans().get(0).isIgnored());
        Assertions.assertNotEquals(filterResult.getSpans().get(0).getText(), filterResult.getSpans().get(0).getReplacement());

    }

    @Test
    public void filter0() throws Exception {

        // This tests PHL-68. When there are no filter strategies just redact.

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withAlertService(alertService)
                .withAnonymizationService(new AgeAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final AgeFilter filter = new AgeFilter(filterConfiguration);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", PIECE, "the patient is 3.5years old.");

        showSpans(filterResult.getSpans());

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 15, 27, FilterType.AGE));
        Assertions.assertEquals("{{{REDACTED-age}}}", filterResult.getSpans().get(0).getReplacement());
        Assertions.assertEquals("3.5years old", filterResult.getSpans().get(0).getText());
        Assertions.assertNotEquals(filterResult.getSpans().get(0).getText(), filterResult.getSpans().get(0).getReplacement());

    }

    @Test
    public void filter1() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(new AgeFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new AgeAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final AgeFilter filter = new AgeFilter(filterConfiguration);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", PIECE, "the patient is 3.5years old.");

        showSpans(filterResult.getSpans());

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 15, 27, FilterType.AGE));
        Assertions.assertNotEquals(filterResult.getSpans().get(0).getText(), filterResult.getSpans().get(0).getReplacement());

    }

    @Test
    public void filter2() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(new AgeFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new AgeAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final AgeFilter filter = new AgeFilter(filterConfiguration);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", PIECE, "the patient age is 3.yrs.");

        showSpans(filterResult.getSpans());

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 19, 24, FilterType.AGE));
        Assertions.assertNotEquals(filterResult.getSpans().get(0).getText(), filterResult.getSpans().get(0).getReplacement());

    }

    @Test
    public void filter3() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(new AgeFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new AgeAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final AgeFilter filter = new AgeFilter(filterConfiguration);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", PIECE, "the patient age is 3yrs.");

        showSpans(filterResult.getSpans());

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 19, 23, FilterType.AGE));
        Assertions.assertNotEquals(filterResult.getSpans().get(0).getText(), filterResult.getSpans().get(0).getReplacement());

    }

    @Test
    public void filter4() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(new AgeFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new AgeAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final AgeFilter filter = new AgeFilter(filterConfiguration);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", PIECE, "the patient is 3.5yrs old.");

        showSpans(filterResult.getSpans());

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 15, 25, FilterType.AGE));
        Assertions.assertNotEquals(filterResult.getSpans().get(0).getText(), filterResult.getSpans().get(0).getReplacement());

    }

    @Test
    public void filter5() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(new AgeFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new AgeAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final AgeFilter filter = new AgeFilter(filterConfiguration);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", PIECE, "the patient is 39yrs. old");

        showSpans(filterResult.getSpans());

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 15, 25, FilterType.AGE));
        Assertions.assertNotEquals(filterResult.getSpans().get(0).getText(), filterResult.getSpans().get(0).getReplacement());

    }

    @Test
    public void filter6() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(new AgeFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new AgeAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final AgeFilter filter = new AgeFilter(filterConfiguration);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", PIECE, "she is aged 39");

        showSpans(filterResult.getSpans());

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 7, 14, FilterType.AGE));
        Assertions.assertNotEquals(filterResult.getSpans().get(0).getText(), filterResult.getSpans().get(0).getReplacement());

    }

    @Test
    public void filter7() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(new AgeFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new AgeAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final AgeFilter filter = new AgeFilter(filterConfiguration);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", PIECE, "she is age 39");

        showSpans(filterResult.getSpans());

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 7, 13, FilterType.AGE));
        Assertions.assertNotEquals(filterResult.getSpans().get(0).getText(), filterResult.getSpans().get(0).getReplacement());

    }

    @Test
    public void filter8() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(new AgeFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new AgeAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final AgeFilter filter = new AgeFilter(filterConfiguration);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", PIECE, "she is age 39.5");

        showSpans(filterResult.getSpans());

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 7, 15, FilterType.AGE));
        Assertions.assertNotEquals(filterResult.getSpans().get(0).getText(), filterResult.getSpans().get(0).getReplacement());

    }

    @Test
    public void filter9() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(new AgeFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new AgeAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final AgeFilter filter = new AgeFilter(filterConfiguration);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", PIECE, "Patient Timothy Hook is 72 Yr. old male lives alone.");

        showSpans(filterResult.getSpans());

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 24, 34, FilterType.AGE));
        Assertions.assertNotEquals(filterResult.getSpans().get(0).getText(), filterResult.getSpans().get(0).getReplacement());

        LOGGER.info("Span text: " + filterResult.getSpans().get(0).getText());

    }

    @Test
    public void filter10() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(new AgeFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new AgeAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final AgeFilter filter = new AgeFilter(filterConfiguration);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", PIECE, "Cari Morris is 75 yo female alert and oriented x’s3 with some mild memory loss.");

        showSpans(filterResult.getSpans());

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 15, 21, FilterType.AGE));
        Assertions.assertNotEquals(filterResult.getSpans().get(0).getText(), filterResult.getSpans().get(0).getReplacement());

        LOGGER.info("Span text: " + filterResult.getSpans().get(0).getText());

    }

    @Test
    public void filter11() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(new AgeFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new AgeAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final AgeFilter filter = new AgeFilter(filterConfiguration);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", PIECE, "Had symptoms for the past 10 years");

        showSpans(filterResult.getSpans());

        Assertions.assertEquals(0, filterResult.getSpans().size());

    }

    @Test
    public void filter12() throws Exception {

        // PHL-175: Date format "64-year-old"

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(new AgeFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new AgeAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final AgeFilter filter = new AgeFilter(filterConfiguration);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", PIECE, "She is a 22-year-old female");

        showSpans(filterResult.getSpans());

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 9, 20, FilterType.AGE));
        Assertions.assertNotEquals(filterResult.getSpans().get(0).getText(), filterResult.getSpans().get(0).getReplacement());

    }

    @Test
    public void filter13() throws Exception {

        // PHL-175: Date format "69 years"

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(new AgeFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new AgeAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final AgeFilter filter = new AgeFilter(filterConfiguration);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", PIECE, "Admit age: 69 years");

        showSpans(filterResult.getSpans());

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 11, 19, FilterType.AGE));
        Assertions.assertNotEquals(filterResult.getSpans().get(0).getText(), filterResult.getSpans().get(0).getReplacement());

    }

    @Test
    public void filter14() throws Exception {

        // PHL-175: Date format "69 years"

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(new AgeFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new AgeAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final AgeFilter filter = new AgeFilter(filterConfiguration);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", PIECE, "Female Admit Age: 69 years");

        showSpans(filterResult.getSpans());

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 18, 26, FilterType.AGE));
        Assertions.assertNotEquals(filterResult.getSpans().get(0).getText(), filterResult.getSpans().get(0).getReplacement());

    }

    @Test
    public void filter15() throws Exception {

        // PHL-184: New line at the end of span

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(new AgeFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new AgeAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final AgeFilter filter = new AgeFilter(filterConfiguration);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", PIECE, "Female Admit Age: 69 years\n");

        showSpans(filterResult.getSpans());

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 18, 26, FilterType.AGE));
        Assertions.assertNotEquals(filterResult.getSpans().get(0).getText(), filterResult.getSpans().get(0).getReplacement());

    }

    @Test
    public void filter16() throws Exception {

        // PHL-238: Support ages like: 61 y/o

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(new AgeFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new AgeAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final AgeFilter filter = new AgeFilter(filterConfiguration);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", PIECE, "patient is 61 y/o and");

        showSpans(filterResult.getSpans());

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 11, 17, FilterType.AGE));
        Assertions.assertNotEquals(filterResult.getSpans().get(0).getText(), filterResult.getSpans().get(0).getReplacement());

    }

    @Test
    public void filter17() throws Exception {

        // PHL-238: Support ages like: 61 y/o

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(new AgeFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new AgeAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final AgeFilter filter = new AgeFilter(filterConfiguration);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", PIECE, "patient is 161 y/o and");

        showSpans(filterResult.getSpans());

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 11, 18, FilterType.AGE));
        Assertions.assertNotEquals(filterResult.getSpans().get(0).getText(), filterResult.getSpans().get(0).getReplacement());

    }

    @Test
    public void filter18() throws Exception {

        // PHL-238: Support ages like: 61 y/o

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(new AgeFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new AgeAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final AgeFilter filter = new AgeFilter(filterConfiguration);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", PIECE, "patient is 4161 y/o and");

        showSpans(filterResult.getSpans());

        Assertions.assertEquals(0, filterResult.getSpans().size());

    }

    @Test
    public void filter_subpoena() throws Exception {

        final AgeFilterStrategy ageFilterStrategy = new AgeFilterStrategy();
        ageFilterStrategy.setExcludeDocumentTypes(Arrays.asList(DocumentType.SUBPOENA));

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(ageFilterStrategy))
                .withAlertService(alertService)
                .withAnonymizationService(new AgeAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .withDocumentAnalysis(new DocumentAnalysis(DocumentType.SUBPOENA, "B254"))
                .build();

        final AgeFilter filter = new AgeFilter(filterConfiguration);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", PIECE, "Female Admit Age: 69 years\n");

        showSpans(filterResult.getSpans());

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 18, 26, FilterType.AGE));

        // The replacement value is the same as the token because the subpoena document type is excluded.
        Assertions.assertEquals(filterResult.getSpans().get(0).getText(), filterResult.getSpans().get(0).getReplacement());

    }

    @Test
    public void filter_no_document_type() throws Exception {

        final AgeFilterStrategy ageFilterStrategy = new AgeFilterStrategy();
        ageFilterStrategy.setExcludeDocumentTypes(Arrays.asList(DocumentType.SUBPOENA));

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(Arrays.asList(ageFilterStrategy))
                .withAlertService(alertService)
                .withAnonymizationService(new AgeAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .withDocumentAnalysis(new DocumentAnalysis(DocumentType.UNKNOWN, "Unknown"))
                .build();

        final AgeFilter filter = new AgeFilter(filterConfiguration);

        final FilterResult filterResult = filter.filter(getFilterProfile(), "context", "documentid", PIECE, "Female Admit Age: 69 years\n");

        showSpans(filterResult.getSpans());

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 18, 26, FilterType.AGE));

        // The replacement value is the same as the token because the subpoena document type is excluded.
        Assertions.assertNotEquals(filterResult.getSpans().get(0).getText(), filterResult.getSpans().get(0).getReplacement());

    }

}
