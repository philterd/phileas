package com.mtnfog.phileas.services.anonymization;

import com.mtnfog.phileas.model.services.AnonymizationCacheService;
import org.apache.commons.lang3.RandomStringUtils;

import java.util.Random;

public class AlphanumericAnonymizationService extends AbstractAnonymizationService {

    private Random random;

    public AlphanumericAnonymizationService(AnonymizationCacheService anonymizationCacheService) {
        super(anonymizationCacheService);
        this.random = new Random();
    }

    @Override
    public String anonymize(String token) {

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < token.length(); i++) {

            final char c = token.charAt(i);

            if (Character.isDigit(c)) {

                sb.append(random.nextInt((9 - 0) + 1) + 0);

            } else if (Character.isAlphabetic(c)) {

                sb.append(RandomStringUtils.randomAlphabetic(1));

            } else if (Character.isSpaceChar(c)) {

                sb.append(" ");

            } else if (c == '_') {

                sb.append("_");

            } else if (c == '-') {

                sb.append("-");

            } else if (c == '.') {

                sb.append(".");

            } else {

                // For everything else return a number.
                sb.append(random.nextInt((9 - 0) + 1) + 0);

            }

        }

        return sb.toString();

    }

}
