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
import com.privacylogistics.FF3Cipher;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;

/**
 * Provides encryption methods.
 */
public class Encryption {

    // FF3 format-preserving encryption only supports a bounded input length: the domain must be
    // large enough to be secure and within FF3's block limits. Values whose format-preservable
    // content falls outside this range cannot be format-preserving encrypted.
    private static final int FPE_MIN_LENGTH = 6;
    private static final int FPE_MAX_LENGTH = 56;

    // AES-GCM (authenticated encryption) with a fresh random nonce per value. The nonce is prepended
    // to the ciphertext so each encrypted value is self-contained and can be decrypted later.
    private static final String GCM_TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_NONCE_LENGTH = 12;     // 96-bit nonce, recommended for GCM
    private static final int GCM_TAG_LENGTH_BITS = 128; // 128-bit authentication tag
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private Encryption() {
        // Access the methods in this class through the static functions.
    }

    public static String formatPreservingEncrypt(final FPE fpe, final String token) {

        final char[] nonDigits = new char[token.length()];
        final StringBuilder tokenBuilder = new StringBuilder();

        for(int i=0; i < token.length(); i++) {

            if(!Character.isDigit(token.charAt(i)) && !Character.isAlphabetic(token.charAt(i))) {
                nonDigits[i] = token.charAt(i);
            } else {
                tokenBuilder.append(token.charAt(i));
            }

        }

        // Encrypt just the non-digit characters.
        final String encryptedToken = doFormatPreservingEncryption(tokenBuilder.toString(), fpe);

        // Put the non-digit characters back in the string.
        int encryptedTokenIndex = 0;
        for(int i=0; i < nonDigits.length; i++) {

            if(nonDigits[i] == 0) {
                nonDigits[i] = encryptedToken.charAt(encryptedTokenIndex++);
            }

        }

        // Return the replacement string.
        return new String(nonDigits);

    }

    /**
     * Encrypts a token using AES-GCM (authenticated encryption). A fresh random nonce is generated
     * for each call and prepended to the ciphertext, so encrypting the same value twice produces
     * different output (no equality leakage across the corpus) and the result carries an
     * authentication tag that detects tampering. The {@code iv} on {@link Crypto} is not used.
     * @param token The token to encrypt.
     * @param crypto The encryption {@link Crypto} key.
     * @return The Base64 encoding of {@code nonce || ciphertext || tag}.
     * @throws Exception Thrown if the token cannot be encrypted.
     */
    public static String encrypt(final String token, final Crypto crypto) throws Exception {

        final byte[] secretKey = Hex.decodeHex(crypto.getKey());

        final byte[] nonce = new byte[GCM_NONCE_LENGTH];
        SECURE_RANDOM.nextBytes(nonce);

        final Cipher cipher = Cipher.getInstance(GCM_TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(secretKey, "AES"), new GCMParameterSpec(GCM_TAG_LENGTH_BITS, nonce));

        final byte[] ciphertext = cipher.doFinal(token.getBytes(StandardCharsets.UTF_8));

        // Prepend the nonce so the encrypted value is self-contained: nonce || ciphertext || tag.
        final byte[] output = new byte[nonce.length + ciphertext.length];
        System.arraycopy(nonce, 0, output, 0, nonce.length);
        System.arraycopy(ciphertext, 0, output, nonce.length, ciphertext.length);

        return Base64.encodeBase64String(output);

    }

