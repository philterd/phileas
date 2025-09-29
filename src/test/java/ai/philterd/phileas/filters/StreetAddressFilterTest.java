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
import ai.philterd.phileas.model.services.DefaultContextService;
import ai.philterd.phileas.services.anonymization.StreetAddressAnonymizationService;
import ai.philterd.phileas.services.filters.regex.StreetAddressFilter;
import ai.philterd.phileas.services.strategies.rules.StreetAddressFilterStrategy;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

public class StreetAddressFilterTest extends AbstractFilterTest {

    @Test
    public void filter1() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new StreetAddressFilterStrategy()))
                .withAnonymizationService(new StreetAddressAnonymizationService(new DefaultContextService()))
                .withWindowSize(windowSize)
                .build();

        final StreetAddressFilter filter = new StreetAddressFilter(filterConfiguration);

        final FilterResult filterResult = filter.filter(getPolicy(), "context",  "documentid", PIECE, "lived at 100 Main St", attributes);

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 9, 20, FilterType.STREET_ADDRESS));

    }

    @Test
    public void filter2() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new StreetAddressFilterStrategy()))
                .withAnonymizationService(new StreetAddressAnonymizationService(new DefaultContextService()))
                .withWindowSize(windowSize)
                .build();

        final StreetAddressFilter filter = new StreetAddressFilter(filterConfiguration);

        final FilterResult filterResult = filter.filter(getPolicy(), "context",  "documentid", PIECE, "lived at 100 S Main St", attributes);

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 9, 22, FilterType.STREET_ADDRESS));

    }

    @Test
    public void filter3() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new StreetAddressFilterStrategy()))
                .withAnonymizationService(new StreetAddressAnonymizationService(new DefaultContextService()))
                .withWindowSize(windowSize)
                .build();

        final StreetAddressFilter filter = new StreetAddressFilter(filterConfiguration);

        final FilterResult filterResult = filter.filter(getPolicy(), "context",  "documentid", PIECE, "lived at 100 South Main St", attributes);

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 9, 26, FilterType.STREET_ADDRESS));

    }

    @Test
    public void filter4() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new StreetAddressFilterStrategy()))
                .withAnonymizationService(new StreetAddressAnonymizationService(new DefaultContextService()))
                .withWindowSize(windowSize)
                .build();

        final StreetAddressFilter filter = new StreetAddressFilter(filterConfiguration);

        final FilterResult filterResult = filter.filter(getPolicy(), "context",  "documentid", PIECE, "lived at 1000 Main Street", attributes);

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 9, 25, FilterType.STREET_ADDRESS));

    }

    @Test
    public void filter5() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new StreetAddressFilterStrategy()))
                .withAnonymizationService(new StreetAddressAnonymizationService(new DefaultContextService()))
                .withWindowSize(windowSize)
                .build();

        final StreetAddressFilter filter = new StreetAddressFilter(filterConfiguration);

        final FilterResult filterResult = filter.filter(getPolicy(), "context",  "documentid", PIECE, "North 2800 Clay Edwards Drive", attributes);

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 6, 29, FilterType.STREET_ADDRESS));

    }

    @Test
    public void filter6() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new StreetAddressFilterStrategy()))
                .withAnonymizationService(new StreetAddressAnonymizationService(new DefaultContextService()))
                .withWindowSize(windowSize)
                .build();

        final StreetAddressFilter filter = new StreetAddressFilter(filterConfiguration);

        final FilterResult filterResult = filter.filter(getPolicy(), "context",  "documentid", PIECE, "14 Southampton St.", attributes);

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 0, 18, FilterType.STREET_ADDRESS));

    }

    @Test
    public void filter7() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new StreetAddressFilterStrategy()))
                .withAnonymizationService(new StreetAddressAnonymizationService(new DefaultContextService()))
                .withWindowSize(windowSize)
                .build();

        final StreetAddressFilter filter = new StreetAddressFilter(filterConfiguration);

        final FilterResult filterResult = filter.filter(getPolicy(), "context",  "documentid", PIECE, "22 Newport Drive", attributes);

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 0, 16, FilterType.STREET_ADDRESS));

    }

    @Test
    public void filter8() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new StreetAddressFilterStrategy()))
                .withAnonymizationService(new StreetAddressAnonymizationService(new DefaultContextService()))
                .withWindowSize(windowSize)
                .build();

        final StreetAddressFilter filter = new StreetAddressFilter(filterConfiguration);

        final FilterResult filterResult = filter.filter(getPolicy(), "context",  "documentid", PIECE, "78 Glendale Street", attributes);

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 0, 18, FilterType.STREET_ADDRESS));

    }

    @Test
    public void filter9() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new StreetAddressFilterStrategy()))
                .withAnonymizationService(new StreetAddressAnonymizationService(new DefaultContextService()))
                .withWindowSize(windowSize)
                .build();

        final StreetAddressFilter filter = new StreetAddressFilter(filterConfiguration);

        final FilterResult filterResult = filter.filter(getPolicy(), "context",  "documentid", PIECE, "6 Berkshire Court", attributes);

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 0, 17, FilterType.STREET_ADDRESS));

    }

    @Test
    public void filter10() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new StreetAddressFilterStrategy()))
                .withAnonymizationService(new StreetAddressAnonymizationService(new DefaultContextService()))
                .withWindowSize(windowSize)
                .build();

        final StreetAddressFilter filter = new StreetAddressFilter(filterConfiguration);

        final FilterResult filterResult = filter.filter(getPolicy(), "context",  "documentid", PIECE, "291 North Pawnee Ave.", attributes);

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 0, 21, FilterType.STREET_ADDRESS));

    }

    @Test
    public void filter11() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new StreetAddressFilterStrategy()))
                .withAnonymizationService(new StreetAddressAnonymizationService(new DefaultContextService()))
                .withWindowSize(windowSize)
                .build();

        final StreetAddressFilter filter = new StreetAddressFilter(filterConfiguration);

        final FilterResult filterResult = filter.filter(getPolicy(), "context",  "documentid", PIECE, "468 William Street", attributes);

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 0, 18, FilterType.STREET_ADDRESS));

    }

    @Test
    public void filter12() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new StreetAddressFilterStrategy()))
                .withAnonymizationService(new StreetAddressAnonymizationService(new DefaultContextService()))
                .withWindowSize(windowSize)
                .build();

        final StreetAddressFilter filter = new StreetAddressFilter(filterConfiguration);

        final FilterResult filterResult = filter.filter(getPolicy(), "context",  "documentid", PIECE, "291 6th Dr.", attributes);

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 0, 11, FilterType.STREET_ADDRESS));

    }

    @Test
    public void filter13() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new StreetAddressFilterStrategy()))
                .withAnonymizationService(new StreetAddressAnonymizationService(new DefaultContextService()))
                .withWindowSize(windowSize)
                .build();

        final StreetAddressFilter filter = new StreetAddressFilter(filterConfiguration);

        final FilterResult filterResult = filter.filter(getPolicy(), "context",  "documentid", PIECE, "9444 Heritage St.", attributes);

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 0, 17, FilterType.STREET_ADDRESS));

    }

    @Test
    public void filter14() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new StreetAddressFilterStrategy()))
                .withAnonymizationService(new StreetAddressAnonymizationService(new DefaultContextService()))
                .withWindowSize(windowSize)
                .build();

        final StreetAddressFilter filter = new StreetAddressFilter(filterConfiguration);

        final FilterResult filterResult = filter.filter(getPolicy(), "context",  "documentid", PIECE, "70 Birchpond Street", attributes);

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 0, 19, FilterType.STREET_ADDRESS));

    }

    @Test
    public void filter15() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new StreetAddressFilterStrategy()))
                .withAnonymizationService(new StreetAddressAnonymizationService(new DefaultContextService()))
                .withWindowSize(windowSize)
                .build();

        final StreetAddressFilter filter = new StreetAddressFilter(filterConfiguration);

        final FilterResult filterResult = filter.filter(getPolicy(), "context",  "documentid", PIECE, "656 S. Inverness St.", attributes);

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 0, 20, FilterType.STREET_ADDRESS));

    }

    @Test
    public void filter16() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new StreetAddressFilterStrategy()))
                .withAnonymizationService(new StreetAddressAnonymizationService(new DefaultContextService()))
                .withWindowSize(windowSize)
                .build();

        final StreetAddressFilter filter = new StreetAddressFilter(filterConfiguration);

        final FilterResult filterResult = filter.filter(getPolicy(), "context",  "documentid", PIECE, "9142 Arlington Court", attributes);

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 0, 20, FilterType.STREET_ADDRESS));

    }

    @Test
    public void filter17() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new StreetAddressFilterStrategy()))
                .withAnonymizationService(new StreetAddressAnonymizationService(new DefaultContextService()))
                .withWindowSize(windowSize)
                .build();

        final StreetAddressFilter filter = new StreetAddressFilter(filterConfiguration);

        final FilterResult filterResult = filter.filter(getPolicy(), "context",  "documentid", PIECE, "4 Devonshire Ct.", attributes);

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 0, 16, FilterType.STREET_ADDRESS));

    }

    @Test
    public void filter18() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new StreetAddressFilterStrategy()))
                .withAnonymizationService(new StreetAddressAnonymizationService(new DefaultContextService()))
                .withWindowSize(windowSize)
                .build();

        final StreetAddressFilter filter = new StreetAddressFilter(filterConfiguration);

        final FilterResult filterResult = filter.filter(getPolicy(), "context",  "documentid", PIECE, "4 Devonshire Ct", attributes);

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 0, 15, FilterType.STREET_ADDRESS));

    }

    @Test
    public void filter19() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new StreetAddressFilterStrategy()))
                .withAnonymizationService(new StreetAddressAnonymizationService(new DefaultContextService()))
                .withWindowSize(windowSize)
                .build();

        final StreetAddressFilter filter = new StreetAddressFilter(filterConfiguration);

        final FilterResult filterResult = filter.filter(getPolicy(), "context",  "documentid", PIECE, "4 Devonshire Ct ste 2", attributes);

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 0, 21, FilterType.STREET_ADDRESS));

    }

    @Test
    public void filter20() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new StreetAddressFilterStrategy()))
                .withAnonymizationService(new StreetAddressAnonymizationService(new DefaultContextService()))
                .withWindowSize(windowSize)
                .build();

        final StreetAddressFilter filter = new StreetAddressFilter(filterConfiguration);

        final FilterResult filterResult = filter.filter(getPolicy(), "context",  "documentid", PIECE, "4 Devonshire Ct apartment 222", attributes);

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 0, 29, FilterType.STREET_ADDRESS));

    }

    @Test
    public void filter21() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new StreetAddressFilterStrategy()))
                .withAnonymizationService(new StreetAddressAnonymizationService(new DefaultContextService()))
                .withWindowSize(windowSize)
                .build();

        final StreetAddressFilter filter = new StreetAddressFilter(filterConfiguration);

        final FilterResult filterResult = filter.filter(getPolicy(), "context",  "documentid", PIECE, "4 Devonshire Ct apt 222", attributes);

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 0, 23, FilterType.STREET_ADDRESS));

    }

    @Test
    public void filter22() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new StreetAddressFilterStrategy()))
                .withAnonymizationService(new StreetAddressAnonymizationService(new DefaultContextService()))
                .withWindowSize(windowSize)
                .build();

        final StreetAddressFilter filter = new StreetAddressFilter(filterConfiguration);

        final FilterResult filterResult = filter.filter(getPolicy(), "context",  "documentid", PIECE, "4 Devonshire Ct apt 222 anywhere", attributes);

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 0, 23, FilterType.STREET_ADDRESS));

    }

    @Test
    public void filter23() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new StreetAddressFilterStrategy()))
                .withAnonymizationService(new StreetAddressAnonymizationService(new DefaultContextService()))
                .withWindowSize(windowSize)
                .build();

        final StreetAddressFilter filter = new StreetAddressFilter(filterConfiguration);

        final FilterResult filterResult = filter.filter(getPolicy(), "context",  "documentid", PIECE, "address is 9444 Heritage St. over there", attributes);

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 11, 28, FilterType.STREET_ADDRESS));

    }

    @Test
    public void filter24() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new StreetAddressFilterStrategy()))
                .withAnonymizationService(new StreetAddressAnonymizationService(new DefaultContextService()))
                .withWindowSize(windowSize)
                .build();

        final StreetAddressFilter filter = new StreetAddressFilter(filterConfiguration);

        final FilterResult filterResult = filter.filter(getPolicy(), "context",  "documentid", PIECE, "address is 9444 Heritage St. apt 2 over there", attributes);

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 11, 34, FilterType.STREET_ADDRESS));

    }

}
