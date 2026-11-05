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
import ai.philterd.phileas.services.filters.regex.BitcoinAddressFilter;
import ai.philterd.phileas.services.strategies.rules.BitcoinAddressFilterStrategy;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

import static ai.philterd.phileas.services.strategies.AbstractFilterStrategy.RANDOM_REPLACE;

public class BitcoinAddressFilterTest extends AbstractFilterTest {

    @Test
    public void filter1() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new BitcoinAddressFilterStrategy()))
                .withWindowSize(windowSize)
                .build();

        final BitcoinAddressFilter filter = new BitcoinAddressFilter(filterConfiguration);

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "the address is 127NVqnjf8gB9BFAW2dnQeM6wqmy1gbGtv.");

        showSpans(filtered.getSpans());

        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 15, 49, FilterType.BITCOIN_ADDRESS));
        Assertions.assertEquals("{{{REDACTED-bitcoin-address}}}", filtered.getSpans().get(0).getReplacement());
        Assertions.assertEquals("127NVqnjf8gB9BFAW2dnQeM6wqmy1gbGtv", filtered.getSpans().get(0).getText());

    }

    @Test
    public void filter2() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new BitcoinAddressFilterStrategy()))
                .withWindowSize(windowSize)
                .build();

        final BitcoinAddressFilter filter = new BitcoinAddressFilter(filterConfiguration);

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "the address is 12qnjf8FAW2dnQeM6wqmy1gbGtv.");

        showSpans(filtered.getSpans());

        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 15, 42, FilterType.BITCOIN_ADDRESS));
        Assertions.assertEquals("{{{REDACTED-bitcoin-address}}}", filtered.getSpans().get(0).getReplacement());
        Assertions.assertEquals("12qnjf8FAW2dnQeM6wqmy1gbGtv", filtered.getSpans().get(0).getText());

    }

    @Test
    public void filterBech32V0() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new BitcoinAddressFilterStrategy()))
                .withWindowSize(windowSize)
                .build();

        final BitcoinAddressFilter filter = new BitcoinAddressFilter(filterConfiguration);

        // A 42 character version 0 witness program.
        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "addr bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4");

        showSpans(filtered.getSpans());

        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 5, 47, FilterType.BITCOIN_ADDRESS));
        Assertions.assertEquals("bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4", filtered.getSpans().get(0).getText());
        Assertions.assertEquals("{{{REDACTED-bitcoin-address}}}", filtered.getSpans().get(0).getReplacement());

    }

    @Test
    public void filterBech32LongerForms() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new BitcoinAddressFilterStrategy()))
                .withWindowSize(windowSize)
                .build();

        final BitcoinAddressFilter filter = new BitcoinAddressFilter(filterConfiguration);

        // A 62 character version 0 witness program.
        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE,
                "addr bc1qrp33g0q5c5txsp9arysrx4k6zdkfs4nce4xj0gdcccefvpysxf3qccfmv3.");

        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertEquals("bc1qrp33g0q5c5txsp9arysrx4k6zdkfs4nce4xj0gdcccefvpysxf3qccfmv3", filtered.getSpans().get(0).getText());

        // A 62 character version 1 (taproot) witness program.
        final Filtered filtered2 = filter.filter(contextService, getPolicy(), "context", PIECE,
                "addr bc1p0xlxvlhemja6c4dqv22uapctqupfhlxm9h8z3k2e72q4k9hcz7vqzk5jj0.");

        Assertions.assertEquals(1, filtered2.getSpans().size());
        Assertions.assertEquals("bc1p0xlxvlhemja6c4dqv22uapctqupfhlxm9h8z3k2e72q4k9hcz7vqzk5jj0", filtered2.getSpans().get(0).getText());

    }

    @Test
    public void filterBech32Uppercase() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new BitcoinAddressFilterStrategy()))
                .withWindowSize(windowSize)
                .build();

        final BitcoinAddressFilter filter = new BitcoinAddressFilter(filterConfiguration);

        // BIP 173 allows an address to be written entirely in uppercase.
        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "addr BC1QW508D6QEJXTDG4Y5R3ZARVARY0C5XW7KV8F3T4");

        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertEquals("BC1QW508D6QEJXTDG4Y5R3ZARVARY0C5XW7KV8F3T4", filtered.getSpans().get(0).getText());

    }

    @Test
    public void filterBech32ExcludedCharacters() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new BitcoinAddressFilterStrategy()))
                .withWindowSize(windowSize)
                .build();

        final BitcoinAddressFilter filter = new BitcoinAddressFilter(filterConfiguration);

        // The bech32 character set omits 1, b, i and o; each is otherwise a valid address.
        final List<String> excluded = List.of(
                "bc1qw508d6qejxtdg4y5r31arvary0c5xw7kv8f3t4",
                "bc1qw508d6qejxtdg4y5r3barvary0c5xw7kv8f3t4",
                "bc1qw508d6qejxtdg4y5r3iarvary0c5xw7kv8f3t4",
                "bc1qw508d6qejxtdg4y5r3oarvary0c5xw7kv8f3t4");

        for(final String address : excluded) {
            final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "addr " + address);
            Assertions.assertEquals(0, filtered.getSpans().size(), address);
        }

    }

    @Test
    public void filterWithCandidates1() throws Exception {

        final List<String> candidates = List.of("address1", "address2");

        final BitcoinAddressFilterStrategy bitcoinAddressFilterStrategy = new BitcoinAddressFilterStrategy();
        bitcoinAddressFilterStrategy.setStrategy(RANDOM_REPLACE);
        bitcoinAddressFilterStrategy.setAnonymizationCandidates(candidates);

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(bitcoinAddressFilterStrategy))
                .withWindowSize(windowSize)
                .build();

        final BitcoinAddressFilter filter = new BitcoinAddressFilter(filterConfiguration);

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "the address is 127NVqnjf8gB9BFAW2dnQeM6wqmy1gbGtv");
        showSpans(filtered.getSpans());
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(candidates.contains(filtered.getSpans().get(0).getReplacement()));

    }

}