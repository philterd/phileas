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
package ai.philterd.phileas.filters;

import ai.philterd.phileas.model.enums.FilterType;
import ai.philterd.phileas.model.objects.FilterResult;
import ai.philterd.phileas.model.services.defaults.DefaultContextService;
import ai.philterd.phileas.services.strategies.rules.MacAddressFilterStrategy;
import ai.philterd.phileas.services.anonymization.MacAddressAnonymizationService;
import ai.philterd.phileas.services.filters.regex.MacAddressFilter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

public class MacAddressFilterTest extends AbstractFilterTest {

    @Test
    public void filter1() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new MacAddressFilterStrategy()))
                .withAnonymizationService(new MacAddressAnonymizationService(new DefaultContextService()))
                .withWindowSize(windowSize)
                .build();

        final MacAddressFilter filter = new MacAddressFilter(filterConfiguration);

        final FilterResult filterResult = filter.filter(getPolicy(), "context",  "documentid", PIECE, "the mac is 00-14-22-04-25-37.", attributes);

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 11, 28, FilterType.MAC_ADDRESS));
        Assertions.assertEquals("00-14-22-04-25-37", filterResult.getSpans().get(0).getText());

    }

    @Test
    public void filter2() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new MacAddressFilterStrategy()))
                .withAnonymizationService(new MacAddressAnonymizationService(new DefaultContextService()))
                .withWindowSize(windowSize)
                .build();

        final MacAddressFilter filter = new MacAddressFilter(filterConfiguration);

        final FilterResult filterResult = filter.filter(getPolicy(), "context",  "documentid", PIECE, "the mac is 00:14:22:04:25:37.", attributes);

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 11, 28, FilterType.MAC_ADDRESS));

    }

}
