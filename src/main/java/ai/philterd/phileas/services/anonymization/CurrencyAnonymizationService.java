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

import org.apache.commons.collections4.CollectionUtils;

import ai.philterd.phileas.services.context.ContextService;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Random;
import java.util.UUID;

public class CurrencyAnonymizationService extends AbstractAnonymizationService {

    public CurrencyAnonymizationService(final ContextService contextService, final Random random, final AnonymizationMethod anonymizationMethod) {
        super(contextService, random, anonymizationMethod);
    }

    public CurrencyAnonymizationService(final ContextService contextService, final Random random, final List<String> candidates) {
        super(contextService, random, candidates);
    }

    public CurrencyAnonymizationService(final ContextService contextService, final Random random) {
        super(contextService, random);
    }

    public CurrencyAnonymizationService(final ContextService contextService) {
        super(contextService);
    }

    @Override
    public ContextService getContextService() {
        return contextService;
    }

    @Override
    public String anonymize(final String token) {

        if (anonymizationMethod == AnonymizationMethod.CUSTOM_LIST) {

            if (CollectionUtils.isNotEmpty(candidates)) {

                String anonymized = candidates.get(random.nextInt(candidates.size()));
                while (anonymized.equalsIgnoreCase(token)) {
                    anonymized = candidates.get(random.nextInt(candidates.size()));
                }
                return anonymized;

            } else {

                // Provided list was empty - return a random UUID.
                return UUID.randomUUID().toString();

            }

        } else if (anonymizationMethod == AnonymizationMethod.UUID) {

            return java.util.UUID.randomUUID().toString();

        } else {

            // REALISTIC_REPLACE
            String anonymized = getAnonymizedCurrency(token);

            while (anonymized.equalsIgnoreCase(token)) {
                anonymized = getAnonymizedCurrency(token);
            }

            return anonymized;

        }

    }

    private String getAnonymizedCurrency(String token) {

        // Replace all digits with other digits.
        final StringBuilder sb = new StringBuilder();

        for (int i = 0; i < token.length(); i++) {

            final char c = token.charAt(i);

            if (Character.isDigit(c)) {

                sb.append(random.nextInt((9) + 1));

            } else {

                // For everything else just leave it as is.
                sb.append(c);

            }

        }

        final String anonymized;

        if(!sb.toString().startsWith("$")) {

            anonymized = "$" + sb;

        } else {

            anonymized = sb.toString();

        }

        return anonymized;

    }

}
