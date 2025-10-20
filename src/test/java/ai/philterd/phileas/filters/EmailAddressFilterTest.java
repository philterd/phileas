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
import ai.philterd.phileas.policy.Policy;
import ai.philterd.phileas.services.anonymization.AlphanumericAnonymizationService;
import ai.philterd.phileas.services.context.DefaultContextService;
import ai.philterd.phileas.services.filters.regex.EmailAddressFilter;
import ai.philterd.phileas.services.strategies.rules.EmailAddressFilterStrategy;
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
                .withAnonymizationService(new AlphanumericAnonymizationService(new DefaultContextService()))
                .withWindowSize(windowSize)
                .build();

        filterEmails(filterConfiguration, true, false);

    }

    @Test
    public void filterEmailRelaxed() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new EmailAddressFilterStrategy()))
                .withAnonymizationService(new AlphanumericAnonymizationService(new DefaultContextService()))
                .withWindowSize(windowSize)
                .build();

        filterEmails(filterConfiguration, false, false);

    }

    @Test
    public void filterEmailOnlyValidTLDs() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new EmailAddressFilterStrategy()))
                .withAnonymizationService(new AlphanumericAnonymizationService(new DefaultContextService()))
                .withWindowSize(windowSize)
                .build();

        final EmailAddressFilter filter = new EmailAddressFilter(filterConfiguration, true, true);

        final Filtered filtered = filter.filter(getPolicy(), "context", PIECE, "my email is none@none.com.");
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 12, 25, FilterType.EMAIL_ADDRESS));
        Assertions.assertEquals("none@none.com", filtered.getSpans().get(0).getText());

    }

    @Test
    public void filterEmailOnlyInvalidTLDs() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new EmailAddressFilterStrategy()))
                .withAnonymizationService(new AlphanumericAnonymizationService(new DefaultContextService()))
                .withWindowSize(windowSize)
                .build();

        final EmailAddressFilter filter = new EmailAddressFilter(filterConfiguration, true, true);

        final Filtered filtered1 = filter.filter(getPolicy(), "context", PIECE, "my email is none@none.codfm.");
        Assertions.assertEquals(0, filtered1.getSpans().size());

        final Filtered filtered2 = filter.filter(getPolicy(), "context", PIECE, "my email is none@none.com.dmf.");
        Assertions.assertEquals(0, filtered2.getSpans().size());

        final Filtered filtered3 = filter.filter(getPolicy(), "context", PIECE, "my email is none@none.cob");
        Assertions.assertEquals(0, filtered3.getSpans().size());

    }

    @Test
    public void filterEmailOnlyInvalidTLDsWithNoStrictMatches() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new EmailAddressFilterStrategy()))
                .withAnonymizationService(new AlphanumericAnonymizationService(new DefaultContextService()))
                .withWindowSize(windowSize)
                .build();

        final EmailAddressFilter filter = new EmailAddressFilter(filterConfiguration, false, true);

        final Filtered filtered4 = filter.filter(getPolicy(), "context", PIECE, "my email is none@lb.co_m");
        showSpans(filtered4.getSpans());
        Assertions.assertEquals(0, filtered4.getSpans().size());

    }

    private void filterEmails(FilterConfiguration filterConfiguration, boolean onlyStrictMatches, boolean onlyValidTLDs) throws Exception {

        final String cxt = "context";
        final EmailAddressFilter filter = new EmailAddressFilter(filterConfiguration, onlyStrictMatches, onlyValidTLDs);
        final Policy policy = getPolicy();

        final Map<String, String> context = Collections.emptyMap();

        final Filtered filtered = filter.filter(policy, cxt, PIECE, "my email is none@none.com.");
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 12, 25, FilterType.EMAIL_ADDRESS));
        Assertions.assertEquals("none@none.com", filtered.getSpans().get(0).getText());

        // 👇 cases adapted from https://www.tumblr.com/codefool/15288874550/list-of-valid-and-invalid-email-addresses

        // valid email addresses
        Assertions.assertEquals(1, filter.filter(policy, cxt, PIECE, "firstname.lastname@example.com").getSpans().size());
        Assertions.assertEquals(1, filter.filter(policy, cxt, PIECE, "email@subdomain.example.com").getSpans().size());
        Assertions.assertEquals(1, filter.filter(policy, cxt, PIECE, "firstname+lastname@example.com").getSpans().size());
        Assertions.assertEquals(1, filter.filter(policy, cxt, PIECE, "email@123.123.123.123").getSpans().size());
        Assertions.assertEquals(1, filter.filter(policy, cxt, PIECE, "1234567890@example.com").getSpans().size());
        Assertions.assertEquals(1, filter.filter(policy, cxt, PIECE, "email@example-one.com").getSpans().size());
        Assertions.assertEquals(1, filter.filter(policy, cxt, PIECE, "_______@example.com").getSpans().size());
        Assertions.assertEquals(1, filter.filter(policy, cxt, PIECE, "email@example.name").getSpans().size());
        Assertions.assertEquals(1, filter.filter(policy, cxt, PIECE, "email@example.museum").getSpans().size());
        Assertions.assertEquals(1, filter.filter(policy, cxt, PIECE, "email@example.co.jp").getSpans().size());
        Assertions.assertEquals(1, filter.filter(policy, cxt, PIECE, "firstname-lastname@example.com").getSpans().size());
        Assertions.assertEquals(1, filter.filter(policy, cxt, PIECE, "very.unusual.“@”.unusual.com@example.com").getSpans().size());
        Assertions.assertEquals(1, filter.filter(policy, cxt, PIECE, "very.“(),:;<>[]”.VERY.“very@\\\\ \"very”.unusual@strange.example.com").getSpans().size());
        //Assertions.assertEquals(1, filter.filter(policy, cxt, PIECE, "“email”@example.com").getSpans().size());                // todo include quotes
        //Assertions.assertEquals(1, filter.filter(policy, cxt, PIECE, "much.“more\\ unusual”@example.com").getSpans().size());  // todo include quotes

        // valid email addresses only detected with strict matching
        Assertions.assertEquals(onlyStrictMatches ? 1 : 0, filter.filter(policy, cxt, PIECE, "email@[123.123.123.123]").getSpans().size());

        // invalid email addresses
        Assertions.assertEquals(0, filter.filter(policy, cxt, PIECE, "plainaddress").getSpans().size());
        Assertions.assertEquals(0, filter.filter(policy, cxt, PIECE, "#@%^%#$@#$@#.com").getSpans().size());
        Assertions.assertEquals(0, filter.filter(policy, cxt, PIECE, "@example.com").getSpans().size());
        Assertions.assertEquals(0, filter.filter(policy, cxt, PIECE, "email.example.com").getSpans().size());
        Assertions.assertEquals(0, filter.filter(policy, cxt, PIECE, "あいうえお@example.com").getSpans().size());
        Assertions.assertEquals(0, filter.filter(policy, cxt, PIECE, "email@example").getSpans().size());
        Assertions.assertEquals(0, filter.filter(policy, cxt, PIECE, "email@example..com").getSpans().size());
        Assertions.assertEquals(0, filter.filter(policy, cxt, PIECE, "“(),:;<>[\\]@example.com").getSpans().size());
        //Assertions.assertEquals(0, filter.filter(policy, cxt, PIECE, "email@example.web").getSpans().size());        // todo detect invalid TLD
        //Assertions.assertEquals(0, filter.filter(policy, cxt, PIECE, "email@111.222.333.44444").getSpans().size());  // todo detect invalid TLD

        // invalid email addresses only rejected with strict matching
        Assertions.assertEquals(onlyStrictMatches ? 0 : 1, filter.filter(policy, cxt, PIECE, "email.@example.com").getSpans().size());
        Assertions.assertEquals(onlyStrictMatches ? 0 : 1, filter.filter(policy, cxt, PIECE, "email@-example.com").getSpans().size());

        // valid partial matches against invalid email addresses
        Assertions.assertEquals(1, filter.filter(policy, cxt, PIECE, "Joe Smith <email@example.com>").getSpans().size());
        Assertions.assertEquals(1, filter.filter(policy, cxt, PIECE, "email@example@example.com").getSpans().size());
        Assertions.assertEquals(1, filter.filter(policy, cxt, PIECE, ".email@example.com").getSpans().size());
        Assertions.assertEquals(1, filter.filter(policy, cxt, PIECE, "email..email@example.com").getSpans().size());
        Assertions.assertEquals(1, filter.filter(policy, cxt, PIECE, "email@example.com (Joe Smith)").getSpans().size());
        Assertions.assertEquals(1, filter.filter(policy, cxt, PIECE, "Abc..123@example.com").getSpans().size());
        Assertions.assertEquals(1, filter.filter(policy, cxt, PIECE, "just\"not\"right@example.com").getSpans().size());
        Assertions.assertEquals(1, filter.filter(policy, cxt, PIECE, "this\\ is\"really\"not\\allowed@example.com").getSpans().size());

    }

}
