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
import ai.philterd.phileas.services.filters.regex.PassportNumberFilter;
import ai.philterd.phileas.services.strategies.rules.PassportNumberFilterStrategy;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

import static ai.philterd.phileas.services.strategies.AbstractFilterStrategy.RANDOM_REPLACE;

public class PassportNumberFilterTest extends AbstractFilterTest {

    @Test
    public void filter1() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new PassportNumberFilterStrategy()))
                .withWindowSize(windowSize)
                .build();

        final PassportNumberFilter filter = new PassportNumberFilter(filterConfiguration);

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "the passport number is 036001231.");

        showSpans(filtered.getSpans());

        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 23, 32, FilterType.PASSPORT_NUMBER));
        Assertions.assertEquals("{{{REDACTED-passport-number}}}", filtered.getSpans().get(0).getReplacement());
        Assertions.assertEquals("036001231", filtered.getSpans().get(0).getText());
        Assertions.assertEquals("US", filtered.getSpans().get(0).getClassification());

    }

    @Test
    public void filterAllNumeric() throws Exception {

        // A bare all-numeric 9-digit US passport book number is detected. This number's leading
        // digits ("22") are not one of the specific passport prefixes above, so it is caught only by
        // the all-numeric pattern.

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new PassportNumberFilterStrategy()))
                .withWindowSize(windowSize)
                .build();

        final PassportNumberFilter filter = new PassportNumberFilter(filterConfiguration);

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "the passport number is 223456789.");

        showSpans(filtered.getSpans());

        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 23, 32, FilterType.PASSPORT_NUMBER));
        Assertions.assertEquals("223456789", filtered.getSpans().get(0).getText());
        Assertions.assertEquals("US", filtered.getSpans().get(0).getClassification());

    }

    @Test
    public void filterPrefixNumberWithoutCueWord() throws Exception {

        // A prefix pattern matches any 6 to 10 character token that starts with its digits, such as an order
        // or invoice number, so the token is identified only after the word "passport".

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new PassportNumberFilterStrategy()))
                .withWindowSize(windowSize)
                .build();

        final PassportNumberFilter filter = new PassportNumberFilter(filterConfiguration);

        final Filtered order = filter.filter(contextService, getPolicy(), "context", PIECE, "Order 2012345 shipped");

        showSpans(order.getSpans());

        Assertions.assertEquals(0, order.getSpans().size());

        final Filtered invoice = filter.filter(contextService, getPolicy(), "context", PIECE, "Invoice 0112345678 paid");

        showSpans(invoice.getSpans());

        Assertions.assertEquals(0, invoice.getSpans().size());

    }

    @Test
    public void filterPrefixNumberAfterCueWord() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new PassportNumberFilterStrategy()))
                .withWindowSize(windowSize)
                .build();

        final PassportNumberFilter filter = new PassportNumberFilter(filterConfiguration);

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "Passport no. 2012345");

        showSpans(filtered.getSpans());

        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 13, 20, FilterType.PASSPORT_NUMBER));
        Assertions.assertEquals("2012345", filtered.getSpans().get(0).getText());
        Assertions.assertEquals("US", filtered.getSpans().get(0).getClassification());

    }

    @Test
    public void filterPrefixNumberCueWordOutsideWindow() throws Exception {

        // The cue word counts only within windowSize (3 here) tokens before the value.

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new PassportNumberFilterStrategy()))
                .withWindowSize(windowSize)
                .build();

        final PassportNumberFilter filter = new PassportNumberFilter(filterConfiguration);

        final Filtered outside = filter.filter(contextService, getPolicy(), "context", PIECE, "Passport on file. Order 2012345 shipped");

        showSpans(outside.getSpans());

        Assertions.assertEquals(0, outside.getSpans().size());

        final Filtered inside = filter.filter(contextService, getPolicy(), "context", PIECE, "Passport on file: 2012345");

        showSpans(inside.getSpans());

        Assertions.assertEquals(1, inside.getSpans().size());
        Assertions.assertEquals("2012345", inside.getSpans().get(0).getText());

    }

    @Test
    public void filterPrefixNumberAfterHyphenatedCueWord() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new PassportNumberFilterStrategy()))
                .withWindowSize(windowSize)
                .build();

        final PassportNumberFilter filter = new PassportNumberFilter(filterConfiguration);

        final Filtered number = filter.filter(contextService, getPolicy(), "context", PIECE, "Passport-number 2012345");
        showSpans(number.getSpans());
        Assertions.assertEquals(1, number.getSpans().size());
        Assertions.assertEquals("2012345", number.getSpans().get(0).getText());

        final Filtered no = filter.filter(contextService, getPolicy(), "context", PIECE, "Passport-No: 2012345");
        showSpans(no.getSpans());
        Assertions.assertEquals(1, no.getSpans().size());

    }

    @Test
    public void filterNineDigitPrefixNumberWithoutCueWord() throws Exception {

        // A nine-digit number needs no cue word and keeps its own confidence, 0.55, even when its first two
        // digits are a prefix pattern, which would match it at 0.90.

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new PassportNumberFilterStrategy()))
                .withWindowSize(windowSize)
                .build();

        final PassportNumberFilter filter = new PassportNumberFilter(filterConfiguration);

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "Order 123456789 shipped");

        showSpans(filtered.getSpans());

        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertEquals("123456789", filtered.getSpans().get(0).getText());
        Assertions.assertEquals("US", filtered.getSpans().get(0).getClassification());
        Assertions.assertEquals(0.55, filtered.getSpans().get(0).getConfidence(), 0.0001);

    }

    @Test
    public void filterWithCandidates1() throws Exception {

        final List<String> candidates = List.of("candidate1", "candidate2");

        final PassportNumberFilterStrategy passportNumberFilterStrategy = new PassportNumberFilterStrategy();
        passportNumberFilterStrategy.setStrategy(RANDOM_REPLACE);
        passportNumberFilterStrategy.setAnonymizationCandidates(candidates);

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(passportNumberFilterStrategy))
                .withWindowSize(windowSize)
                .build();

        final PassportNumberFilter filter = new PassportNumberFilter(filterConfiguration);

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "the passport number is 036001231.");
        showSpans(filtered.getSpans());
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(candidates.contains(filtered.getSpans().get(0).getReplacement()));

    }

}