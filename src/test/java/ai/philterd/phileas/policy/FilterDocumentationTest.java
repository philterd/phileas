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
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.annotations.SerializedName;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Guards the filter pages under {@code docs/docs/filter_policies/filters/} against documenting policy keys
 * and strategies the code does not read. An unknown key in a policy is ignored rather than rejected, so a
 * page that drifts from the code produces policies whose settings silently have no effect.
 */
public class FilterDocumentationTest {

    private static final Path FILTER_PAGES = Path.of("docs", "docs", "filter_policies", "filters");

    private static final Pattern STRATEGIES_KEY = Pattern.compile("\\b(\\w+FilterStrategies)\\b");
    private static final Pattern CODE_BLOCK = Pattern.compile("```[a-z]*\\n(.*?)```", Pattern.DOTALL);
    private static final Pattern TABLE_KEY = Pattern.compile("^\\|\\s*`([^`]+)`\\s*\\|");

    @Test
    public void everyDocumentedStrategiesKeyIsDeclared() throws IOException {

        final Set<String> declared = new HashSet<>();
        for (final Class<?> filter : filterClasses()) {
            for (final Field field : fieldsOf(filter)) {
                final SerializedName name = field.getAnnotation(SerializedName.class);
                if (name != null && name.value().endsWith("FilterStrategies")) {
                    declared.add(name.value());
                }
            }
        }

        final Set<String> undeclared = new TreeSet<>();
        for (final Path page : pages()) {
            final Matcher matcher = STRATEGIES_KEY.matcher(Files.readString(page));
            while (matcher.find()) {
                if (!declared.contains(matcher.group(1))) {
                    undeclared.add(FILTER_PAGES.relativize(page) + ": " + matcher.group(1));
                }
            }
        }

        Assertions.assertTrue(undeclared.isEmpty(), "Filter pages document strategies keys that no policy class "
                + "declares, so strategies written under them are ignored: " + undeclared);

    }

    @Test
    public void everyExamplePolicyIsValid() throws IOException {

        final List<String> invalid = new ArrayList<>();
        int checked = 0;

        for (final Path page : pages()) {
            for (final JsonObject example : examplePolicies(page)) {
                checked++;
                if (!PolicySchema.validate(example.toString())) {
                    invalid.add(FILTER_PAGES.relativize(page) + ": " + example);
                }
            }
        }

        Assertions.assertTrue(checked > 30, "expected to find most filter pages' example policies, found " + checked);
        Assertions.assertTrue(invalid.isEmpty(), "Filter pages have example policies the policy schema rejects: " + invalid);

    }

    @Test
    public void everyDocumentedStrategyIsAccepted() throws Exception {

        final Set<String> unaccepted = new TreeSet<>();
        int checked = 0;

        for (final Path page : pages()) {

            final Set<String> filters = new HashSet<>();
            for (final JsonObject example : examplePolicies(page)) {
                filters.addAll(example.getAsJsonObject("identifiers").keySet());
            }

            // Strategy names are compared case-insensitively.
            final Set<String> accepted = new HashSet<>();
            for (final String filter : filters) {
                for (final String strategy : acceptedStrategies(filter)) {
                    accepted.add(strategy.toUpperCase(Locale.ROOT));
                }
            }

            for (final String strategy : strategyTableEntries(Files.readString(page))) {
                checked++;
                if (!accepted.contains(strategy.toUpperCase(Locale.ROOT))) {
                    unaccepted.add(FILTER_PAGES.relativize(page) + ": " + strategy);
                }
            }

        }

        Assertions.assertTrue(checked > 100, "expected to find most filter pages' strategy tables, found " + checked);
        Assertions.assertTrue(unaccepted.isEmpty(), "Filter pages document strategies their filter does not accept: " + unaccepted);

    }

    @Test
    public void everyDocumentedOptionIsRead() throws Exception {

        final Set<String> unread = new TreeSet<>();
        int checked = 0;

        for (final Path page : pages()) {

            final Set<String> read = new HashSet<>();
            for (final JsonObject example : examplePolicies(page)) {
                for (final String filter : example.getAsJsonObject("identifiers").keySet()) {
                    final Class<?> policyClass = listElementType(fieldNamed(Identifiers.class, filter));
                    read.addAll(serializedNames(policyClass, new HashSet<>()));
                    read.addAll(serializedNames(strategyClass(policyClass), new HashSet<>()));
                }
            }

            for (final String option : tableEntries(Files.readString(page), "Parameter|Option")) {
                checked++;
                if (!read.contains(option)) {
                    unread.add(FILTER_PAGES.relativize(page) + ": " + option);
                }
            }

        }

        Assertions.assertTrue(checked > 200, "expected to find most filter pages' option tables, found " + checked);
        Assertions.assertTrue(unread.isEmpty(), "Filter pages document options their filter does not read: " + unread);

    }

