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
package ai.philterd.phileas.services;

import ai.philterd.phileas.policy.Identifiers;
import ai.philterd.phileas.policy.Policy;
import ai.philterd.phileas.policy.config.LeftoverCheck;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class LeftoverCheckerTest {

    private static String strategiesKey(final String identifier) {
        final String key = LeftoverChecker.BUILT_IN_FILTERS.get(identifier);
        return key != null ? key : LeftoverChecker.CONFIGURED_FILTERS.get(identifier);
    }

    private static Set<String> serializedNames(final Class<?> type) {
        final Set<String> names = new HashSet<>();
        for (Class<?> c = type; c != null; c = c.getSuperclass()) {
            for (final Field field : c.getDeclaredFields()) {
                final SerializedName name = field.getAnnotation(SerializedName.class);
                if (name != null) {
                    names.add(name.value());
                }
            }
        }
        return names;
    }

    @Test
    public void everyIdentifierIsKnownWithItsStrategiesKey() {
        // A new identifier must be added to the check, or broad would silently not run it.
        for (final Field field : Identifiers.class.getDeclaredFields()) {

            final SerializedName name = field.getAnnotation(SerializedName.class);
            if (name == null || name.value().equals("medicalCondition")) {
                continue;
            }

            final String strategiesKey = strategiesKey(name.value());
            Assertions.assertNotNull(strategiesKey, name.value());

            final Class<?> filterType = List.class.isAssignableFrom(field.getType())
                    ? (Class<?>) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0]
                    : field.getType();
            Assertions.assertTrue(serializedNames(filterType).contains(strategiesKey), name.value() + " " + strategiesKey);

        }
    }

    @Test
    public void builtInsAreTheSingleFiltersExceptModelBackedOnes() {
        for (final Field field : Identifiers.class.getDeclaredFields()) {
            final SerializedName name = field.getAnnotation(SerializedName.class);
            if (name == null || name.value().equals("medicalCondition") || name.value().equals("person")
                    || List.class.isAssignableFrom(field.getType())) {
                continue;
            }
            // The name dictionaries run only as the policy defines them.
            if (name.value().equals("firstName") || name.value().equals("surname")) {
                Assertions.assertTrue(LeftoverChecker.CONFIGURED_FILTERS.containsKey(name.value()), name.value());
                Assertions.assertFalse(LeftoverChecker.BUILT_IN_FILTERS.containsKey(name.value()), name.value());
                continue;
            }
            Assertions.assertTrue(LeftoverChecker.BUILT_IN_FILTERS.containsKey(name.value()), name.value());
        }
    }

    @Test
    public void broadDoesNotAddTheNameDictionariesButKeepsThePolicys() {
        final Policy without = new Gson().fromJson("{\"identifiers\":{\"ssn\":{}}}", Policy.class);
        final JsonObject added = new Gson().toJsonTree(LeftoverChecker.checkPolicy(without, LeftoverCheck.Using.BROAD))
                .getAsJsonObject().getAsJsonObject("identifiers");
        Assertions.assertFalse(added.has("firstName"));
        Assertions.assertFalse(added.has("surname"));

        final Policy with = new Gson().fromJson("{\"identifiers\":{\"firstName\":{},\"surname\":{}}}", Policy.class);
        final Policy check = LeftoverChecker.checkPolicy(with, LeftoverCheck.Using.BROAD);
        Assertions.assertEquals("REDACT", check.getIdentifiers().getFirstName().getFirstNameFilterStrategies().get(0).getStrategy());
        Assertions.assertEquals("REDACT", check.getIdentifiers().getSurname().getSurnameFilterStrategies().get(0).getStrategy());

        // Disabled in the policy, it stays off, unlike a built-in broad adds.
        final Policy disabled = new Gson().fromJson("{\"identifiers\":{\"firstName\":{\"enabled\":false}}}", Policy.class);
        Assertions.assertFalse(LeftoverChecker.checkPolicy(disabled, LeftoverCheck.Using.BROAD).getIdentifiers().getFirstName().isEnabled());
    }

    @Test
    public void checkPolicyKeepsFiltersAsWrittenWithoutTheirIgnoreListsOrConditions() {
        final Policy policy = new Gson().fromJson("{\"identifiers\":{"
                + "\"ssn\":{\"ignored\":[\"a\"],\"ignoredFiles\":[\"b\"],\"ignoredPatterns\":[{\"pattern\":\"c\"}],"
                + "\"ssnFilterStrategies\":[{\"strategy\":\"RANDOM_REPLACE\",\"condition\":\"token == \\\"x\\\"\"}]},"
                + "\"zipCode\":{\"enabled\":false},"
                + "\"identifiers\":[{\"pattern\":\"X[0-9]+\",\"ignored\":[\"X1\"]}]},"
                + "\"ignored\":[{\"terms\":[\"kept\"]}],"
                + "\"config\":{\"review\":{\"leftoverCheck\":{\"mode\":\"always\"}}}}", Policy.class);

        for (final LeftoverCheck.Using using : LeftoverCheck.Using.values()) {

            final Policy check = LeftoverChecker.checkPolicy(policy, using);
            final JsonObject identifiers = new Gson().toJsonTree(check).getAsJsonObject().getAsJsonObject("identifiers");

            final JsonObject ssn = identifiers.getAsJsonObject("ssn");
            Assertions.assertTrue(ssn.getAsJsonArray("ignored").isEmpty());
            Assertions.assertTrue(ssn.getAsJsonArray("ignoredFiles").isEmpty());
            Assertions.assertTrue(ssn.getAsJsonArray("ignoredPatterns").isEmpty());
            Assertions.assertEquals(1, ssn.getAsJsonArray("ssnFilterStrategies").size());
            Assertions.assertEquals("REDACT", check.getIdentifiers().getSsn().getSsnFilterStrategies().get(0).getStrategy());
            Assertions.assertTrue(check.getIdentifiers().getSsn().getSsnFilterStrategies().get(0).getCondition().isEmpty());

            final JsonObject identifier = identifiers.getAsJsonArray("identifiers").get(0).getAsJsonObject();
            Assertions.assertEquals("X[0-9]+", identifier.get("pattern").getAsString());
            Assertions.assertTrue(identifier.getAsJsonArray("ignored").isEmpty());

            // The top-level ignore list is kept, and the check does not carry its own review settings.
            Assertions.assertEquals(1, check.getIgnored().size());
            Assertions.assertEquals(LeftoverCheck.Mode.OFF, check.getConfig().getReview().getLeftoverCheck().getMode());

            if (using == LeftoverCheck.Using.BROAD) {
                Assertions.assertTrue(check.getIdentifiers().getZipCode().isEnabled());
                for (final String builtIn : LeftoverChecker.BUILT_IN_FILTERS.keySet()) {
                    Assertions.assertTrue(identifiers.has(builtIn), builtIn);
                }
            } else {
                Assertions.assertFalse(check.getIdentifiers().getZipCode().isEnabled());
                Assertions.assertFalse(identifiers.has("emailAddress"));
            }

        }
    }

}
