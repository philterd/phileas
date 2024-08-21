/*
 *     Copyright 2024 Philterd, LLC @ https://www.philterd.ai
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
import ai.philterd.phileas.model.policy.Policy;
import ai.philterd.phileas.model.policy.filters.strategies.rules.EmailAddressFilterStrategy;
import ai.philterd.phileas.model.services.AlertService;
import ai.philterd.phileas.services.anonymization.AlphanumericAnonymizationService;
import ai.philterd.phileas.services.anonymization.cache.LocalAnonymizationCacheService;
import ai.philterd.phileas.services.filters.regex.EmailAddressFilter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.List;

public class EmailAddressFilterTest extends AbstractFilterTest {

    private final AlertService alertService = Mockito.mock(AlertService.class);

    @Test
    public void filterEmail() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new EmailAddressFilterStrategy()))
                .withAlertService(alertService)
                .withAnonymizationService(new AlphanumericAnonymizationService(new LocalAnonymizationCacheService()))
                .withWindowSize(windowSize)
                .build();

        final String cxt = "context";
        final String doc = "documentid";
        final EmailAddressFilter filter = new EmailAddressFilter(filterConfiguration);
        final Policy policy = getPolicy();

        final FilterResult filterResult = filter.filter(policy, cxt, doc, PIECE, "my email is none@none.com.", attributes);
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 12, 25, FilterType.EMAIL_ADDRESS));
        Assertions.assertEquals("none@none.com", filterResult.getSpans().get(0).getText());

        // 👇 cases adapted from https://www.tumblr.com/codefool/15288874550/list-of-valid-and-invalid-email-addresses

        // valid email addresses
        Assertions.assertEquals(1, filter.filter(policy, cxt, doc, PIECE, "firstname.lastname@example.com", attributes).getSpans().size());
        Assertions.assertEquals(1, filter.filter(policy, cxt, doc, PIECE, "email@subdomain.example.com", attributes).getSpans().size());
        Assertions.assertEquals(1, filter.filter(policy, cxt, doc, PIECE, "firstname+lastname@example.com", attributes).getSpans().size());
        Assertions.assertEquals(1, filter.filter(policy, cxt, doc, PIECE, "email@123.123.123.123", attributes).getSpans().size());
        Assertions.assertEquals(1, filter.filter(policy, cxt, doc, PIECE, "email@[123.123.123.123]", attributes).getSpans().size());
        Assertions.assertEquals(1, filter.filter(policy, cxt, doc, PIECE, "1234567890@example.com", attributes).getSpans().size());
        Assertions.assertEquals(1, filter.filter(policy, cxt, doc, PIECE, "email@example-one.com", attributes).getSpans().size());
        Assertions.assertEquals(1, filter.filter(policy, cxt, doc, PIECE, "_______@example.com", attributes).getSpans().size());
        Assertions.assertEquals(1, filter.filter(policy, cxt, doc, PIECE, "email@example.name", attributes).getSpans().size());
        Assertions.assertEquals(1, filter.filter(policy, cxt, doc, PIECE, "email@example.museum", attributes).getSpans().size());
        Assertions.assertEquals(1, filter.filter(policy, cxt, doc, PIECE, "email@example.co.jp", attributes).getSpans().size());
        Assertions.assertEquals(1, filter.filter(policy, cxt, doc, PIECE, "firstname-lastname@example.com", attributes).getSpans().size());
        Assertions.assertEquals(1, filter.filter(policy, cxt, doc, PIECE, "very.unusual.“@”.unusual.com@example.com", attributes).getSpans().size());
        Assertions.assertEquals(1, filter.filter(policy, cxt, doc, PIECE, "very.“(),:;<>[]”.VERY.“very@\\\\ \"very”.unusual@strange.example.com", attributes).getSpans().size());
        //Assertions.assertEquals(1, filter.filter(policy, cxt, doc, PIECE, "“email”@example.com", attributes).getSpans().size());                // todo not including quotes
        //Assertions.assertEquals(1, filter.filter(policy, cxt, doc, PIECE, "much.“more\\ unusual”@example.com", attributes).getSpans().size());  // todo not including quotes

        // invalid email addresses
        Assertions.assertEquals(0, filter.filter(policy, cxt, doc, PIECE, "plainaddress", attributes).getSpans().size());
        Assertions.assertEquals(0, filter.filter(policy, cxt, doc, PIECE, "#@%^%#$@#$@#.com", attributes).getSpans().size());
        Assertions.assertEquals(0, filter.filter(policy, cxt, doc, PIECE, "@example.com", attributes).getSpans().size());
        Assertions.assertEquals(0, filter.filter(policy, cxt, doc, PIECE, "email.example.com", attributes).getSpans().size());
        Assertions.assertEquals(0, filter.filter(policy, cxt, doc, PIECE, "email.@example.com", attributes).getSpans().size());
        Assertions.assertEquals(0, filter.filter(policy, cxt, doc, PIECE, "あいうえお@example.com", attributes).getSpans().size());
        Assertions.assertEquals(0, filter.filter(policy, cxt, doc, PIECE, "email@example", attributes).getSpans().size());
        Assertions.assertEquals(0, filter.filter(policy, cxt, doc, PIECE, "email@-example.com", attributes).getSpans().size());
        Assertions.assertEquals(0, filter.filter(policy, cxt, doc, PIECE, "email@example..com", attributes).getSpans().size());
        Assertions.assertEquals(0, filter.filter(policy, cxt, doc, PIECE, "“(),:;<>[\\]@example.com", attributes).getSpans().size());
        //Assertions.assertEquals(0, filter.filter(policy, cxt, doc, PIECE, "email@example.web", attributes).getSpans().size());        // todo invalid TLD
        //Assertions.assertEquals(0, filter.filter(policy, cxt, doc, PIECE, "email@111.222.333.44444", attributes).getSpans().size());  // todo invalid TLD

        // valid partial matches against invalid email addresses
        Assertions.assertEquals(1, filter.filter(policy, cxt, doc, PIECE, "Joe Smith <email@example.com>", attributes).getSpans().size());
        Assertions.assertEquals(1, filter.filter(policy, cxt, doc, PIECE, "email@example@example.com", attributes).getSpans().size());
        Assertions.assertEquals(1, filter.filter(policy, cxt, doc, PIECE, ".email@example.com", attributes).getSpans().size());
        Assertions.assertEquals(1, filter.filter(policy, cxt, doc, PIECE, "email..email@example.com", attributes).getSpans().size());
        Assertions.assertEquals(1, filter.filter(policy, cxt, doc, PIECE, "email@example.com (Joe Smith)", attributes).getSpans().size());
        Assertions.assertEquals(1, filter.filter(policy, cxt, doc, PIECE, "Abc..123@example.com", attributes).getSpans().size());
        Assertions.assertEquals(1, filter.filter(policy, cxt, doc, PIECE, "just\"not\"right@example.com", attributes).getSpans().size());
        Assertions.assertEquals(1, filter.filter(policy, cxt, doc, PIECE, "this\\ is\"really\"not\\allowed@example.com", attributes).getSpans().size());

    }

}
