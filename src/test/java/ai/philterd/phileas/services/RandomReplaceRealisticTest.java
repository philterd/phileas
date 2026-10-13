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
import ai.philterd.phileas.model.filtering.TextFilterResult;
import ai.philterd.phileas.policy.Policy;
import ai.philterd.phileas.services.context.DefaultContextService;
import ai.philterd.phileas.services.disambiguation.vector.InMemoryVectorService;
import ai.philterd.phileas.services.filters.filtering.PlainTextFilterService;
import ai.philterd.phileas.services.validators.LuhnValidator;
import ai.philterd.phileas.utils.IbanValidator;
import com.google.gson.Gson;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Properties;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * RANDOM_REPLACE with the default REALISTIC method, applied through a policy to each filter type: the
 * replacement must differ from the identified text and have the shape of a value of that type.
 */
public class RandomReplaceRealisticTest {

    private static final int RUNS = 20;

    private static Stream<Arguments> filters() {
        return Stream.of(
                // filter, strategies key, text before, identified text, text after, replacement pattern
                Arguments.of("age", "ageFilterStrategies", "she is ", "47", " years old.", "\\d{2}"),
                Arguments.of("bankRoutingNumber", "bankRoutingNumberFilterStrategies", "routing ", "021000021", " ok.", "\\d{9}"),
                Arguments.of("bitcoinAddress", "bitcoinFilterStrategies", "send to ", "127NVqnjf8gB9BFAW2dnQeM6wqmy1gbGtv", " now.", "[A-Za-z0-9]{32}"),
                Arguments.of("canadaSin", "canadaSinFilterStrategies", "sin ", "046-454-286", ".", "\\d{3}-\\d{3}-\\d{3}"),
                Arguments.of("city", "cityFilterStrategies", "he lived in ", "Boston", " last year.", "[A-Z][A-Za-z .'-]*"),
                Arguments.of("county", "countyFilterStrategies", "in ", "Fairfax County", " today.", "[A-Z][A-Za-z .'-]*"),
                Arguments.of("creditCard", "creditCardFilterStrategies", "card ", "4111111111111111", " ok.", "\\d{4}-\\d{4}-\\d{4}-\\d{4}"),
                Arguments.of("currency", "currencyFilterStrategies", "paid ", "$100.00", " today.", "\\$\\d{3}\\.\\d{2}"),
                Arguments.of("date", "dateFilterStrategies", "born ", "May 22, 1999", " here.", "[A-Z][a-z]+ \\d{1,2}, \\d{4}"),
                Arguments.of("driversLicense", "driversLicenseFilterStrategies", "license ", "123456789", " ok.", "\\d{9}"),
                Arguments.of("ein", "einFilterStrategies", "ein ", "12-3456789", ".", "\\d{2}-\\d{7}"),
                Arguments.of("emailAddress", "emailAddressFilterStrategies", "mail ", "john@example.com", " now.", "[A-Za-z0-9]+@[a-z]+\\.com"),
                Arguments.of("firstName", "firstNameFilterStrategies", "hello ", "John", " there.", "[A-Z][a-z'-]*"),
                Arguments.of("hospital", "hospitalFilterStrategies", "at ", "Mayo Clinic", " today.", "[A-Z][A-Za-z .'&-]*"),
                Arguments.of("ibanCode", "ibanCodeFilterStrategies", "iban ", "GB82WEST12345698765432", " ok.", "GB\\d{2}[A-Z]{4}\\d{14}"),
                Arguments.of("ipAddress", "ipAddressFilterStrategies", "ip ", "192.168.1.1", " ok.", "\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}"),
                Arguments.of("itin", "itinFilterStrategies", "itin ", "912-70-1234", ".", "9\\d{2}-\\d{2}-\\d{4}"),
                Arguments.of("macAddress", "macAddressFilterStrategies", "mac ", "00:1A:2B:3C:4D:5E", " ok.", "[0-9a-f]{2}(:[0-9a-f]{2}){5}"),
                Arguments.of("passportNumber", "passportNumberFilterStrategies", "passport ", "123456789", " ok.", "\\d{9}"),
                Arguments.of("passportNumber", "passportNumberFilterStrategies", "passport ", "03X4567", " ok.", "\\d{2}[A-Z]\\d{4}"),
                Arguments.of("phoneNumber", "phoneNumberFilterStrategies", "call ", "(202) 555-0143", " now.", "\\(\\d{3}\\) \\d{3}-\\d{4}"),
                Arguments.of("phoneNumberExtension", "phoneNumberExtensionFilterStrategies", "call ", "x1234", " now.", "[a-z]\\d{4}"),
                Arguments.of("ssn", "ssnFilterStrategies", "ssn ", "123-45-6789", ".", "\\d{3}-\\d{2}-\\d{4}"),
                Arguments.of("state", "stateFilterStrategies", "lives in ", "Virginia", " now.", "[A-Z][A-Za-z ]*"),
                Arguments.of("stateAbbreviation", "stateAbbreviationFilterStrategies", "the state is ", "VA", ".", "[A-Z]{2}"),
                Arguments.of("streetAddress", "streetAddressFilterStrategies", "at ", "123 Main Street", " today.", "\\d+ [A-Za-z'-]+ [A-Za-z]+"),
                Arguments.of("surname", "surnameFilterStrategies", "hello Mr ", "Smith", " there.", "[A-Z][a-z'-]*"),
                Arguments.of("trackingNumber", "trackingNumberFilterStrategies", "tracking ", "1Z999AA10123456784", " ok.", "\\d[A-Z]\\d{3}[A-Z]{2}\\d{11}"),
                Arguments.of("url", "urlFilterStrategies", "see ", "https://example.com/page", " now.", "https?://[A-Za-z0-9.-]+\\.[a-z]+(/\\S*)?"),
                Arguments.of("vin", "vinFilterStrategies", "vin ", "1HGCM82633A004352", " ok.", "[A-HJ-NPR-Z0-9]{17}"),
                Arguments.of("zipCode", "zipCodeFilterStrategies", "zip ", "90210", " ok.", "\\d{5}"));
    }

