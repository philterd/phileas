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
package ai.philterd.test.phileas.services.filters;

import ai.philterd.phileas.model.enums.FilterType;
import ai.philterd.phileas.model.filter.FilterConfiguration;
import ai.philterd.phileas.model.objects.FilterResult;
import ai.philterd.phileas.model.policy.filters.strategies.rules.PhoneNumberFilterStrategy;
import ai.philterd.phileas.model.anonymization.MacAddressAnonymizationService;
import ai.philterd.phileas.services.filters.custom.PhoneNumberRulesFilter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.List;

public class PhoneNumberFilterTest extends AbstractFilterTest {

    private static final Logger LOGGER = LogManager.getLogger(PhoneNumberFilterTest.class);

    @Test
    public void filterPhone1() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new PhoneNumberFilterStrategy()))
                .withAnonymizationService(new MacAddressAnonymizationService())
                .withWindowSize(windowSize)
                .build();

        final PhoneNumberRulesFilter filter = new PhoneNumberRulesFilter(filterConfiguration);

        final FilterResult filterResult = filter.filter(getPolicy(), "context", Collections.emptyMap(), "documentid", PIECE, "the number is (123) 456-7890.", attributes);
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 14, 28, FilterType.PHONE_NUMBER));
        Assertions.assertEquals("(123) 456-7890", filterResult.getSpans().get(0).getText());
        Assertions.assertEquals(0.95, filterResult.getSpans().get(0).getConfidence());

    }

    @Test
    public void filterPhone2() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new PhoneNumberFilterStrategy()))
                .withAnonymizationService(new MacAddressAnonymizationService())
                .withWindowSize(windowSize)
                .build();

        final PhoneNumberRulesFilter filter = new PhoneNumberRulesFilter(filterConfiguration);

        final FilterResult filterResult = filter.filter(getPolicy(), "context", Collections.emptyMap(), "documentid", PIECE, "the number is (123) 456-7890 and (123) 456-7890.", attributes);
        Assertions.assertEquals(2, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 14, 28, FilterType.PHONE_NUMBER));
        Assertions.assertEquals(0.95, filterResult.getSpans().get(0).getConfidence());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(1), 33, 47, FilterType.PHONE_NUMBER));
        Assertions.assertEquals(0.95, filterResult.getSpans().get(1).getConfidence());

    }

    @Test
    public void filterPhone3() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new PhoneNumberFilterStrategy()))
                .withAnonymizationService(new MacAddressAnonymizationService())
                .withWindowSize(windowSize)
                .build();

        final PhoneNumberRulesFilter filter = new PhoneNumberRulesFilter(filterConfiguration);

        final FilterResult filterResult = filter.filter(getPolicy(), "context", Collections.emptyMap(), "documentid", PIECE, "the number is 123-456-7890.", attributes);
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 14, 26, FilterType.PHONE_NUMBER));
        Assertions.assertEquals(0.95, filterResult.getSpans().get(0).getConfidence());

    }

    @Test
    public void filterPhone4() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new PhoneNumberFilterStrategy()))
                .withAnonymizationService(new MacAddressAnonymizationService())
                .withWindowSize(windowSize)
                .build();

        final PhoneNumberRulesFilter filter = new PhoneNumberRulesFilter(filterConfiguration);

        final FilterResult filterResult = filter.filter(getPolicy(), "context", Collections.emptyMap(), "documentid", PIECE, "the number is 123-456-7890 and he was ok.", attributes);
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 14, 26, FilterType.PHONE_NUMBER));

    }

    @Test
    public void filterPhone5() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new PhoneNumberFilterStrategy()))
                .withAnonymizationService(new MacAddressAnonymizationService())
                .withWindowSize(windowSize)
                .build();

        final PhoneNumberRulesFilter filter = new PhoneNumberRulesFilter(filterConfiguration);

        final FilterResult filterResult = filter.filter(getPolicy(), "context", Collections.emptyMap(), "documentid", PIECE, "the number is ( 800 ) 123-4567 and he was ok.", attributes);
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 14, 30, FilterType.PHONE_NUMBER));
        Assertions.assertEquals(0.75, filterResult.getSpans().get(0).getConfidence());

    }

    @Test
    public void filterPhone6() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new PhoneNumberFilterStrategy()))
                .withAnonymizationService(new MacAddressAnonymizationService())
                .withWindowSize(windowSize)
                .build();

        final PhoneNumberRulesFilter filter = new PhoneNumberRulesFilter(filterConfiguration);

        final FilterResult filterResult = filter.filter(getPolicy(), "context", Collections.emptyMap(), "documentid", PIECE, "the number is (800) 123-4567 x532 and he was ok.", attributes);
        showSpans(filterResult.getSpans());

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 14, 33, FilterType.PHONE_NUMBER));
        Assertions.assertEquals(0.75, filterResult.getSpans().get(0).getConfidence());

    }

    @Test
    public void filterPhone7() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new PhoneNumberFilterStrategy()))
                .withAnonymizationService(new MacAddressAnonymizationService())
                .withWindowSize(windowSize)
                .build();

        final PhoneNumberRulesFilter filter = new PhoneNumberRulesFilter(filterConfiguration);

        final FilterResult filterResult = filter.filter(getPolicy(), "context", Collections.emptyMap(), "documentid", PIECE, "the number is (800) 123-4567x532 and he was ok.", attributes);
        showSpans(filterResult.getSpans());

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 14, 32, FilterType.PHONE_NUMBER));
        Assertions.assertEquals(0.75, filterResult.getSpans().get(0).getConfidence());

    }

    @Test
    public void filterPhone8() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new PhoneNumberFilterStrategy()))
                .withAnonymizationService(new MacAddressAnonymizationService())
                .withWindowSize(windowSize)
                .build();

        final PhoneNumberRulesFilter filter = new PhoneNumberRulesFilter(filterConfiguration);

        final FilterResult filterResult = filter.filter(getPolicy(), "context", Collections.emptyMap(), "documentid", PIECE, "7 64116-3220", attributes);
        showSpans(filterResult.getSpans());

        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertEquals(0.60, filterResult.getSpans().get(0).getConfidence());

    }

}
