package com.mtnfog.test.phileas.model.utils;

import com.mtnfog.phileas.model.profile.Crypto;
import com.mtnfog.phileas.model.utils.Encryption;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

public class EncryptionTest {

    private static final Logger LOGGER = LogManager.getLogger(EncryptionTest.class);

    private static final String KEY = "9EE7A356FDFE43F069500B0086758346E66D8583E0CE1CFCA04E50F67ECCE5D1";
    private static final String IV = "B674D3B8F1C025AEFF8F6D5FA1AEAD3A";

    @Test
    public void encrypt1() throws Exception {

        final String token = "asdf";
        final String encrypted = Encryption.encrypt(token, new Crypto(KEY, IV));

        LOGGER.info("Encrypted '{}' is '{}'", token, encrypted);

        Assert.assertEquals("r6cPN50ikH9qBZD0FNPG2g==", encrypted);

    }

    @Test
    public void encrypt2() throws Exception {

        final String token = "346596542547526";
        final String encrypted = Encryption.encrypt(token, new Crypto(KEY, IV));

        LOGGER.info("Encrypted '{}' is '{}'", token, encrypted);

        Assert.assertEquals("5G4lCAQADM68uvVumZ9Lxw==", encrypted);

    }

}
