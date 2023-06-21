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
package ai.philterd.phileas.services.anonymization;

import ai.philterd.phileas.model.services.AnonymizationCacheService;
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
