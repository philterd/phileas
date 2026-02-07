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

import ai.philterd.phileas.model.filtering.FilterType;
import ai.philterd.phileas.model.filtering.Filtered;
import ai.philterd.phileas.policy.filters.Identifier;
import ai.philterd.phileas.services.context.DefaultContextService;
import ai.philterd.phileas.services.filters.regex.IdentifierFilter;
import ai.philterd.phileas.services.strategies.rules.IdentifierFilterStrategy;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.security.SecureRandom;
import java.util.List;

import static ai.philterd.phileas.services.strategies.AbstractFilterStrategy.RANDOM_REPLACE;

public class IdentifierFilterTest extends AbstractFilterTest {


    @Test
    public void filterId1() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new IdentifierFilterStrategy()))
                .withContextService(contextService)
                .withRandom(random)
                .withWindowSize(windowSize)
                .build();

        final IdentifierFilter filter = new IdentifierFilter(filterConfiguration, "name", Identifier.DEFAULT_IDENTIFIER_REGEX, true, 0);

        final Filtered filtered = filter.filter(getPolicy(), "context", PIECE, "the id is AB4736021 in california.");

        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 10,19, FilterType.IDENTIFIER));
        Assertions.assertEquals("AB4736021", filtered.getSpans().get(0).getText());

    }

    @Test
    public void filterId2() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new IdentifierFilterStrategy()))
                .withContextService(contextService)
                .withRandom(random)
                .withWindowSize(windowSize)
                .build();

        final IdentifierFilter filter = new IdentifierFilter(filterConfiguration, "name", Identifier.DEFAULT_IDENTIFIER_REGEX, true, 0);

        final Filtered filtered = filter.filter(getPolicy(), "context", PIECE, "the id is AB473-6021 in california.");

        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 10,20, FilterType.IDENTIFIER));

    }

    @Test
    public void filterId3() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new IdentifierFilterStrategy()))
                .withContextService(contextService)
                .withRandom(random)
                .withWindowSize(windowSize)
                .build();

        final IdentifierFilter filter = new IdentifierFilter(filterConfiguration, "name", Identifier.DEFAULT_IDENTIFIER_REGEX, true, 0);

        final Filtered filtered = filter.filter(getPolicy(), "context", PIECE, "the id is 473-6AB021 in california.");

        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 10,20, FilterType.IDENTIFIER));

    }

    @Test
    public void filterId4() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new IdentifierFilterStrategy()))
                .withContextService(contextService)
                .withRandom(random)
                .withWindowSize(windowSize)
                .build();

        final IdentifierFilter filter = new IdentifierFilter(filterConfiguration, "name", Identifier.DEFAULT_IDENTIFIER_REGEX, true, 0);

        final Filtered filtered = filter.filter(getPolicy(), "context", PIECE, "the id is AB473-6021 in california.");

        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 10,20, FilterType.IDENTIFIER));

    }

    @Test
    public void filterId5() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new IdentifierFilterStrategy()))
                .withContextService(contextService)
                .withRandom(random)
                .withWindowSize(windowSize)
                .build();

        final IdentifierFilter filter = new IdentifierFilter(filterConfiguration, "name", Identifier.DEFAULT_IDENTIFIER_REGEX, true, 0);

        final Filtered filtered = filter.filter(getPolicy(), "context", PIECE, "the id is 473-6AB021 in california.");

        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 10,20, FilterType.IDENTIFIER));

    }

    @Test
    public void filterId6() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new IdentifierFilterStrategy()))
                .withContextService(contextService)
                .withRandom(random)
                .withWindowSize(windowSize)
                .build();

        final IdentifierFilter filter = new IdentifierFilter(filterConfiguration, "name", Identifier.DEFAULT_IDENTIFIER_REGEX, true, 0);

        final Filtered filtered = filter.filter(getPolicy(), "context", PIECE, "the id is 123-45-6789 in california.");

        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 10,21, FilterType.IDENTIFIER));

    }

    @Test
    public void filterId7() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new IdentifierFilterStrategy()))
                .withContextService(contextService)
                .withRandom(random)
                .withWindowSize(windowSize)
                .build();

        final IdentifierFilter filter = new IdentifierFilter(filterConfiguration, "name", Identifier.DEFAULT_IDENTIFIER_REGEX, true, 0);

        final Filtered filtered = filter.filter(getPolicy(), "context", PIECE, "George Washington was president and his ssn was 123-45-6789 and he lived at 90210. Patient id 00076A and 93821A. He is on biotin. Diagnosed with A01000.");

        Assertions.assertEquals(4, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 48, 59, FilterType.IDENTIFIER));
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(1), 94, 100, FilterType.IDENTIFIER));
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(2), 105, 111, FilterType.IDENTIFIER));
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(3), 145, 151, FilterType.IDENTIFIER));

    }

    @Test
    public void filterId8() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new IdentifierFilterStrategy()))
                .withContextService(contextService)
                .withRandom(random)
                .withWindowSize(windowSize)
                .build();

        final IdentifierFilter filter = new IdentifierFilter(filterConfiguration, "name", Identifier.DEFAULT_IDENTIFIER_REGEX, true, 0);

        final Filtered filtered = filter.filter(getPolicy(), "context", PIECE, "the id is 000-00-00-00 ABC123 in california.");

        Assertions.assertEquals(2, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 10, 22, FilterType.IDENTIFIER));
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(1), 23, 29, FilterType.IDENTIFIER));

    }

    @Test
    public void filterId9() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new IdentifierFilterStrategy()))
                .withContextService(contextService)
                .withRandom(random)
                .withWindowSize(windowSize)
                .build();

        final IdentifierFilter filter = new IdentifierFilter(filterConfiguration, "name", Identifier.DEFAULT_IDENTIFIER_REGEX, true, 0);

        final Filtered filtered = filter.filter(getPolicy(), "context", PIECE, "the id is AZ12 ABC1234/123ABC4 in california.");

        Assertions.assertEquals(2, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 15, 22, FilterType.IDENTIFIER));
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(1), 23, 30, FilterType.IDENTIFIER));

    }

    @Test
    public void filterId10() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new IdentifierFilterStrategy()))
                .withContextService(contextService)
                .withRandom(random)
                .withWindowSize(windowSize)
                .build();

        final IdentifierFilter filter = new IdentifierFilter(filterConfiguration, "name", Identifier.DEFAULT_IDENTIFIER_REGEX, true, 0);

        final Filtered filtered = filter.filter(getPolicy(), "context", PIECE, "the id is H3SNPUHYEE7JD3H in california.");

        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 10,25, FilterType.IDENTIFIER));

    }

    @Test
    public void filterId11() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new IdentifierFilterStrategy()))
                .withContextService(contextService)
                .withRandom(random)
                .withWindowSize(windowSize)
                .build();

        final IdentifierFilter filter = new IdentifierFilter(filterConfiguration, "name", Identifier.DEFAULT_IDENTIFIER_REGEX, true, 0);

        final Filtered filtered = filter.filter(getPolicy(), "context", PIECE, "the id is 86637729 in california.");

        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 10,18, FilterType.IDENTIFIER));

    }

    @Test
    public void filterId12() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new IdentifierFilterStrategy()))
                .withContextService(contextService)
                .withRandom(random)
                .withWindowSize(windowSize)
                .build();

        final IdentifierFilter filter = new IdentifierFilter(filterConfiguration, "name", Identifier.DEFAULT_IDENTIFIER_REGEX, true, 0);

        final Filtered filtered = filter.filter(getPolicy(), "context", PIECE, "the id is 33778376 in california.");

        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 10,18, FilterType.IDENTIFIER));

    }

    @Test
    public void filterId13() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new IdentifierFilterStrategy()))
                .withContextService(contextService)
                .withRandom(random)
                .withWindowSize(windowSize)
                .build();

        final IdentifierFilter filter = new IdentifierFilter(filterConfiguration, "name", "\\b[A-Z]{4,}\\b", true, 0);

        final Filtered filtered = filter.filter(getPolicy(), "context", PIECE, "the id is ABCD.");

        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 10,14, FilterType.IDENTIFIER));

    }

    @Test
    public void filterId14() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new IdentifierFilterStrategy()))
                .withContextService(contextService)
                .withRandom(random)
                .withWindowSize(windowSize)
                .build();

        final IdentifierFilter filter = new IdentifierFilter(filterConfiguration, "name", Identifier.DEFAULT_IDENTIFIER_REGEX, true, 0);

        final Filtered filtered = filter.filter(getPolicy(), "context", PIECE, "the id is 123456.");

        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 10,16, FilterType.IDENTIFIER));

    }

    @Test
    public void filterId15() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new IdentifierFilterStrategy()))
                .withContextService(contextService)
                .withRandom(random)
                .withWindowSize(windowSize)
                .build();

        final IdentifierFilter filter = new IdentifierFilter(filterConfiguration, "name", Identifier.DEFAULT_IDENTIFIER_REGEX, true, 0);

        final Filtered filtered = filter.filter(getPolicy(), "context", PIECE, "John Smith, patient ID A203493, was seen on February 18.");

        showSpans(filtered.getSpans());
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 23,30, FilterType.IDENTIFIER));

    }

    @Test
    public void filterId16() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new IdentifierFilterStrategy()))
                .withContextService(contextService)
                .withRandom(random)
                .withWindowSize(windowSize)
                .build();

        final IdentifierFilter filter = new IdentifierFilter(filterConfiguration, "name", "\\b\\d{3,8}\\b", false, 0);

        final Filtered filtered = filter.filter(getPolicy(), "context", PIECE, "The ID is 123456.");

        showSpans(filtered.getSpans());
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 10,16, FilterType.IDENTIFIER));

    }

    @Test
    public void filterId17() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new IdentifierFilterStrategy()))
                .withContextService(contextService)
                .withRandom(random)
                .withWindowSize(windowSize)
                .build();

        final IdentifierFilter filter = new IdentifierFilter(filterConfiguration, "name", "(?i)([^Application Name])(.*)$", false, 0);

        final Filtered filtered = filter.filter(getPolicy(), "context", PIECE, "Application Name John Smith");

        showSpans(filtered.getSpans());
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 17,27, FilterType.IDENTIFIER));

    }

    @Test
    public void filterId18() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new IdentifierFilterStrategy()))
                .withContextService(contextService)
                .withRandom(random)
                .withWindowSize(windowSize)
                .build();

        final IdentifierFilter filter = new IdentifierFilter(filterConfiguration, "name", "\\d{3}-\\d{3}-\\d{3}", false, 0);

        final Filtered filtered = filter.filter(getPolicy(), "context", PIECE, "his id was 123-456-789");

        showSpans(filtered.getSpans());
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 11,22, FilterType.IDENTIFIER));

    }

    @Test
    public void filterWithCandidates1() throws Exception {

        final List<String> candidates = List.of("candidate1", "candidate2");

        final IdentifierFilterStrategy identifierFilterStrategy = new IdentifierFilterStrategy();
        identifierFilterStrategy.setStrategy(RANDOM_REPLACE);
        identifierFilterStrategy.setAnonymizationCandidates(candidates);

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(identifierFilterStrategy))
                .withContextService(contextService)
                .withRandom(random)
                .withWindowSize(windowSize)
                .build();

        final IdentifierFilter filter = new IdentifierFilter(filterConfiguration, "name", Identifier.DEFAULT_IDENTIFIER_REGEX, true, 0);

        final Filtered filtered = filter.filter(getPolicy(), "context", PIECE, "the id is AB4736021 in california.");
        showSpans(filtered.getSpans());
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(candidates.contains(filtered.getSpans().get(0).getReplacement()));

    }

}
