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
import ai.philterd.phileas.model.filtering.Span;
import ai.philterd.phileas.services.filters.regex.ZipCodeFilter;
import ai.philterd.phileas.services.strategies.rules.ZipCodeFilterStrategy;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.util.List;

import static ai.philterd.phileas.services.strategies.AbstractFilterStrategy.RANDOM_REPLACE;

public class ZipCodeFilterTest extends AbstractFilterTest {
    
    @Test
    public void filterZipCode1() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new ZipCodeFilterStrategy()))
                .withWindowSize(windowSize)
                .build();

        final ZipCodeFilter filter = new ZipCodeFilter(filterConfiguration, true, false);

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "the zip is 90210.");
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 11, 16, FilterType.ZIP_CODE));
        Assertions.assertEquals("90210", filtered.getSpans().get(0).getText());

    }

    @Test
    public void filterZipCode2() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new ZipCodeFilterStrategy()))
                .withWindowSize(windowSize)
                .build();

        final ZipCodeFilter filter = new ZipCodeFilter(filterConfiguration, true, false);

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "the zip is 90210abd.");
        Assertions.assertEquals(0, filtered.getSpans().size());

    }

    @Test
    public void filterZipCode3() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new ZipCodeFilterStrategy()))
                .withWindowSize(windowSize)
                .build();

        final ZipCodeFilter filter = new ZipCodeFilter(filterConfiguration, true, false);

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "the zip is 90210 in california.");
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 11, 16, FilterType.ZIP_CODE));

    }

    @Test
    public void filterZipCode4() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new ZipCodeFilterStrategy()))
                .withWindowSize(windowSize)
                .build();

        final ZipCodeFilter filter = new ZipCodeFilter(filterConfiguration, true, false);

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "the zip is 85055 in california.");
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 11, 16, FilterType.ZIP_CODE));

    }

    @Test
    public void filterZipCode5() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new ZipCodeFilterStrategy()))
                .withWindowSize(windowSize)
                .build();

        final ZipCodeFilter filter = new ZipCodeFilter(filterConfiguration, true, false);

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "the zip is 90213-1544 in california.");
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 11, 21, FilterType.ZIP_CODE));

    }

    @Test
    public void filterZipCode6() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new ZipCodeFilterStrategy()))
                .withWindowSize(windowSize)
                .build();

        final ZipCodeFilter filter = new ZipCodeFilter(filterConfiguration, true, false);

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "George Washington was president and his ssn was 123-45-6789 and he lived in 90210.");
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 76, 81, FilterType.ZIP_CODE));

    }

    @Test
    public void filterZipCode7() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new ZipCodeFilterStrategy()))
                .withWindowSize(windowSize)
                .build();

        final ZipCodeFilter filter = new ZipCodeFilter(filterConfiguration, true, false);

        // Tests whole word only.
        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "George Washington was president and his ssn was 123-45-6789 and he lived in 9021032.");
        Assertions.assertEquals(0, filtered.getSpans().size());

    }

    @Test
    public void filterZipCode8() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new ZipCodeFilterStrategy()))
                .withWindowSize(windowSize)
                .build();

        final ZipCodeFilter filter = new ZipCodeFilter(filterConfiguration, true, false);

        // Tests whole word only.
        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "George Washington was president and his ssn was 123-45-6789 and he lived in 90210-1234.");
        Assertions.assertEquals(1, filtered.getSpans().size());

    }

    @Test
    public void filterZipCode9() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new ZipCodeFilterStrategy()))
                .withWindowSize(windowSize)
                .build();

        final ZipCodeFilter filter = new ZipCodeFilter(filterConfiguration, false, false);

        // Tests without delimiter.
        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "George Washington was president and his ssn was 123-45-6789 and he lived in 902101234.");
        Assertions.assertEquals(1, filtered.getSpans().size());

    }

    @Test
    public void filterZipCode10() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new ZipCodeFilterStrategy()))
                .withWindowSize(windowSize)
                .build();

        final ZipCodeFilter filter = new ZipCodeFilter(filterConfiguration, true, false);

        // Tests without delimiter.
        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "George Washington was president and his ssn was 123-45-6789 and he lived in 902101234.");
        Assertions.assertEquals(0, filtered.getSpans().size());

    }

    @Test
    public void filterZipCodeAndValidate1() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new ZipCodeFilterStrategy()))
                .withWindowSize(windowSize)
                .build();

        final ZipCodeFilter filter = new ZipCodeFilter(filterConfiguration, true, true);

        // 09865 is an invalid zip code.
        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "George Washington lived in 90210 and 09865.");
        Assertions.assertEquals(2, filtered.getSpans().size());

        for(final Span span : filtered.getSpans()) {

            Assertions.assertTrue(span.getText().equals("90210") || span.getText().equals("09865"));

            if(span.getText().equals("90210")) {
                Assertions.assertTrue(span.isApplied());
            } else {
                Assertions.assertFalse(span.isApplied());
            }

        }

    }

    @Test
    public void filterZipCodeAndValidate2() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new ZipCodeFilterStrategy()))
                .withWindowSize(windowSize)
                .build();

        final ZipCodeFilter filter = new ZipCodeFilter(filterConfiguration, true, true);

        // 09865 is an invalid zip code.
        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "George Washington lived in 90210-1234 and 09865.");
        Assertions.assertEquals(2, filtered.getSpans().size());

        for(final Span span : filtered.getSpans()) {

            Assertions.assertTrue(span.getText().equals("90210-1234") || span.getText().equals("09865"));

            if(span.getText().equals("90210-1234")) {
                Assertions.assertTrue(span.isApplied());
            } else {
                Assertions.assertFalse(span.isApplied());
            }

        }

    }

    @Test
    public void filterZipCodeAndValidate3() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new ZipCodeFilterStrategy()))
                .withWindowSize(windowSize)
                .build();

        final ZipCodeFilter filter = new ZipCodeFilter(filterConfiguration, false, true);

        // 09865 is an invalid zip code.
        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "George Washington lived in 902101234 and 09865.");
        Assertions.assertEquals(2, filtered.getSpans().size());

        for(final Span span : filtered.getSpans()) {

            Assertions.assertTrue(span.getText().equals("902101234") || span.getText().equals("09865"));

            if(span.getText().equals("902101234")) {
                Assertions.assertTrue(span.isApplied());
            } else {
                Assertions.assertFalse(span.isApplied());
            }

        }

    }

    @Test
    public void filterZipCodeAndValidate4() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new ZipCodeFilterStrategy()))
                .withWindowSize(windowSize)
                .build();

        final ZipCodeFilter filter = new ZipCodeFilter(filterConfiguration, true, true);

        // 09865 is an invalid zip code.
        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "George Washington lived in 90210-1234 and 09865-1234.");
        Assertions.assertEquals(2, filtered.getSpans().size());

        for(final Span span : filtered.getSpans()) {

            Assertions.assertTrue(span.getText().equals("90210-1234") || span.getText().equals("09865-1234"));

            if(span.getText().equals("90210-1234")) {
                Assertions.assertTrue(span.isApplied());
            } else {
                Assertions.assertFalse(span.isApplied());
            }

        }

    }

    @Test
    public void filterWithCandidates1() throws Exception {

        final List<String> candidates = List.of("candidate1", "candidate2");

        final ZipCodeFilterStrategy zipCodeFilterStrategy = new ZipCodeFilterStrategy();
        zipCodeFilterStrategy.setStrategy(RANDOM_REPLACE);
        zipCodeFilterStrategy.setAnonymizationCandidates(candidates);

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(zipCodeFilterStrategy))
                .withWindowSize(windowSize)
                .build();

        final ZipCodeFilter filter = new ZipCodeFilter(filterConfiguration, false, false);

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "the zip code is 90210");
        showSpans(filtered.getSpans());
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(candidates.contains(filtered.getSpans().get(0).getReplacement()));

    }

    @Test
    public void filterWithNoCandidates1() throws Exception {

        final ZipCodeFilterStrategy zipCodeFilterStrategy = new ZipCodeFilterStrategy();
        zipCodeFilterStrategy.setStrategy(RANDOM_REPLACE);

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(zipCodeFilterStrategy))
                .withWindowSize(windowSize)
                .build();

        final ZipCodeFilter filter = new ZipCodeFilter(filterConfiguration, false, false);

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "the zip code is 90210");
        showSpans(filtered.getSpans());
        Assertions.assertEquals(1, filtered.getSpans().size());
        // With no candidates, the default REALISTIC method replaces the zip code with another zip code.
        Assertions.assertTrue(filtered.getSpans().get(0).getReplacement().matches("\\d{5}"), filtered.getSpans().get(0).getReplacement());
        Assertions.assertNotEquals("90210", filtered.getSpans().get(0).getReplacement());

    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void filterDoesNotMatchDigitsJoinedByAHyphen(final boolean requireDelimiter) throws Exception {

        final ZipCodeFilter filter = new ZipCodeFilter(getFilterConfiguration(), requireDelimiter, false);

        final String input = "Re: Matter No. 2026-00847\nSend it to Boston, MA 02116.\nReference FR-48213, invoice INV-12345, case 1:26-cv-00847.";
        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, input);

        showSpans(filtered.getSpans());
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertEquals("02116", filtered.getSpans().get(0).getText());

    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void filterMatchesBothZipCodesOfARange(final boolean requireDelimiter) throws Exception {

        final ZipCodeFilter filter = new ZipCodeFilter(getFilterConfiguration(), requireDelimiter, false);

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "Serving ZIP codes 02116-02118 only.");

        showSpans(filtered.getSpans());
        Assertions.assertEquals(2, filtered.getSpans().size());
        Assertions.assertEquals("02116", filtered.getSpans().get(0).getText());
        Assertions.assertEquals("02118", filtered.getSpans().get(1).getText());

    }

    @Test
    public void filterDoesNotMatchDigitsJoinedByANonBreakingHyphen() throws Exception {

        final ZipCodeFilter filter = new ZipCodeFilter(getFilterConfiguration(), true, false);

        final String input = "Reference FR" + (char) 0x2011 + "48213 was filed.";
        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, input);

        Assertions.assertEquals(0, filtered.getSpans().size());

    }

    @Test
    public void filterAndValidateDoesNotMatchARealZipCodeJoinedByAHyphen() throws Exception {

        // 48213 is a real zip code (Detroit), so validation alone does not drop it.
        final ZipCodeFilter filter = new ZipCodeFilter(getFilterConfiguration(), true, true);

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "Reference FR-48213 was filed in Boston, MA 02116.");

        showSpans(filtered.getSpans());
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertEquals("02116", filtered.getSpans().get(0).getText());
        Assertions.assertTrue(filtered.getSpans().get(0).isApplied());

    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "Send it to Boston, MA 02116.|02116",
            "Send it to Boston, MA 02116-1234.|02116-1234",
            "Send it to Boston 02116 today.|02116",
            "zip:02116|02116",
            "(02116)|02116",
            "Boston, MA, 02116-1234|02116-1234"
    })
    public void filterMatchesZipCodesInOrdinaryUse(final String input, final String zipCode) throws Exception {

        for(final boolean requireDelimiter : new boolean[] {true, false}) {

            final ZipCodeFilter filter = new ZipCodeFilter(getFilterConfiguration(), requireDelimiter, false);
            final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, input);

            Assertions.assertEquals(1, filtered.getSpans().size(), input);
            Assertions.assertEquals(zipCode, filtered.getSpans().get(0).getText(), input);

        }

    }

    private FilterConfiguration getFilterConfiguration() throws IOException {

        return new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new ZipCodeFilterStrategy()))
                .withWindowSize(windowSize)
                .build();

    }

}
