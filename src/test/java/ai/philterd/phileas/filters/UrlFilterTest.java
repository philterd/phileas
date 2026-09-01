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
import ai.philterd.phileas.services.filters.regex.UrlFilter;
import ai.philterd.phileas.services.strategies.rules.UrlFilterStrategy;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

import static ai.philterd.phileas.services.strategies.AbstractFilterStrategy.RANDOM_REPLACE;

// The many near-identical test methods predate this fix; converting them to @ParameterizedTest
// groups would be a separate, unrelated refactor of the whole file.
@SuppressWarnings("java:S5976")
public class UrlFilterTest extends AbstractFilterTest {

    @Test
    public void filterUrl1() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new UrlFilterStrategy()))
                .withWindowSize(windowSize)
                .build();

        final UrlFilter filter = new UrlFilter(filterConfiguration, true);

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE,"the page is http://page.com.");
        showSpans(filtered.getSpans());
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 12, 27, FilterType.URL));
        Assertions.assertEquals("http://page.com", filtered.getSpans().get(0).getText());

    }

    @Test
    public void filterUrl2() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new UrlFilterStrategy()))
                .withWindowSize(windowSize)
                .build();

        final UrlFilter filter = new UrlFilter(filterConfiguration, true);

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "the page is myhomepage.com.");
        showSpans(filtered.getSpans());
        Assertions.assertEquals(0, filtered.getSpans().size());

    }

    @Test
    public void filterUrl3() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new UrlFilterStrategy()))
                .withWindowSize(windowSize)
                .build();

        final UrlFilter filter = new UrlFilter(filterConfiguration, true);

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "the page is http://myhomepage.com/folder/page.html.");
        showSpans(filtered.getSpans());
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 12, 51, FilterType.URL));

    }

    @Test
    public void filterUrl4() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new UrlFilterStrategy()))
                .withWindowSize(windowSize)
                .build();

        final UrlFilter filter = new UrlFilter(filterConfiguration, true);

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "the page is http://www.myhomepage.com/folder/page.html");
        showSpans(filtered.getSpans());
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 12, 54, FilterType.URL));

    }

    @Test
    public void filterUrl5() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new UrlFilterStrategy()))
                .withWindowSize(windowSize)
                .build();

        final UrlFilter filter = new UrlFilter(filterConfiguration, true);

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "the page is www.myhomepage.com/folder/page.html.");
        showSpans(filtered.getSpans());
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 12, 48, FilterType.URL));

    }

    @Test
    public void filterUrl6() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new UrlFilterStrategy()))
                .withWindowSize(windowSize)
                .build();

        final UrlFilter filter = new UrlFilter(filterConfiguration, false);

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "the page is myhomepage.com.");
        showSpans(filtered.getSpans());
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 12, 26, FilterType.URL));

    }

    @Test
    public void filterUrl7() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new UrlFilterStrategy()))
                .withWindowSize(windowSize)
                .build();

        final UrlFilter filter = new UrlFilter(filterConfiguration, true);

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "the page is www.myhomepage.com:80/folder/page.html.");
        showSpans(filtered.getSpans());
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 12, 51, FilterType.URL));

    }

    @Test
    public void filterUrl8() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new UrlFilterStrategy()))
                .withWindowSize(windowSize)
                .build();

        final UrlFilter filter = new UrlFilter(filterConfiguration, true);

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "the page is http://192.168.1.1:80/folder/page.html.");
        showSpans(filtered.getSpans());
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 12, 51, FilterType.URL));

    }

    @Test
    public void filterUrl9() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new UrlFilterStrategy()))
                .withWindowSize(windowSize)
                .build();

        final UrlFilter filter = new UrlFilter(filterConfiguration, false);

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "the page is 192.168.1.1:80/folder/page.html.");
        showSpans(filtered.getSpans());
        Assertions.assertEquals(2, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 34, 43, FilterType.URL));
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(1), 12, 44, FilterType.URL));

    }

    @Test
    public void filterUrl10() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new UrlFilterStrategy()))
                .withWindowSize(windowSize)
                .build();

        final UrlFilter filter = new UrlFilter(filterConfiguration, false);

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "the page is http://192.168.1.1:80/folder/page.html.");
        showSpans(filtered.getSpans());
        Assertions.assertEquals(2, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 41, 50, FilterType.URL));
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(1), 12, 51, FilterType.URL));

    }

    @Test
    public void filterUrl11() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new UrlFilterStrategy()))
                .withWindowSize(windowSize)
                .build();

        final UrlFilter filter = new UrlFilter(filterConfiguration, false);

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "the page is https://192.168.1.1:80/folder/page.html.");
        showSpans(filtered.getSpans());
        Assertions.assertEquals(2, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 42, 51, FilterType.URL));
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(1), 12, 52, FilterType.URL));

    }

    @Test
    public void filterUrl12() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new UrlFilterStrategy()))
                .withWindowSize(windowSize)
                .build();

        final UrlFilter filter = new UrlFilter(filterConfiguration, true);

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "the page is test.ok new sentence");
        showSpans(filtered.getSpans());
        Assertions.assertEquals(0, filtered.getSpans().size());

    }

    @Test
    public void filterUrl13() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new UrlFilterStrategy()))
                .withWindowSize(windowSize)
                .build();

        final UrlFilter filter = new UrlFilter(filterConfiguration, false);

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "the page is http://2001:0db8:85a3:0000:0000:8a2e:0370:7334/test.html.");
        showSpans(filtered.getSpans());
        Assertions.assertEquals(2, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 59, 68, FilterType.URL));
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(1), 12, 69, FilterType.URL));

    }

    @Test
    public void filterUrl14() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new UrlFilterStrategy()))
                .withWindowSize(windowSize)
                .build();

        final UrlFilter filter = new UrlFilter(filterConfiguration, false);

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "the page is http://2001:0db8:85a3:0000:0000:8a2e:0370:7334/test/.");
        showSpans(filtered.getSpans());
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 12, 65, FilterType.URL));

    }

    @Test
    public void filterUrl15() throws Exception {

        // https://github.com/philterd/phileas/issues/342
        // The path used to run to the end of the string, taking the rest of the sentence with it.
        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new UrlFilterStrategy()))
                .withWindowSize(windowSize)
                .build();

        final UrlFilter filter = new UrlFilter(filterConfiguration, false);

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "the page is https://192.168.1.1:80/folder/page.html. this is a new sentence.");
        showSpans(filtered.getSpans());
        Assertions.assertEquals(2, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 42, 51, FilterType.URL));
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(1), 12, 51, FilterType.URL));
        Assertions.assertEquals("https://192.168.1.1:80/folder/page.html", filtered.getSpans().get(1).getText());

    }

    @Test
    public void filterUrl16() throws Exception {

        // https://github.com/philterd/phileas/pull/348#pullrequestreview
        // A non-atomic (?:...)* here compiles to a recursive call per path character in
        // java.util.regex, so a long path overflows the stack well before this length.
        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new UrlFilterStrategy()))
                .withWindowSize(windowSize)
                .build();

        final UrlFilter filter = new UrlFilter(filterConfiguration, true);

        final String longPath = "a".repeat(10_000);
        final String input = "the page is http://myhomepage.com/" + longPath;

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, input);
        showSpans(filtered.getSpans());
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 12, input.length(), FilterType.URL));

    }

    @Test
    public void filterUrl17() throws Exception {

        // https://github.com/philterd/phileas/issues/351
        // A compressed address used to match only as far as its "::", leaving the rest in the clear.
        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new UrlFilterStrategy()))
                .withWindowSize(windowSize)
                .build();

        final UrlFilter filter = new UrlFilter(filterConfiguration, false);

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "host FE80::1");
        showSpans(filtered.getSpans());
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 5, 12, FilterType.URL));
        Assertions.assertEquals("FE80::1", filtered.getSpans().get(0).getText());

    }

    @Test
    public void filterUrl18() throws Exception {

        // https://github.com/philterd/phileas/issues/351
        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new UrlFilterStrategy()))
                .withWindowSize(windowSize)
                .build();

        final UrlFilter filter = new UrlFilter(filterConfiguration, false);

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "host 2001:db8:85a3::8a2e:370:7334");
        showSpans(filtered.getSpans());
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 5, 33, FilterType.URL));
        Assertions.assertEquals("2001:db8:85a3::8a2e:370:7334", filtered.getSpans().get(0).getText());

    }

    @Test
    public void filterUrl19() throws Exception {

        // https://github.com/philterd/phileas/issues/351
        // An IPv4-mapped address. The embedded IPv4 address is also matched by the IPv4 pattern, so
        // the whole address and the trailing dotted quad are both spans.
        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new UrlFilterStrategy()))
                .withWindowSize(windowSize)
                .build();

        final UrlFilter filter = new UrlFilter(filterConfiguration, false);

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "host ::ffff:192.0.2.128");
        showSpans(filtered.getSpans());
        Assertions.assertEquals(2, filtered.getSpans().size());
        Assertions.assertEquals("192.0.2.128", filtered.getSpans().get(0).getText());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(1), 5, 23, FilterType.URL));
        Assertions.assertEquals("::ffff:192.0.2.128", filtered.getSpans().get(1).getText());

    }

    @Test
    public void filterUrl20() throws Exception {

        // https://github.com/philterd/phileas/issues/351
        // A zone identifier. Without the zone in the pattern the boundary rejects every alternative
        // that stops at the "%", so the address matched nothing at all.
        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new UrlFilterStrategy()))
                .withWindowSize(windowSize)
                .build();

        final UrlFilter filter = new UrlFilter(filterConfiguration, false);

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "host fe80::1%eth0");
        showSpans(filtered.getSpans());
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 5, 17, FilterType.URL));
        Assertions.assertEquals("fe80::1%eth0", filtered.getSpans().get(0).getText());

    }

    @Test
    public void filterUrl21() throws Exception {

        // https://github.com/philterd/phileas/issues/351
        // A bracketed host, which is the only form that can carry a port.
        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new UrlFilterStrategy()))
                .withWindowSize(windowSize)
                .build();

        final UrlFilter filter = new UrlFilter(filterConfiguration, false);

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "http://[2001:db8::1]:8080/x");
        showSpans(filtered.getSpans());
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 0, 27, FilterType.URL));
        Assertions.assertEquals("http://[2001:db8::1]:8080/x", filtered.getSpans().get(0).getText());

    }

    @Test
    public void filterUrl22() throws Exception {

        // https://github.com/philterd/phileas/issues/351
        // Colon-separated text that is not an address stays unmatched.
        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new UrlFilterStrategy()))
                .withWindowSize(windowSize)
                .build();

        final UrlFilter filter = new UrlFilter(filterConfiguration, false);

        Assertions.assertEquals(0, filter.filter(contextService, getPolicy(), "context", PIECE, "the time is 12:30 and the ratio is 1:2").getSpans().size());
        Assertions.assertEquals(0, filter.filter(contextService, getPolicy(), "context", PIECE, "see chapter 3:16 for details").getSpans().size());
        Assertions.assertEquals(0, filter.filter(contextService, getPolicy(), "context", PIECE, "the mac address is 00:1b:44:11:3a:b7").getSpans().size());

    }

    @Test
    public void filterUrl23() throws Exception {

        // https://github.com/philterd/phileas/issues/351
        // The fully expanded form, on its own rather than inside a URL as in filterUrl13 and 14.
        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new UrlFilterStrategy()))
                .withWindowSize(windowSize)
                .build();

        final UrlFilter filter = new UrlFilter(filterConfiguration, false);

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "host 2001:0db8:85a3:0000:0000:8a2e:0370:7334");
        showSpans(filtered.getSpans());
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 5, 44, FilterType.URL));
        Assertions.assertEquals("2001:0db8:85a3:0000:0000:8a2e:0370:7334", filtered.getSpans().get(0).getText());

    }

    @Test
    public void filterWithCandidates1() throws Exception {

        final List<String> candidates = List.of("http://candidate1.com", "https://candidate2.com");

        final UrlFilterStrategy urlFilterStrategy = new UrlFilterStrategy();
        urlFilterStrategy.setStrategy(RANDOM_REPLACE);
        urlFilterStrategy.setAnonymizationCandidates(candidates);

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(urlFilterStrategy))
                .withWindowSize(windowSize)
                .build();

        final UrlFilter filter = new UrlFilter(filterConfiguration, true);

        final Filtered filtered = filter.filter(contextService, getPolicy(), "context", PIECE, "visit http://example.com now");
        showSpans(filtered.getSpans());
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(candidates.contains(filtered.getSpans().get(0).getReplacement()));

    }

}
