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


import ai.philterd.phileas.data.generators.BitcoinAddressGenerator;
import java.security.SecureRandom;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class BitcoinAddressAnonymizationService extends AbstractAnonymizationService {

    public BitcoinAddressAnonymizationService(final SecureRandom random, final AnonymizationMethod anonymizationMethod) {
        super(random, anonymizationMethod);
    }

    public BitcoinAddressAnonymizationService(final SecureRandom random, final List<String> candidates) {
        super(random, candidates);
    }

    public BitcoinAddressAnonymizationService(final SecureRandom random) {
        super(random);
    }

    public BitcoinAddressAnonymizationService() {
        super();
    }

    @Override
    public String anonymize(final String token) {

        if (anonymizationMethod == AnonymizationMethod.FROM_LIST) {

            return anonymizeFromList(token);

        } else if (anonymizationMethod == AnonymizationMethod.UUID) {

            return java.util.UUID.randomUUID().toString();

        } else {

            // REALISTIC_REPLACE
            return anonymizeRealistic(token, () -> getAnonymizedBitcoinAddress(token));

        }

    }

    /**
     * A random address of the same form as the token, with a valid checksum: a legacy address beginning
     * with the same 1 or 3, or a segregated witness address of the same witness version and program
     * length. An upper-case segregated witness address stays upper case.
     */
    private String getAnonymizedBitcoinAddress(final String token) {

        final BitcoinAddressGenerator generator = new BitcoinAddressGenerator(random);
        final String lower = token.toLowerCase(Locale.ROOT);

        if (lower.startsWith("bc1")) {

            // A version 0 address is 42 characters for a 20-byte program and 62 for a 32-byte one.
            final String address = lower.startsWith("bc1p")
                    ? generator.segwit(1, 32)
                    : generator.segwit(0, lower.length() > 42 ? 32 : 20);

            return token.equals(token.toUpperCase(Locale.ROOT)) ? address.toUpperCase(Locale.ROOT) : address;

        } else if (token.startsWith("3")) {
            return generator.legacy(BitcoinAddressGenerator.P2SH_VERSION);
        } else {
            return generator.legacy(BitcoinAddressGenerator.P2PKH_VERSION);
        }

    }

}
