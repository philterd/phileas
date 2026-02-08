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
import ai.philterd.phileas.services.filters.custom.PhoneNumberRulesFilter;
import ai.philterd.phileas.services.filters.regex.IdentifierFilter;
import ai.philterd.phileas.services.strategies.rules.IdentifierFilterStrategy;
import ai.philterd.phileas.services.strategies.rules.PhoneNumberFilterStrategy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.security.SecureRandom;
import java.util.List;

import static ai.philterd.phileas.services.strategies.AbstractFilterStrategy.RANDOM_REPLACE;

public class PhoneNumberFilterTest extends AbstractFilterTest {

    private static final Logger LOGGER = LogManager.getLogger(PhoneNumberFilterTest.class);

    @Test
    public void filterPhone1() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new PhoneNumberFilterStrategy()))
                .withContextService(contextService)
                .withRandom(random)
                .withWindowSize(windowSize)
                .build();

        final PhoneNumberRulesFilter filter = new PhoneNumberRulesFilter(filterConfiguration);

        final Filtered filtered = filter.filter(getPolicy(), "context", PIECE, "the number is (123) 456-7890.");
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 14, 28, FilterType.PHONE_NUMBER));
        Assertions.assertEquals("(123) 456-7890", filtered.getSpans().get(0).getText());
        Assertions.assertEquals(0.95, filtered.getSpans().get(0).getConfidence());

    }

    @Test
    public void filterPhone2() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new PhoneNumberFilterStrategy()))
                .withContextService(contextService)
                .withRandom(random)
                .withWindowSize(windowSize)
                .build();

        final PhoneNumberRulesFilter filter = new PhoneNumberRulesFilter(filterConfiguration);

        final Filtered filtered = filter.filter(getPolicy(), "context", PIECE, "the number is (123) 456-7890 and (123) 456-7890.");
        Assertions.assertEquals(2, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 14, 28, FilterType.PHONE_NUMBER));
        Assertions.assertEquals(0.95, filtered.getSpans().get(0).getConfidence());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(1), 33, 47, FilterType.PHONE_NUMBER));
        Assertions.assertEquals(0.95, filtered.getSpans().get(1).getConfidence());

    }

    @Test
    public void filterPhone3() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new PhoneNumberFilterStrategy()))
                .withContextService(contextService)
                .withRandom(random)
                .withWindowSize(windowSize)
                .build();

        final PhoneNumberRulesFilter filter = new PhoneNumberRulesFilter(filterConfiguration);

        final Filtered filtered = filter.filter(getPolicy(), "context", PIECE, "the number is 123-456-7890.");
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 14, 26, FilterType.PHONE_NUMBER));
        Assertions.assertEquals(0.95, filtered.getSpans().get(0).getConfidence());

    }

    @Test
    public void filterPhone4() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new PhoneNumberFilterStrategy()))
                .withContextService(contextService)
                .withRandom(random)
                .withWindowSize(windowSize)
                .build();

        final PhoneNumberRulesFilter filter = new PhoneNumberRulesFilter(filterConfiguration);

        final Filtered filtered = filter.filter(getPolicy(), "context", PIECE, "the number is 123-456-7890 and he was ok.");
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 14, 26, FilterType.PHONE_NUMBER));

    }

    @Test
    public void filterPhone5() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new PhoneNumberFilterStrategy()))
                .withContextService(contextService)
                .withRandom(random)
                .withWindowSize(windowSize)
                .build();

        final PhoneNumberRulesFilter filter = new PhoneNumberRulesFilter(filterConfiguration);

        final Filtered filtered = filter.filter(getPolicy(), "context", PIECE, "the number is ( 800 ) 123-4567 and he was ok.");
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 14, 30, FilterType.PHONE_NUMBER));
        Assertions.assertEquals(0.75, filtered.getSpans().get(0).getConfidence());

    }

    @Test
    public void filterPhone6() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new PhoneNumberFilterStrategy()))
                .withContextService(contextService)
                .withRandom(random)
                .withWindowSize(windowSize)
                .build();

        final PhoneNumberRulesFilter filter = new PhoneNumberRulesFilter(filterConfiguration);

        final Filtered filtered = filter.filter(getPolicy(), "context", PIECE, "the number is (800) 123-4567 x532 and he was ok.");
        showSpans(filtered.getSpans());

        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 14, 33, FilterType.PHONE_NUMBER));
        Assertions.assertEquals(0.75, filtered.getSpans().get(0).getConfidence());

    }

    @Test
    public void filterPhone7() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new PhoneNumberFilterStrategy()))
                .withContextService(contextService)
                .withRandom(random)
                .withWindowSize(windowSize)
                .build();

        final PhoneNumberRulesFilter filter = new PhoneNumberRulesFilter(filterConfiguration);

        final Filtered filtered = filter.filter(getPolicy(), "context", PIECE, "the number is (800) 123-4567x532 and he was ok.");
        showSpans(filtered.getSpans());

        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 14, 32, FilterType.PHONE_NUMBER));
        Assertions.assertEquals(0.75, filtered.getSpans().get(0).getConfidence());

    }

    @Test
    public void filterPhone8() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new PhoneNumberFilterStrategy()))
                .withContextService(contextService)
                .withRandom(random)
                .withWindowSize(windowSize)
                .build();

        final PhoneNumberRulesFilter filter = new PhoneNumberRulesFilter(filterConfiguration);

        final Filtered filtered = filter.filter(getPolicy(), "context", PIECE, "7 64116-3220");
        showSpans(filtered.getSpans());

        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertEquals(0.60, filtered.getSpans().get(0).getConfidence());

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

    @Test
    public void filterPhone9() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new PhoneNumberFilterStrategy()))
                .withContextService(contextService)
                .withRandom(random)
                .withWindowSize(windowSize)
                .build();

        final PhoneNumberRulesFilter filter = new PhoneNumberRulesFilter(filterConfiguration);

        final Filtered filtered = filter.filter(getPolicy(), "context", PIECE, "George Washington was president and his SSN was 123-45-6789. His phone number was (555) 123-9988 and he lived in 20001.");
        showSpans(filtered.getSpans());

        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertEquals(0.95, filtered.getSpans().get(0).getConfidence());
        Assertions.assertEquals(82, filtered.getSpans().get(0).getCharacterStart());
        Assertions.assertEquals(96, filtered.getSpans().get(0).getCharacterEnd());

    }

}
