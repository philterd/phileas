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
package ai.philterd.phileas.model.conditions;

import ai.philterd.phileas.model.filtering.FilterType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ConditionSupportTest {

    private static String reason(final String condition, final FilterType filterType) {
        final ParsedCondition parsed = ParserListener.getTerminals(condition).get(0);
        return ConditionSupport.unsupported(parsed, filterType);
    }

    @Test
    public void everyEvaluatedConditionIsSupported() {
        for (final String condition : new String[]{
                "token == \"a\"", "token != \"a\"", "token startswith \"a\"",
                "token is \"birthdate\"", "token is not \"deathdate\"", "token is \"BIRTHDATE OR DEATHDATE\"",
                "context == \"c\"", "context != \"c\"",
                "confidence > 0.5", "confidence < 0.5", "confidence >= 0.5", "confidence <= 0.5", "confidence == 1", "confidence != 1"}) {
            Assertions.assertNull(reason(condition, FilterType.SSN), condition);
        }
        for (final String operator : new String[]{">", "<", ">=", "<=", "==", "!="}) {
            Assertions.assertNull(reason("population " + operator + " 4500", FilterType.ZIP_CODE), operator);
        }
    }

    @Test
    public void aConditionNoStrategyEvaluatesIsNotSupported() {
        Assertions.assertEquals("token supports ==, !=, startswith, is, and is not, not >.", reason("token > \"a\"", FilterType.SSN));
        Assertions.assertEquals("token is compares with \"birthdate\", \"deathdate\", or \"birthdate or deathdate\" only.",
                reason("token is \"123-45-6789\"", FilterType.SSN));
        Assertions.assertEquals("token is not compares with \"birthdate\", \"deathdate\", or \"birthdate or deathdate\" only.",
                reason("token is not \"birthday\"", FilterType.DATE));
        Assertions.assertEquals("context supports == and !=, not startswith.", reason("context startswith \"c\"", FilterType.SSN));
        Assertions.assertEquals("confidence supports >, <, >=, <=, ==, and !=, not is.", reason("confidence is 0.5", FilterType.SSN));
        Assertions.assertEquals("population applies only to the zip code filter.", reason("population > 4500", FilterType.SSN));
        Assertions.assertEquals("population supports >, <, >=, <=, ==, and !=, not startswith.", reason("population startswith 45", FilterType.ZIP_CODE));
        Assertions.assertEquals("population compares with a whole number.", reason("population > 4500.5", FilterType.ZIP_CODE));
        Assertions.assertEquals("no filter evaluates type.", reason("type == PER", FilterType.SSN));
    }

    @Test
    public void aReasonNeverRepeatsTheValue() {
        Assertions.assertFalse(reason("token is \"123-45-6789\"", FilterType.SSN).contains("123-45-6789"));
        Assertions.assertFalse(reason("token > \"123-45-6789\"", FilterType.SSN).contains("123-45-6789"));
    }

}
