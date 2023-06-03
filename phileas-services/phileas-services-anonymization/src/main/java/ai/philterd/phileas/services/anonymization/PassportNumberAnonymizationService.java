package ai.philterd.phileas.services.anonymization;

import ai.philterd.phileas.model.services.AnonymizationCacheService;

import java.security.SecureRandom;

public class PassportNumberAnonymizationService extends AbstractAnonymizationService {

    private SecureRandom secureRandom;

    public PassportNumberAnonymizationService(AnonymizationCacheService anonymizationCacheService) {
        super(anonymizationCacheService);

        this.secureRandom = new SecureRandom();
    }

    @Override
    public String anonymize(String token) {

        byte[] macAddr = new byte[6];
        secureRandom.nextBytes(macAddr);

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