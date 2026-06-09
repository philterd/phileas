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
package ai.philterd.phileas.policy;

import ai.philterd.phileas.services.strategies.AbstractFilterStrategy;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Guards against the redaction policy schema (the published contract) drifting away from what the
 * Phileas runtime actually implements. The schema is authored and versioned externally (in
 * philterd/phisql, published to philterd.ai) and provided to Phileas on the classpath by the
 * {@code phisql} dependency; this test fails the build if the schema declares a filter strategy for
 * which Phileas has no corresponding {@link AbstractFilterStrategy} constant.
 */
public class SchemaConformanceTest {

    @Test
    public void everyStrategyDeclaredBySchemaIsKnownToPhileas() throws Exception {

        final Set<String> phileasConstants = stringConstantsOf(AbstractFilterStrategy.class);
        final Set<String> schemaStrategies = strategyNamesIn(PolicySchema.getSchema());

        Assertions.assertFalse(schemaStrategies.isEmpty(),
                "expected the embedded policy schema to declare filter strategies");

        for (final String strategy : schemaStrategies) {
            Assertions.assertTrue(phileasConstants.contains(strategy),
                    "The policy schema declares strategy '" + strategy + "' but Phileas has no matching "
                            + "constant in AbstractFilterStrategy. The published schema and the runtime have drifted.");
        }
    }

    /** Every public static final String constant value declared on the class. */
    private static Set<String> stringConstantsOf(final Class<?> type) throws IllegalAccessException {
        final Set<String> values = new HashSet<>();
        for (final Field field : type.getDeclaredFields()) {
            final int modifiers = field.getModifiers();
            if (Modifier.isStatic(modifiers) && Modifier.isFinal(modifiers) && field.getType() == String.class) {
                values.add((String) field.get(null));
            }
        }
        return values;
    }

    /** The union of every {@code strategy} property enum anywhere in the schema. */
    private static Set<String> strategyNamesIn(final String schemaJson) {
        final Set<String> names = new HashSet<>();
        collectStrategyEnums(new Gson().fromJson(schemaJson, JsonElement.class), names);
        return names;
    }

    private static void collectStrategyEnums(final JsonElement element, final Set<String> out) {
        if (element != null && element.isJsonObject()) {
            final JsonObject object = element.getAsJsonObject();
            for (final Map.Entry<String, JsonElement> entry : object.entrySet()) {
                final JsonElement value = entry.getValue();
                if ("strategy".equals(entry.getKey()) && value.isJsonObject()
                        && value.getAsJsonObject().has("enum")) {
                    for (final JsonElement enumValue : value.getAsJsonObject().getAsJsonArray("enum")) {
                        out.add(enumValue.getAsString());
                    }
                }
                collectStrategyEnums(value, out);
            }
        } else if (element != null && element.isJsonArray()) {
            for (final JsonElement value : element.getAsJsonArray()) {
                collectStrategyEnums(value, out);
            }
        }
    }

}
