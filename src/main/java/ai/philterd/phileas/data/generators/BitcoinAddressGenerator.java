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

import ai.philterd.phileas.data.DataGenerator;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;

/**
 * Generates random Bitcoin addresses with valid checksums: legacy (Base58Check) pay-to-public-key-hash
 * and pay-to-script-hash addresses, and segregated witness (bech32 and bech32m) addresses. The hashes
 * and witness programs are random, so an address is well formed but is not expected to be in use.
 */
public class BitcoinAddressGenerator implements DataGenerator.Generator<String> {

    private static final String BASE58 = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz";
    private static final String BECH32 = "qpzry9x8gf2tvdw0s3jn54khce6mua7l";
    private static final int[] BECH32_GENERATOR = {0x3b6a57b2, 0x26508e6d, 0x1ea119fa, 0x3d4233dd, 0x2a1462b3};
    private static final int BECH32_CONSTANT = 1;
    private static final int BECH32M_CONSTANT = 0x2bc830a3;

    /** The version byte of a pay-to-public-key-hash address, which begins with 1. */
    public static final byte P2PKH_VERSION = 0x00;

    /** The version byte of a pay-to-script-hash address, which begins with 3. */
    public static final byte P2SH_VERSION = 0x05;

    private final SecureRandom random;

    /**
     * Creates a new Bitcoin address generator.
     * @param random The {@link SecureRandom} to use.
     */
    public BitcoinAddressGenerator(final SecureRandom random) {
        this.random = random;
    }

    /**
     * A random pay-to-public-key-hash address, beginning with 1.
     * @return A Bitcoin address.
     */
    @Override
    public String random() {
        return legacy(P2PKH_VERSION);
    }

    /**
     * A random legacy address with a random 20-byte hash.
     * @param version {@link #P2PKH_VERSION} for an address beginning with 1, or {@link #P2SH_VERSION}
     *                for one beginning with 3.
     * @return A Base58Check-encoded address.
     */
    public String legacy(final byte version) {
        return base58Check(version, randomBytes(20));
    }

    /**
     * A random segregated witness address on the Bitcoin main network, beginning with {@code bc1}.
     * @param witnessVersion 0 for a bech32 address ({@code bc1q}), or 1 for a bech32m taproot address
     *                       ({@code bc1p}).
     * @param programLength The witness program length in bytes: 20 or 32 for version 0, 32 for version 1.
     * @return A bech32 or bech32m address, in lower case.
     */
    public String segwit(final int witnessVersion, final int programLength) {
        return segwitAddress("bc", witnessVersion, randomBytes(programLength));
    }

    @Override
    public long poolSize() {
        return Long.MAX_VALUE;
    }

    private byte[] randomBytes(final int length) {
        final byte[] bytes = new byte[length];
        random.nextBytes(bytes);
        return bytes;
    }

    /**
     * Base58Check encoding: the version byte and payload, followed by the first four bytes of the
     * double SHA-256 of both, in base 58, with a leading 1 for each leading zero byte.
     * @param version The version byte.
     * @param payload The payload.
     * @return The encoded value.
     */
    public static String base58Check(final byte version, final byte[] payload) {

        final byte[] versioned = new byte[payload.length + 1];
        versioned[0] = version;
        System.arraycopy(payload, 0, versioned, 1, payload.length);

        final byte[] checksum = Arrays.copyOf(sha256(sha256(versioned)), 4);
        final byte[] data = Arrays.copyOf(versioned, versioned.length + 4);
        System.arraycopy(checksum, 0, data, versioned.length, 4);

        final StringBuilder sb = new StringBuilder();
        BigInteger value = new BigInteger(1, data);
        final BigInteger base = BigInteger.valueOf(58);

        while (value.signum() > 0) {
            final BigInteger[] divided = value.divideAndRemainder(base);
            sb.append(BASE58.charAt(divided[1].intValue()));
            value = divided[0];
        }

        for (int i = 0; i < data.length && data[i] == 0; i++) {
            sb.append('1');
        }

        return sb.reverse().toString();

    }

    /**
     * A segregated witness address (BIP 173 and BIP 350): bech32 for witness version 0 and bech32m for
     * later versions.
     * @param hrp The human-readable part, {@code bc} for the Bitcoin main network.
     * @param witnessVersion The witness version, 0 to 16.
     * @param program The witness program.
     * @return The address, in lower case.
     */
    public static String segwitAddress(final String hrp, final int witnessVersion, final byte[] program) {

        final ByteArrayOutputStream data = new ByteArrayOutputStream();
        data.write(witnessVersion);

        // Regroup the program's 8-bit bytes into 5-bit groups, padding the last group with zeros.
        int accumulator = 0;
        int bits = 0;
        for (final byte b : program) {
            accumulator = (accumulator << 8) | (b & 0xff);
            bits += 8;
            while (bits >= 5) {
                bits -= 5;
                data.write((accumulator >> bits) & 31);
            }
        }
        if (bits > 0) {
            data.write((accumulator << (5 - bits)) & 31);
        }

        final byte[] values = data.toByteArray();
        final int constant = witnessVersion == 0 ? BECH32_CONSTANT : BECH32M_CONSTANT;

        final ByteArrayOutputStream checked = new ByteArrayOutputStream();
        final byte[] expanded = expandHrp(hrp);
        checked.write(expanded, 0, expanded.length);
        checked.write(values, 0, values.length);
        checked.write(new byte[6], 0, 6);
        final int polymod = polymod(checked.toByteArray()) ^ constant;

        final StringBuilder sb = new StringBuilder(hrp).append('1');
        for (final byte value : values) {
            sb.append(BECH32.charAt(value));
        }
        for (int i = 0; i < 6; i++) {
            sb.append(BECH32.charAt((polymod >> (5 * (5 - i))) & 31));
        }

        return sb.toString();

    }

    private static byte[] expandHrp(final String hrp) {
        final byte[] expanded = new byte[hrp.length() * 2 + 1];
        for (int i = 0; i < hrp.length(); i++) {
            expanded[i] = (byte) (hrp.charAt(i) >> 5);
            expanded[i + hrp.length() + 1] = (byte) (hrp.charAt(i) & 31);
        }
        return expanded;
    }

    private static int polymod(final byte[] values) {
        int checksum = 1;
        for (final byte value : values) {
            final int top = checksum >>> 25;
            checksum = ((checksum & 0x1ffffff) << 5) ^ value;
            for (int i = 0; i < 5; i++) {
                if (((top >> i) & 1) == 1) {
                    checksum ^= BECH32_GENERATOR[i];
                }
            }
        }
        return checksum;
    }

    private static byte[] sha256(final byte[] input) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(input);
        } catch (final NoSuchAlgorithmException ex) {
            // Every Java platform is required to support SHA-256.
            throw new IllegalStateException(ex);
        }
    }

}
