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
package ai.philterd.phileas.filters;

import ai.philterd.phileas.policy.Crypto;
import ai.philterd.phileas.services.strategies.AbstractFilterStrategy;
import ai.philterd.phileas.services.strategies.rules.SsnFilterStrategy;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * Validates that {@link FilterConfiguration} fails fast at configuration time when a CRYPTO_REPLACE
 * strategy is given an unusable encryption key, rather than deferring the failure to per-document
 * encryption. A 256-bit key is 64 hexadecimal characters; AES keys must be 16, 24, or 32 bytes.
 */
public class FilterConfigurationTest extends AbstractFilterTest {

    private static final String VALID_AES_256_KEY = "9EE7A356FDFE43F069500B0086758346E66D8583E0CE1CFCA04E50F67ECCE5D1";

    private FilterConfiguration.FilterConfigurationBuilder cryptoBuilderWithKey(final String key) {
        final SsnFilterStrategy strategy = new SsnFilterStrategy();
        strategy.setStrategy(AbstractFilterStrategy.CRYPTO_REPLACE);
        return new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(strategy))
                .withContextService(contextService)
                .withRandom(random)
                .withWindowSize(windowSize)
                .withCrypto(new Crypto(key, null));
    }

    @Test
    public void validCryptoKeyPasses() {
        Assertions.assertDoesNotThrow(() -> cryptoBuilderWithKey(VALID_AES_256_KEY).build());
    }

    @Test
    public void nonHexCryptoKeyIsRejected() {
        final RuntimeException ex = Assertions.assertThrows(RuntimeException.class,
                () -> cryptoBuilderWithKey("not-hexadecimal!").build());
        Assertions.assertTrue(ex.getMessage().contains("hexadecimal"));
    }

    @Test
    public void wrongLengthCryptoKeyIsRejected() {
        // "ABCD" decodes to two bytes - not a legal AES key length.
        final RuntimeException ex = Assertions.assertThrows(RuntimeException.class,
                () -> cryptoBuilderWithKey("ABCD").build());
        Assertions.assertTrue(ex.getMessage().contains("AES key"));
    }

    @Test
    public void missingCryptoKeyIsRejected() {
        final RuntimeException ex = Assertions.assertThrows(RuntimeException.class,
                () -> cryptoBuilderWithKey(null).build());
        Assertions.assertTrue(ex.getMessage().contains("Missing crypto encryption key"));
    }

}
