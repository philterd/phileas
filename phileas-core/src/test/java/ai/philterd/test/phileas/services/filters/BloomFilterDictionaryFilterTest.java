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

import ai.philterd.phileas.model.enums.FilterType;
import ai.philterd.phileas.model.filter.FilterConfiguration;
import ai.philterd.phileas.model.filter.rules.dictionary.BloomFilterDictionaryFilter;
import ai.philterd.phileas.model.objects.FilterResult;
import ai.philterd.phileas.model.objects.Span;
import ai.philterd.phileas.model.policy.filters.strategies.custom.CustomDictionaryFilterStrategy;
import ai.philterd.phileas.model.services.AlertService;
import ai.philterd.phileas.services.anonymization.AlphanumericAnonymizationService;
import ai.philterd.phileas.services.anonymization.cache.LocalAnonymizationCacheService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BloomFilterDictionaryFilterTest extends AbstractFilterTest {

    private static final Logger LOGGER = LogManager.getLogger(BloomFilterDictionaryFilterTest.class);

    private final AlertService alertService = Mockito.mock(AlertService.class);

    @Test
    public void filterDictionaryExactMatch() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new CustomDictionaryFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new AlphanumericAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final Set<String> names = new HashSet<>(Arrays.asList("george", "ted", "Bill", "john"));
        final BloomFilterDictionaryFilter filter = new BloomFilterDictionaryFilter(FilterType.CUSTOM_DICTIONARY, filterConfiguration, names, "none");

         final FilterResult filterResult = filter.filter(getPolicy(), "context", "documentid", PIECE, "He lived with Bill in California.", attributes);

        showSpans(filterResult.getSpans());

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 14, 18, FilterType.CUSTOM_DICTIONARY));
        Assertions.assertEquals("Bill", filterResult.getSpans().get(0).getText());

    }

    @Test
    public void filterDictionaryCaseInsensitiveMatch() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new CustomDictionaryFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new AlphanumericAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final Set<String> names = new HashSet<>(Arrays.asList("george", "ted", "bill", "john"));
        final BloomFilterDictionaryFilter filter = new BloomFilterDictionaryFilter(FilterType.CUSTOM_DICTIONARY, filterConfiguration, names, "none");

        final FilterResult filterResult = filter.filter(getPolicy(), "context", "documentid", PIECE, "He lived with Bill in California.", attributes);

        showSpans(filterResult.getSpans());

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 14, 18, FilterType.CUSTOM_DICTIONARY));
        Assertions.assertEquals("Bill", filterResult.getSpans().get(0).getText());

    }

    @Test
    public void filterDictionaryNoMatch() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new CustomDictionaryFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new AlphanumericAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final Set<String> names = new HashSet<>(Arrays.asList("george", "ted", "bill", "john"));
        final BloomFilterDictionaryFilter filter = new BloomFilterDictionaryFilter(FilterType.CUSTOM_DICTIONARY, filterConfiguration, names, "none");

        final FilterResult filterResult = filter.filter(getPolicy(), "context", "documentid", PIECE, "He lived with Sam in California.", attributes);

        showSpans(filterResult.getSpans());

        Assertions.assertEquals(0, filterResult.getSpans().size());

    }

    @Test
    public void filterDictionaryPhraseMatch1() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new CustomDictionaryFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new AlphanumericAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final Set<String> names = new HashSet<>(Arrays.asList("george jones", "ted", "bill", "john"));
        final BloomFilterDictionaryFilter filter = new BloomFilterDictionaryFilter(FilterType.CUSTOM_DICTIONARY, filterConfiguration, names, "none");

        final FilterResult filterResult = filter.filter(getPolicy(), "context", "documentid", PIECE,"He lived with george jones in California.", attributes);

        showSpans(filterResult.getSpans());

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 14, 26, FilterType.CUSTOM_DICTIONARY));
        Assertions.assertEquals("george jones", filterResult.getSpans().get(0).getText());

    }

    @Test
    public void filterDictionaryPhraseMatch2() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new CustomDictionaryFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new AlphanumericAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final Set<String> names = new HashSet<>(Arrays.asList("george jones jr", "ted", "bill smith", "john"));
        final BloomFilterDictionaryFilter filter = new BloomFilterDictionaryFilter(FilterType.CUSTOM_DICTIONARY, filterConfiguration, names, "none");

        final FilterResult filterResult = filter.filter(getPolicy(), "context", "documentid", PIECE,"Bill Smith lived with george jones jr in California.", attributes);

        showSpans(filterResult.getSpans());

        Assertions.assertEquals(2, filterResult.getSpans().size());

        for(final Span span : filterResult.getSpans()) {
            Assertions.assertTrue(span.getText().equals("george jones jr") || span.getText().equals("Bill Smith"));
            Assertions.assertTrue(span.getCharacterStart() == 0 || span.getCharacterStart() == 22);
            Assertions.assertTrue(span.getCharacterEnd() == 10 || span.getCharacterEnd() == 37);
        }

    }

    @Test
    public void filterDictionaryPhraseMatchMultipleMatches() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new CustomDictionaryFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new AlphanumericAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final Set<String> names = new HashSet<>(Arrays.asList("george jones", "ted", "bill", "john"));
        final BloomFilterDictionaryFilter filter = new BloomFilterDictionaryFilter(FilterType.CUSTOM_DICTIONARY, filterConfiguration, names, "none");

        final FilterResult filterResult = filter.filter(getPolicy(), "context", "documentid", PIECE,"He lived with george jones and george jones in California.", attributes);

        showSpans(filterResult.getSpans());

        Assertions.assertEquals(2, filterResult.getSpans().size());

        for(final Span span : filterResult.getSpans()) {
            Assertions.assertEquals("george jones", span.getText());
            Assertions.assertTrue(span.getCharacterStart() == 31 || span.getCharacterStart() == 14);
            Assertions.assertTrue(span.getCharacterEnd() == 43 || span.getCharacterEnd() == 26);
        }

    }

}
