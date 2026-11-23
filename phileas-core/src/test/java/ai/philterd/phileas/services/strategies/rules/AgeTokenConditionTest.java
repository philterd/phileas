/*
 *     Copyright 2026 Philterd, LLC @ https://www.philterd.ai
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
package ai.philterd.phileas.services.strategies.rules;

import ai.philterd.phileas.PhileasConfiguration;
import ai.philterd.phileas.policy.InvalidPolicyConditionException;
import ai.philterd.phileas.policy.Policy;
import ai.philterd.phileas.services.context.DefaultContextService;
import ai.philterd.phileas.services.disambiguation.vector.InMemoryVectorService;
import ai.philterd.phileas.services.filters.filtering.PlainTextFilterService;
import com.google.gson.Gson;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.OptionalDouble;
import java.util.Properties;

/**
 * An age filter strategy's condition can compare the age in years with a number, as in {@code token > 89}
 * (https://github.com/philterd/phileas/issues/454).
 */
public class AgeTokenConditionTest {

    private static final String SAFE_HARBOR_POLICY = """
            {
              "identifiers": {
                "age": {
                  "ageFilterStrategies": [
                    { "condition": "token > 89", "strategy": "STATIC_REPLACE", "staticReplacement": "90 or older" }
                  ]
                }
              }
            }
            """;