    @ParameterizedTest(name = "{0}: {3}")
    @MethodSource("filters")
    public void replacementHasTheShapeOfItsType(final String filter, final String strategiesKey, final String before,
                                                final String identified, final String after, final String shape) {

        final String json = "{ \"identifiers\": { \"" + filter + "\": { \"" + strategiesKey + "\": [ { \"strategy\": \"RANDOM_REPLACE\" } ] } } }";

        for (int run = 0; run < RUNS; run++) {
            final String replacement = replacement(json, before, identified, after);
            Assertions.assertFalse(replacement.equalsIgnoreCase(identified), filter + " left " + identified + " unchanged");
            Assertions.assertTrue(replacement.matches(shape), filter + ": '" + replacement + "' does not match " + shape);
        }

    }

    @Test
    public void ibanReplacementIsAValidIban() throws Exception {

        final String json = "{ \"identifiers\": { \"ibanCode\": { \"ibanCodeFilterStrategies\": [ { \"strategy\": \"RANDOM_REPLACE\" } ] } } }";

        assertEveryReplacement(json, "iban ", "GB82WEST12345698765432", " ok.", IbanValidator::isValid);
        assertEveryReplacement(json, "iban ", "DE89370400440532013000", " ok.", IbanValidator::isValid);

        // With spaces, the spaces are kept and the value without them is valid.
        final String spaced = "{ \"identifiers\": { \"ibanCode\": { \"allowSpaces\": true, \"ibanCodeFilterStrategies\": [ { \"strategy\": \"RANDOM_REPLACE\" } ] } } }";
        assertEveryReplacement(spaced, "iban ", "GB82 WEST 1234 5698 7654 32", " ok.",
                r -> r.matches("GB\\d{2} [A-Z]{4} \\d{4} \\d{4} \\d{4} \\d{2}") && IbanValidator.isValid(r.replace(" ", "")));

    }

    @Test
    public void canadaSinReplacementPassesLuhn() throws Exception {

        final String json = "{ \"identifiers\": { \"canadaSin\": { \"canadaSinFilterStrategies\": [ { \"strategy\": \"RANDOM_REPLACE\" } ] } } }";

        assertEveryReplacement(json, "sin ", "046-454-286", ".", LuhnValidator::isValid);

    }

    @Test
    public void passportReplacementIsDetectedAsAPassport() throws Exception {

        final String replace = "{ \"identifiers\": { \"passportNumber\": { \"passportNumberFilterStrategies\": [ { \"strategy\": \"RANDOM_REPLACE\" } ] } } }";
        final Policy redact = new Gson().fromJson("{ \"identifiers\": { \"passportNumber\": { } } }", Policy.class);

        for (final String passport : new String[]{"123456789", "03X4567", "1712345678"}) {
            for (int run = 0; run < RUNS; run++) {
                final String replacement = replacement(replace, "passport ", passport, " ok.");
                final String redacted = service().filter(redact, "context", "passport " + replacement + " ok.").getFilteredText().trim();
                Assertions.assertEquals("passport {{{REDACTED-passport-number}}} ok.", redacted, passport + " was replaced with " + replacement);
            }
        }

    }

    @Test
    public void namesFollowTheCaseOfTheIdentifiedText() throws Exception {

        final String json = "{ \"identifiers\": { \"firstName\": { \"firstNameFilterStrategies\": [ { \"strategy\": \"RANDOM_REPLACE\" } ] } } }";

        assertEveryReplacement(json, "hello ", "John", " there.", r -> r.matches("[A-Z][a-z'-]*"));
        assertEveryReplacement(json, "hello ", "JOHN", " there.", r -> r.matches("[A-Z'-]+"));

    }

    private static void assertEveryReplacement(final String json, final String before, final String identified,
                                               final String after, final Predicate<String> check) throws Exception {

        for (int run = 0; run < RUNS; run++) {
            final String replacement = replacement(json, before, identified, after);
            Assertions.assertFalse(replacement.equalsIgnoreCase(identified), identified + " was not replaced");
            Assertions.assertTrue(check.test(replacement), identified + " was replaced with " + replacement);
        }

    }

    /** The replacement for the identified text, read out of the filtered text. */
    private static String replacement(final String json, final String before, final String identified, final String after) {

        final Policy policy = new Gson().fromJson(json, Policy.class);

        final TextFilterResult result = Assertions.assertTimeoutPreemptively(Duration.ofSeconds(30),
                () -> service().filter(policy, "context" + System.nanoTime(), before + identified + after));

        final Matcher matcher = Pattern.compile(Pattern.quote(before) + "(.*)" + Pattern.quote(after), Pattern.DOTALL)
                .matcher(result.getFilteredText().trim());

        Assertions.assertTrue(matcher.matches(), "'" + identified + "' was not replaced on its own: " + result.getFilteredText());

        return matcher.group(1);

    }

    private static PlainTextFilterService service() {
        return new PlainTextFilterService(new PhileasConfiguration(new Properties()), new DefaultContextService(), new InMemoryVectorService(), null);
    }

}
