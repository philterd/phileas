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
package ai.philterd.phileas.utils;

import ai.philterd.phileas.policy.Crypto;
import ai.philterd.phileas.policy.FPE;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class EncryptionTest {

    private static final Logger LOGGER = LogManager.getLogger(EncryptionTest.class);

    private static final String KEY = "9EE7A356FDFE43F069500B0086758346E66D8583E0CE1CFCA04E50F67ECCE5D1";
    private static final String IV = "B674D3B8F1C025AEFF8F6D5FA1AEAD3A";

    @Test
    public void formatPreservingEncryption1() {

        // Test from https://csrc.nist.gov/csrc/media/projects/cryptographic-standards-and-guidelines/documents/examples/ff3samples.pdf
        // Also done in the library: https://github.com/mysto/java-fpe/blob/main/src/test/java/com/privacylogistics/FF3CipherTest.java
        
        final String plainText = "890121234567890000";
        final FPE fpe = new FPE("EF4359D8D580AA4F7F036D6F04FC6A94", "D8E7920AFA330A73");
        final String encrypted = Encryption.formatPreservingEncrypt(fpe, plainText);

        Assertions.assertEquals("750918814058654607", encrypted);

    }

    @Test
    public void formatPreservingEncryption2() {

        // Test from https://csrc.nist.gov/csrc/media/projects/cryptographic-standards-and-guidelines/documents/examples/ff3samples.pdf
        // Also done in the library: https://github.com/mysto/java-fpe/blob/main/src/test/java/com/privacylogistics/FF3CipherTest.java

        final String plainText = "890121234567890000";
        final FPE fpe = new FPE("EF4359D8D580AA4F7F036D6F04FC6A94", "9A768A92F60E12D8");
        final String encrypted = Encryption.formatPreservingEncrypt(fpe, plainText);

        Assertions.assertEquals("018989839189395384", encrypted);

    }

    @Test
    public void formatPreservingEncryption3() {

        // Test from https://csrc.nist.gov/csrc/media/projects/cryptographic-standards-and-guidelines/documents/examples/ff3samples.pdf
        // Also done in the library: https://github.com/mysto/java-fpe/blob/main/src/test/java/com/privacylogistics/FF3CipherTest.java

        final String plainText = "89012123456789000000789000000";
        final FPE fpe = new FPE("EF4359D8D580AA4F7F036D6F04FC6A94", "D8E7920AFA330A73");
        final String encrypted = Encryption.formatPreservingEncrypt(fpe, plainText);

        Assertions.assertEquals("48598367162252569629397416226", encrypted);

    }

    @Test
    public void encrypt1() throws Exception {

        final String token = "asdf";
        final String encrypted = Encryption.encrypt(token, new Crypto(KEY, IV));

        LOGGER.info("Encrypted '{}' is '{}'", token, encrypted);

        Assertions.assertEquals("r6cPN50ikH9qBZD0FNPG2g==", encrypted);

    }

    @Test
    public void encrypt2() throws Exception {

        final String token = "346596542547526";
        final String encrypted = Encryption.encrypt(token, new Crypto(KEY, IV));

        LOGGER.info("Encrypted '{}' is '{}'", token, encrypted);

        Assertions.assertEquals("5G4lCAQADM68uvVumZ9Lxw==", encrypted);

    }

}
