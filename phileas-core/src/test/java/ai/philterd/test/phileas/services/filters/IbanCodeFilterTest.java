/*
 *     Copyright 2025 Philterd, LLC @ https://www.philterd.ai
 *
 * Licensed under the Apache License, Version 2.0 (the "License", attributes);
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
package ai.philterd.test.phileas.services.filters;

import ai.philterd.phileas.model.enums.FilterType;
import ai.philterd.phileas.model.filter.Filter;
import ai.philterd.phileas.model.filter.FilterConfiguration;
import ai.philterd.phileas.model.objects.FilterResult;
import ai.philterd.phileas.model.policy.filters.strategies.rules.IbanCodeFilterStrategy;
import ai.philterd.phileas.model.services.AlertService;
import ai.philterd.phileas.services.anonymization.IbanCodeAnonymizationService;
import ai.philterd.phileas.services.anonymization.cache.LocalAnonymizationCacheService;
import ai.philterd.phileas.services.filters.regex.IbanCodeFilter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.List;

public class IbanCodeFilterTest extends AbstractFilterTest {

    private final AlertService alertService = Mockito.mock(AlertService.class);

    private Filter getFilter(boolean validate, boolean allowSpaces) {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new IbanCodeFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new IbanCodeAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final IbanCodeFilter filter = new IbanCodeFilter(filterConfiguration, validate, allowSpaces);

        return filter;

    }

    @Test
    public void filter1() throws Exception {

        final Filter filter = getFilter(true, false);

        final FilterResult filterResult = filter.filter(getPolicy(), "context", "documentid", PIECE, "bank code of GB33BUKB20201555555555 ok?", attributes);

        showSpans(filterResult.getSpans());

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 13, 35, FilterType.IBAN_CODE));
        Assertions.assertEquals("{{{REDACTED-iban-code}}}", filterResult.getSpans().get(0).getReplacement());
        Assertions.assertEquals("GB33BUKB20201555555555", filterResult.getSpans().get(0).getText());

    }

    @Test
    public void filter2() throws Exception {

        final Filter filter = getFilter(false, false);

        final FilterResult filterResult = filter.filter(getPolicy(), "context", "documentid", PIECE, "bank code of GB15MIDL40051512345678 ok?", attributes);

        showSpans(filterResult.getSpans());

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 13, 35, FilterType.IBAN_CODE));
        Assertions.assertEquals("{{{REDACTED-iban-code}}}", filterResult.getSpans().get(0).getReplacement());
        Assertions.assertEquals("GB15MIDL40051512345678", filterResult.getSpans().get(0).getText());

    }

    @Test
    public void filter3() throws Exception {

        final Filter filter = getFilter(true, true);

        final FilterResult filterResult = filter.filter(getPolicy(), "context", "documentid", PIECE, "bank code of GB15 MIDL 4005 1512 3456 78 ok?", attributes);

        showSpans(filterResult.getSpans());

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 13, 40, FilterType.IBAN_CODE));
        Assertions.assertEquals("{{{REDACTED-iban-code}}}", filterResult.getSpans().get(0).getReplacement());
        Assertions.assertEquals("GB15 MIDL 4005 1512 3456 78", filterResult.getSpans().get(0).getText());

    }

    @Test
    public void filter4() throws Exception {

        final Filter filter = getFilter(true, true);

        final FilterResult filterResult = filter.filter(getPolicy(), "context", "documentid", PIECE, "bank code of GB15 MIDL 4005 1512 3456 zz ok?", attributes);

        showSpans(filterResult.getSpans());

        Assertions.assertEquals(1, filterResult.getSpans().size());

    }

}