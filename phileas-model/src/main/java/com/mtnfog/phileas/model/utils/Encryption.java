package com.mtnfog.phileas.model.utils;

import com.mtnfog.phileas.model.profile.Crypto;
import org.apache.commons.codec.binary.Base64;
import org.hl7.fhir.r4.model.StringType;

import javax.crypto.Cipher;
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

    // echo "j6HcaY8m7hPACVVyQtj4PQ=="| openssl enc -a -d -aes-256-cbc -K 9EE7A356FDFE43F069500B0086758346E66D8583E0CE1CFCA04E50F67ECCE5D1 -iv B674D3B8F1C025AEFF8F6D5FA1AEAD3A

    /**
     * Encrypt a string token.
     * @param token The token to encrypt.
     * @param crypto The encryption {@link Crypto} key.
     * @return An encrypted string.
     * @throws Exception Thrown if the token cannot be encrypted.
     */
    public static String encrypt(String token, Crypto crypto) throws Exception {

        final Cipher cipher = getCipher(crypto);

        final byte[] encrypted = cipher.doFinal(token.getBytes(Charset.defaultCharset()));
        final String output = Base64.encodeBase64String(encrypted);

        return output;

    }

    /**
     *
     * @param tokens A list of tokens to encrypt.
     * @param crypto crypto The encryption {@link Crypto} key.
     * @return A list of encrypted tokens.
     * @throws Exception Thrown if the tokens cannot be encrypted.
     */
    public static List<StringType> encrypt(List<StringType> tokens, Crypto crypto) throws Exception {

        final Cipher cipher = getCipher(crypto);

        final List<StringType> encryptedTokens = new LinkedList<>();

        for(StringType token : tokens) {

            final byte[] encrypted = cipher.doFinal(token.asStringValue().getBytes(Charset.defaultCharset()));
            final String output = Base64.encodeBase64String(encrypted);

            encryptedTokens.add(new StringType(output));

        }

        return encryptedTokens;

    }

    private static Cipher getCipher(final Crypto crypto) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException {

        final byte[] secretKey = javax.xml.bind.DatatypeConverter.parseHexBinary(crypto.getKey());
        final byte[] initVector = javax.xml.bind.DatatypeConverter.parseHexBinary(crypto.getIv());
        final Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(secretKey, "AES"), new IvParameterSpec(initVector, 0, cipher.getBlockSize()));

        return cipher;

    }

}