    /**
     * Decrypts a value produced by {@link #encrypt(String, Crypto)}. The nonce is read from the
     * front of the decoded bytes and the authentication tag is verified; decryption fails if the
     * value has been tampered with or the wrong key is used.
     * @param encrypted The Base64 encoding of {@code nonce || ciphertext || tag}.
     * @param crypto The encryption {@link Crypto} key.
     * @return The decrypted plain text.
     * @throws Exception Thrown if the value cannot be decrypted (including a failed authentication check).
     */
    public static String decrypt(final String encrypted, final Crypto crypto) throws Exception {

        final byte[] secretKey = Hex.decodeHex(crypto.getKey());
        final byte[] input = Base64.decodeBase64(encrypted);

        if (input.length < GCM_NONCE_LENGTH) {
            throw new IllegalArgumentException("The encrypted value is too short to contain a nonce.");
        }

        final byte[] nonce = Arrays.copyOfRange(input, 0, GCM_NONCE_LENGTH);
        final byte[] ciphertext = Arrays.copyOfRange(input, GCM_NONCE_LENGTH, input.length);

        final Cipher cipher = Cipher.getInstance(GCM_TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(secretKey, "AES"), new GCMParameterSpec(GCM_TAG_LENGTH_BITS, nonce));

        final byte[] plaintext = cipher.doFinal(ciphertext);

        return new String(plaintext, StandardCharsets.UTF_8);

    }

    /**
     * Reverses a value produced by {@link #formatPreservingEncrypt(FPE, String)}. The same key and
     * tweak used to encrypt must be provided. Non-alphanumeric characters are treated as structural
     * and passed through unchanged, exactly as they were during encryption.
     * @param fpe The FPE key and tweak.
     * @param encryptedToken The format-preserving-encrypted token to reverse.
     * @return The original plain text.
     * @throws FormatPreservingEncryptionException If the value cannot be decrypted.
     */
    public static String formatPreservingDecrypt(final FPE fpe, final String encryptedToken) {

        final char[] structural = new char[encryptedToken.length()];
        final StringBuilder alphanumeric = new StringBuilder();

        for (int i = 0; i < encryptedToken.length(); i++) {
            if (!Character.isDigit(encryptedToken.charAt(i)) && !Character.isAlphabetic(encryptedToken.charAt(i))) {
                structural[i] = encryptedToken.charAt(i);
            } else {
                alphanumeric.append(encryptedToken.charAt(i));
            }
        }

        final String decryptedPart = doFormatPreservingDecryption(alphanumeric.toString(), fpe);

        int idx = 0;
        for (int i = 0; i < structural.length; i++) {
            if (structural[i] == 0) {
                structural[i] = decryptedPart.charAt(idx++);
            }
        }

        return new String(structural);

    }

    /**
     * Encrypts the <code>plainText</code> using format-preserving encryption.
     * @param plainText The plain text.
     * @return The encrypted text.
     * @throws FormatPreservingEncryptionException If the value cannot be format-preserving encrypted,
     *         for example because its length is outside the range FF3 supports. The plain text is not
     *         included in the exception message.
     */
    private static String doFormatPreservingEncryption(final String plainText, final FPE fpe) {

        if(plainText.length() < FPE_MIN_LENGTH || plainText.length() > FPE_MAX_LENGTH) {
            throw new FormatPreservingEncryptionException("The value's format-preservable content (" + plainText.length()
                    + " characters) is outside the supported range of " + FPE_MIN_LENGTH + " to " + FPE_MAX_LENGTH + " characters.");
        }

        try {

            final FF3Cipher c = new FF3Cipher(fpe.getKey(), fpe.getTweak());

            return c.encrypt(plainText);

        } catch (final Exception ex) {
            // Wrap any FF3 failure so callers can fall back for this token rather than failing the
            // whole document. The plain text is intentionally not included in the message.
            throw new FormatPreservingEncryptionException("The value could not be format-preserving encrypted.", ex);
        }

    }

    private static String doFormatPreservingDecryption(final String cipherText, final FPE fpe) {

        if (cipherText.length() < FPE_MIN_LENGTH || cipherText.length() > FPE_MAX_LENGTH) {
            throw new FormatPreservingEncryptionException("The value's format-preservable content (" + cipherText.length()
                    + " characters) is outside the supported range of " + FPE_MIN_LENGTH + " to " + FPE_MAX_LENGTH + " characters.");
        }

        try {

            final FF3Cipher c = new FF3Cipher(fpe.getKey(), fpe.getTweak());

            return c.decrypt(cipherText);

        } catch (final Exception ex) {
            throw new FormatPreservingEncryptionException("The value could not be format-preserving decrypted.", ex);
        }

    }

}
