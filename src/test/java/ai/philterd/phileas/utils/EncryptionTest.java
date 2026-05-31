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
import org.apache.commons.codec.binary.Base64;
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
    public void formatPreservingEncryptionRejectsShortInput() {

        // FF3 cannot encrypt content shorter than its minimum supported length.
        final FPE fpe = new FPE("EF4359D8D580AA4F7F036D6F04FC6A94", "D8E7920AFA330A73");

        Assertions.assertThrows(FormatPreservingEncryptionException.class,
                () -> Encryption.formatPreservingEncrypt(fpe, "12345"));

    }

    @Test
    public void formatPreservingEncryptionRejectsLongInput() {

        // FF3 cannot encrypt content longer than its maximum supported length.
        final FPE fpe = new FPE("EF4359D8D580AA4F7F036D6F04FC6A94", "D8E7920AFA330A73");
        final String tooLong = "1".repeat(57);

        Assertions.assertThrows(FormatPreservingEncryptionException.class,
                () -> Encryption.formatPreservingEncrypt(fpe, tooLong));

    }

    @Test
    public void encryptDecryptRoundTrips() throws Exception {

        final Crypto crypto = new Crypto(KEY, IV);
        final String token = "346596542547526";

        final String encrypted = Encryption.encrypt(token, crypto);
        LOGGER.info("Encrypted '{}' is '{}'", token, encrypted);

        Assertions.assertEquals(token, Encryption.decrypt(encrypted, crypto));

    }

    @Test
    public void encryptionIsNonDeterministic() throws Exception {

        // A fresh random nonce per call means the same plaintext encrypts to different ciphertext,
        // so identical values do not produce identical redactions across the corpus.
        final Crypto crypto = new Crypto(KEY, IV);
        final String token = "346596542547526";

        Assertions.assertNotEquals(Encryption.encrypt(token, crypto), Encryption.encrypt(token, crypto));

    }

    @Test
    public void decryptRejectsTamperedCiphertext() throws Exception {

        final Crypto crypto = new Crypto(KEY, IV);
        final String encrypted = Encryption.encrypt("sensitive-value", crypto);

        // Flip the last byte (part of the GCM authentication tag); decryption must fail.
        final byte[] raw = Base64.decodeBase64(encrypted);
        raw[raw.length - 1] ^= 0x01;
        final String tampered = Base64.encodeBase64String(raw);

        Assertions.assertThrows(Exception.class, () -> Encryption.decrypt(tampered, crypto));

    }

}
