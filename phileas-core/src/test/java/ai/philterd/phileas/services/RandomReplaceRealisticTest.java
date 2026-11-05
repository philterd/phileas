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

import java.math.BigInteger;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
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
                Arguments.of("bitcoinAddress", "bitcoinFilterStrategies", "send to ", "127NVqnjf8gB9BFAW2dnQeM6wqmy1gbGtv", " now.", "1[a-km-zA-HJ-NP-Z1-9]{25,33}"),
                Arguments.of("canadaSin", "canadaSinFilterStrategies", "sin ", "046-454-286", ".", "\\d{3}-\\d{3}-\\d{3}"),
                Arguments.of("city", "cityFilterStrategies", "he lived in ", "Boston", " last year.", "[A-Z][A-Za-z .'/-]*"),
                Arguments.of("county", "countyFilterStrategies", "in ", "Fairfax County", " today.", "[A-Z][A-Za-z .'-]*"),
                Arguments.of("creditCard", "creditCardFilterStrategies", "card ", "4111111111111111", " ok.", "4\\d{15}"),
                Arguments.of("currency", "currencyFilterStrategies", "paid ", "$100.00", " today.", "\\$\\d{3}\\.\\d{2}"),
                Arguments.of("date", "dateFilterStrategies", "born ", "May 22, 1999", " here.", "[A-Z][a-z]+ \\d{1,2}, \\d{4}"),
                Arguments.of("driversLicense", "driversLicenseFilterStrategies", "license ", "123456789", " ok.", "\\d{9}"),
                Arguments.of("ein", "einFilterStrategies", "ein ", "12-3456789", ".", "\\d{2}-\\d{7}"),
                Arguments.of("emailAddress", "emailAddressFilterStrategies", "mail ", "john@example.com", " now.", "[A-Za-z0-9]+@[a-z]+\\.com"),
                Arguments.of("firstName", "firstNameFilterStrategies", "hello ", "John", " there.", "[A-Z][a-z'-]*"),
                Arguments.of("hospital", "hospitalFilterStrategies", "at ", "Mayo Clinic", " today.", "[A-Z][A-Za-z .,'&/-]*"),
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
    public void creditCardReplacementIsAValidCardOfTheSameBrandAndLayout() throws Exception {

        // Card networks' published test numbers, one per brand the credit card filter knows.
        final String[] cards = {"4111111111111111", "4111 1111 1111 1111", "4111-1111-1111-1111", "4222222222222",
                "5555555555554444", "2223000048400011", "378282246310005", "30569309025904", "6011111111111117",
                "3530111333300000"};

        final String replace = "{ \"identifiers\": { \"creditCard\": { \"creditCardFilterStrategies\": [ { \"strategy\": \"RANDOM_REPLACE\" } ] } } }";
        final Policy onlyValid = new Gson().fromJson("{ \"identifiers\": { \"creditCard\": { \"onlyValidCreditCardNumbers\": true } } }", Policy.class);

        for (final String card : cards) {

            // The filter detects the original with onlyValidCreditCardNumbers, so a valid replacement is detected too.
            Assertions.assertEquals("card {{{REDACTED-credit-card}}} ok.",
                    service().filter(onlyValid, "context", "card " + card + " ok.").getFilteredText().trim(), card);

            for (int run = 0; run < RUNS; run++) {

                final String replacement = replacement(replace, "card ", card, " ok.");

                Assertions.assertNotEquals(card, replacement);
                Assertions.assertEquals(card.replaceAll("[0-9]", "0"), replacement.replaceAll("[0-9]", "0"), card + " -> " + replacement);
                Assertions.assertTrue(LuhnValidator.isValid(replacement), card + " -> " + replacement);
                Assertions.assertEquals(brand(card), brand(replacement), card + " -> " + replacement);
                Assertions.assertEquals("card {{{REDACTED-credit-card}}} ok.",
                        service().filter(onlyValid, "context", "card " + replacement + " ok.").getFilteredText().trim(), card + " -> " + replacement);

            }

        }

    }

    @Test
    public void bitcoinReplacementIsAValidAddressOfTheSameForm() {

        final String replace = "{ \"identifiers\": { \"bitcoinAddress\": { \"bitcoinFilterStrategies\": [ { \"strategy\": \"RANDOM_REPLACE\" } ] } } }";

        // BIP 13, BIP 173, and BIP 350 example addresses.
        final String[][] addresses = {
                {"127NVqnjf8gB9BFAW2dnQeM6wqmy1gbGtv", "1[a-km-zA-HJ-NP-Z1-9]{25,33}"},
                {"3J98t1WpEZ73CNmQviecrnyiWrnqRhWNLy", "3[a-km-zA-HJ-NP-Z1-9]{25,33}"},
                {"bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4", "bc1q[ac-hj-np-z02-9]{38}"},
                {"BC1QW508D6QEJXTDG4Y5R3ZARVARY0C5XW7KV8F3T4", "BC1Q[AC-HJ-NP-Z02-9]{38}"},
                {"bc1qrp33g0q5c5txsp9arysrx4k6zdkfs4nce4xj0gdcccefvpysxf3qccfmv3", "bc1q[ac-hj-np-z02-9]{58}"},
                {"bc1p0xlxvlhemja6c4dqv22uapctqupfhlxm9h8z3k2e72q4k9hcz7vqzk5jj0", "bc1p[ac-hj-np-z02-9]{58}"}};

        for (final String[] address : addresses) {

            Assertions.assertTrue(validBitcoinAddress(address[0]), "the example " + address[0] + " should be valid");

            for (int run = 0; run < RUNS; run++) {
                final String replacement = replacement(replace, "send to ", address[0], " now.");
                Assertions.assertNotEquals(address[0], replacement);
                Assertions.assertTrue(replacement.matches(address[1]), address[0] + " -> " + replacement);
                Assertions.assertTrue(validBitcoinAddress(replacement), address[0] + " -> " + replacement);
            }

        }

    }

    @Test
    public void vinReplacementHasAValidCheckDigit() {

        final String replace = "{ \"identifiers\": { \"vin\": { \"vinFilterStrategies\": [ { \"strategy\": \"RANDOM_REPLACE\" } ] } } }";

        Assertions.assertTrue(validVin("1HGCM82633A004352"), "the example VIN should be valid");

        for (int run = 0; run < RUNS; run++) {
            final String replacement = replacement(replace, "vin ", "1HGCM82633A004352", " ok.");
            Assertions.assertTrue(replacement.matches("[A-HJ-NPR-Z0-9]{17}"), replacement);
            Assertions.assertTrue(validVin(replacement), replacement);
        }

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

    /** The card brand, by the patterns the credit card filter's onlyValidCreditCardNumbers option checks. */
    private static String brand(final String card) {
        final String digits = card.replaceAll("[^0-9]", "");
        if (digits.matches("4[0-9]{12}(?:[0-9]{3})?")) return "visa";
        if (digits.matches("(?:5[1-5][0-9]{2}|222[1-9]|22[3-9][0-9]|2[3-6][0-9]{2}|27[01][0-9]|2720)[0-9]{12}")) return "mastercard";
        if (digits.matches("3[47][0-9]{13}")) return "amex";
        if (digits.matches("3(?:0[0-5]|[68][0-9])[0-9]{11}")) return "diners";
        if (digits.matches("6(?:011|5[0-9]{2})[0-9]{12}")) return "discover";
        if (digits.matches("(?:2131|1800|35[0-9]{3})[0-9]{11}")) return "jcb";
        return "none";
    }

    /** Checks a Base58Check, bech32, or bech32m address's checksum, independently of the generator. */
    private static boolean validBitcoinAddress(final String address) {

        if (address.toLowerCase(Locale.ROOT).startsWith("bc1")) {

            final String lower = address.toLowerCase(Locale.ROOT);
            final String charset = "qpzry9x8gf2tvdw0s3jn54khce6mua7l";
            final int[] generator = {0x3b6a57b2, 0x26508e6d, 0x1ea119fa, 0x3d4233dd, 0x2a1462b3};

            final List<Integer> values = new ArrayList<>(List.of(3, 3, 0, 2, 3));
            for (final char c : lower.substring(3).toCharArray()) {
                values.add(charset.indexOf(c));
            }

            int checksum = 1;
            for (final int value : values) {
                final int top = checksum >>> 25;
                checksum = ((checksum & 0x1ffffff) << 5) ^ value;
                for (int i = 0; i < 5; i++) {
                    if (((top >> i) & 1) == 1) {
                        checksum ^= generator[i];
                    }
                }
            }

            // Witness version 0 uses bech32; later versions use bech32m.
            return checksum == (lower.charAt(3) == 'q' ? 1 : 0x2bc830a3);

        }

        final String base58 = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz";
        BigInteger value = BigInteger.ZERO;
        for (final char c : address.toCharArray()) {
            value = value.multiply(BigInteger.valueOf(58)).add(BigInteger.valueOf(base58.indexOf(c)));
        }

        // A version byte, a 20-byte hash, and a 4-byte checksum.
        final byte[] magnitude = value.toByteArray();
        final byte[] decoded = new byte[25];
        final int length = Math.min(magnitude.length, 25);
        System.arraycopy(magnitude, magnitude.length - length, decoded, 25 - length, length);

        try {
            final MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            final byte[] hash = sha256.digest(sha256.digest(Arrays.copyOf(decoded, 21)));
            return Arrays.equals(Arrays.copyOf(hash, 4), Arrays.copyOfRange(decoded, 21, 25));
        } catch (final Exception ex) {
            throw new IllegalStateException(ex);
        }

    }

    /** Checks a VIN's check digit (position 9), independently of the generator. */
    private static boolean validVin(final String vin) {

        final String letters = "ABCDEFGHJKLMNPRSTUVWXYZ";
        final int[] letterValues = {1, 2, 3, 4, 5, 6, 7, 8, 1, 2, 3, 4, 5, 7, 9, 2, 3, 4, 5, 6, 7, 8, 9};
        final int[] weights = {8, 7, 6, 5, 4, 3, 2, 10, 0, 9, 8, 7, 6, 5, 4, 3, 2};

        int sum = 0;
        for (int i = 0; i < 17; i++) {
            final char c = vin.charAt(i);
            final int value = Character.isDigit(c) ? c - '0' : letterValues[letters.indexOf(c)];
            sum += value * weights[i];
        }

        final int check = sum % 11;
        return vin.charAt(8) == (check == 10 ? 'X' : (char) ('0' + check));

    }

    private static PlainTextFilterService service() {
        return new PlainTextFilterService(new PhileasConfiguration(new Properties()), new DefaultContextService(), new InMemoryVectorService(), null);
    }

}
