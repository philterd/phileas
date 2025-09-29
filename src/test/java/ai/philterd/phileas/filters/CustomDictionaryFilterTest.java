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
import ai.philterd.phileas.filters.rules.dictionary.BloomFilterDictionaryFilter;
import ai.philterd.phileas.model.objects.FilterResult;
import ai.philterd.phileas.model.services.defaults.DefaultContextService;
import ai.philterd.phileas.services.strategies.custom.CustomDictionaryFilterStrategy;
import ai.philterd.phileas.services.anonymization.AlphanumericAnonymizationService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CustomDictionaryFilterTest extends AbstractFilterTest {

    private static final Logger LOGGER = LogManager.getLogger(CustomDictionaryFilterTest.class);

    @Test
    public void filterDictionaryExactMatch() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new CustomDictionaryFilterStrategy()))
                .withAnonymizationService(new AlphanumericAnonymizationService(new DefaultContextService()))
                .withWindowSize(windowSize)
                .build();

        final Set<String> names = new HashSet<>(Arrays.asList("george", "ted", "bill", "john"));
        final BloomFilterDictionaryFilter filter = new BloomFilterDictionaryFilter(FilterType.CUSTOM_DICTIONARY, filterConfiguration, names, "names");

        final FilterResult filterResult = filter.filter(getPolicy(), "context",  "documentid", PIECE,"He lived with Bill in California.", attributes);
        showSpans(filterResult.getSpans());

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 14, 18, FilterType.CUSTOM_DICTIONARY));
        Assertions.assertEquals("Bill", filterResult.getSpans().get(0).getText());

    }

    @Test
    public void filterDictionaryNoMatch() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new CustomDictionaryFilterStrategy()))
                .withAnonymizationService(new AlphanumericAnonymizationService(new DefaultContextService()))
                .withWindowSize(windowSize)
                .build();

        final Set<String> names = new HashSet<>(Arrays.asList("george", "ted", "bill", "john"));
        final BloomFilterDictionaryFilter filter = new BloomFilterDictionaryFilter(FilterType.CUSTOM_DICTIONARY, filterConfiguration, names, "names");

        final FilterResult filterResult = filter.filter(getPolicy(), "context",  "documentid", PIECE,"He lived with Sam in California.", attributes);
        showSpans(filterResult.getSpans());

        Assertions.assertEquals(0, filterResult.getSpans().size());

    }

}
