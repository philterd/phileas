/*
 * Copyright 2026 Philterd, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ai.philterd.phileas.data.generators;

import org.junit.jupiter.api.Test;

import java.security.SecureRandom;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BitcoinAddressGeneratorTest {

    @Test
    public void testGenerateBitcoinAddress() {
        final BitcoinAddressGenerator generator = new BitcoinAddressGenerator(new SecureRandom());
        final String bitcoin = generator.random();
        assertNotNull(bitcoin);
        assertTrue(bitcoin.startsWith("1"));
        assertTrue(bitcoin.length() >= 26 && bitcoin.length() <= 35);
    }

    @Test
    public void base58CheckMatchesPublishedAddresses() {
        // The address of the all-zero hash, and the worked example from the Bitcoin wiki's
        // "Technical background of version 1 Bitcoin addresses".
        assertEquals("1111111111111111111114oLvT2", BitcoinAddressGenerator.base58Check((byte) 0x00, new byte[20]));
        assertEquals("16UwLL9Risc3QfPqBUvKofHmBQ7wMtjvM",
                BitcoinAddressGenerator.base58Check((byte) 0x00, HexFormat.of().parseHex("010966776006953d5567439e5e39f86a0d273bee")));
    }

    @Test
    public void segwitMatchesPublishedAddresses() {
        // BIP 173 (bech32, witness version 0) and BIP 350 (bech32m, witness version 1) test vectors.
        assertEquals("bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4",
                BitcoinAddressGenerator.segwitAddress("bc", 0, HexFormat.of().parseHex("751e76e8199196d454941c45d1b3a323f1433bd6")));
        assertEquals("bc1p0xlxvlhemja6c4dqv22uapctqupfhlxm9h8z3k2e72q4k9hcz7vqzk5jj0",
                BitcoinAddressGenerator.segwitAddress("bc", 1, HexFormat.of().parseHex("79be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798")));
    }

    @Test
    public void generatedAddressesHaveTheirForm() {
        final BitcoinAddressGenerator generator = new BitcoinAddressGenerator(new SecureRandom());
        for (int i = 0; i < 100; i++) {
            assertTrue(generator.legacy(BitcoinAddressGenerator.P2SH_VERSION).matches("3[a-km-zA-HJ-NP-Z1-9]{25,34}"));
            assertTrue(generator.segwit(0, 20).matches("bc1q[ac-hj-np-z02-9]{38}"));
            assertTrue(generator.segwit(0, 32).matches("bc1q[ac-hj-np-z02-9]{58}"));
            assertTrue(generator.segwit(1, 32).matches("bc1p[ac-hj-np-z02-9]{58}"));
        }
    }

    @Test
    public void testPoolSize() {
        final BitcoinAddressGenerator generator = new BitcoinAddressGenerator(new SecureRandom());
        assertEquals(Long.MAX_VALUE, generator.poolSize());
    }

}
