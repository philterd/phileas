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
import ai.philterd.phileas.model.filtering.Span;
import ai.philterd.phileas.services.filters.regex.CueWordFilter;
import ai.philterd.phileas.services.filters.regex.HealthPlanMemberIdFilter;
import ai.philterd.phileas.services.strategies.rules.HealthPlanMemberIdFilterStrategy;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

// All member IDs in these tests are invented.
public class HealthPlanMemberIdFilterTest extends AbstractFilterTest {

    private HealthPlanMemberIdFilter filter(final List<String> patterns, final List<String> cueWords,
                                            final boolean defaultCueWords) {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new HealthPlanMemberIdFilterStrategy()))
                .withWindowSize(windowSize)
                .build();

        return new HealthPlanMemberIdFilter(filterConfiguration, patterns, cueWords, defaultCueWords);

    }

    private HealthPlanMemberIdFilter filter() {
        return filter(null, null, true);
    }

    private List<Span> spans(final HealthPlanMemberIdFilter filter, final String text) throws Exception {
        return filter.filter(contextService, getPolicy(), "context", PIECE, text).getSpans();
    }

    @Test
    public void aMemberIdAfterACueWordIsMatchedAndTheCueWordIsNot() throws Exception {

        final Filtered filtered = filter().filter(contextService, getPolicy(), "context", PIECE, "Member ID: XYZ123456789");
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 11, 23, FilterType.HEALTH_PLAN_MEMBER_ID));
        Assertions.assertEquals(CueWordFilter.YIELDS_TO, filtered.getSpans().get(0).getYieldsTo());

    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "member id XYZ123456789             | XYZ123456789",
            "MEMBER ID# XYZ123456789            | XYZ123456789",
            "Member #: W1234-5678               | W1234-5678",
            "Member#XYZ123456789                | XYZ123456789",
            "Subscriber Number: 0012345678      | 0012345678",
            "(Medicaid ID: MD12345678)          | MD12345678",
            "Insurance ID is XYZ123456789       | XYZ123456789",
            "Beneficiary ID - 98765432A         | 98765432A"
    })
    public void eachDefaultCueWordFormIsMatched(final String text, final String expected) throws Exception {

        final List<Span> spans = spans(filter(), text);
        Assertions.assertEquals(1, spans.size(), text);
        Assertions.assertEquals(expected, spans.get(0).getText(), text);

    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Account XYZ123456789",                                  // No cue word.
            "XYZ123456789 is the member ID",                         // The cue word comes after the value.
            "Member ID was written on the form as XYZ123456789",     // The cue word is outside the window.
            "Member ID: ABCDEFGHIJ",                                 // No digit.
            "Member ID: A1234",                                      // Too short.
            "Member ID: A123456789012345678901",                     // Too long.
            "Membership ID: XYZ123456789",                           // Not the whole word.
            "Remember ID XYZ123456789"                               // Not the whole word.
    })
    public void valuesWithoutACueWordBeforeThemAreNotMatched(final String text) throws Exception {
        Assertions.assertEquals(0, spans(filter(), text).size(), text);
    }

    @Test
    public void patternsReplaceTheDefaultPattern() throws Exception {

        final HealthPlanMemberIdFilter filter = filter(List.of("\\b[A-Z]{3}[0-9]{9}\\b"), null, true);

        Assertions.assertEquals(1, spans(filter, "Member ID: XYZ123456789").size());
        Assertions.assertEquals(0, spans(filter, "Member ID: 12345678").size());

    }

    @Test
    public void cueWordsAddToTheDefaults() throws Exception {

        final HealthPlanMemberIdFilter filter = filter(null, List.of("Plan ID"), true);

        Assertions.assertEquals(1, spans(filter, "Plan ID: XYZ123456789").size());
        Assertions.assertEquals(1, spans(filter, "Member ID: XYZ123456789").size());

    }

    @Test
    public void cueWordsReplaceTheDefaultsWhenDefaultCueWordsIsFalse() throws Exception {

        final HealthPlanMemberIdFilter filter = filter(null, List.of("Plan ID"), false);

        Assertions.assertEquals(1, spans(filter, "Plan ID: XYZ123456789").size());
        Assertions.assertEquals(0, spans(filter, "Member ID: XYZ123456789").size());

    }

    @Test
    public void defaultCueWordsFalseWithNoCueWordsIsRefused() {

        Assertions.assertThrows(IllegalArgumentException.class, () -> filter(null, null, false));
        Assertions.assertThrows(IllegalArgumentException.class, () -> filter(null, List.of(), false));

    }

}
