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
import ai.philterd.phileas.services.anonymization.NpiAnonymizationService;
import ai.philterd.phileas.services.filters.regex.NpiFilter;
import ai.philterd.phileas.services.strategies.rules.NpiFilterStrategy;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

// 1234567893 is the example NPI CMS publishes with the check digit algorithm. Other numbers are derived from it.
public class NpiFilterTest extends AbstractFilterTest {

    private NpiFilter filter() {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new NpiFilterStrategy()))
                .withWindowSize(windowSize)
                .build();

        return new NpiFilter(filterConfiguration);

    }

    @Test
    public void filterNpi() throws Exception {

        final Filtered filtered = filter().filter(contextService, getPolicy(), "context", PIECE, "the npi is 1234567893.");
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 11, 21, FilterType.NPI));
        Assertions.assertEquals("1234567893", filtered.getSpans().get(0).getText());

    }

    @Test
    public void anNpiFailingTheNpiCheckDigitIsNotMatchedEvenIfItPassesPlainLuhn() throws Exception {

        // 1234567897 passes the plain Luhn check but not the NPI check, which prefixes 80840.
        final Filtered filtered = filter().filter(contextService, getPolicy(), "context", PIECE, "the npi is 1234567897.");
        Assertions.assertEquals(0, filtered.getSpans().size());

    }

    @ParameterizedTest
    @ValueSource(strings = {
            "the number is 3234567893.",     // Does not begin with 1 or 2.
            "the number is 123456789.",      // Nine digits.
            "the number is 12345678931.",    // Eleven digits.
            "the number is 91234567893.",    // Part of a longer number.
            "the number is A1234567893."     // Part of a longer token.
    })
    public void lookAlikeNumbersAreNotMatched(final String text) throws Exception {

        final Filtered filtered = filter().filter(contextService, getPolicy(), "context", PIECE, text);
        Assertions.assertEquals(0, filtered.getSpans().size(), text);

    }

    @Test
    public void checkDigit() {

        Assertions.assertEquals(3, NpiFilter.checkDigit("123456789"));
        Assertions.assertTrue(NpiFilter.isValid("1234567893"));
        Assertions.assertFalse(NpiFilter.isValid("1234567893 "));
        Assertions.assertFalse(NpiFilter.isValid(null));

    }

    @Test
    public void randomReplacementIsAnNpi() {

        final NpiAnonymizationService service = new NpiAnonymizationService(random);

        for(int i = 0; i < 100; i++) {
            final String replacement = service.anonymize("1234567893");
            Assertions.assertTrue(NpiFilter.isValid(replacement), replacement);
            Assertions.assertNotEquals("1234567893", replacement);
        }

    }

}
