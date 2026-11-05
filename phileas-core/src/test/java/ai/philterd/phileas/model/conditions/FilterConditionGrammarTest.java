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
package ai.philterd.phileas.model.conditions;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

public class FilterConditionGrammarTest {

    private static final Logger LOGGER = LogManager.getLogger(FilterConditionGrammarTest.class);

    @Test
    public void testPopulationWithAND() {

        final List<ParsedCondition> conditions = ParserListener.getTerminals("population < 2500 and population > 10000");

        Assertions.assertEquals(2, conditions.size());

        ParsedCondition parsedCondition = conditions.get(0);

        Assertions.assertEquals("population", parsedCondition.getField());
        Assertions.assertEquals("<", parsedCondition.getOperator());
        Assertions.assertEquals("2500", parsedCondition.getValue());

        parsedCondition = conditions.get(1);

        Assertions.assertEquals("population", parsedCondition.getField());
        Assertions.assertEquals(">", parsedCondition.getOperator());
        Assertions.assertEquals("10000", parsedCondition.getValue());

    }

    @Test
    public void testPopulation() {

        final List<ParsedCondition> conditions = ParserListener.getTerminals("population < 2500");

        final ParsedCondition parsedCondition = conditions.get(0);

        Assertions.assertEquals("population", parsedCondition.getField());
        Assertions.assertEquals("<", parsedCondition.getOperator());
        Assertions.assertEquals("2500", parsedCondition.getValue());

    }

    @Test
    public void testToken() {

        final List<ParsedCondition> conditions = ParserListener.getTerminals("token == \"test\"");

        ParsedCondition parsedCondition = conditions.get(0);

        Assertions.assertEquals("token", parsedCondition.getField());
        Assertions.assertEquals("==", parsedCondition.getOperator());
        Assertions.assertEquals("\"test\"", parsedCondition.getValue());

    }

    @Test
    public void testNerConditions1() {

        final List<ParsedCondition> conditions = ParserListener.getTerminals("confidence != 0.5 and type != LOC");

        Assertions.assertEquals(2, conditions.size());

        ParsedCondition parsedCondition = conditions.get(0);

        Assertions.assertEquals("confidence", parsedCondition.getField());
        Assertions.assertEquals("!=", parsedCondition.getOperator());
        Assertions.assertEquals("0.5", parsedCondition.getValue());

        parsedCondition = conditions.get(1);

        Assertions.assertEquals("type", parsedCondition.getField());
        Assertions.assertEquals("!=", parsedCondition.getOperator());
        Assertions.assertEquals("LOC", parsedCondition.getValue());

    }

    @Test
    public void testNerConditions2() {

        final List<ParsedCondition> conditions = ParserListener.getTerminals("confidence < 0.4 and type == PER");

        Assertions.assertEquals(2, conditions.size());

        ParsedCondition parsedCondition = conditions.get(0);

        Assertions.assertEquals("confidence", parsedCondition.getField());
        Assertions.assertEquals("<", parsedCondition.getOperator());
        Assertions.assertEquals("0.4", parsedCondition.getValue());

        parsedCondition = conditions.get(1);

        Assertions.assertEquals("type", parsedCondition.getField());
        Assertions.assertEquals("==", parsedCondition.getOperator());
        Assertions.assertEquals("PER", parsedCondition.getValue());

    }

    private static final java.time.Duration LIMIT = java.time.Duration.ofSeconds(5);

    @Test
    public void malformedConditionsAreRefusedPromptly() {

        // Each of these used to make the lexer return zero-length tokens without end (issue #438).
        final String[] malformed = {
                "token == x",                       // a value not in double quotes
                "token is birthdate",               // the same, with is
                "confidence > abc",                 // a word where a number belongs
                "tokn == \"x\"",                    // a misspelled field
                "",                                 // nothing at all
                "   ",                              // only spaces
                "token == \"x",                      // an unterminated value
                "token == \"x\" and",                 // a dangling and
                "token == \"x\" or token == \"y\"",   // or is not supported
                "token\t==\t\"x\"",                   // tabs between the parts
                "classification == \"x\"",          // a field no strategy evaluates
                "token ==",                         // no value
                "== \"x\"",                           // no field
                "\u00FF\u0000%^&*(",                   // noise
        };

        for (final String condition : malformed) {
            final InvalidConditionException ex = Assertions.assertThrows(InvalidConditionException.class,
                    () -> Assertions.assertTimeoutPreemptively(LIMIT, () -> ParserListener.getTerminals(condition)),
                    "expected '" + condition + "' to be refused");
            Assertions.assertTrue(ex.getPosition() >= 0 && ex.getPosition() <= condition.length(), condition);
        }

    }

    @Test
    public void errorsGiveThePositionButNotTheValue() {

        final InvalidConditionException ex = Assertions.assertThrows(InvalidConditionException.class,
                () -> ParserListener.getTerminals("token == SECRET123"));

        Assertions.assertEquals(9, ex.getPosition());
        Assertions.assertTrue(ex.getMessage().contains("character 10"), ex.getMessage());
        Assertions.assertFalse(ex.getMessage().contains("SECRET123"), ex.getMessage());

    }

    @Test
    public void randomInputIsParsedOrRefusedPromptly() {

        final java.util.Random random = new java.util.Random(438);
        final String alphabet = "tokenconfidcxpulais=!<>\"0123456789. \t\\ANDPERLOC";

        Assertions.assertTimeoutPreemptively(java.time.Duration.ofSeconds(60), () -> {
            for (int i = 0; i < 2000; i++) {
                final StringBuilder sb = new StringBuilder();
                final int length = random.nextInt(30);
                for (int j = 0; j < length; j++) {
                    sb.append(alphabet.charAt(random.nextInt(alphabet.length())));
                }
                try {
                    ParserListener.getTerminals(sb.toString());
                } catch (final InvalidConditionException ex) {
                    // Refused, as expected for most random input.
                }
            }
        });

    }

    @Test
    public void validConditionsParseAsBefore() {

        assertParses("token is not \"birthdate\"", "token", "is not", "\"birthdate\"");
        assertParses("token is \"birthdate or deathdate\"", "token", "is", "\"birthdate or deathdate\"");
        assertParses("token startswith \"90\"", "token", "startswith", "\"90\"");
        assertParses("token != \"127.0.0.1\"", "token", "!=", "\"127.0.0.1\"");
        assertParses("context == \"my-context\"", "context", "==", "\"my-context\"");
        assertParses("population >= 2500", "population", ">=", "2500");
        assertParses("type == loc", "type", "==", "loc");

        final List<ParsedCondition> joined = ParserListener.getTerminals("token != \"123-45-6789\" AND context == \"c\" and confidence <= 0.9");
        Assertions.assertEquals(3, joined.size());
        Assertions.assertEquals("token", joined.get(0).getField());
        Assertions.assertEquals("context", joined.get(1).getField());
        Assertions.assertEquals("confidence", joined.get(2).getField());
        Assertions.assertEquals("<=", joined.get(2).getOperator());
        Assertions.assertEquals("0.9", joined.get(2).getValue());

    }

    private static void assertParses(final String condition, final String field, final String operator, final String value) {
        final List<ParsedCondition> conditions = ParserListener.getTerminals(condition);
        Assertions.assertEquals(1, conditions.size(), condition);
        Assertions.assertEquals(field, conditions.get(0).getField(), condition);
        Assertions.assertEquals(operator, conditions.get(0).getOperator(), condition);
        Assertions.assertEquals(value, conditions.get(0).getValue(), condition);
    }

}
