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
import ai.philterd.phileas.policy.filters.Validator;
import ai.philterd.phileas.services.filters.regex.IdentifierFilter;
import ai.philterd.phileas.services.strategies.rules.IdentifierFilterStrategy;
import ai.philterd.phileas.services.validators.BicStructuralValidator;
import ai.philterd.phileas.services.validators.DePersonalausweisValidator;
import ai.philterd.phileas.services.validators.DeSteuerIdValidator;
import ai.philterd.phileas.services.validators.IdentifierValidators;
import ai.philterd.phileas.services.validators.LuhnValidator;
import ai.philterd.phileas.services.validators.SpanValidator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static ai.philterd.phileas.services.strategies.AbstractFilterStrategy.RANDOM_REPLACE;

public class IdentifierFilterTest extends AbstractFilterTest {


    @Test
    public void filterId1() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new IdentifierFilterStrategy()))
                .withRandom(random)
                .withWindowSize(windowSize)
                .build();

        final IdentifierFilter filter = new IdentifierFilter(filterConfiguration, "name", Identifier.DEFAULT_IDENTIFIER_REGEX, true, 0);

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "the id is AB4736021 in california.");

        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 10,19, FilterType.IDENTIFIER));
        Assertions.assertEquals("AB4736021", filtered.getSpans().get(0).getText());

    }

    @Test
    public void filterId2() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new IdentifierFilterStrategy()))
                .withRandom(random)
                .withWindowSize(windowSize)
                .build();

        final IdentifierFilter filter = new IdentifierFilter(filterConfiguration, "name", Identifier.DEFAULT_IDENTIFIER_REGEX, true, 0);

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "the id is AB473-6021 in california.");

        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 10,20, FilterType.IDENTIFIER));

    }

    @Test
    public void filterId3() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new IdentifierFilterStrategy()))
                .withRandom(random)
                .withWindowSize(windowSize)
                .build();

        final IdentifierFilter filter = new IdentifierFilter(filterConfiguration, "name", Identifier.DEFAULT_IDENTIFIER_REGEX, true, 0);

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "the id is 473-6AB021 in california.");

        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 10,20, FilterType.IDENTIFIER));

    }

    @Test
    public void filterId4() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new IdentifierFilterStrategy()))
                .withRandom(random)
                .withWindowSize(windowSize)
                .build();

        final IdentifierFilter filter = new IdentifierFilter(filterConfiguration, "name", Identifier.DEFAULT_IDENTIFIER_REGEX, true, 0);

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "the id is AB473-6021 in california.");

        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 10,20, FilterType.IDENTIFIER));

    }

    @Test
    public void filterId5() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new IdentifierFilterStrategy()))
                .withRandom(random)
                .withWindowSize(windowSize)
                .build();

        final IdentifierFilter filter = new IdentifierFilter(filterConfiguration, "name", Identifier.DEFAULT_IDENTIFIER_REGEX, true, 0);

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "the id is 473-6AB021 in california.");

        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 10,20, FilterType.IDENTIFIER));

    }

    @Test
    public void filterId6() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new IdentifierFilterStrategy()))
                .withRandom(random)
                .withWindowSize(windowSize)
                .build();

        final IdentifierFilter filter = new IdentifierFilter(filterConfiguration, "name", Identifier.DEFAULT_IDENTIFIER_REGEX, true, 0);

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "the id is 123-45-6789 in california.");

        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 10,21, FilterType.IDENTIFIER));

    }

    @Test
    public void filterId7() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new IdentifierFilterStrategy()))
                .withRandom(random)
                .withWindowSize(windowSize)
                .build();

        final IdentifierFilter filter = new IdentifierFilter(filterConfiguration, "name", Identifier.DEFAULT_IDENTIFIER_REGEX, true, 0);

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "George Washington was president and his ssn was 123-45-6789 and he lived at 90210. Patient id 00076A and 93821A. He is on biotin. Diagnosed with A01000.");

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
                .withRandom(random)
                .withWindowSize(windowSize)
                .build();

        final IdentifierFilter filter = new IdentifierFilter(filterConfiguration, "name", Identifier.DEFAULT_IDENTIFIER_REGEX, true, 0);

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "the id is 000-00-00-00 ABC123 in california.");

        Assertions.assertEquals(2, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 10, 22, FilterType.IDENTIFIER));
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(1), 23, 29, FilterType.IDENTIFIER));

    }

    @Test
    public void filterId9() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new IdentifierFilterStrategy()))
                .withRandom(random)
                .withWindowSize(windowSize)
                .build();

        final IdentifierFilter filter = new IdentifierFilter(filterConfiguration, "name", Identifier.DEFAULT_IDENTIFIER_REGEX, true, 0);

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "the id is AZ12 ABC1234/123ABC4 in california.");

        Assertions.assertEquals(2, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 15, 22, FilterType.IDENTIFIER));
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(1), 23, 30, FilterType.IDENTIFIER));

    }

    @Test
    public void filterId10() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new IdentifierFilterStrategy()))
                .withRandom(random)
                .withWindowSize(windowSize)
                .build();

        final IdentifierFilter filter = new IdentifierFilter(filterConfiguration, "name", Identifier.DEFAULT_IDENTIFIER_REGEX, true, 0);

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "the id is H3SNPUHYEE7JD3H in california.");

        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 10,25, FilterType.IDENTIFIER));

    }

    @Test
    public void filterId11() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new IdentifierFilterStrategy()))
                .withRandom(random)
                .withWindowSize(windowSize)
                .build();

        final IdentifierFilter filter = new IdentifierFilter(filterConfiguration, "name", Identifier.DEFAULT_IDENTIFIER_REGEX, true, 0);

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "the id is 86637729 in california.");

        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 10,18, FilterType.IDENTIFIER));

    }

    @Test
    public void filterId12() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new IdentifierFilterStrategy()))
                .withRandom(random)
                .withWindowSize(windowSize)
                .build();

        final IdentifierFilter filter = new IdentifierFilter(filterConfiguration, "name", Identifier.DEFAULT_IDENTIFIER_REGEX, true, 0);

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "the id is 33778376 in california.");

        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 10,18, FilterType.IDENTIFIER));

    }

    @Test
    public void filterId13() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new IdentifierFilterStrategy()))
                .withRandom(random)
                .withWindowSize(windowSize)
                .build();

        final IdentifierFilter filter = new IdentifierFilter(filterConfiguration, "name", "\\b[A-Z]{4,}\\b", true, 0);

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "the id is ABCD.");

        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 10,14, FilterType.IDENTIFIER));

    }

    @Test
    public void filterId14() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new IdentifierFilterStrategy()))
                .withRandom(random)
                .withWindowSize(windowSize)
                .build();

        final IdentifierFilter filter = new IdentifierFilter(filterConfiguration, "name", Identifier.DEFAULT_IDENTIFIER_REGEX, true, 0);

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "the id is 123456.");

        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 10,16, FilterType.IDENTIFIER));

    }

    @Test
    public void filterId15() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new IdentifierFilterStrategy()))
                .withRandom(random)
                .withWindowSize(windowSize)
                .build();

        final IdentifierFilter filter = new IdentifierFilter(filterConfiguration, "name", Identifier.DEFAULT_IDENTIFIER_REGEX, true, 0);

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "John Smith, patient ID A203493, was seen on February 18.");

        showSpans(filtered.getSpans());
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 23,30, FilterType.IDENTIFIER));

    }

    @Test
    public void filterId16() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new IdentifierFilterStrategy()))
                .withRandom(random)
                .withWindowSize(windowSize)
                .build();

        final IdentifierFilter filter = new IdentifierFilter(filterConfiguration, "name", "\\b\\d{3,8}\\b", false, 0);

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "The ID is 123456.");

        showSpans(filtered.getSpans());
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 10,16, FilterType.IDENTIFIER));

    }

    @Test
    public void filterId17() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new IdentifierFilterStrategy()))
                .withRandom(random)
                .withWindowSize(windowSize)
                .build();

        final IdentifierFilter filter = new IdentifierFilter(filterConfiguration, "name", "(?i)([^Application Name])(.*)$", false, 0);

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "Application Name John Smith");

        showSpans(filtered.getSpans());
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 17,27, FilterType.IDENTIFIER));

    }

    @Test
    public void filterId18() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new IdentifierFilterStrategy()))
                .withRandom(random)
                .withWindowSize(windowSize)
                .build();

        final IdentifierFilter filter = new IdentifierFilter(filterConfiguration, "name", "\\d{3}-\\d{3}-\\d{3}", false, 0);

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "his id was 123-456-789");

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
                .withRandom(random)
                .withWindowSize(windowSize)
                .build();

        final IdentifierFilter filter = new IdentifierFilter(filterConfiguration, "name", Identifier.DEFAULT_IDENTIFIER_REGEX, true, 0);

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "the id is AB4736021 in california.");
        showSpans(filtered.getSpans());
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(candidates.contains(filtered.getSpans().get(0).getReplacement()));

    }

    /**
     * A catastrophic-backtracking (ReDoS) pattern from the policy must be aborted by the regex time
     * budget rather than running unbounded. The filter returns no spans (fail safe) and completes
     * well within the preemptive timeout; without the guard this match runs far longer than the
     * budget. (Modern JVMs optimize the classic exponential patterns, so this uses a polynomial
     * pattern - nested greedy .* under a bounded repetition - which still backtracks badly.)
     */
    @Test
    public void catastrophicPatternIsAbortedByTheRegexBudget() {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new IdentifierFilterStrategy()))
                .withRandom(random)
                .withWindowSize(windowSize)
                .withRegexTimeoutMs(200)
                .build();

        // Nested greedy .* under a bounded repetition, with a trailing 'b' that never appears, so
        // the match cannot succeed and the engine exhausts an enormous backtracking space (unguarded,
        // this runs for many seconds).
        final IdentifierFilter filter = new IdentifierFilter(filterConfiguration, "name", "(.*a){16}b", true, 0);
        final String input = "the id is " + "a".repeat(30) + "!";

        Assertions.assertTimeoutPreemptively(Duration.ofSeconds(5), () -> {
            final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, input);
            Assertions.assertTrue(filtered.getSpans().isEmpty(),
                    "a timed-out pattern should yield no spans");
        });

    }

    /**
     * A small budget must not break legitimate matching: a well-behaved pattern still finds its
     * matches quickly, so the guard does not produce false timeouts.
     */
    @Test
    public void legitimatePatternStillMatchesUnderASmallBudget() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new IdentifierFilterStrategy()))
                .withRandom(random)
                .withWindowSize(windowSize)
                .withRegexTimeoutMs(200)
                .build();

        final IdentifierFilter filter = new IdentifierFilter(filterConfiguration, "name", Identifier.DEFAULT_IDENTIFIER_REGEX, true, 0);

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "the id is AB4736021 in california.");

        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 10, 19, FilterType.IDENTIFIER));

    }

    private static final String SIN_PATTERN = "\\b\\d{3}[ -]?\\d{3}[ -]?\\d{3}\\b";

    /**
     * With the luhn validator attached, a Luhn-valid Canadian SIN is kept.
     */
    @Test
    public void luhnValidatorKeepsValidSin() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new IdentifierFilterStrategy()))
                .withRandom(random)
                .withWindowSize(windowSize)
                .build();

        final IdentifierFilter filter = new IdentifierFilter(filterConfiguration, "canada-sin", SIN_PATTERN, false, 0, LuhnValidator.getInstance());

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "the sin is 046 454 286 on file.");

        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertEquals("046 454 286", filtered.getSpans().get(0).getText());

    }

    /**
     * With the luhn validator attached, a value that matches the pattern but fails the checksum
     * is dropped. Without the validator the same value would be redacted, so this is the behavior
     * the validator adds.
     */
    @Test
    public void luhnValidatorDropsChecksumInvalidValue() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new IdentifierFilterStrategy()))
                .withRandom(random)
                .withWindowSize(windowSize)
                .build();

        final IdentifierFilter filter = new IdentifierFilter(filterConfiguration, "canada-sin", SIN_PATTERN, false, 0, LuhnValidator.getInstance());

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "the number 123 456 789 is not a sin.");

        Assertions.assertEquals(0, filtered.getSpans().size());

    }

    /**
     * The same pattern with no validator keeps the checksum-invalid value, confirming the drop
     * above is the validator's doing and not the pattern's.
     */
    @Test
    public void noValidatorKeepsChecksumInvalidValue() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new IdentifierFilterStrategy()))
                .withRandom(random)
                .withWindowSize(windowSize)
                .build();

        final IdentifierFilter filter = new IdentifierFilter(filterConfiguration, "canada-sin", SIN_PATTERN, false, 0);

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "the number 123 456 789 is not a sin.");

        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertEquals("123 456 789", filtered.getSpans().get(0).getText());

    }

    // A context-anchored pattern that captures only the SIN digits in group 1.
    private static final String SIN_CONTEXT_PATTERN = "SIN[:\\s]*((?:\\d{3}[ -]?){2}\\d{3})";

    /**
     * The validator runs against the captured group (what gets redacted), not the whole match, so
     * a context-cued pattern with a capture group still validates the identifier itself.
     */
    @Test
    public void luhnValidatorAppliesToCapturedGroupValidSin() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new IdentifierFilterStrategy()))
                .withRandom(random)
                .withWindowSize(windowSize)
                .build();

        final IdentifierFilter filter = new IdentifierFilter(filterConfiguration, "canada-sin", SIN_CONTEXT_PATTERN, false, 1, LuhnValidator.getInstance());

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "the SIN: 046 454 286 is on file.");

        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertEquals("046 454 286", filtered.getSpans().get(0).getText());

    }

    /**
     * The captured group is checksum-invalid, so it is dropped even though the surrounding context
     * matched.
     */
    @Test
    public void luhnValidatorAppliesToCapturedGroupInvalidSin() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new IdentifierFilterStrategy()))
                .withRandom(random)
                .withWindowSize(windowSize)
                .build();

        final IdentifierFilter filter = new IdentifierFilter(filterConfiguration, "canada-sin", SIN_CONTEXT_PATTERN, false, 1, LuhnValidator.getInstance());

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "the SIN: 123 456 789 is on file.");

        Assertions.assertEquals(0, filtered.getSpans().size());

    }

    private static final String BIC_PATTERN = "\\b[A-Z]{6}[A-Z0-9]{2}([A-Z0-9]{3})?\\b";

    /**
     * With the bic-structural validator attached, a structurally valid BIC with a real country code
     * is kept.
     */
    @Test
    public void bicValidatorKeepsValidBic() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new IdentifierFilterStrategy()))
                .withRandom(random)
                .withWindowSize(windowSize)
                .build();

        final IdentifierFilter filter = new IdentifierFilter(filterConfiguration, "bic", BIC_PATTERN, true, 0, BicStructuralValidator.getInstance());

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "wire to DEUTDEFF please.");

        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertEquals("DEUTDEFF", filtered.getSpans().get(0).getText());

    }

    /**
     * A token that matches the BIC shape but has an unassigned country code is dropped.
     */
    @Test
    public void bicValidatorDropsInvalidCountry() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new IdentifierFilterStrategy()))
                .withRandom(random)
                .withWindowSize(windowSize)
                .build();

        final IdentifierFilter filter = new IdentifierFilter(filterConfiguration, "bic", BIC_PATTERN, true, 0, BicStructuralValidator.getInstance());

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "wire to DEUTZZFF please.");

        Assertions.assertEquals(0, filtered.getSpans().size());

    }

    // A leading letter followed by nine digits (the last is the check digit).
    private static final String DE_ID_PATTERN = "\\b[A-Z]\\d{9}\\b";

    /**
     * With the de-personalausweis validator attached, a number with a correct check digit is kept.
     */
    @Test
    public void dePersonalausweisValidatorKeepsValidNumber() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new IdentifierFilterStrategy()))
                .withRandom(random)
                .withWindowSize(windowSize)
                .build();

        final IdentifierFilter filter = new IdentifierFilter(filterConfiguration, "de-id", DE_ID_PATTERN, true, 0, DePersonalausweisValidator.getInstance());

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "ID card T220001293 on file.");

        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertEquals("T220001293", filtered.getSpans().get(0).getText());

    }

    /**
     * A number that matches the shape but fails the check digit is dropped.
     */
    @Test
    public void dePersonalausweisValidatorDropsBadCheckDigit() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new IdentifierFilterStrategy()))
                .withRandom(random)
                .withWindowSize(windowSize)
                .build();

        final IdentifierFilter filter = new IdentifierFilter(filterConfiguration, "de-id", DE_ID_PATTERN, true, 0, DePersonalausweisValidator.getInstance());

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "ID card T220001294 on file.");

        Assertions.assertEquals(0, filtered.getSpans().size());

    }

    private static final String DE_STEUERID_PATTERN = "\\b\\d{11}\\b";

    /**
     * With the de-steuerid validator attached, a number that satisfies the repetition rule and the
     * check digit is kept.
     */
    @Test
    public void deSteuerIdValidatorKeepsValidNumber() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new IdentifierFilterStrategy()))
                .withRandom(random)
                .withWindowSize(windowSize)
                .build();

        final IdentifierFilter filter = new IdentifierFilter(filterConfiguration, "de-steuerid", DE_STEUERID_PATTERN, true, 0, DeSteuerIdValidator.getInstance());

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "tax id 86095742719 on file.");

        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertEquals("86095742719", filtered.getSpans().get(0).getText());

    }

    /**
     * A number that matches the shape but fails the check digit is dropped.
     */
    @Test
    public void deSteuerIdValidatorDropsBadCheckDigit() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new IdentifierFilterStrategy()))
                .withRandom(random)
                .withWindowSize(windowSize)
                .build();

        final IdentifierFilter filter = new IdentifierFilter(filterConfiguration, "de-steuerid", DE_STEUERID_PATTERN, true, 0, DeSteuerIdValidator.getInstance());

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "tax id 86095742718 on file.");

        Assertions.assertEquals(0, filtered.getSpans().size());

    }

    private static final String CPF_PATTERN = "\\b\\d{11}\\b";

    /**
     * A parameterized validator (mod11 with the cpf variant) resolved through the registry keeps a
     * valid CPF, proving the params path works end to end through the filter.
     */
    @Test
    public void mod11CpfValidatorKeepsValidCpf() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new IdentifierFilterStrategy()))
                .withRandom(random)
                .withWindowSize(windowSize)
                .build();

        final SpanValidator cpf = IdentifierValidators.fromPolicy(new Validator("mod11", Map.of("variant", "cpf")));
        final IdentifierFilter filter = new IdentifierFilter(filterConfiguration, "cpf", CPF_PATTERN, true, 0, cpf);

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "cpf 52998224725 on file.");

        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertEquals("52998224725", filtered.getSpans().get(0).getText());

    }

    /**
     * The same validator drops a value that matches the shape but fails the mod-11 check digits.
     */
    @Test
    public void mod11CpfValidatorDropsInvalidCpf() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new IdentifierFilterStrategy()))
                .withRandom(random)
                .withWindowSize(windowSize)
                .build();

        final SpanValidator cpf = IdentifierValidators.fromPolicy(new Validator("mod11", Map.of("variant", "cpf")));
        final IdentifierFilter filter = new IdentifierFilter(filterConfiguration, "cpf", CPF_PATTERN, true, 0, cpf);

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "cpf 52998224724 on file.");

        Assertions.assertEquals(0, filtered.getSpans().size());

    }

}
