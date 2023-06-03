package ai.philterd.phileas.services.anonymization;

import ai.philterd.phileas.model.services.AnonymizationCacheService;
import org.apache.commons.lang3.StringUtils;

import java.util.Random;

public class CurrencyAnonymizationService extends AbstractAnonymizationService {

    private Random random;

    public CurrencyAnonymizationService(AnonymizationCacheService anonymizationCacheService) {
        super(anonymizationCacheService);
        this.random = new Random();
    }

    @Override
    public String anonymize(String token) {

        // Replace all digits with other digits.

        final StringBuilder sb = new StringBuilder();

        for (int i = 0; i < token.length(); i++) {

            final char c = token.charAt(i);

            if (Character.isDigit(c)) {

                sb.append(random.nextInt((9 - 0) + 1) + 0);

            } else {

                // For everything else just leave it as is.
                sb.append(c);

            }

        }

        final String anonymized;

        if(!sb.toString().startsWith("$")) {

            anonymized = "$" + sb;

        } else {

            anonymized = sb.toString();

        }

        // Just ensure that the new one does not equal the original.
        if(StringUtils.equalsIgnoreCase(token, anonymized)) {
            return anonymize(token);
        }

        return anonymized;

    }

}
