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
package ai.philterd.phileas.services;

import ai.philterd.phileas.PhileasConfiguration;
import ai.philterd.phileas.filters.Filter;
import ai.philterd.phileas.policy.FPE;
import ai.philterd.phileas.policy.Identifiers;
import ai.philterd.phileas.policy.Policy;
import ai.philterd.phileas.policy.filters.AbstractFilter;
import ai.philterd.phileas.services.context.DefaultContextService;
import ai.philterd.phileas.services.disambiguation.vector.InMemoryVectorService;
import ai.philterd.phileas.services.filters.filtering.PlainTextFilterService;
import ai.philterd.phileas.utils.Encryption;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

/**
 * Every filter is given the policy's FPE key and tweak, so a policy using FPE_ENCRYPT_REPLACE loads for each.
 * The filters are read from {@link Identifiers}, so a filter added later without the FPE wiring fails here.
 */
public class FpeWiringTest {

    private static final String FPE = "\"fpe\": { \"key\": \"EF4359D8D580AA4F7F036D6F04FC6A94\", \"tweak\": \"D8E7920AFA330A73\" }";

    private static final String STRATEGY = "[ { \"strategy\": \"FPE_ENCRYPT_REPLACE\" } ]";

    /** Each filter in the policy model: its policy key, whether it is a list, and its strategies key. */
    static List<Arguments> filters() {

        final List<Arguments> filters = new ArrayList<>();

        for (final Field field : Identifiers.class.getDeclaredFields()) {

            final SerializedName name = field.getAnnotation(SerializedName.class);
            if (name == null) {
                continue;
            }

            final boolean list = List.class.isAssignableFrom(field.getType());
            final Class<?> type = list
                    ? (Class<?>) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0]
                    : field.getType();

            // Only filters; a field that is not one, such as a retired filter kept to refuse it, is skipped.
            if (AbstractFilter.class.isAssignableFrom(type)) {
                filters.add(Arguments.of(name.value(), list, strategiesKey(type)));
            }

        }

        return filters;

    }

    private static String strategiesKey(final Class<?> type) {

        for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
            for (final Field field : c.getDeclaredFields()) {
                final SerializedName name = field.getAnnotation(SerializedName.class);
                if (name != null && name.value().endsWith("FilterStrategies")) {
                    return name.value();
                }
            }
        }

        throw new IllegalStateException(type.getSimpleName() + " has no strategies field.");

    }

    /** A policy that enables just one filter, with what that filter requires and an FPE_ENCRYPT_REPLACE strategy. */
    private static Policy policy(final String key, final boolean list, final String strategiesKey, final boolean withFpe) {

        final String required = switch (key) {
            case "identifiers" -> "\"pattern\": \"[0-9]{4}\", ";
            case "sections" -> "\"startPattern\": \"START\", \"endPattern\": \"END\", ";
            case "dictionaries" -> "\"terms\": [ \"ada\" ], ";
            default -> "";
        };

        final String body = "{ " + required + "\"" + strategiesKey + "\": " + STRATEGY + " }";
        final String value = list ? "[ " + body + " ]" : body;

        return new Gson().fromJson("{ " + (withFpe ? FPE + ", " : "") + "\"identifiers\": { \"" + key + "\": "
                + value + " } }", Policy.class);

    }

    private static List<Filter> load(final Policy policy) throws Exception {
        return new FilterPolicyLoader(new PhileasConfiguration(new Properties()), new SecureRandom(), null)
                .getFiltersForPolicy(policy, new HashMap<>());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("filters")
    public void fpeEncryptReplaceLoadsWithTheFpeSection(final String key, final boolean list, final String strategiesKey) throws Exception {
        Assertions.assertFalse(load(policy(key, list, strategiesKey, true)).isEmpty(), key);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("filters")
    public void fpeEncryptReplaceWithoutTheFpeSectionIsRefused(final String key, final boolean list, final String strategiesKey) {

        final Policy policy = policy(key, list, strategiesKey, false);
        final Exception ex = Assertions.assertThrows(Exception.class, () -> load(policy), key);
        Assertions.assertTrue(String.valueOf(ex.getMessage()).contains("Missing FPE encryption property"),
                key + ": " + ex.getMessage());

    }


    private static final FPE FPE_KEY = new FPE("EF4359D8D580AA4F7F036D6F04FC6A94", "D8E7920AFA330A73");

    /**
     * The filters that were not given the FPE key, now format-preserving encrypting a value through a policy.
     * Values outside FF3's 6 to 56 character range, and the date filter, which does not accept the strategy,
     * are redacted instead, as documented.
     */
    @ParameterizedTest(name = "{0}: {3}")
    @CsvSource(delimiter = '|', value = {
            "age                  | {\"ageFilterStrategies\": STRATEGY}                                    | The patient is 47 years old.         | 47 years old          | encrypted",
            "currency             | {\"currencyFilterStrategies\": STRATEGY}                               | The bill was $1,234.56 today.        | $1,234.56             | encrypted",
            "phoneNumber          | {\"phoneNumberFilterStrategies\": STRATEGY}                            | Call 412-555-0187 now.               | 412-555-0187          | encrypted",
            "phoneNumberExtension | {\"phoneNumberExtensionFilterStrategies\": STRATEGY}                   | Call 412-555-0187 x123456 now.       | x123456               | encrypted",
            "streetAddress        | {\"streetAddressFilterStrategies\": STRATEGY}                          | He lives at 123 Main Street today.   | 123 Main Street       | encrypted",
            "hospital             | {\"terms\": [\"Mercy Hospital\"], \"hospitalFilterStrategies\": STRATEGY} | Admitted to Mercy Hospital today.    | Mercy Hospital        | encrypted",
            "sections             | [{\"startPattern\": \"START\", \"endPattern\": \"END\", \"sectionFilterStrategies\": STRATEGY}] | before START secret 12345 END after | START secret 12345 END | encrypted",
            "dictionaries         | [{\"terms\": [\"alphabeta\"], \"customFilterStrategies\": STRATEGY}]     | the word alphabeta here              | alphabeta             | encrypted",
            "phoneNumberExtension | {\"phoneNumberExtensionFilterStrategies\": STRATEGY}                   | Call 412-555-0187 x100 now.          | x100                  | {{{REDACTED-phone-number-extension}}}",
            "stateAbbreviation    | {\"stateAbbreviationFilterStrategies\": STRATEGY}                      | She moved to Pittsburgh, PA.         | PA                    | {{{REDACTED-state-abbreviation}}}",
            "date                 | {\"dateFilterStrategies\": STRATEGY}                                   | Seen on 01/05/2020 at noon.          | 01/05/2020            | {{{REDACTED-date}}}"
    })
    public void fpeEncryptReplaceIsAppliedThroughThePolicy(final String key, final String body, final String text,
                                                         final String value, final String expected) throws Exception {

        final Policy policy = new Gson().fromJson("{ " + FPE + ", \"identifiers\": { \"" + key + "\": "
                + body.replace("STRATEGY", STRATEGY) + " } }", Policy.class);

        final PlainTextFilterService service = new PlainTextFilterService(new PhileasConfiguration(new Properties()),
                new DefaultContextService(), new InMemoryVectorService(), null);

        final String replacement = "encrypted".equals(expected)
                ? Encryption.formatPreservingEncrypt(FPE_KEY, value)
                : expected;

        Assertions.assertEquals(text.replace(value, replacement), service.filter(policy, "context", text).getFilteredText());

    }

}