    private static String filter(final String policyJson, final String text) throws Exception {
        final Policy policy = new Gson().fromJson(policyJson, Policy.class);
        final PlainTextFilterService service = new PlainTextFilterService(new PhileasConfiguration(new Properties()),
                new DefaultContextService(), new InMemoryVectorService(), null);
        return service.filter(policy, "context", text).getFilteredText();
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "She is 93 years old.                | She is 90 or older.",
            "She is 72 years old.                | She is 72 years old.",
            "a 93-year-old woman                 | a 90 or older woman",
            "Patient is 93yo.                    | Patient is 90 or older.",
            "Patient is 93 y/o.                  | Patient is 90 or older.",
            "Age: 95                             | 90 or older",
            "He died aged 93.                    | He died 90 or older.",
            "She died at the age of 91.          | She died at the 90 or older.",
            "Age: 89                             | Age: 89",
            "Age: 90                             | 90 or older",
            "Age: 89.5                           | 90 or older",
            "She is ninety-three years old.      | She is 90 or older.",
            "She is eighty-nine years old.       | She is eighty-nine years old.",
            "He is one hundred and two years old. | He is 90 or older.",
            "The infant, aged 18 months, was seen. | The infant, aged 18 months, was seen.",
            "The infant, aged 1095 months, was seen. | The infant, 90 or older months, was seen.",
            "The infant is age 100 days today.   | The infant is age 100 days today."
    })
    public void agesOver89AreReplacedAndYoungerAgesAreKept(final String text, final String expected) throws Exception {
        Assertions.assertEquals(expected, filter(SAFE_HARBOR_POLICY, text), text);
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "The patient is 93 years old.        | The patient is {{{REDACTED-age}}}.",
            "The patient is 93 YEARS OLD.        | The patient is {{{REDACTED-age}}}.",
            "The patient is 93 Years Old.        | The patient is {{{REDACTED-age}}}.",
            "Patient is 93 YO.                   | Patient is {{{REDACTED-age}}}.",
            "Patient is 61 Y/O.                  | Patient is {{{REDACTED-age}}}.",
            "PATIENT IS THIRTY-FIVE YEARS OLD.   | PATIENT IS {{{REDACTED-age}}}."
    })
    public void agesInAnyCaseAreRedacted(final String text, final String expected) throws Exception {
        Assertions.assertEquals(expected, filter("{ \"identifiers\": { \"age\": {} } }", text));
    }

    @Test
    public void aLaterStrategyAppliesToAgesTheConditionDoesNotMatch() throws Exception {

        final String policy = """
                {
                  "identifiers": {
                    "age": {
                      "ageFilterStrategies": [
                        { "condition": "token > 89", "strategy": "STATIC_REPLACE", "staticReplacement": "90 or older" },
                        { "strategy": "REDACT", "redactionFormat": "{{{REDACTED-%t}}}" }
                      ]
                    }
                  }
                }
                """;

        Assertions.assertEquals("One is 90 or older, the other {{{REDACTED-age}}}.",
                filter(policy, "One is 93 years old, the other 72 years old."));

    }

    @Test
    public void everyOperatorComparesTheAgeInYears() {

        final AgeFilterStrategy strategy = new AgeFilterStrategy();
        final String[] window = {"she", "is", "90", "years", "old"};

        final String[][] cases = {
                // operator, 89, 90, 91
                {">", "true", "false", "false"},
                {">=", "true", "true", "false"},
                {"<", "false", "false", "true"},
                {"<=", "false", "true", "true"},
                {"==", "false", "true", "false"},
                {"!=", "true", "false", "true"}};

        for (final String[] c : cases) {
            for (int i = 0; i < 3; i++) {
                final String condition = "token " + c[0] + " " + (89 + i);
                Assertions.assertEquals(Boolean.parseBoolean(c[i + 1]),
                        strategy.evaluateCondition(null, "context", "90 years old", window, condition, 0.9), condition);
            }
        }

    }

    @Test
    public void aQuotedValueIsStillComparedAsText() {

        final AgeFilterStrategy strategy = new AgeFilterStrategy();
        final String[] window = {"she", "is", "93", "years", "old"};

        Assertions.assertTrue(strategy.evaluateCondition(null, "context", "93 years old", window, "token == \"93 years old\"", 0.9));
        Assertions.assertFalse(strategy.evaluateCondition(null, "context", "93 years old", window, "token == \"93\"", 0.9));
        Assertions.assertTrue(strategy.evaluateCondition(null, "context", "93 years old", window, "token startswith \"9\"", 0.9));

    }

    @Test
    public void anAgeThatCannotBeReadSatisfiesNoComparison() {

        final AgeFilterStrategy strategy = new AgeFilterStrategy();

        for (final String operator : new String[]{">", ">=", "<", "<=", "==", "!="}) {
            Assertions.assertFalse(strategy.evaluateCondition(null, "context", "age .", new String[]{"age"},
                    "token " + operator + " 89", 0.9), operator);
        }

    }

    @Test
    public void aNumberJoinedWithOtherConditions() {

        final AgeFilterStrategy strategy = new AgeFilterStrategy();
        final String[] window = {"she", "is", "93", "years", "old"};

        Assertions.assertTrue(strategy.evaluateCondition(null, "ctx", "93 years old", window, "token > 89 and context == \"ctx\"", 0.9));
        Assertions.assertFalse(strategy.evaluateCondition(null, "other", "93 years old", window, "token > 89 and context == \"ctx\"", 0.9));
        Assertions.assertFalse(strategy.evaluateCondition(null, "ctx", "93 years old", window, "token > 89 and token < 93", 0.9));

    }

    @Test
    public void aNumberComparedWithTokenIsRefusedOnOtherFilters() {

        final String policy = """
                { "identifiers": { "ssn": { "ssnFilterStrategies": [ { "condition": "token > 89", "strategy": "REDACT" } ] } } }
                """;

        final InvalidPolicyConditionException ex = Assertions.assertThrows(InvalidPolicyConditionException.class,
                () -> filter(policy, "SSN 123-45-6789"));

        Assertions.assertEquals("Strategy 1 of the ssn filter has a condition that is not supported: token compares with "
                + "a number only on the age filter.", ex.getMessage());

    }

    @Test
    public void aNumberComparedWithStartswithIsRefused() {

        final String policy = """
                { "identifiers": { "age": { "ageFilterStrategies": [ { "condition": "token startswith 9", "strategy": "REDACT" } ] } } }
                """;

        final InvalidPolicyConditionException ex = Assertions.assertThrows(InvalidPolicyConditionException.class,
                () -> filter(policy, "She is 93 years old."));

        Assertions.assertEquals("Strategy 1 of the age filter has a condition that is not supported: token supports >, "
                + "<, >=, <=, ==, and != with a number, not startswith.", ex.getMessage());

    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "47 years old          | she is 47 years old today      | 47",
            "3.5 years old         | the child is 3.5 years old     | 3.5",
            "47yo                  | a 47yo male                    | 47",
            "61 y/o                | a 61 y/o female                | 61",
            "22-year-old           | a 22-year-old student          | 22",
            "69 years              | he was 69 years                | 69",
            "Age: 47               | Age 47 Sex F                   | 47",
            "Age:47                | Age47 Sex F                    | 47",
            "aged 39               | sisters aged 39 and 41         | 39",
            "age of 39             | at the age of 39 she           | 39",
            "Age: 47               | Age 47 years                   | 47",
            "thirty-five years old | he is thirty-five years old    | 35",
            "thirty-five-year-old  | a thirty-five-year-old man     | 35",
            "aged forty-two        | she died aged forty-two        | 42",
            "Age: thirty-five      | Age thirty-five Sex M          | 35",
            "one hundred years old | she is one hundred years old   | 100",
            "a hundred years old   | she is a hundred years old     | 100",
            "one hundred and two years old | he is one hundred and two years old | 102",
            "five yo               | a five yo boy                  | 5",
            "zero years old        | it is zero years old           | 0"
    })
    public void theAgeIsReadInYears(final String token, final String window, final double expected) {
        assertAge(token, window, expected);
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "aged 18          | the infant aged 18 months was        | 1.5",
            "aged 18          | the infant aged 18 mos was           | 1.5",
            "Age: 6           | Age 6 mo Sex F                       | 0.5",
            "age 2            | a baby age 2 weeks today             | 0.0383561643835616",
            "aged 3           | aged 3 wks                           | 0.0575342465753425",
            "age 730          | age 730 days                         | 2",
            "aged eighteen    | aged eighteen months                 | 1.5",
            "aged 18          | aged 18 years                        | 18",
            "aged 18          | aged 18                              | 18"
    })
    public void aKeywordAgeTakesTheUnitAfterIt(final String token, final String window, final double expected) {
        assertAge(token, window, expected);
    }

    /** Asserts the age in years read from a span, with the window given as space-separated words. */
    private static void assertAge(final String token, final String window, final double expected) {
        final OptionalDouble age = AgeInYears.of(token, window.split(" "));
        Assertions.assertTrue(age.isPresent(), token);
        Assertions.assertEquals(expected, age.getAsDouble(), 1e-9, token);
    }

    @Test
    public void aYearUnitInTheSpanIsNotChangedByTheWordAfterIt() {
        Assertions.assertEquals(18, AgeInYears.of("18 years", new String[]{"18", "years", "months"}).getAsDouble(), 1e-9);
    }

    @Test
    public void aSpanWithNoNumberHasNoAge() {
        Assertions.assertTrue(AgeInYears.of("age .", new String[]{"age"}).isEmpty());
        Assertions.assertTrue(AgeInYears.of(null, new String[0]).isEmpty());
        Assertions.assertEquals(18, AgeInYears.of("aged 18", null).getAsDouble(), 1e-9);
    }

}
