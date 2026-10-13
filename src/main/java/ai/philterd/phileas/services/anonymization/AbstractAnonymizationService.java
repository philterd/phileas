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
package ai.philterd.phileas.services.anonymization;

import org.apache.commons.text.RandomStringGenerator;

import java.security.SecureRandom;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Supplier;

/** Base for anonymization services, providing the shared random and candidate-list handling. */
public abstract class AbstractAnonymizationService implements AnonymizationService {

    protected SecureRandom random;
    protected List<String> candidates;
    protected AnonymizationMethod anonymizationMethod;

    public AbstractAnonymizationService() {
        this.random = new SecureRandom();
        this.candidates = Collections.emptyList();
        this.anonymizationMethod = AnonymizationMethod.REALISTIC;
    }

    public AbstractAnonymizationService(final SecureRandom random) {
        this.random = random;
        this.candidates = Collections.emptyList();
        this.anonymizationMethod = AnonymizationMethod.REALISTIC;
    }

    protected AbstractAnonymizationService(final SecureRandom random, final AnonymizationMethod anonymizationMethod) {
        this.random = random;
        this.candidates = Collections.emptyList();
        this.anonymizationMethod = anonymizationMethod;
    }

    protected AbstractAnonymizationService(final SecureRandom random, final List<String> candidates) {
        this.random = random;
        this.candidates = candidates;
        this.anonymizationMethod = AnonymizationMethod.FROM_LIST;
    }

    public AnonymizationMethod getAnonymizationMethod() {
        return anonymizationMethod;
    }

    public void setAnonymizationMethod(final AnonymizationMethod anonymizationMethod) {
        this.anonymizationMethod = anonymizationMethod;
    }

    /**
     * How many realistic values are generated, looking for one that differs from the token, before
     * falling back to a UUID.
     */
    protected static final int MAX_REALISTIC_ATTEMPTS = 100;

    /**
     * A realistic value that differs from the token, ignoring case. A generator can only produce the
     * token itself for some inputs (an age written in words has no digits to replace, for example), so
     * after {@link #MAX_REALISTIC_ATTEMPTS} attempts a random UUID is returned instead, which never
     * reveals the token.
     * @param token The token being anonymized.
     * @param generator Generates a candidate realistic value.
     * @return A value that differs from the token.
     */
    protected String anonymizeRealistic(final String token, final Supplier<String> generator) {

        for (int attempt = 0; attempt < MAX_REALISTIC_ATTEMPTS; attempt++) {

            final String anonymized = generator.get();

            if (anonymized != null && !anonymized.equalsIgnoreCase(token)) {
                return anonymized;
            }

        }

        return UUID.randomUUID().toString();

    }

    /**
     * A random value with the token's shape: each digit is replaced with a random digit, each letter
     * with a random letter of the same case (a letter with no case becomes a lower-case letter), and
     * every other character, such as a hyphen, space, or parenthesis, is kept.
     * @param token The token.
     * @return A random value with the token's shape.
     */
    protected String replaceLikeForLike(final String token) {

        final StringBuilder sb = new StringBuilder(token.length());

        // By code point, so a letter outside the Basic Multilingual Plane is replaced rather than kept.
        token.codePoints().forEach(c -> {
            if (Character.isDigit(c)) {
                sb.append((char) ('0' + random.nextInt(10)));
            } else if (Character.isUpperCase(c)) {
                sb.append((char) ('A' + random.nextInt(26)));
            } else if (Character.isLetter(c)) {
                sb.append((char) ('a' + random.nextInt(26)));
            } else {
                sb.appendCodePoint(c);
            }
        });

        return sb.toString();

    }

    /**
     * The value written in the token's case: upper case when the token's letters are all upper case,
     * capitalized when the token begins with an upper-case letter, and lower case otherwise.
     * @param token The token whose case to follow.
     * @param value The value.
     * @return The value in the token's case.
     */
    protected static String matchCase(final String token, final String value) {

        if (value.isEmpty() || token.isEmpty()) {
            return value;
        }

        final boolean hasLetter = token.chars().anyMatch(Character::isLetter);

        if (hasLetter && token.equals(token.toUpperCase(Locale.ROOT)) && token.length() > 1) {
            return value.toUpperCase(Locale.ROOT);
        } else if (Character.isUpperCase(token.charAt(0))) {
            return Character.toUpperCase(value.charAt(0)) + value.substring(1).toLowerCase(Locale.ROOT);
        } else {
            return value.toLowerCase(Locale.ROOT);
        }

    }

    protected int generateInteger(final int min, final int max) {
        return random.nextInt((max - min) + 1) + min;
    }

    protected String generateNumeric(final int length) {

        final RandomStringGenerator randomStringGenerator = new RandomStringGenerator.Builder()
                .withinRange('0', '9')
                .filteredBy(Character::isDigit)
                .usingRandom(random::nextInt)
                .get();

        return randomStringGenerator.generate(length);

    }

    protected String generateAlphanumeric(final int length) {

        final RandomStringGenerator randomStringGenerator = new RandomStringGenerator.Builder()
                .withinRange('0', 'z')
                .filteredBy(Character::isLetterOrDigit)
                .usingRandom(random::nextInt)
                .get();

        return randomStringGenerator.generate(length);

    }

}
