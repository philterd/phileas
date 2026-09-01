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
import ai.philterd.phileas.services.filters.regex.IpAddressFilter;
import ai.philterd.phileas.services.strategies.rules.IpAddressFilterStrategy;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static ai.philterd.phileas.services.strategies.AbstractFilterStrategy.RANDOM_REPLACE;

public class IpAddressFilterTest extends AbstractFilterTest {

    private IpAddressFilter filter() {

        return new IpAddressFilter(new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new IpAddressFilterStrategy()))
                .withWindowSize(windowSize)
                .build());

    }

    /**
     * Every address form produces exactly one span covering the whole address. The IPv6 rows are
     * from https://github.com/philterd/phileas/issues/354, where a compressed address produced a
     * truncated span alongside the full one, or none at all.
     */
    @ParameterizedTest(name = "{0}")
    @CsvSource({
            "'the ip is 192.168.1.101.', 192.168.1.101, 10, 23",
            "'host 192.168.1.1', 192.168.1.1, 5, 16",
            // https://github.com/philterd/phileas/issues/335
            "'ip 255.255.255.255', 255.255.255.255, 3, 18",
            "'the ip is 1::', 1::, 10, 13",
            "'the ip is 2001:0db8:85a3:0000:0000:8a2e:0370:7334', 2001:0db8:85a3:0000:0000:8a2e:0370:7334, 10, 49",
            "'the ip is fe80::0202:B3FF:FE1E:8329', fe80::0202:B3FF:FE1E:8329, 10, 35",
            "'host FE80::1', FE80::1, 5, 12",
            "'host 2001:db8:85a3::8a2e:370:7334', 2001:db8:85a3::8a2e:370:7334, 5, 33",
            "'host ::1', ::1, 5, 8",
            // The IPv4 pattern also matches the trailing dotted quad, so this once produced three spans.
            "'host ::ffff:192.0.2.128', ::ffff:192.0.2.128, 5, 23",
            // The zone identifier was left in the clear.
            "'host fe80::1%eth0', fe80::1%eth0, 5, 17",
            // Six hextets and a dotted quad, which was matched as two adjacent spans before.
            "'ip 1:2:3:4:5:6:1.2.3.4', 1:2:3:4:5:6:1.2.3.4, 3, 22"
    })
    public void addressProducesOneSpan(final String input, final String expected, final int start, final int end) throws Exception {

        final Filtered filtered = filter().filter(contextService, getPolicy(), "context", PIECE, input);

        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), start, end, FilterType.IP_ADDRESS));
        Assertions.assertEquals(expected, filtered.getSpans().get(0).getText());

    }

    @Test
    public void filterIpv4OutOfRangeLeadingOctetProducesNoSpan() throws Exception {

        // https://github.com/philterd/phileas/issues/336
        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new IpAddressFilterStrategy()))
                .withWindowSize(windowSize)
                .build();

        final IpAddressFilter filter = new IpAddressFilter(filterConfiguration);

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "ip 256.1.1.1");

        Assertions.assertEquals(0, filtered.getSpans().size());

    }

    @Test
    public void filterWithCandidates1() throws Exception {

        final List<String> candidates = List.of("1.1.1.1", "2.2.2.2");

        final IpAddressFilterStrategy ipAddressFilterStrategy = new IpAddressFilterStrategy();
        ipAddressFilterStrategy.setStrategy(RANDOM_REPLACE);
        ipAddressFilterStrategy.setAnonymizationCandidates(candidates);

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(ipAddressFilterStrategy))
                .withWindowSize(windowSize)
                .build();

        final IpAddressFilter filter = new IpAddressFilter(filterConfiguration);

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "the ip is 192.168.1.101.");
        showSpans(filtered.getSpans());
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(candidates.contains(filtered.getSpans().get(0).getReplacement()));

    }

}
