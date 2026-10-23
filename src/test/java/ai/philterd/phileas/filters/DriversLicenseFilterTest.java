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
import ai.philterd.phileas.services.filters.regex.DriversLicenseFilter;
import ai.philterd.phileas.services.strategies.rules.DriversLicenseFilterStrategy;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static ai.philterd.phileas.services.strategies.AbstractFilterStrategy.RANDOM_REPLACE;

public class DriversLicenseFilterTest extends AbstractFilterTest {

    @Test
    public void filterWithCandidates1() throws Exception {

        final List<String> candidates = List.of("candidate1", "candidate2");

        final DriversLicenseFilterStrategy driversLicenseFilterStrategy = new DriversLicenseFilterStrategy();
        driversLicenseFilterStrategy.setStrategy(RANDOM_REPLACE);
        driversLicenseFilterStrategy.setAnonymizationCandidates(candidates);

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(driversLicenseFilterStrategy))
                .withWindowSize(windowSize)
                .build();

        final DriversLicenseFilter filter = new DriversLicenseFilter(filterConfiguration);

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "the license number is 123456789.");
        showSpans(filtered.getSpans());
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(candidates.contains(filtered.getSpans().get(0).getReplacement()));

    }

    @Test
    public void filterThirteenCharacter() throws Exception {

        // A 13-character license (one letter followed by 12 digits, e.g. Florida/Maryland/Michigan)
        // is detected as a driver's-license number.

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new DriversLicenseFilterStrategy()))
                .withWindowSize(windowSize)
                .build();

        final DriversLicenseFilter filter = new DriversLicenseFilter(filterConfiguration);

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "the license is A123456789012.");

        showSpans(filtered.getSpans());

        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 15, 28, FilterType.DRIVERS_LICENSE_NUMBER));
        Assertions.assertEquals("A123456789012", filtered.getSpans().get(0).getText());

    }

    private DriversLicenseFilter getFilter() {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new DriversLicenseFilterStrategy()))
                .withWindowSize(windowSize)
                .build();

        return new DriversLicenseFilter(filterConfiguration);

    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Address: 1234 Oak Street, Austin, TX 78701.",
            "The visit lasted 45 minutes and cost 2500 dollars.",
            "the number is 123456789.",
            "Account 12345678 was closed.",
            "Call 1234567890 for details.",
            "The reference is 12-3456789.",
            "Order 1234-56-7890 shipped.",
            "Hemoglobin 1350 mg per dL today.",
            "Volume 2500 dl recorded."
    })
    public void numbersWithoutContextAreNotLicenses(final String input) throws Exception {

        final Filtered filtered = getFilter().filter(contextService, getPolicy(), "context", PIECE, input);

        showSpans(filtered.getSpans());

        Assertions.assertEquals(0, filtered.getSpans().size());

    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Driver's license 12345678 on file.",
            "drivers license number: 12345678",
            "DL# 12345678",
            "DLN 12345678",
            "D.L. 12345678 issued in Texas.",
            "Licence no. 12345678.",
            "12345678 is the license on file."
    })
    public void numbersWithContextAreLicenses(final String input) throws Exception {

        final Filtered filtered = getFilter().filter(contextService, getPolicy(), "context", PIECE, input);

        showSpans(filtered.getSpans());

        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertEquals("12345678", filtered.getSpans().get(0).getText());
        Assertions.assertEquals(FilterType.DRIVERS_LICENSE_NUMBER, filtered.getSpans().get(0).getFilterType());

    }

    @Test
    public void contextOutsideTheWindowIsNotEnough() throws Exception {

        final Filtered filtered = getFilter().filter(contextService, getPolicy(), "context", PIECE,
                "The license was renewed and then on a later visit the patient paid 2500 dollars.");

        showSpans(filtered.getSpans());

        Assertions.assertEquals(0, filtered.getSpans().size());

    }

    @ParameterizedTest
    @ValueSource(strings = {
            "License D1234567 on file.",
            "Here is D1234567 for you."
    })
    public void numbersWithALetterNeedNoContext(final String input) throws Exception {

        final Filtered filtered = getFilter().filter(contextService, getPolicy(), "context", PIECE, input);

        showSpans(filtered.getSpans());

        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertEquals("D1234567", filtered.getSpans().get(0).getText());

    }

    @Test
    public void longerNumberIsNotMatchedInPart() throws Exception {

        // An alternation without a group dropped the word boundary from one side, so the last nine
        // digits of a longer number matched.
        final Filtered filtered = getFilter().filter(contextService, getPolicy(), "context", PIECE,
                "license 12345678901234567");

        showSpans(filtered.getSpans());

        Assertions.assertEquals(0, filtered.getSpans().size());

    }

    @Test
    public void ohioAndNewYorkFormats() throws Exception {

        final Filtered ohio = getFilter().filter(contextService, getPolicy(), "context", PIECE, "Here is AB123456 now.");
        Assertions.assertEquals(1, ohio.getSpans().size());
        Assertions.assertEquals("AB123456", ohio.getSpans().get(0).getText());

        final Filtered newYork = getFilter().filter(contextService, getPolicy(), "context", PIECE, "license 123-456-789 now.");
        Assertions.assertEquals(1, newYork.getSpans().size());
        Assertions.assertEquals("123-456-789", newYork.getSpans().get(0).getText());

    }

}
