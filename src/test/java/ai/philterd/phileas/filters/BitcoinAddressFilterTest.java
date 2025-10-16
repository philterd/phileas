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

import ai.philterd.phileas.model.enums.FilterType;
import ai.philterd.phileas.model.objects.FilterResult;
import ai.philterd.phileas.services.context.DefaultContextService;
import ai.philterd.phileas.services.strategies.rules.BitcoinAddressFilterStrategy;
import ai.philterd.phileas.services.anonymization.BitcoinAddressAnonymizationService;
import ai.philterd.phileas.services.filters.regex.BitcoinAddressFilter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

public class BitcoinAddressFilterTest extends AbstractFilterTest {

    @Test
    public void filter1() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new BitcoinAddressFilterStrategy()))
                .withAnonymizationService(new BitcoinAddressAnonymizationService(new DefaultContextService()))
                .withWindowSize(windowSize)
                .build();

        final BitcoinAddressFilter filter = new BitcoinAddressFilter(filterConfiguration);

        final FilterResult filterResult = filter.filter(getPolicy(), "context", PIECE, "the address is 127NVqnjf8gB9BFAW2dnQeM6wqmy1gbGtv.");

        showSpans(filterResult.getSpans());

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 15, 49, FilterType.BITCOIN_ADDRESS));
        Assertions.assertEquals("{{{REDACTED-bitcoin-address}}}", filterResult.getSpans().get(0).getReplacement());
        Assertions.assertEquals("127NVqnjf8gB9BFAW2dnQeM6wqmy1gbGtv", filterResult.getSpans().get(0).getText());

    }

    @Test
    public void filter2() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new BitcoinAddressFilterStrategy()))
                .withAnonymizationService(new BitcoinAddressAnonymizationService(new DefaultContextService()))
                .withWindowSize(windowSize)
                .build();

        final BitcoinAddressFilter filter = new BitcoinAddressFilter(filterConfiguration);

        final FilterResult filterResult = filter.filter(getPolicy(), "context", PIECE, "the address is 12qnjf8FAW2dnQeM6wqmy1gbGtv.");

        showSpans(filterResult.getSpans());

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 15, 42, FilterType.BITCOIN_ADDRESS));
        Assertions.assertEquals("{{{REDACTED-bitcoin-address}}}", filterResult.getSpans().get(0).getReplacement());
        Assertions.assertEquals("12qnjf8FAW2dnQeM6wqmy1gbGtv", filterResult.getSpans().get(0).getText());

    }

    @Test
    public void filter3() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new BitcoinAddressFilterStrategy()))
                .withAnonymizationService(new BitcoinAddressAnonymizationService(new DefaultContextService()))
                .withWindowSize(windowSize)
                .build();

        final BitcoinAddressFilter filter = new BitcoinAddressFilter(filterConfiguration);

        final FilterResult filterResult = filter.filter(getPolicy(), "context", PIECE, "the address is 126wqmy1gbGtv.");

        showSpans(filterResult.getSpans());

        Assertions.assertEquals(0, filterResult.getSpans().size());

    }

}