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
import ai.philterd.phileas.model.policy.Policy;
import ai.philterd.phileas.model.policy.filters.strategies.rules.EmailAddressFilterStrategy;
import ai.philterd.phileas.model.anonymization.AlphanumericAnonymizationService;
import ai.philterd.phileas.services.filters.regex.EmailAddressFilter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class EmailAddressFilterTest extends AbstractFilterTest {

    @Test
    public void filterEmailStrict() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new EmailAddressFilterStrategy()))
                .withAnonymizationService(new AlphanumericAnonymizationService())
                .withWindowSize(windowSize)
                .build();

        filterEmails(filterConfiguration, true, false);

    }

    @Test
    public void filterEmailRelaxed() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new EmailAddressFilterStrategy()))
                .withAnonymizationService(new AlphanumericAnonymizationService())
                .withWindowSize(windowSize)
                .build();

        filterEmails(filterConfiguration, false, false);

    }

    @Test
    public void filterEmailOnlyValidTLDs() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new EmailAddressFilterStrategy()))
                .withAnonymizationService(new AlphanumericAnonymizationService())
                .withWindowSize(windowSize)
                .build();

        final EmailAddressFilter filter = new EmailAddressFilter(filterConfiguration, true, true);

        final FilterResult filterResult = filter.filter(getPolicy(), "context", Collections.emptyMap(),"documentid", PIECE, "my email is none@none.com.", attributes);
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 12, 25, FilterType.EMAIL_ADDRESS));
        Assertions.assertEquals("none@none.com", filterResult.getSpans().get(0).getText());

    }

    @Test
    public void filterEmailOnlyInvalidTLDs() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new EmailAddressFilterStrategy()))
                .withAnonymizationService(new AlphanumericAnonymizationService())
                .withWindowSize(windowSize)
                .build();

        final EmailAddressFilter filter = new EmailAddressFilter(filterConfiguration, true, true);

        final FilterResult filterResult1 = filter.filter(getPolicy(), "context", Collections.emptyMap(),"documentid", PIECE, "my email is none@none.codfm.", attributes);
        Assertions.assertEquals(0, filterResult1.getSpans().size());

        final FilterResult filterResult2 = filter.filter(getPolicy(), "context", Collections.emptyMap(),"documentid", PIECE, "my email is none@none.com.dmf.", attributes);
        Assertions.assertEquals(0, filterResult2.getSpans().size());

        final FilterResult filterResult3 = filter.filter(getPolicy(), "context", Collections.emptyMap(),"documentid", PIECE, "my email is none@none.cob", attributes);
        Assertions.assertEquals(0, filterResult3.getSpans().size());

    }

    @Test
    public void filterEmailOnlyInvalidTLDsWithNoStrictMatches() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new EmailAddressFilterStrategy()))
                .withAnonymizationService(new AlphanumericAnonymizationService())
                .withWindowSize(windowSize)
                .build();

        final EmailAddressFilter filter = new EmailAddressFilter(filterConfiguration, false, true);

        final FilterResult filterResult4 = filter.filter(getPolicy(), "context", Collections.emptyMap(),"documentid", PIECE, "my email is none@lb.co_m", attributes);
        showSpans(filterResult4.getSpans());
        Assertions.assertEquals(0, filterResult4.getSpans().size());

    }

    private void filterEmails(FilterConfiguration filterConfiguration, boolean onlyStrictMatches, boolean onlyValidTLDs) throws Exception {

        final String cxt = "context";
        final String doc = "documentid";
        final EmailAddressFilter filter = new EmailAddressFilter(filterConfiguration, onlyStrictMatches, onlyValidTLDs);
        final Policy policy = getPolicy();

        final Map<String, String> context = Collections.emptyMap();

        final FilterResult filterResult = filter.filter(policy, cxt, context, doc, PIECE, "my email is none@none.com.", attributes);
        Assertions.assertEquals(1, filterResult.getSpans().size());
        Assertions.assertTrue(checkSpan(filterResult.getSpans().get(0), 12, 25, FilterType.EMAIL_ADDRESS));
        Assertions.assertEquals("none@none.com", filterResult.getSpans().get(0).getText());

        // 👇 cases adapted from https://www.tumblr.com/codefool/15288874550/list-of-valid-and-invalid-email-addresses

        // valid email addresses
        Assertions.assertEquals(1, filter.filter(policy, cxt, context, doc, PIECE, "firstname.lastname@example.com", attributes).getSpans().size());
        Assertions.assertEquals(1, filter.filter(policy, cxt, context, doc, PIECE, "email@subdomain.example.com", attributes).getSpans().size());
        Assertions.assertEquals(1, filter.filter(policy, cxt, context, doc, PIECE, "firstname+lastname@example.com", attributes).getSpans().size());
        Assertions.assertEquals(1, filter.filter(policy, cxt, context, doc, PIECE, "email@123.123.123.123", attributes).getSpans().size());
        Assertions.assertEquals(1, filter.filter(policy, cxt, context, doc, PIECE, "1234567890@example.com", attributes).getSpans().size());
        Assertions.assertEquals(1, filter.filter(policy, cxt, context, doc, PIECE, "email@example-one.com", attributes).getSpans().size());
        Assertions.assertEquals(1, filter.filter(policy, cxt, context, doc, PIECE, "_______@example.com", attributes).getSpans().size());
        Assertions.assertEquals(1, filter.filter(policy, cxt, context, doc, PIECE, "email@example.name", attributes).getSpans().size());
        Assertions.assertEquals(1, filter.filter(policy, cxt, context, doc, PIECE, "email@example.museum", attributes).getSpans().size());
        Assertions.assertEquals(1, filter.filter(policy, cxt, context, doc, PIECE, "email@example.co.jp", attributes).getSpans().size());
        Assertions.assertEquals(1, filter.filter(policy, cxt, context, doc, PIECE, "firstname-lastname@example.com", attributes).getSpans().size());
        Assertions.assertEquals(1, filter.filter(policy, cxt, context, doc, PIECE, "very.unusual.“@”.unusual.com@example.com", attributes).getSpans().size());
        Assertions.assertEquals(1, filter.filter(policy, cxt, context, doc, PIECE, "very.“(),:;<>[]”.VERY.“very@\\\\ \"very”.unusual@strange.example.com", attributes).getSpans().size());
        //Assertions.assertEquals(1, filter.filter(policy, cxt, context, doc, PIECE, "“email”@example.com", attributes).getSpans().size());                // todo include quotes
        //Assertions.assertEquals(1, filter.filter(policy, cxt, context, doc, PIECE, "much.“more\\ unusual”@example.com", attributes).getSpans().size());  // todo include quotes

        // valid email addresses only detected with strict matching
        Assertions.assertEquals(onlyStrictMatches ? 1 : 0, filter.filter(policy, cxt, context, doc, PIECE, "email@[123.123.123.123]", attributes).getSpans().size());

        // invalid email addresses
        Assertions.assertEquals(0, filter.filter(policy, cxt, context, doc, PIECE, "plainaddress", attributes).getSpans().size());
        Assertions.assertEquals(0, filter.filter(policy, cxt, context, doc, PIECE, "#@%^%#$@#$@#.com", attributes).getSpans().size());
        Assertions.assertEquals(0, filter.filter(policy, cxt, context, doc, PIECE, "@example.com", attributes).getSpans().size());
        Assertions.assertEquals(0, filter.filter(policy, cxt, context, doc, PIECE, "email.example.com", attributes).getSpans().size());
        Assertions.assertEquals(0, filter.filter(policy, cxt, context, doc, PIECE, "あいうえお@example.com", attributes).getSpans().size());
        Assertions.assertEquals(0, filter.filter(policy, cxt, context, doc, PIECE, "email@example", attributes).getSpans().size());
        Assertions.assertEquals(0, filter.filter(policy, cxt, context, doc, PIECE, "email@example..com", attributes).getSpans().size());
        Assertions.assertEquals(0, filter.filter(policy, cxt, context, doc, PIECE, "“(),:;<>[\\]@example.com", attributes).getSpans().size());
        //Assertions.assertEquals(0, filter.filter(policy, cxt, context, doc, PIECE, "email@example.web", attributes).getSpans().size());        // todo detect invalid TLD
        //Assertions.assertEquals(0, filter.filter(policy, cxt, context, doc, PIECE, "email@111.222.333.44444", attributes).getSpans().size());  // todo detect invalid TLD

        // invalid email addresses only rejected with strict matching
        Assertions.assertEquals(onlyStrictMatches ? 0 : 1, filter.filter(policy, cxt, context, doc, PIECE, "email.@example.com", attributes).getSpans().size());
        Assertions.assertEquals(onlyStrictMatches ? 0 : 1, filter.filter(policy, cxt, context, doc, PIECE, "email@-example.com", attributes).getSpans().size());

        // valid partial matches against invalid email addresses
        Assertions.assertEquals(1, filter.filter(policy, cxt, context, doc, PIECE, "Joe Smith <email@example.com>", attributes).getSpans().size());
        Assertions.assertEquals(1, filter.filter(policy, cxt, context, doc, PIECE, "email@example@example.com", attributes).getSpans().size());
        Assertions.assertEquals(1, filter.filter(policy, cxt, context, doc, PIECE, ".email@example.com", attributes).getSpans().size());
        Assertions.assertEquals(1, filter.filter(policy, cxt, context, doc, PIECE, "email..email@example.com", attributes).getSpans().size());
        Assertions.assertEquals(1, filter.filter(policy, cxt, context, doc, PIECE, "email@example.com (Joe Smith)", attributes).getSpans().size());
        Assertions.assertEquals(1, filter.filter(policy, cxt, context, doc, PIECE, "Abc..123@example.com", attributes).getSpans().size());
        Assertions.assertEquals(1, filter.filter(policy, cxt, context, doc, PIECE, "just\"not\"right@example.com", attributes).getSpans().size());
        Assertions.assertEquals(1, filter.filter(policy, cxt, context, doc, PIECE, "this\\ is\"really\"not\\allowed@example.com", attributes).getSpans().size());

    }

}
