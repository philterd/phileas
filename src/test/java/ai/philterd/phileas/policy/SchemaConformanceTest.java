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

import ai.philterd.phileas.policy.Identifiers;
import ai.philterd.phisql.Catalog;
import ai.philterd.phileas.PhileasConfiguration;
import ai.philterd.phileas.policy.config.Analysis;
import ai.philterd.phileas.policy.config.Splitting;
import ai.philterd.phileas.policy.filters.AbstractFilter;
import ai.philterd.phileas.services.FilterPolicyLoader;
import ai.philterd.phileas.services.strategies.rules.DateFilterStrategy;
import ai.philterd.phileas.services.strategies.AbstractFilterStrategy;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Properties;
import java.util.List;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Guards against the redaction policy schema (the published contract) drifting away from what the
 * Phileas runtime actually implements. The schema is authored and versioned externally (in
 * philterd/phisql, published to philterd.ai) and provided to Phileas on the classpath by the
 * {@code phisql} dependency; this test fails the build if the schema declares a filter strategy, or
 * an identifier (filter type), for which Phileas has no corresponding runtime support.
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

    @Test
    public void everyIdentifierDeclaredBySchemaIsModeledByPhileas() {

        final Set<String> phileasIdentifiers = serializedNamesOf(Identifiers.class);
        final Set<String> schemaIdentifiers = identifierNamesIn(PolicySchema.getSchema());

        Assertions.assertFalse(schemaIdentifiers.isEmpty(),
                "expected the embedded policy schema to declare identifiers");

        for (final String identifier : schemaIdentifiers) {
            Assertions.assertTrue(phileasIdentifiers.contains(identifier),
                    "The policy schema declares identifier '" + identifier + "' but Phileas has no matching "
                            + "@SerializedName field in Identifiers. The published schema and the runtime have drifted.");
        }
    }

    @Test
    public void everyFilterPropertyDeclaredBySchemaIsModeledByPhileas() {

        final JsonObject schema = schema();

        final Set<String> properties = propertiesOf(schema, "abstractFilterProperties");
        Assertions.assertFalse(properties.isEmpty(), "expected the schema to declare shared filter properties");

        for (final String property : properties) {
            assertModeled("abstractFilterProperties", property, AbstractFilter.class);
        }

    }

    @Test
    public void everyPerFilterPropertyDeclaredBySchemaIsModeledByPhileas() {

        final JsonObject schema = schema();

        for (final String identifier : identifierNamesIn(PolicySchema.getSchema())) {

            final String definition = definitionFor(schema, "identifiers", identifier);
            final Class<?> type = policyTypeOf(Identifiers.class, identifier);

            Assertions.assertNotNull(type, "Identifiers has no field for schema identifier '" + identifier + "'");

            for (final String property : propertiesOf(schema, definition)) {
                assertModeled(definition, property, type);
            }

        }

    }

    @Test
    public void everyStrategyPropertyDeclaredBySchemaIsModeledByPhileas() {

        final JsonObject schema = schema();

        final Set<String> properties = propertiesOf(schema, "baseFilterStrategy");
        Assertions.assertFalse(properties.isEmpty(), "expected the schema to declare strategy properties");

        for (final String property : properties) {
            assertModeled("baseFilterStrategy", property, AbstractFilterStrategy.class);
        }

        for (final String property : propertiesOf(schema, "dateFilterStrategy")) {
            assertModeled("dateFilterStrategy", property, DateFilterStrategy.class);
        }

    }

    @Test
    public void everyConfigPropertyDeclaredBySchemaIsModeledByPhileas() {

        final JsonObject schema = schema();

        final Set<String> properties = propertiesOf(schema, "config");
        Assertions.assertFalse(properties.isEmpty(), "expected the schema to declare config properties");

        for (final String property : properties) {

            assertModeled("config", property, Config.class);

            final String definition = definitionFor(schema, "config", property);
            final Class<?> type = policyTypeOf(Config.class, property);

            if (definition != null && type != null) {
                for (final String child : propertiesOf(schema, definition)) {
                    assertModeled(definition, child, type);
                }
            }

        }

    }

    @Test
    public void everyIdentifierDeclaredBySchemaProducesAFilter() throws Exception {

        final FilterPolicyLoader loader = new FilterPolicyLoader(
                new PhileasConfiguration(new Properties()), new SecureRandom(), null);

        for (final String identifier : identifierNamesIn(PolicySchema.getSchema())) {

            final Policy policy = new Gson().fromJson(minimalPolicyFor(identifier), Policy.class);
            final boolean built = !loader.getFiltersForPolicy(policy, new HashMap<>()).isEmpty();

            if (EXPECTED_GAPS.containsKey(identifier)) {
                Assertions.assertFalse(built, "'" + identifier + "' is listed as an expected gap but now "
                        + "builds a filter. Remove it from EXPECTED_GAPS.");
            } else {
                Assertions.assertTrue(built, "The policy schema declares identifier '" + identifier + "' but "
                        + "FilterPolicyLoader builds no filter for it, so a policy enabling it does nothing.");
            }

        }

    }

    @Test
    public void everyCatalogStrategiesFieldIsReadableByPhileas() {

        final Catalog catalog = Catalog.loadDefault();
        int checked = 0;

        for (final String identifier : identifierNamesIn(PolicySchema.getSchema())) {

            final String entity = catalog.entityNameForField(identifier);

            if (entity == null) {
                continue;
            }

            final Catalog.EntityType entityType = catalog.getEntity(entity);
            final Class<?> type = policyTypeOf(Identifiers.class, identifier);
            final Set<String> modeled = serializedNamesOf(type);

            Assertions.assertTrue(modeled.contains(entityType.phileasStrategiesField()),
                    entity + " compiles to '" + entityType.phileasStrategiesField() + "' but "
                            + type.getSimpleName() + " does not read that name, so its strategies are dropped.");

            for (final String alias : entityType.phileasStrategiesFieldAliases()) {
                Assertions.assertTrue(modeled.contains(alias), entity + " must still read the earlier name '"
                        + alias + "', but " + type.getSimpleName() + " does not.");
            }

            checked++;

        }

        Assertions.assertTrue(checked > 20, "expected the catalog to map most identifiers, mapped " + checked);

    }

    @Test
    public void everyStrategyPhileasAcceptsIsDeclaredBySchema() throws Exception {

        final Set<String> declared = strategyNamesIn(PolicySchema.getSchema());

        Assertions.assertFalse(STRATEGIES.isEmpty(), "expected to find filter strategies on the classpath");

        for (final AbstractFilterStrategy strategy : STRATEGIES) {
            for (final String accepted : strategy.getAcceptedStrategies()) {

                final String name = accepted.toUpperCase(Locale.ROOT);

                if (EXPECTED_GAPS.containsKey(name)) {
                    Assertions.assertFalse(declared.contains(name), "'" + name + "' is listed as an expected gap "
                            + "but the schema now declares it. Remove it from EXPECTED_GAPS.");
                } else {
                    Assertions.assertTrue(declared.contains(name), strategy.getClass().getSimpleName()
                            + " accepts strategy '" + name + "' but the policy schema does not declare it, so a "
                            + "policy using it fails validation.");
                }

            }
        }

    }

    @Test
    public void everyExpectedGapIsStillAGap() {

        final JsonObject schema = schema();

        for (final Map.Entry<String, String> gap : EXPECTED_GAPS.entrySet()) {

            final String[] parts = gap.getKey().split("\\.", 2);

            if (parts.length == 2) {
                final Class<?> type = GAP_OWNERS.get(parts[0]);
                Assertions.assertNotNull(type, "no owner registered for expected gap " + gap.getKey());
                Assertions.assertTrue(propertiesOf(schema, parts[0]).contains(parts[1]),
                        "the schema no longer declares " + gap.getKey() + "; remove it from EXPECTED_GAPS");
                Assertions.assertFalse(serializedNamesOf(type).contains(parts[1]),
                        gap.getKey() + " is modeled now. Remove it from EXPECTED_GAPS.");
            }

        }

    }

    /** Fails unless the schema property has a matching field, allowing for a listed gap. */
    private static void assertModeled(final String definition, final String property, final Class<?> type) {

        final boolean modeled = serializedNamesOf(type).contains(property);
        final String key = definition + "." + property;

        if (EXPECTED_GAPS.containsKey(key)) {
            Assertions.assertFalse(modeled, key + " is modeled now. Remove it from EXPECTED_GAPS.");
        } else {
            Assertions.assertTrue(modeled, "The policy schema declares " + key + " but " + type.getSimpleName()
                    + " has no matching @SerializedName field, so a policy setting it is silently dropped.");
        }

    }

    private static JsonObject schema() {
        return new Gson().fromJson(PolicySchema.getSchema(), JsonObject.class);
    }

    /** A policy that enables just the one filter, with whatever that filter requires. */
    private static String minimalPolicyFor(final String identifier) {

        final String body = switch (identifier) {
            case "identifiers" -> "[ { \"pattern\": \"[0-9]{4}\" } ]";
            case "sections" -> "[ { \"startPattern\": \"START\", \"endPattern\": \"END\" } ]";
            case "dictionaries" -> "[ { \"terms\": [ \"ada\" ] } ]";
            case "pheyes" -> "[ { } ]";
            default -> "{ }";
        };

        return "{ \"identifiers\": { \"" + identifier + "\": " + body + " } }";

    }

    private static final List<AbstractFilterStrategy> STRATEGIES = strategies();

    /** Every concrete filter strategy on the classpath, so the list cannot drift. */
    private static List<AbstractFilterStrategy> strategies() {

        final List<AbstractFilterStrategy> strategies = new ArrayList<>();

        try {

            final Path root = Path.of(AbstractFilterStrategy.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI());

            try (var paths = Files.walk(root.resolve("ai/philterd/phileas/services/strategies"))) {

                for (final Path path : paths.filter(p -> p.getFileName().toString().endsWith("FilterStrategy.class")).toList()) {

                    final String name = root.relativize(path).toString()
                            .replace(File.separatorChar, '.').replace(".class", "");

                    final Class<?> type = Class.forName(name);

                    if (!Modifier.isAbstract(type.getModifiers())) {
                        strategies.add((AbstractFilterStrategy) type.getDeclaredConstructor().newInstance());
                    }

                }

            }

        } catch (final Exception ex) {
            throw new IllegalStateException("Could not load the filter strategies.", ex);
        }

        return strategies;

    }

    /** Where an expected gap's property lives, for the staleness check. */
    private static final Map<String, Class<?>> GAP_OWNERS = Map.of(
            "splitting", Splitting.class,
            "analysis", Analysis.class,
            "baseFilterStrategy", AbstractFilterStrategy.class,
            "dateFilterStrategy", DateFilterStrategy.class);

    /**
     * Conformance gaps that exist today, with why. Each is asserted to still be a gap, so
     * implementing one fails this test until its entry is removed.
     */
    private static final Map<String, String> EXPECTED_GAPS = Map.of(
            "baseFilterStrategy.color", "PDF redaction takes its color from the policy-wide setting",
            "dateFilterStrategy.color", "PDF redaction takes its color from the policy-wide setting",
            "medicalCondition", "no filter is built for it; the entity type is being retired",
            "ZERO_LEADING", "the zip code strategy is not in the schema's strategy enum");

    /** Every {@code @SerializedName} value and alternate on the class and its superclasses. */
    private static Set<String> serializedNamesOf(final Class<?> type) {
        final Set<String> values = new HashSet<>();
        for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
            for (final Field field : c.getDeclaredFields()) {
                final SerializedName annotation = field.getAnnotation(SerializedName.class);
                if (annotation != null) {
                    values.add(annotation.value());
                    values.addAll(Arrays.asList(annotation.alternate()));
                }
            }
        }
        return values;
    }

    /** The Java type a policy field deserializes into, unwrapping a list. */
    private static Class<?> policyTypeOf(final Class<?> owner, final String serializedName) {
        for (Class<?> c = owner; c != null && c != Object.class; c = c.getSuperclass()) {
            for (final Field field : c.getDeclaredFields()) {
                final SerializedName annotation = field.getAnnotation(SerializedName.class);
                if (annotation != null && annotation.value().equals(serializedName)) {
                    if (List.class.isAssignableFrom(field.getType())) {
                        final Type generic = field.getGenericType();
                        if (generic instanceof ParameterizedType parameterized) {
                            return (Class<?>) parameterized.getActualTypeArguments()[0];
                        }
                    }
                    return field.getType();
                }
            }
        }
        return null;
    }

    /** The property names declared by a schema definition, following a $ref or array items. */
    private static Set<String> propertiesOf(final JsonObject schema, final String definition) {
        final JsonObject resolved = resolve(schema, schema.getAsJsonObject("$defs").get(definition));
        return resolved.has("properties") ? resolved.getAsJsonObject("properties").keySet() : Set.of();
    }

    private static JsonObject resolve(final JsonObject schema, final JsonElement element) {
        JsonObject object = element.getAsJsonObject();
        if (object.has("items")) {
            object = object.getAsJsonObject("items");
        }
        if (object.has("$ref")) {
            final String name = object.get("$ref").getAsString().replace("#/$defs/", "");
            return resolve(schema, schema.getAsJsonObject("$defs").get(name));
        }
        return object;
    }

    /** The definition name an identifier or config property points at. */
    private static String definitionFor(final JsonObject schema, final String owner, final String property) {
        JsonObject object = schema.getAsJsonObject("$defs").getAsJsonObject(owner)
                .getAsJsonObject("properties").getAsJsonObject(property);
        if (object.has("items")) {
            object = object.getAsJsonObject("items");
        }
        return object.has("$ref") ? object.get("$ref").getAsString().replace("#/$defs/", "") : null;
    }

    /** The names of every identifier (filter) declared under {@code $defs.identifiers.properties}. */
    private static Set<String> identifierNamesIn(final String schemaJson) {
        final JsonObject schema = new Gson().fromJson(schemaJson, JsonObject.class);
        return schema.getAsJsonObject("$defs")
                .getAsJsonObject("identifiers")
                .getAsJsonObject("properties")
                .keySet();
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
