/*
 *     Copyright 2023 Philerd, LLC @ https://www.philterd.ai
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
package ai.philterd.phileas.model.utils;

import ai.philterd.phileas.model.profile.Crypto;
import ai.philterd.phileas.model.profile.FPE;
import com.privacylogistics.FF3Cipher;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.Charset;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedList;
import java.util.List;

/**
 * Provides encryption methods.
 */
public class Encryption {

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
     * Encrypt a string token.
     * @param token The token to encrypt.
     * @param crypto The encryption {@link Crypto} key.
     * @return An encrypted string.
     * @throws Exception Thrown if the token cannot be encrypted.
     */
    public static String encrypt(String token, Crypto crypto) throws Exception {

        // echo "j6HcaY8m7hPACVVyQtj4PQ=="| openssl enc -a -d -aes-256-cbc -K 9EE7A356FDFE43F069500B0086758346E66D8583E0CE1CFCA04E50F67ECCE5D1 -iv B674D3B8F1C025AEFF8F6D5FA1AEAD3A

        final Cipher cipher = getCipher(crypto);

        final byte[] encrypted = cipher.doFinal(token.getBytes(Charset.defaultCharset()));
        return Base64.encodeBase64String(encrypted);

    }

    // This is for FHIR values.
    /*public static List<StringType> encrypt(List<StringType> tokens, Crypto crypto) throws Exception {

        final Cipher cipher = getCipher(crypto);

        final List<StringType> encryptedTokens = new LinkedList<>();

        for(StringType token : tokens) {

            final byte[] encrypted = cipher.doFinal(token.asStringValue().getBytes(Charset.defaultCharset()));
            final String output = Base64.encodeBase64String(encrypted);

            encryptedTokens.add(new StringType(output));

        }

        return encryptedTokens;

    }*/
    private static Cipher getCipher(final Crypto crypto) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, DecoderException {

        final byte[] secretKey = Hex.decodeHex(crypto.getKey());
        final byte[] initVector = Hex.decodeHex(crypto.getIv());
        final Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(secretKey, "AES"), new IvParameterSpec(initVector, 0, cipher.getBlockSize()));

        return cipher;

    }

    /**
     * Encrypts the <code>plainText</code> using format preserving encryption.
     * @param plainText The plain text.
     * @return The encrypted text.
     */
    private static String doFormatPreservingEncryption(final String plainText, final FPE fpe) {

        // TODO: Handle shorter and longer strings.
        if(plainText.length() < 6 || plainText.length() > 56) {
            throw new RuntimeException("Plain text is outside the acceptable length.");
        }

        try {

            final FF3Cipher c = new FF3Cipher(fpe.getKey(), fpe.getTweak());
            final String ciphertext = c.encrypt(plainText);

            return ciphertext;

        } catch (IllegalBlockSizeException | BadPaddingException ex) {
            throw new RuntimeException("Unable to encrypt plain text value.", ex);
        }

    }

}
