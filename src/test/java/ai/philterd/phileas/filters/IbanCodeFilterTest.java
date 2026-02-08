/*
 *     Copyright 2025 Philterd, LLC @ https://www.philterd.ai
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

import ai.philterd.phileas.model.filtering.FilterType;
import ai.philterd.phileas.model.filtering.Filtered;
import ai.philterd.phileas.services.context.DefaultContextService;
import ai.philterd.phileas.services.filters.regex.IbanCodeFilter;
import ai.philterd.phileas.services.strategies.rules.IbanCodeFilterStrategy;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.security.SecureRandom;
import java.util.List;

import static ai.philterd.phileas.services.strategies.AbstractFilterStrategy.RANDOM_REPLACE;

public class IbanCodeFilterTest extends AbstractFilterTest {

    private Filter getFilter(boolean validate, boolean allowSpaces) {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new IbanCodeFilterStrategy()))
                .withContextService(contextService)
                .withRandom(random)
                .withWindowSize(windowSize)
                .build();

        return new IbanCodeFilter(filterConfiguration, validate, allowSpaces);

    }

    @Test
    public void filter1() throws Exception {

        final Filter filter = getFilter(true, false);

        final Filtered filtered = filter.filter(getPolicy(), "context", PIECE, "bank code of GB33BUKB20201555555555 ok?");

        showSpans(filtered.getSpans());

        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 13, 35, FilterType.IBAN_CODE));
        Assertions.assertEquals("{{{REDACTED-iban-code}}}", filtered.getSpans().get(0).getReplacement());
        Assertions.assertEquals("GB33BUKB20201555555555", filtered.getSpans().get(0).getText());

    }

    @Test
    public void filter2() throws Exception {

        final Filter filter = getFilter(false, false);

        final Filtered filtered = filter.filter(getPolicy(), "context", PIECE, "bank code of GB15MIDL40051512345678 ok?");

        showSpans(filtered.getSpans());

        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 13, 35, FilterType.IBAN_CODE));
        Assertions.assertEquals("{{{REDACTED-iban-code}}}", filtered.getSpans().get(0).getReplacement());
        Assertions.assertEquals("GB15MIDL40051512345678", filtered.getSpans().get(0).getText());

    }

    @Test
    public void filter3() throws Exception {

        final Filter filter = getFilter(true, true);

        final Filtered filtered = filter.filter(getPolicy(), "context", PIECE, "bank code of GB15 MIDL 4005 1512 3456 78 ok?");

        showSpans(filtered.getSpans());

        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 13, 40, FilterType.IBAN_CODE));
        Assertions.assertEquals("{{{REDACTED-iban-code}}}", filtered.getSpans().get(0).getReplacement());
        Assertions.assertEquals("GB15 MIDL 4005 1512 3456 78", filtered.getSpans().get(0).getText());

    }

    @Test
    public void filter4() throws Exception {

        final Filter filter = getFilter(false, true);

        final Filtered filtered = filter.filter(getPolicy(), "context", PIECE, "bank code of GB15 MIDL 4005 1512 3456 zz ok?");

        showSpans(filtered.getSpans());

        Assertions.assertEquals(1, filtered.getSpans().size());

    }

    @Test
    public void invalidIbanCode() throws Exception {

        final Filter filter = getFilter(true, false);

        final Filtered filtered = filter.filter(getPolicy(), "context", PIECE, "bank code of AV01AZ ok?");

        showSpans(filtered.getSpans());

        Assertions.assertEquals(0, filtered.getSpans().size());

    }

    @Test
    public void invalidButNoValidation() throws Exception {

        final Filter filter = getFilter(false, false);

        final Filtered filtered = filter.filter(getPolicy(), "context", PIECE, "bank code of AV01AZ ok?");

        showSpans(filtered.getSpans());

        Assertions.assertEquals(1, filtered.getSpans().size());

    }

    @Test
    public void filterWithCandidates1() throws Exception {

        final List<String> candidates = List.of("candidate1", "candidate2");

        final IbanCodeFilterStrategy ibanCodeFilterStrategy = new IbanCodeFilterStrategy();
        ibanCodeFilterStrategy.setStrategy(RANDOM_REPLACE);
        ibanCodeFilterStrategy.setAnonymizationCandidates(candidates);

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(ibanCodeFilterStrategy))
                .withContextService(contextService)
                .withRandom(random)
                .withWindowSize(windowSize)
                .build();

        final IbanCodeFilter filter = new IbanCodeFilter(filterConfiguration, true, false);

        final Filtered filtered = filter.filter(getPolicy(), "context", PIECE, "bank code of GB33BUKB20201555555555 ok?");
        showSpans(filtered.getSpans());
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(candidates.contains(filtered.getSpans().get(0).getReplacement()));

    }

}