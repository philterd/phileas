/*
 *     Copyright 2025 Philterd, LLC @ https://www.philterd.ai
 *
 * Licensed under the Apache License, Version 2.0 (the "License", attributes);
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

import ai.philterd.phileas.model.enums.FilterType;
import ai.philterd.phileas.model.objects.FilterResult;
import ai.philterd.phileas.model.objects.Span;
import ai.philterd.phileas.services.context.DefaultContextService;
import ai.philterd.phileas.services.strategies.rules.ZipCodeFilterStrategy;
import ai.philterd.phileas.services.anonymization.ZipCodeAnonymizationService;
import ai.philterd.phileas.services.filters.regex.ZipCodeFilter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

public class ZipCodeFilterTest extends AbstractFilterTest {
    
    @Test
    public void filterZipCode1() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new ZipCodeFilterStrategy()))
                .withAnonymizationService(new ZipCodeAnonymizationService(new DefaultContextService()))
                .withWindowSize(windowSize)
                .build();

        final ZipCodeFilter filter = new ZipCodeFilter(filterConfiguration, true, false);

        final FilterResult filterResult = filter.filter(getPolicy(), "context", PIECE, "the zip is 90210.", attributes);
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 11, 16, FilterType.ZIP_CODE));
        Assertions.assertEquals("90210", filterResult.getSpans().get(0).getText());

    }

    @Test
    public void filterZipCode2() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new ZipCodeFilterStrategy()))
                .withAnonymizationService(new ZipCodeAnonymizationService(new DefaultContextService()))
                .withWindowSize(windowSize)
                .build();

        final ZipCodeFilter filter = new ZipCodeFilter(filterConfiguration, true, false);

        final FilterResult filterResult = filter.filter(getPolicy(), "context", PIECE, "the zip is 90210abd.", attributes);
        Assertions.assertEquals(0, filterResult.getSpans().size());

    }

    @Test
    public void filterZipCode3() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new ZipCodeFilterStrategy()))
                .withAnonymizationService(new ZipCodeAnonymizationService(new DefaultContextService()))
                .withWindowSize(windowSize)
                .build();

        final ZipCodeFilter filter = new ZipCodeFilter(filterConfiguration, true, false);

        final FilterResult filterResult = filter.filter(getPolicy(), "context", PIECE, "the zip is 90210 in california.", attributes);
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 11, 16, FilterType.ZIP_CODE));

    }

    @Test
    public void filterZipCode4() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new ZipCodeFilterStrategy()))
                .withAnonymizationService(new ZipCodeAnonymizationService(new DefaultContextService()))
                .withWindowSize(windowSize)
                .build();

        final ZipCodeFilter filter = new ZipCodeFilter(filterConfiguration, true, false);

        final FilterResult filterResult = filter.filter(getPolicy(), "context", PIECE, "the zip is 85055 in california.", attributes);
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 11, 16, FilterType.ZIP_CODE));

    }

    @Test
    public void filterZipCode5() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new ZipCodeFilterStrategy()))
                .withAnonymizationService(new ZipCodeAnonymizationService(new DefaultContextService()))
                .withWindowSize(windowSize)
                .build();

        final ZipCodeFilter filter = new ZipCodeFilter(filterConfiguration, true, false);

        final FilterResult filterResult = filter.filter(getPolicy(), "context", PIECE, "the zip is 90213-1544 in california.", attributes);
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 11, 21, FilterType.ZIP_CODE));

    }

    @Test
    public void filterZipCode6() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new ZipCodeFilterStrategy()))
                .withAnonymizationService(new ZipCodeAnonymizationService(new DefaultContextService()))
                .withWindowSize(windowSize)
                .build();

        final ZipCodeFilter filter = new ZipCodeFilter(filterConfiguration, true, false);

        final FilterResult filterResult = filter.filter(getPolicy(), "context", PIECE, "George Washington was president and his ssn was 123-45-6789 and he lived in 90210.", attributes);
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 76, 81, FilterType.ZIP_CODE));

    }

    @Test
    public void filterZipCode7() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new ZipCodeFilterStrategy()))
                .withAnonymizationService(new ZipCodeAnonymizationService(new DefaultContextService()))
                .withWindowSize(windowSize)
                .build();

        final ZipCodeFilter filter = new ZipCodeFilter(filterConfiguration, true, false);

        // Tests whole word only.
        final FilterResult filterResult = filter.filter(getPolicy(), "context", PIECE, "George Washington was president and his ssn was 123-45-6789 and he lived in 9021032.", attributes);
        Assertions.assertEquals(0, filterResult.getSpans().size());

    }

    @Test
    public void filterZipCode8() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new ZipCodeFilterStrategy()))
                .withAnonymizationService(new ZipCodeAnonymizationService(new DefaultContextService()))
                .withWindowSize(windowSize)
                .build();

        final ZipCodeFilter filter = new ZipCodeFilter(filterConfiguration, true, false);

        // Tests whole word only.
        final FilterResult filterResult = filter.filter(getPolicy(), "context", PIECE, "George Washington was president and his ssn was 123-45-6789 and he lived in 90210-1234.", attributes);
        Assertions.assertEquals(1, filterResult.getSpans().size());

    }

    @Test
    public void filterZipCode9() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new ZipCodeFilterStrategy()))
                .withAnonymizationService(new ZipCodeAnonymizationService(new DefaultContextService()))
                .withWindowSize(windowSize)
                .build();

        final ZipCodeFilter filter = new ZipCodeFilter(filterConfiguration, false, false);

        // Tests without delimiter.
        final FilterResult filterResult = filter.filter(getPolicy(), "context", PIECE, "George Washington was president and his ssn was 123-45-6789 and he lived in 902101234.", attributes);
        Assertions.assertEquals(1, filterResult.getSpans().size());

    }

    @Test
    public void filterZipCode10() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new ZipCodeFilterStrategy()))
                .withAnonymizationService(new ZipCodeAnonymizationService(new DefaultContextService()))
                .withWindowSize(windowSize)
                .build();

        final ZipCodeFilter filter = new ZipCodeFilter(filterConfiguration, true, false);

        // Tests without delimiter.
        final FilterResult filterResult = filter.filter(getPolicy(), "context", PIECE, "George Washington was president and his ssn was 123-45-6789 and he lived in 902101234.", attributes);
        Assertions.assertEquals(0, filterResult.getSpans().size());

    }

    @Test
    public void filterZipCodeAndValidate1() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new ZipCodeFilterStrategy()))
                .withAnonymizationService(new ZipCodeAnonymizationService(new DefaultContextService()))
                .withWindowSize(windowSize)
                .build();

        final ZipCodeFilter filter = new ZipCodeFilter(filterConfiguration, true, true);

        // 09865 is an invalid zip code.
        final FilterResult filterResult = filter.filter(getPolicy(), "context", PIECE, "George Washington lived in 90210 and 09865.", attributes);
        Assertions.assertEquals(2, filterResult.getSpans().size());

        for(final Span span : filterResult.getSpans()) {

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
                .withAnonymizationService(new ZipCodeAnonymizationService(new DefaultContextService()))
                .withWindowSize(windowSize)
                .build();

        final ZipCodeFilter filter = new ZipCodeFilter(filterConfiguration, true, true);

        // 09865 is an invalid zip code.
        final FilterResult filterResult = filter.filter(getPolicy(), "context", PIECE, "George Washington lived in 90210-1234 and 09865.", attributes);
        Assertions.assertEquals(2, filterResult.getSpans().size());

        for(final Span span : filterResult.getSpans()) {

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
                .withAnonymizationService(new ZipCodeAnonymizationService(new DefaultContextService()))
                .withWindowSize(windowSize)
                .build();

        final ZipCodeFilter filter = new ZipCodeFilter(filterConfiguration, false, true);

        // 09865 is an invalid zip code.
        final FilterResult filterResult = filter.filter(getPolicy(), "context", PIECE, "George Washington lived in 902101234 and 09865.", attributes);
        Assertions.assertEquals(2, filterResult.getSpans().size());

        for(final Span span : filterResult.getSpans()) {

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
                .withAnonymizationService(new ZipCodeAnonymizationService(new DefaultContextService()))
                .withWindowSize(windowSize)
                .build();

        final ZipCodeFilter filter = new ZipCodeFilter(filterConfiguration, true, true);

        // 09865 is an invalid zip code.
        final FilterResult filterResult = filter.filter(getPolicy(), "context", PIECE, "George Washington lived in 90210-1234 and 09865-1234.", attributes);
        Assertions.assertEquals(2, filterResult.getSpans().size());

        for(final Span span : filterResult.getSpans()) {

            Assertions.assertTrue(span.getText().equals("90210-1234") || span.getText().equals("09865-1234"));

            if(span.getText().equals("90210-1234")) {
                Assertions.assertTrue(span.isApplied());
            } else {
                Assertions.assertFalse(span.isApplied());
            }

        }

    }

    @Test
    public void filterZipCodeAndValidate5() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new ZipCodeFilterStrategy()))
                .withAnonymizationService(new ZipCodeAnonymizationService(new DefaultContextService()))
                .withWindowSize(windowSize)
                .build();

        final ZipCodeFilter filter = new ZipCodeFilter(filterConfiguration, false, true);

        // 09865 is an invalid zip code.
        final FilterResult filterResult = filter.filter(getPolicy(), "context", PIECE, "George Washington lived in 902101234 and 098651234.", attributes);
        Assertions.assertEquals(2, filterResult.getSpans().size());

        for(final Span span : filterResult.getSpans()) {

            Assertions.assertTrue(span.getText().equals("902101234") || span.getText().equals("098651234"));

            if(span.getText().equals("902101234")) {
                Assertions.assertTrue(span.isApplied());
            } else {
                Assertions.assertFalse(span.isApplied());
            }

        }

    }

}
