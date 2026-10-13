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
import ai.philterd.phileas.services.anonymization.ItinAnonymizationService;
import ai.philterd.phileas.services.filters.regex.ItinFilter;
import ai.philterd.phileas.services.filters.regex.SsnFilter;
import ai.philterd.phileas.services.strategies.AbstractFilterStrategy;
import ai.philterd.phileas.services.strategies.rules.ItinFilterStrategy;
import ai.philterd.phileas.services.strategies.rules.SsnFilterStrategy;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

// All numbers in these tests are invented.
public class ItinFilterTest extends AbstractFilterTest {

    private ItinFilter filter(final boolean onlyValidRanges, final AbstractFilterStrategy strategy) {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(strategy))
                .withWindowSize(windowSize)
                .build();

        return new ItinFilter(filterConfiguration, onlyValidRanges);

    }

    private ItinFilter filter(final boolean onlyValidRanges) {
        return filter(onlyValidRanges, new ItinFilterStrategy());
    }

    private SsnFilter ssnFilter() {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new SsnFilterStrategy()))
                .withWindowSize(windowSize)
                .build();

        return new SsnFilter(filterConfiguration);

    }

    @Test
    public void filterItinHyphenated() throws Exception {

        final Filtered filtered = filter(false).filter(contextService, getPolicy(), "context", PIECE, "the itin is 912-70-1234.");
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 12, 23, FilterType.ITIN));
        Assertions.assertEquals("912-70-1234", filtered.getSpans().get(0).getText());

    }

    @Test
    public void filterItinSpaceSeparated() throws Exception {

        final Filtered filtered = filter(false).filter(contextService, getPolicy(), "context", PIECE, "the itin is 912 70 1234.");
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 12, 23, FilterType.ITIN));
        Assertions.assertEquals("912 70 1234", filtered.getSpans().get(0).getText());

    }

    @Test
    public void filterItinUnformatted() throws Exception {

        final Filtered filtered = filter(false).filter(contextService, getPolicy(), "context", PIECE, "the itin is 912701234.");
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 12, 21, FilterType.ITIN));
        Assertions.assertEquals("912701234", filtered.getSpans().get(0).getText());

    }

    @Test
    public void filterItinWrappedAfterHyphen() throws Exception {

        final Filtered filtered = filter(false).filter(contextService, getPolicy(), "context", PIECE, "the itin is 912-\n70-1234.");
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertEquals("912-\n70-1234", filtered.getSpans().get(0).getText());

    }

    @Test
    public void filterItinNotWithinLongerRun() throws Exception {

        final ItinFilter filter = filter(false);

        Assertions.assertEquals(0, filter.filter(contextService, getPolicy(), "context", PIECE, "account 1912701234 closed.").getSpans().size());
        Assertions.assertEquals(0, filter.filter(contextService, getPolicy(), "context", PIECE, "account 9127012345 closed.").getSpans().size());
        Assertions.assertEquals(0, filter.filter(contextService, getPolicy(), "context", PIECE, "the code is A912-70-1234.").getSpans().size());

    }

    @Test
    public void filterSsnNotClaimedAsItin() throws Exception {

        final ItinFilter filter = filter(false);

        Assertions.assertEquals(0, filter.filter(contextService, getPolicy(), "context", PIECE, "the ssn is 123-45-6789.").getSpans().size());
        Assertions.assertEquals(0, filter.filter(contextService, getPolicy(), "context", PIECE, "the ssn is 123 45 6789.").getSpans().size());
        Assertions.assertEquals(0, filter.filter(contextService, getPolicy(), "context", PIECE, "the ssn is 123456789.").getSpans().size());

        // The same value is still an SSN.
        final Filtered ssn = ssnFilter().filter(contextService, getPolicy(), "context", PIECE, "the ssn is 123-45-6789.");
        Assertions.assertEquals(1, ssn.getSpans().size());
        Assertions.assertEquals(FilterType.SSN, ssn.getSpans().get(0).getFilterType());

    }

    @Test
    public void filterItinNotClaimedAsSsn() throws Exception {

        final SsnFilter ssnFilter = ssnFilter();

        for (final String itin : List.of("912-70-1234", "912 70 1234", "912701234", "912-\n70-1234", "999-99-9999", "900-00-0001")) {
            final Filtered filtered = ssnFilter.filter(contextService, getPolicy(), "context", PIECE, "the number is " + itin + ".");
            Assertions.assertEquals(0, filtered.getSpans().size(), itin);
        }

    }

    @Test
    public void filterItinOutsideValidRanges() throws Exception {

        // 40 is outside the IRS ITIN ranges: detected by default, dropped with onlyValidRanges.
        final String input = "the itin is 912-40-1234.";

        final Filtered byDefault = filter(false).filter(contextService, getPolicy(), "context", PIECE, input);
        Assertions.assertEquals(1, byDefault.getSpans().size());
        Assertions.assertEquals(FilterType.ITIN, byDefault.getSpans().get(0).getFilterType());

        final Filtered onlyValid = filter(true).filter(contextService, getPolicy(), "context", PIECE, input);
        Assertions.assertEquals(0, onlyValid.getSpans().size());

    }

    @Test
    public void filterAtin() throws Exception {

        // An ATIN (fourth and fifth digits 93) is an ITIN by default and dropped with onlyValidRanges.
        final String input = "the atin is 912-93-1234.";

        final Filtered byDefault = filter(false).filter(contextService, getPolicy(), "context", PIECE, input);
        Assertions.assertEquals(1, byDefault.getSpans().size());
        Assertions.assertEquals(FilterType.ITIN, byDefault.getSpans().get(0).getFilterType());

        final Filtered onlyValid = filter(true).filter(contextService, getPolicy(), "context", PIECE, input);
        Assertions.assertEquals(0, onlyValid.getSpans().size());

    }

    @Test
    public void filterItinInsideValidRanges() throws Exception {

        final ItinFilter filter = filter(true);

        for (final String itin : List.of("912-50-1234", "912-65-1234", "912-70-1234", "912 88 1234", "912901234", "912-92-1234", "912-94-1234", "912-99-1234")) {
            final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "the itin is " + itin + ".");
            Assertions.assertEquals(1, filtered.getSpans().size(), itin);
        }

        for (final String itin : List.of("912-49-1234", "912-66-1234", "912-69-1234", "912-89-1234", "912 93 1234", "912001234")) {
            final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "the itin is " + itin + ".");
            Assertions.assertEquals(0, filtered.getSpans().size(), itin);
        }

    }

    @Test
    public void filterItinLastFour() throws Exception {

        final ItinFilterStrategy strategy = new ItinFilterStrategy();
        strategy.setStrategy(AbstractFilterStrategy.LAST_4);

        final Filtered filtered = filter(false, strategy).filter(contextService, getPolicy(), "context", PIECE, "the itin is 912-70-1234.");
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertEquals("1234", filtered.getSpans().get(0).getReplacement());

    }

    @Test
    public void filterItinMask() throws Exception {

        final ItinFilterStrategy strategy = new ItinFilterStrategy();
        strategy.setStrategy(AbstractFilterStrategy.MASK);

        final Filtered filtered = filter(false, strategy).filter(contextService, getPolicy(), "context", PIECE, "the itin is 912-70-1234.");
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertEquals("***********", filtered.getSpans().get(0).getReplacement());

    }

    @Test
    public void filterItinUsesItinAnonymization() throws Exception {

        final ItinFilterStrategy strategy = new ItinFilterStrategy();
        strategy.setStrategy(AbstractFilterStrategy.RANDOM_REPLACE);

        filter(false, strategy);

        // RANDOM_REPLACE on an ITIN draws from the ITIN anonymization service, not the generic one
        // SSN uses, so a realistic replacement is ITIN-shaped. See ItinFilterStrategyTest.
        Assertions.assertInstanceOf(ItinAnonymizationService.class, strategy.getAnonymizationService());

    }

}
