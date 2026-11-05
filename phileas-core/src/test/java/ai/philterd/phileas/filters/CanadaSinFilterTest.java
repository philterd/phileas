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

import ai.philterd.phileas.model.filtering.FilterType;
import ai.philterd.phileas.model.filtering.Filtered;
import ai.philterd.phileas.services.anonymization.CanadaSinAnonymizationService;
import ai.philterd.phileas.services.filters.regex.CanadaSinFilter;
import ai.philterd.phileas.services.strategies.AbstractFilterStrategy;
import ai.philterd.phileas.services.strategies.rules.CanadaSinFilterStrategy;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

// All numbers in these tests are invented, except 046 454 286, the published sample SIN.
public class CanadaSinFilterTest extends AbstractFilterTest {

    private CanadaSinFilter filter(final boolean onlyValidPrefixes, final AbstractFilterStrategy strategy) {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(strategy))
                .withWindowSize(windowSize)
                .build();

        return new CanadaSinFilter(filterConfiguration, onlyValidPrefixes);

    }

    private CanadaSinFilter filter(final boolean onlyValidPrefixes) {
        return filter(onlyValidPrefixes, new CanadaSinFilterStrategy());
    }

    @Test
    public void filterSinUnformatted() throws Exception {

        final Filtered filtered = filter(false).filter(contextService, getPolicy(), "context", PIECE, "the sin is 046454286.");
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 11, 20, FilterType.CANADA_SIN));
        Assertions.assertEquals("046454286", filtered.getSpans().get(0).getText());

    }

    @Test
    public void filterSinSpaceSeparated() throws Exception {

        final Filtered filtered = filter(false).filter(contextService, getPolicy(), "context", PIECE, "the sin is 046 454 286.");
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 11, 22, FilterType.CANADA_SIN));
        Assertions.assertEquals("046 454 286", filtered.getSpans().get(0).getText());

    }

    @Test
    public void filterSinHyphenated() throws Exception {

        final Filtered filtered = filter(false).filter(contextService, getPolicy(), "context", PIECE, "the sin is 046-454-286.");
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 11, 22, FilterType.CANADA_SIN));
        Assertions.assertEquals("046-454-286", filtered.getSpans().get(0).getText());

    }

    @Test
    public void filterSinUnicodeHyphen() throws Exception {

        final Filtered filtered = filter(false).filter(contextService, getPolicy(), "context", PIECE, "the sin is 046‑454‑286.");
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 11, 22, FilterType.CANADA_SIN));

    }

    @Test
    public void filterSinWrappedAfterHyphen() throws Exception {

        final Filtered filtered = filter(false).filter(contextService, getPolicy(), "context", PIECE, "the sin is 046-454-\n286.");
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 11, 23, FilterType.CANADA_SIN));
        Assertions.assertEquals("046-454-\n286", filtered.getSpans().get(0).getText());

    }

    @Test
    public void filterSinNotJoinedAcrossLinesWithoutHyphen() throws Exception {

        final Filtered filtered = filter(false).filter(contextService, getPolicy(), "context", PIECE, "046\n454\n286");
        Assertions.assertEquals(0, filtered.getSpans().size());

    }

    @Test
    public void filterSinTemporaryResident() throws Exception {

        // A SIN beginning with 9 is detected like any other, with or without onlyValidPrefixes.
        for (final boolean onlyValidPrefixes : new boolean[]{false, true}) {
            final Filtered filtered = filter(onlyValidPrefixes).filter(contextService, getPolicy(), "context", PIECE, "the sin is 912-345-675.");
            Assertions.assertEquals(1, filtered.getSpans().size());
            Assertions.assertEquals("912-345-675", filtered.getSpans().get(0).getText());
        }

    }

    @Test
    public void filterSinLuhnInvalid() throws Exception {

        final CanadaSinFilter filter = filter(false);

        Assertions.assertEquals(0, filter.filter(contextService, getPolicy(), "context", PIECE, "the number 123 456 789 is not a sin.").getSpans().size());
        Assertions.assertEquals(0, filter.filter(contextService, getPolicy(), "context", PIECE, "the number 046 454 287 is not a sin.").getSpans().size());

    }

    @Test
    public void filterSinNotWithinLongerRun() throws Exception {

        final CanadaSinFilter filter = filter(false);

        Assertions.assertEquals(0, filter.filter(contextService, getPolicy(), "context", PIECE, "account 1046454286 closed.").getSpans().size());
        Assertions.assertEquals(0, filter.filter(contextService, getPolicy(), "context", PIECE, "account 0464542861 closed.").getSpans().size());
        Assertions.assertEquals(0, filter.filter(contextService, getPolicy(), "context", PIECE, "account 046454286046454286 closed.").getSpans().size());
        Assertions.assertEquals(0, filter.filter(contextService, getPolicy(), "context", PIECE, "the code is A046-454-286.").getSpans().size());

    }

    @Test
    public void filterSinPrefixes() throws Exception {

        // 0 and 8 are detected by default and dropped with onlyValidPrefixes; 1 to 7 and 9 are kept either way.
        for (final String sin : List.of("046 454 286", "812 345 676")) {
            Assertions.assertEquals(1, filter(false).filter(contextService, getPolicy(), "context", PIECE, "the sin is " + sin + ".").getSpans().size(), sin);
            Assertions.assertEquals(0, filter(true).filter(contextService, getPolicy(), "context", PIECE, "the sin is " + sin + ".").getSpans().size(), sin);
        }

        for (final String sin : List.of("123 456 782", "912 345 675")) {
            Assertions.assertEquals(1, filter(false).filter(contextService, getPolicy(), "context", PIECE, "the sin is " + sin + ".").getSpans().size(), sin);
            Assertions.assertEquals(1, filter(true).filter(contextService, getPolicy(), "context", PIECE, "the sin is " + sin + ".").getSpans().size(), sin);
        }

    }

    @Test
    public void filterSinLastFour() throws Exception {

        final CanadaSinFilterStrategy strategy = new CanadaSinFilterStrategy();
        strategy.setStrategy(AbstractFilterStrategy.LAST_4);

        final Filtered filtered = filter(false, strategy).filter(contextService, getPolicy(), "context", PIECE, "the sin is 046-454-286.");
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertEquals("-286", filtered.getSpans().get(0).getReplacement());

    }

    @Test
    public void filterSinMask() throws Exception {

        final CanadaSinFilterStrategy strategy = new CanadaSinFilterStrategy();
        strategy.setStrategy(AbstractFilterStrategy.MASK);

        final Filtered filtered = filter(false, strategy).filter(contextService, getPolicy(), "context", PIECE, "the sin is 046-454-286.");
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertEquals("***********", filtered.getSpans().get(0).getReplacement());

    }

    @Test
    public void filterSinUsesSinAnonymization() throws Exception {

        final CanadaSinFilterStrategy strategy = new CanadaSinFilterStrategy();
        strategy.setStrategy(AbstractFilterStrategy.RANDOM_REPLACE);

        filter(false, strategy);

        // RANDOM_REPLACE on a SIN draws from the SIN anonymization service, so a realistic
        // replacement is a Luhn-valid SIN. See CanadaSinFilterStrategyTest.
        Assertions.assertInstanceOf(CanadaSinAnonymizationService.class, strategy.getAnonymizationService());

    }

    @Test
    public void filterSinSeparatorsMustMatch() throws Exception {

        // 046454286 passes the Luhn check, so these are refused only for their separators.
        // https://github.com/philterd/phileas/issues/460
        final CanadaSinFilter filter = filter(false);

        for (final String mismatched : List.of("046454-286", "046-454286", "046 454-286", "046-454 286", "046454 286")) {
            Assertions.assertEquals(0, filter.filter(contextService, getPolicy(), "context", PIECE, "the value is " + mismatched + ".").getSpans().size(), mismatched);
        }

    }

}
