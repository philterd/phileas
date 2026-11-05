/*
 *     Copyright 2026 Philterd, LLC @ https://www.philterd.ai
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
package ai.philterd.phileas.filters;

import ai.philterd.phileas.services.anonymization.AbstractAnonymizationService;
import ai.philterd.phileas.services.anonymization.AnonymizationMethod;
import ai.philterd.phileas.services.anonymization.NumericAnonymizationService;
import ai.philterd.phileas.services.filters.regex.SsnFilter;
import ai.philterd.phileas.services.strategies.AbstractFilterStrategy;
import ai.philterd.phileas.services.strategies.rules.SsnFilterStrategy;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.security.SecureRandom;
import java.util.Collections;
import java.util.List;

public class AnonymizationMethodTest {

    private static final List<String> CANDIDATES = List.of("000-00-0001");

    @Test
    public void methodIsUsedAsGiven() {

        Assertions.assertEquals(AnonymizationMethod.REALISTIC, Filter.resolveAnonymizationMethod(AnonymizationMethod.REALISTIC, Collections.emptyList()));
        Assertions.assertEquals(AnonymizationMethod.REALISTIC, Filter.resolveAnonymizationMethod(AnonymizationMethod.REALISTIC, null));
        Assertions.assertEquals(AnonymizationMethod.UUID, Filter.resolveAnonymizationMethod(AnonymizationMethod.UUID, Collections.emptyList()));
        Assertions.assertEquals(AnonymizationMethod.FROM_LIST, Filter.resolveAnonymizationMethod(AnonymizationMethod.FROM_LIST, CANDIDATES));
        Assertions.assertEquals(AnonymizationMethod.FROM_LIST, Filter.resolveAnonymizationMethod(AnonymizationMethod.FROM_LIST, Collections.emptyList()));

    }

    @Test
    public void candidatesAreUsedUnlessUuid() {

        // A policy that sets candidates without a method keeps drawing from them.
        Assertions.assertEquals(AnonymizationMethod.FROM_LIST, Filter.resolveAnonymizationMethod(AnonymizationMethod.REALISTIC, CANDIDATES));
        Assertions.assertEquals(AnonymizationMethod.UUID, Filter.resolveAnonymizationMethod(AnonymizationMethod.UUID, CANDIDATES));

    }

    @Test
    public void unrecognizedMethodIsUuid() {

        // An unrecognized value deserializes to null.
        Assertions.assertEquals(AnonymizationMethod.UUID, Filter.resolveAnonymizationMethod(null, Collections.emptyList()));
        Assertions.assertEquals(AnonymizationMethod.UUID, Filter.resolveAnonymizationMethod(null, CANDIDATES));

    }

    @Test
    public void filterAppliesTheMethodToTheStrategy() {

        for (final AnonymizationMethod method : AnonymizationMethod.values()) {

            final SsnFilterStrategy strategy = new SsnFilterStrategy();
            strategy.setStrategy(AbstractFilterStrategy.RANDOM_REPLACE);
            strategy.setAnonymizationMethod(method);

            new SsnFilter(new FilterConfiguration.FilterConfigurationBuilder()
                    .withRandom(new SecureRandom())
                    .withStrategies(List.of(strategy))
                    .build());

            Assertions.assertEquals(method, ((AbstractAnonymizationService) strategy.getAnonymizationService()).getAnonymizationMethod());

        }

    }

    @Test
    public void realisticFallsBackToUuidWhenEveryValueEqualsTheToken() {

        final AbstractAnonymizationService service = new AbstractAnonymizationService() {
            @Override
            public String anonymize(final String token) {
                return anonymizeRealistic(token, () -> token.toUpperCase());
            }
        };

        Assertions.assertTrue(service.anonymize("thirty-five").matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"));

    }

    @Test
    public void likeForLikeKeepsTheShapeAndReplacesEveryLetterAndDigit() {

        final AbstractAnonymizationService service = new AbstractAnonymizationService() {
            @Override
            public String anonymize(final String token) {
                return replaceLikeForLike(token);
            }
        };

        // \uD840\uDC00 is U+20000, a CJK letter outside the Basic Multilingual Plane.
        final String token = "Ab-12 (x)\n\u4E2D\uD840\uDC00\u0663";

        for (int i = 0; i < 50; i++) {
            final String replaced = service.anonymize(token);
            Assertions.assertTrue(replaced.matches("[A-Z][a-z]-[0-9]{2} \\([a-z]\\)\\n[a-z]{2}[0-9]"), replaced);
        }

    }

    @Test
    public void fromListChoosesOnlyCandidatesThatDiffer() {

        final List<String> candidates = java.util.Arrays.asList("123-45-6789", null, "000-00-0001", "000-00-0002");
        final NumericAnonymizationService service = new NumericAnonymizationService(new SecureRandom(), candidates);

        final java.util.Set<String> chosen = new java.util.HashSet<>();
        for (int i = 0; i < 200; i++) {
            chosen.add(service.anonymize("123-45-6789"));
        }

        // Both differing candidates are chosen, and nothing else.
        Assertions.assertEquals(java.util.Set.of("000-00-0001", "000-00-0002"), chosen);

    }

    @Test
    public void fromListIsUuidWhenNoCandidateDiffers() {

        final String uuid = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";

        Assertions.assertTimeoutPreemptively(java.time.Duration.ofSeconds(10), () -> {
            for (final List<String> candidates : List.of(List.of("Jane"), List.of("JANE"), List.of("jane", "Jane", "JANE"),
                    java.util.Arrays.asList("Jane", null), Collections.<String>emptyList())) {
                final String replaced = new NumericAnonymizationService(new SecureRandom(), candidates).anonymize("jane");
                Assertions.assertTrue(replaced.matches(uuid), candidates + " gave " + replaced);
            }
        });

    }

    @Test
    public void defaultConstructorsAreRealistic() {

        Assertions.assertEquals(AnonymizationMethod.REALISTIC, new NumericAnonymizationService().getAnonymizationMethod());
        Assertions.assertEquals(AnonymizationMethod.FROM_LIST, new NumericAnonymizationService(new SecureRandom(), CANDIDATES).getAnonymizationMethod());

    }

}
