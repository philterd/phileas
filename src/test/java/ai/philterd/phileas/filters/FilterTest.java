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
import ai.philterd.phileas.model.objects.FilterResult;
import ai.philterd.phileas.services.context.DefaultContextService;
import ai.philterd.phileas.services.anonymization.AgeAnonymizationService;
import ai.philterd.phileas.services.filters.regex.AgeFilter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

public class FilterTest extends AbstractFilterTest {

    protected static final Logger LOGGER = LogManager.getLogger(FilterTest.class);

    @Test
    public void window0() throws Exception {

        // This tests span window creation.
        int windowSize = 3;


        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withAnonymizationService(new AgeAnonymizationService(new DefaultContextService()))
                .withWindowSize(windowSize)
                .build();

        final AgeFilter filter = new AgeFilter(filterConfiguration);

        final FilterResult filterResult = filter.filter(getPolicy(), "context", PIECE, "this is a first sentence. the patient is 3.5 years old and he's cool. this is a surrounding sentence.", attributes);

        showSpans(filterResult.getSpans());

        final String[] window = new String[]{"the", "patient", "is", "35", "years", "old", "and", "hes", "cool"};

        LOGGER.info("Expected: {}", Arrays.toString(window));
        LOGGER.info("Actual:   {}", Arrays.toString(filterResult.getSpans().get(0).getWindow()));

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 41, 54, FilterType.AGE));
        Assertions.assertEquals("{{{REDACTED-age}}}", filterResult.getSpans().get(0).getReplacement());
        Assertions.assertArrayEquals(window, filterResult.getSpans().get(0).getWindow());
        Assertions.assertEquals("3.5 years old", filterResult.getSpans().get(0).getText());

    }

    @Test
    public void window1() throws Exception {

        // This tests span window creation.
        int windowSize = 5;

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withAnonymizationService(new AgeAnonymizationService(new DefaultContextService()))
                .withWindowSize(windowSize)
                .build();

        final AgeFilter filter = new AgeFilter(filterConfiguration);

        final FilterResult filterResult = filter.filter(getPolicy(), "context", PIECE, "this is a first sentence. the patient is 3.5 years old and he's cool. this is a surrounding sentence.", attributes);

        showSpans(filterResult.getSpans());

        final String[] window = new String[]{"first", "sentence", "the", "patient", "is", "35", "years", "old", "and", "hes", "cool", "this", "is"};

        LOGGER.info("Expected: {}", Arrays.toString(window));
        LOGGER.info("Actual:   {}", Arrays.toString(filterResult.getSpans().get(0).getWindow()));

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 41, 54, FilterType.AGE));
        Assertions.assertEquals("{{{REDACTED-age}}}", filterResult.getSpans().get(0).getReplacement());
        Assertions.assertArrayEquals(window, filterResult.getSpans().get(0).getWindow());
        Assertions.assertEquals("3.5 years old", filterResult.getSpans().get(0).getText());

    }

    @Test
    public void window2() throws Exception {

        // Getting window of size 5 for start -1 and end 12

        int windowSize = 5;

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withAnonymizationService(new AgeAnonymizationService(new DefaultContextService()))
                .withWindowSize(windowSize)
                .build();

        final AgeFilter filter = new AgeFilter(filterConfiguration);

        final FilterResult filterResult = filter.filter(getPolicy(), "context", PIECE, "this is a first sentence. the patient is 3.5 years old and he's cool. this is a surrounding sentence.", attributes);

        showSpans(filterResult.getSpans());

        final String[] window = new String[]{"first", "sentence", "the", "patient", "is", "35", "years", "old", "and", "hes", "cool", "this", "is"};

        LOGGER.info("Expected: {}", Arrays.toString(window));
        LOGGER.info("Actual:   {}", Arrays.toString(filterResult.getSpans().get(0).getWindow()));

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 41, 54, FilterType.AGE));
        Assertions.assertEquals("{{{REDACTED-age}}}", filterResult.getSpans().get(0).getReplacement());
        Assertions.assertArrayEquals(window, filterResult.getSpans().get(0).getWindow());
        Assertions.assertEquals("3.5 years old", filterResult.getSpans().get(0).getText());

    }

}