    @Test
    public void everyPageHasAnExamplePolicy() throws IOException {

        // The option and strategy checks find a page's filter through its example policy.
        for (final Path page : pages()) {
            Assertions.assertFalse(examplePolicies(page).isEmpty(), FILTER_PAGES.relativize(page)
                    + " has no example policy, so its options and strategies cannot be checked against its filter.");
        }

    }

    private static List<Path> pages() throws IOException {

        Assertions.assertTrue(Files.isDirectory(FILTER_PAGES), "expected the filter pages at " + FILTER_PAGES.toAbsolutePath());

        try (Stream<Path> paths = Files.walk(FILTER_PAGES)) {
            return paths.filter(p -> p.toString().endsWith(".md")).sorted().toList();
        }

    }

    /** The code blocks on a page that are whole policies, skipping snippets and non-JSON blocks. */
    private static List<JsonObject> examplePolicies(final Path page) throws IOException {

        final List<JsonObject> examples = new ArrayList<>();
        final Matcher matcher = CODE_BLOCK.matcher(Files.readString(page));

        while (matcher.find()) {
            try {
                final JsonElement element = JsonParser.parseString(matcher.group(1));
                if (element.isJsonObject() && element.getAsJsonObject().has("identifiers")) {
                    examples.add(element.getAsJsonObject());
                }
            } catch (final RuntimeException ex) {
                // Not a policy, such as sample text or a fragment of one.
            }
        }

        return examples;

    }

    /** The strategies listed in tables whose first column is headed "Strategy". */
    private static List<String> strategyTableEntries(final String page) {
        return tableEntries(page, "Strategy");
    }

    /** The backticked first-column entries of tables whose first column heading matches. */
    private static List<String> tableEntries(final String page, final String heading) {

        final List<String> entries = new ArrayList<>();
        boolean inTable = false;

        for (final String line : page.split("\\R")) {

            if (!line.startsWith("|")) {
                inTable = false;
            } else if (line.matches("^\\|\\s*(" + heading + ")\\s*\\|.*")) {
                inTable = true;
            } else if (inTable) {
                final Matcher matcher = TABLE_KEY.matcher(line);
                if (matcher.find()) {
                    entries.add(matcher.group(1));
                }
            }

        }

        return entries;

    }

    /** Every serialized name on a policy class, including those of the policy objects it holds. */
    private static Set<String> serializedNames(final Class<?> type, final Set<Class<?>> seen) {

        final Set<String> names = new HashSet<>();

        if (!seen.add(type)) {
            return names;
        }

        for (final Field field : fieldsOf(type)) {
            final SerializedName name = field.getAnnotation(SerializedName.class);
            if (name != null) {
                names.add(name.value());
                names.addAll(List.of(name.alternate()));
                final Class<?> held = listElementType(field);
                if (held.getName().startsWith("ai.philterd.phileas.policy")) {
                    names.addAll(serializedNames(held, seen));
                }
            }
        }

        return names;

    }

    /** The strategies a filter's strategy class accepts, found through its policy class. */
    private static Set<String> acceptedStrategies(final String filter) throws Exception {

        final Class<?> strategyClass = strategyClass(listElementType(fieldNamed(Identifiers.class, filter)));

        return ((AbstractFilterStrategy) strategyClass.getDeclaredConstructor().newInstance()).getAcceptedStrategies();

    }

    /** The strategy class a filter's policy class holds its strategies in. */
    private static Class<?> strategyClass(final Class<?> policyClass) {

        for (final Field field : fieldsOf(policyClass)) {
            final SerializedName name = field.getAnnotation(SerializedName.class);
            if (name != null && name.value().endsWith("FilterStrategies")) {
                return listElementType(field);
            }
        }

        Assertions.fail(policyClass.getSimpleName() + " declares no strategies field");
        return null;

    }

    /** The policy class of every filter {@link Identifiers} declares. */
    private static Set<Class<?>> filterClasses() {

        final Set<Class<?>> classes = new HashSet<>();
        for (final Field field : fieldsOf(Identifiers.class)) {
            if (field.getAnnotation(SerializedName.class) != null) {
                classes.add(listElementType(field));
            }
        }
        return classes;

    }

    private static Field fieldNamed(final Class<?> type, final String serializedName) {

        for (final Field field : fieldsOf(type)) {
            final SerializedName name = field.getAnnotation(SerializedName.class);
            if (name != null && name.value().equals(serializedName)) {
                return field;
            }
        }

        Assertions.fail(type.getSimpleName() + " has no field for '" + serializedName + "'");
        return null;

    }

    private static Class<?> listElementType(final Field field) {

        if (List.class.isAssignableFrom(field.getType()) && field.getGenericType() instanceof ParameterizedType parameterized) {
            return (Class<?>) parameterized.getActualTypeArguments()[0];
        }

        return field.getType();

    }

    private static List<Field> fieldsOf(final Class<?> type) {

        final List<Field> fields = new ArrayList<>();
        for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
            fields.addAll(List.of(c.getDeclaredFields()));
        }
        return fields;

    }

}
