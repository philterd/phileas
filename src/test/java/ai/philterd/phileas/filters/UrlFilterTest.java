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

public class UrlFilterTest extends AbstractFilterTest {

    @Test
    public void filterUrl1() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new UrlFilterStrategy()))
                .withContextService(contextService)
                .withRandom(random)
                .withWindowSize(windowSize)
                .build();

        final UrlFilter filter = new UrlFilter(filterConfiguration, true);

        final Filtered filtered = filter.filter(getPolicy(), "context", PIECE,"the page is http://page.com.");
        showSpans(filtered.getSpans());
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 12, 27, FilterType.URL));
        Assertions.assertEquals("http://page.com", filtered.getSpans().get(0).getText());

    }

    @Test
    public void filterUrl2() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new UrlFilterStrategy()))
                .withContextService(contextService)
                .withRandom(random)
                .withWindowSize(windowSize)
                .build();

        final UrlFilter filter = new UrlFilter(filterConfiguration, true);

        final Filtered filtered = filter.filter(getPolicy(), "context", PIECE, "the page is myhomepage.com.");
        showSpans(filtered.getSpans());
        Assertions.assertEquals(0, filtered.getSpans().size());

    }

    @Test
    public void filterUrl3() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new UrlFilterStrategy()))
                .withContextService(contextService)
                .withRandom(random)
                .withWindowSize(windowSize)
                .build();

        final UrlFilter filter = new UrlFilter(filterConfiguration, true);

        final Filtered filtered = filter.filter(getPolicy(), "context", PIECE, "the page is http://myhomepage.com/folder/page.html.");
        showSpans(filtered.getSpans());
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 12, 51, FilterType.URL));

    }

    @Test
    public void filterUrl4() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new UrlFilterStrategy()))
                .withContextService(contextService)
                .withRandom(random)
                .withWindowSize(windowSize)
                .build();

        final UrlFilter filter = new UrlFilter(filterConfiguration, true);

        final Filtered filtered = filter.filter(getPolicy(), "context", PIECE, "the page is http://www.myhomepage.com/folder/page.html");
        showSpans(filtered.getSpans());
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 12, 54, FilterType.URL));

    }

    @Test
    public void filterUrl5() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new UrlFilterStrategy()))
                .withContextService(contextService)
                .withRandom(random)
                .withWindowSize(windowSize)
                .build();

        final UrlFilter filter = new UrlFilter(filterConfiguration, true);

        final Filtered filtered = filter.filter(getPolicy(), "context", PIECE, "the page is www.myhomepage.com/folder/page.html.");
        showSpans(filtered.getSpans());
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 12, 48, FilterType.URL));

    }

    @Test
    public void filterUrl6() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new UrlFilterStrategy()))
                .withContextService(contextService)
                .withRandom(random)
                .withWindowSize(windowSize)
                .build();

        final UrlFilter filter = new UrlFilter(filterConfiguration, false);

        final Filtered filtered = filter.filter(getPolicy(), "context", PIECE, "the page is myhomepage.com.");
        showSpans(filtered.getSpans());
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 12, 26, FilterType.URL));

    }

    @Test
    public void filterUrl7() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new UrlFilterStrategy()))
                .withContextService(contextService)
                .withRandom(random)
                .withWindowSize(windowSize)
                .build();

        final UrlFilter filter = new UrlFilter(filterConfiguration, true);

        final Filtered filtered = filter.filter(getPolicy(), "context", PIECE, "the page is www.myhomepage.com:80/folder/page.html.");
        showSpans(filtered.getSpans());
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 12, 51, FilterType.URL));

    }

    @Test
    public void filterUrl8() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new UrlFilterStrategy()))
                .withContextService(contextService)
                .withRandom(random)
                .withWindowSize(windowSize)
                .build();

        final UrlFilter filter = new UrlFilter(filterConfiguration, true);

        final Filtered filtered = filter.filter(getPolicy(), "context", PIECE, "the page is http://192.168.1.1:80/folder/page.html.");
        showSpans(filtered.getSpans());
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 12, 51, FilterType.URL));

    }

    @Test
    public void filterUrl9() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new UrlFilterStrategy()))
                .withContextService(contextService)
                .withRandom(random)
                .withWindowSize(windowSize)
                .build();

        final UrlFilter filter = new UrlFilter(filterConfiguration, false);

        final Filtered filtered = filter.filter(getPolicy(), "context", PIECE, "the page is 192.168.1.1:80/folder/page.html.");
        showSpans(filtered.getSpans());
        Assertions.assertEquals(2, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 34, 43, FilterType.URL));
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(1), 12, 44, FilterType.URL));

    }

    @Test
    public void filterUrl10() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new UrlFilterStrategy()))
                .withContextService(contextService)
                .withRandom(random)
                .withWindowSize(windowSize)
                .build();

        final UrlFilter filter = new UrlFilter(filterConfiguration, false);

        final Filtered filtered = filter.filter(getPolicy(), "context", PIECE, "the page is http://192.168.1.1:80/folder/page.html.");
        showSpans(filtered.getSpans());
        Assertions.assertEquals(2, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 41, 50, FilterType.URL));
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(1), 12, 51, FilterType.URL));

    }

    @Test
    public void filterUrl11() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new UrlFilterStrategy()))
                .withContextService(contextService)
                .withRandom(random)
                .withWindowSize(windowSize)
                .build();

        final UrlFilter filter = new UrlFilter(filterConfiguration, false);

        final Filtered filtered = filter.filter(getPolicy(), "context", PIECE, "the page is https://192.168.1.1:80/folder/page.html.");
        showSpans(filtered.getSpans());
        Assertions.assertEquals(2, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 42, 51, FilterType.URL));
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(1), 12, 52, FilterType.URL));

    }

    @Test
    public void filterUrl12() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new UrlFilterStrategy()))
                .withContextService(contextService)
                .withRandom(random)
                .withWindowSize(windowSize)
                .build();

        final UrlFilter filter = new UrlFilter(filterConfiguration, true);

        final Filtered filtered = filter.filter(getPolicy(), "context", PIECE, "the page is test.ok new sentence");
        showSpans(filtered.getSpans());
        Assertions.assertEquals(0, filtered.getSpans().size());

    }

    @Test
    public void filterUrl13() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new UrlFilterStrategy()))
                .withContextService(contextService)
                .withRandom(random)
                .withWindowSize(windowSize)
                .build();

        final UrlFilter filter = new UrlFilter(filterConfiguration, false);

        final Filtered filtered = filter.filter(getPolicy(), "context", PIECE, "the page is http://2001:0db8:85a3:0000:0000:8a2e:0370:7334/test.html.");
        showSpans(filtered.getSpans());
        Assertions.assertEquals(2, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 59, 68, FilterType.URL));
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(1), 12, 69, FilterType.URL));

    }

    @Test
    public void filterUrl14() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new UrlFilterStrategy()))
                .withContextService(contextService)
                .withRandom(random)
                .withWindowSize(windowSize)
                .build();

        final UrlFilter filter = new UrlFilter(filterConfiguration, false);

        final Filtered filtered = filter.filter(getPolicy(), "context", PIECE, "the page is http://2001:0db8:85a3:0000:0000:8a2e:0370:7334/test/.");
        showSpans(filtered.getSpans());
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 12, 65, FilterType.URL));

    }

    @Test
    public void filterUrl15() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new UrlFilterStrategy()))
                .withContextService(contextService)
                .withRandom(random)
                .withWindowSize(windowSize)
                .build();

        final UrlFilter filter = new UrlFilter(filterConfiguration, false);

        final Filtered filtered = filter.filter(getPolicy(), "context", PIECE, "the page is https://192.168.1.1:80/folder/page.html. this is a new sentence.");
        showSpans(filtered.getSpans());
        Assertions.assertEquals(2, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 42, 51, FilterType.URL));
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(1), 12, 76, FilterType.URL));

    }

    @Test
    public void filterWithCandidates1() throws Exception {

        final List<String> candidates = List.of("http://candidate1.com", "https://candidate2.com");

        final UrlFilterStrategy urlFilterStrategy = new UrlFilterStrategy();
        urlFilterStrategy.setStrategy(RANDOM_REPLACE);
        urlFilterStrategy.setAnonymizationCandidates(candidates);

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(urlFilterStrategy))
                .withContextService(contextService)
                .withRandom(random)
                .withWindowSize(windowSize)
                .build();

        final UrlFilter filter = new UrlFilter(filterConfiguration, true);

        final Filtered filtered = filter.filter(getPolicy(), "context", PIECE, "visit http://example.com now");
        showSpans(filtered.getSpans());
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(candidates.contains(filtered.getSpans().get(0).getReplacement()));

    }

}
