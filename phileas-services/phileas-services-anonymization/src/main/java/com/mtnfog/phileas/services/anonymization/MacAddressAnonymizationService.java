package com.mtnfog.phileas.services.anonymization;

import com.mtnfog.phileas.model.services.AnonymizationCacheService;

import java.util.Random;

public class MacAddressAnonymizationService extends AbstractAnonymizationService {

    private Random random;

    public MacAddressAnonymizationService(AnonymizationCacheService anonymizationCacheService) {
        super(anonymizationCacheService);

        this.random = new Random();

    }

    @Override
    public String anonymize(String token) {

        byte[] macAddr = new byte[6];
        random.nextBytes(macAddr);

        final StringBuilder sb = new StringBuilder(18);

        for(byte b : macAddr){

            if(sb.length() > 0) {
                sb.append(":");
            }

            sb.append(String.format("%02x", b));

        }

        return sb.toString();

    }

}
