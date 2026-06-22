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
import java.util.Random;

public abstract class AbstractAnonymizationService implements AnonymizationService {

    protected Random random;
    protected List<String> candidates;
    protected AnonymizationMethod anonymizationMethod;

    public AbstractAnonymizationService() {
        this.random = new SecureRandom();
        this.candidates = Collections.emptyList();
        this.anonymizationMethod = AnonymizationMethod.REALISTIC;
    }

    public AbstractAnonymizationService(final Random random) {
        this.random = random;
        this.candidates = Collections.emptyList();
        this.anonymizationMethod = AnonymizationMethod.REALISTIC;
    }

    protected AbstractAnonymizationService(final Random random, final AnonymizationMethod anonymizationMethod) {
        this.random = random;
        this.candidates = Collections.emptyList();
        this.anonymizationMethod = anonymizationMethod;
    }

    protected AbstractAnonymizationService(final Random random, final List<String> candidates) {
        this.random = random;
        this.candidates = candidates;
        this.anonymizationMethod = AnonymizationMethod.FROM_LIST;
    }

    protected int generateInteger(final int min, final int max) {
        return random.nextInt((max - min) + 1) + min;
    }

    protected String generateNumeric(final int length) {

        final RandomStringGenerator randomStringGenerator = new RandomStringGenerator.Builder()
                .withinRange('0', '0')
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
