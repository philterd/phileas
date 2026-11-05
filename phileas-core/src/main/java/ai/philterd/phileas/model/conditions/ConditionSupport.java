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

import java.util.Set;

/**
 * Which conditions the filter strategies evaluate. The condition grammar accepts any field with any operator, but a
 * strategy evaluates only some of them; any other condition is never satisfied on its own, and after a satisfied
 * clause it is ignored. Such a condition is refused when the policy is loaded rather than silently changing which
 * values are redacted.
 */
public final class ConditionSupport {

    private static final Set<String> COMPARISONS = Set.of(">", "<", ">=", "<=", "==", "!=");
    private static final Set<String> TOKEN_OPERATORS = Set.of("==", "!=", "startswith", "is", "is not");
    private static final Set<String> DATE_CATEGORIES = Set.of("birthdate", "deathdate", "birthdate or deathdate");

    private ConditionSupport() {
    }

    /**
     * Why a parsed condition cannot be evaluated by a strategy of this filter type, or null when it can. The reason
     * names the field and operator but never the value, since a value compared with the token can be sensitive.
     * @param condition The parsed condition.
     * @param filterType The type of the filter whose strategy has the condition.
     * @return The reason, or null.
     */
    public static String unsupported(final ParsedCondition condition, final FilterType filterType) {

        final String field = condition.getField() == null ? "" : condition.getField().toLowerCase();
        final String operator = condition.getOperator();

        switch (field) {
            case "token":
                if (!TOKEN_OPERATORS.contains(operator)) {
                    return "token supports ==, !=, startswith, is, and is not, not " + operator + ".";
                }
                if (("is".equals(operator) || "is not".equals(operator)) && !DATE_CATEGORIES.contains(unquoted(condition.getValue()))) {
                    return "token " + operator + " compares with \"birthdate\", \"deathdate\", or \"birthdate or deathdate\" only.";
                }
                return null;
            case "context":
                return "==".equals(operator) || "!=".equals(operator) ? null
                        : "context supports == and !=, not " + operator + ".";
            case "confidence":
                return COMPARISONS.contains(operator) ? null
                        : "confidence supports >, <, >=, <=, ==, and !=, not " + operator + ".";
            case "population":
                if (filterType != FilterType.ZIP_CODE) {
                    return "population applies only to the zip code filter.";
                }
                if (!COMPARISONS.contains(operator)) {
                    return "population supports >, <, >=, <=, ==, and !=, not " + operator + ".";
                }
                return condition.getValue() != null && condition.getValue().matches("[0-9]+") ? null
                        : "population compares with a whole number.";
            default:
                return "no filter evaluates " + field + ".";
        }

    }

    private static String unquoted(final String value) {
        return value == null ? "" : value.replace("\"", "").toLowerCase();
    }

}
