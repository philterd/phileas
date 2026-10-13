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
