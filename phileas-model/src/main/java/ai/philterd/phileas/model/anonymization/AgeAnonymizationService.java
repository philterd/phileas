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
package ai.philterd.phileas.model.anonymization;

import org.apache.commons.lang3.StringUtils;

import java.util.Map;
import java.util.Random;

public class AgeAnonymizationService extends AbstractAnonymizationService {

    private final Random random;

    public AgeAnonymizationService() {
        this.random = new Random();
    }

    public AgeAnonymizationService(final Map<String, String> context) {
        super(context);
        this.random = new Random();
    }

    @Override
    public String anonymize(final String token) {

        // Replace all digits with other digits.
        int numberOfDigits = 0;

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < token.length(); i++) {

            final char c = token.charAt(i);

            if (Character.isDigit(c)) {

                sb.append(random.nextInt((9 - 0) + 1) + 0);
                numberOfDigits++;

            } else {

                // For everything else just leave it as is.
                sb.append(c);

            }

        }

        final String anonymized = sb.toString();

        // Just ensure that the new one does not equal the original.
        if(numberOfDigits > 0) {
            if (token.equalsIgnoreCase(anonymized)) {
                return anonymize(token);
            }
        }

        return anonymized;

    }

}
