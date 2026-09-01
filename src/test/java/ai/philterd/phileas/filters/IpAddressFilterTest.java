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

import java.util.List;

import static ai.philterd.phileas.services.strategies.AbstractFilterStrategy.RANDOM_REPLACE;

public class IpAddressFilterTest extends AbstractFilterTest {

    @Test
    public void filterIpv41() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new IpAddressFilterStrategy()))
                .withWindowSize(windowSize)
                .build();

        final IpAddressFilter filter = new IpAddressFilter(filterConfiguration);

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "the ip is 192.168.1.101.");

        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 10, 23, FilterType.IP_ADDRESS));
        Assertions.assertEquals("192.168.1.101", filtered.getSpans().get(0).getText());

    }

    @Test
    public void filterIpv61() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new IpAddressFilterStrategy()))
                .withWindowSize(windowSize)
                .build();

        final IpAddressFilter filter = new IpAddressFilter(filterConfiguration);

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "the ip is 1::");

        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 10, 13, FilterType.IP_ADDRESS));

    }

    @Test
    public void filterIpv62() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new IpAddressFilterStrategy()))
                .withWindowSize(windowSize)
                .build();

        final IpAddressFilter filter = new IpAddressFilter(filterConfiguration);

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "the ip is 2001:0db8:85a3:0000:0000:8a2e:0370:7334");

        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 10, 49, FilterType.IP_ADDRESS));

    }

    @Test
    public void filterIpv63() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new IpAddressFilterStrategy()))
                .withWindowSize(windowSize)
                .build();

        final IpAddressFilter filter = new IpAddressFilter(filterConfiguration);

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "the ip is fe80::0202:B3FF:FE1E:8329");

        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 10, 35, FilterType.IP_ADDRESS));

    }

    @Test
    public void filterIpv4AllOnesOctetNotTruncated() throws Exception {

        // https://github.com/philterd/phileas/issues/335
        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new IpAddressFilterStrategy()))
                .withWindowSize(windowSize)
                .build();

        final IpAddressFilter filter = new IpAddressFilter(filterConfiguration);

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "ip 255.255.255.255");

        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 3, 18, FilterType.IP_ADDRESS));
        Assertions.assertEquals("255.255.255.255", filtered.getSpans().get(0).getText());

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
    public void filterIpv6CompressedProducesOneSpan() throws Exception {

        // https://github.com/philterd/phileas/issues/354
        // A compressed address used to produce the full span plus a truncated one from a second pattern.
        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new IpAddressFilterStrategy()))
                .withWindowSize(windowSize)
                .build();

        final IpAddressFilter filter = new IpAddressFilter(filterConfiguration);

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "host FE80::1");

        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 5, 12, FilterType.IP_ADDRESS));
        Assertions.assertEquals("FE80::1", filtered.getSpans().get(0).getText());

    }

    @Test
    public void filterIpv6CompressedMidAddressProducesOneSpan() throws Exception {

        // https://github.com/philterd/phileas/issues/354
        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new IpAddressFilterStrategy()))
                .withWindowSize(windowSize)
                .build();

        final IpAddressFilter filter = new IpAddressFilter(filterConfiguration);

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "host 2001:db8:85a3::8a2e:370:7334");

        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 5, 33, FilterType.IP_ADDRESS));
        Assertions.assertEquals("2001:db8:85a3::8a2e:370:7334", filtered.getSpans().get(0).getText());

    }

    @Test
    public void filterIpv6LoopbackProducesOneSpan() throws Exception {

        // https://github.com/philterd/phileas/issues/354
        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new IpAddressFilterStrategy()))
                .withWindowSize(windowSize)
                .build();

        final IpAddressFilter filter = new IpAddressFilter(filterConfiguration);

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "host ::1");

        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 5, 8, FilterType.IP_ADDRESS));
        Assertions.assertEquals("::1", filtered.getSpans().get(0).getText());

    }

    @Test
    public void filterIpv6MappedIpv4ProducesOneSpan() throws Exception {

        // https://github.com/philterd/phileas/issues/354
        // The IPv4 pattern also matches the trailing dotted quad, so this used to produce three spans.
        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new IpAddressFilterStrategy()))
                .withWindowSize(windowSize)
                .build();

        final IpAddressFilter filter = new IpAddressFilter(filterConfiguration);

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "host ::ffff:192.0.2.128");

        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 5, 23, FilterType.IP_ADDRESS));
        Assertions.assertEquals("::ffff:192.0.2.128", filtered.getSpans().get(0).getText());

    }

    @Test
    public void filterIpv6ZoneIdentifierIsIncluded() throws Exception {

        // https://github.com/philterd/phileas/issues/354
        // The zone identifier used to be left in the clear.
        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new IpAddressFilterStrategy()))
                .withWindowSize(windowSize)
                .build();

        final IpAddressFilter filter = new IpAddressFilter(filterConfiguration);

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "host fe80::1%eth0");

        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 5, 17, FilterType.IP_ADDRESS));
        Assertions.assertEquals("fe80::1%eth0", filtered.getSpans().get(0).getText());

    }

    @Test
    public void filterIpv4StillProducesOneSpan() throws Exception {

        // https://github.com/philterd/phileas/issues/354
        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new IpAddressFilterStrategy()))
                .withWindowSize(windowSize)
                .build();

        final IpAddressFilter filter = new IpAddressFilter(filterConfiguration);

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "host 192.168.1.1");

        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 5, 16, FilterType.IP_ADDRESS));
        Assertions.assertEquals("192.168.1.1", filtered.getSpans().get(0).getText());

    }

    @Test
    public void filterIpv6ExpandedMixedProducesOneSpan() throws Exception {

        // https://github.com/philterd/phileas/issues/354
        // Six hextets and a dotted quad. This form was matched as two adjacent spans before, so the
        // single-pattern rewrite has to keep covering it.
        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new IpAddressFilterStrategy()))
                .withWindowSize(windowSize)
                .build();

        final IpAddressFilter filter = new IpAddressFilter(filterConfiguration);

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "ip 1:2:3:4:5:6:1.2.3.4");

        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 3, 22, FilterType.IP_ADDRESS));
        Assertions.assertEquals("1:2:3:4:5:6:1.2.3.4", filtered.getSpans().get(0).getText());

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
