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
package ai.philterd.phileas.services.pdf;

import ai.philterd.phileas.filters.AbstractFilterTest;
import ai.philterd.phileas.filters.FilterConfiguration;
import ai.philterd.phileas.model.filtering.FilterType;
import ai.philterd.phileas.model.filtering.Filtered;
import ai.philterd.phileas.model.filtering.Span;
import ai.philterd.phileas.PhileasConfiguration;
import ai.philterd.phileas.policy.Identifiers;
import ai.philterd.phileas.policy.Policy;
import ai.philterd.phileas.policy.filters.Ssn;
import ai.philterd.phileas.services.disambiguation.vector.InMemoryVectorService;
import ai.philterd.phileas.services.filters.filtering.PlainTextFilterService;
import ai.philterd.phileas.services.filters.regex.SsnFilter;
import ai.philterd.phileas.services.strategies.rules.SsnFilterStrategy;
import org.apache.pdfbox.pdmodel.graphics.color.PDColor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Properties;

class StrategyColorTest extends AbstractFilterTest {

    private static final float[] BLACK = {0, 0, 0};
    private static final float[] RED = {1, 0, 0};
    private static final float[] BLUE = {0, 0, 1};

    private float[] colorFor(final Policy policy, final String spanColor) throws Exception {

        final Span span = Span.make(0, 1, FilterType.AGE, "ctx", 0.5, "text", "repl", null, false, true, null, 0);
        span.setColor(spanColor);

        final PdfRedactor redactor = new PdfRedactor(policy, List.of(span), new PdfRedactionOptions());

        final Method method = PdfRedactor.class.getDeclaredMethod("redactionColorFor", Span.class);
        method.setAccessible(true);

        return ((PDColor) method.invoke(redactor, span)).getComponents();

    }

    @Test
    void aFilteredSpanCarriesItsStrategyColor() throws Exception {

        final SsnFilterStrategy strategy = new SsnFilterStrategy();
        strategy.setColor("red");

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(strategy))
                .withWindowSize(5)
                .build();

        final Filtered filtered = new SsnFilter(filterConfiguration).filter(contextService,
                getPolicy(), "context", PIECE, "the ssn is 123-45-6789");

        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertEquals("red", filtered.getSpans().get(0).getColor());

    }

    @Test
    void aFilteredSpanHasNoColorWhenTheStrategySetsNone() throws Exception {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new SsnFilterStrategy()))
                .withWindowSize(5)
                .build();

        final Filtered filtered = new SsnFilter(filterConfiguration).filter(contextService,
                getPolicy(), "context", PIECE, "the ssn is 123-45-6789");

        Assertions.assertNull(filtered.getSpans().get(0).getColor());

    }

    @Test
    void aShiftedOrCopiedSpanKeepsItsColor() {

        final Span span = Span.make(0, 4, FilterType.SSN, "ctx", 0.5, "text", "repl", null, false, true, null, 0);
        span.setColor("red");

        Assertions.assertEquals("red", span.copy().getColor());
        Assertions.assertEquals("red", Span.shiftSpans(10, List.of(span)).get(0).getColor());

    }

    @Test
    void textRedactionIsIdenticalWithAndWithoutAColor() throws Exception {

        final String input = "the ssn is 123-45-6789 and another is 987-65-4321";

        Assertions.assertEquals(filteredText(input, null), filteredText(input, "red"));
        Assertions.assertEquals(filteredText(input, null), filteredText(input, "#ff8800"));
        Assertions.assertEquals(filteredText(input, null), filteredText(input, "chartreuse"));

    }

    /** The filtered text for an SSN policy whose strategy carries the given color. */
    private String filteredText(final String input, final String color) throws Exception {

        final SsnFilterStrategy strategy = new SsnFilterStrategy();
        strategy.setColor(color);

        final Ssn ssn = new Ssn();
        ssn.setSsnFilterStrategies(List.of(strategy));

        final Identifiers identifiers = new Identifiers();
        identifiers.setSsn(ssn);

        final Policy policy = new Policy();
        policy.setIdentifiers(identifiers);

        return new PlainTextFilterService(new PhileasConfiguration(new Properties()), contextService,
                new InMemoryVectorService(), null)
                .filter(policy, "context", input)
                .getFilteredText();

    }

    @Test
    void theExpandedPaletteAppliesToBoundingBoxesAndTheFontColor() throws Exception {

        // Both resolve through the same table, so a hex value works for them too.
        final Method method = PdfRedactor.class.getDeclaredMethod("colorOrBlack", String.class);
        method.setAccessible(true);

        Assertions.assertArrayEquals(new float[]{0, 0, 1}, ((PDColor) method.invoke(null, "blue")).getComponents());
        Assertions.assertArrayEquals(new float[]{1, 136 / 255F, 0}, ((PDColor) method.invoke(null, "#FF8800")).getComponents());
        Assertions.assertArrayEquals(BLACK, ((PDColor) method.invoke(null, "not-a-color")).getComponents());

    }

    @Test
    void namedColorsUseOneComponentScale() throws Exception {

        // Every component is a 0 to 1 fraction, so none is a 0 to 255 value left over.
        for (final String name : List.of("black", "white", "red", "orange", "yellow", "green", "blue", "gray")) {
            for (final float component : colorFor(new Policy(), name)) {
                Assertions.assertTrue(component >= 0 && component <= 1, name + " component " + component);
            }
        }

        Assertions.assertArrayEquals(new float[]{1, 1, 0}, colorFor(new Policy(), "yellow"));

    }

    @Test
    void aStrategyColorOverridesThePolicyColor() throws Exception {

        final Policy policy = new Policy();
        policy.getConfig().getPdf().setRedactionColor("blue");

        Assertions.assertArrayEquals(RED, colorFor(policy, "red"));

    }

    @Test
    void thePolicyColorAppliesWhenTheStrategySetsNone() throws Exception {

        final Policy policy = new Policy();
        policy.getConfig().getPdf().setRedactionColor("blue");

        Assertions.assertArrayEquals(BLUE, colorFor(policy, null));
        Assertions.assertArrayEquals(BLUE, colorFor(policy, "   "));

    }

    @Test
    void blackAppliesWhenNeitherIsSet() throws Exception {
        Assertions.assertArrayEquals(BLACK, colorFor(new Policy(), null));
    }

    @Test
    void aHexColorIsAccepted() throws Exception {

        Assertions.assertArrayEquals(new float[]{1, 136 / 255F, 0}, colorFor(new Policy(), "#ff8800"));
        Assertions.assertArrayEquals(new float[]{1, 136 / 255F, 0}, colorFor(new Policy(), "#FF8800"));

    }

    @Test
    void anUnrecognizedStrategyColorRendersBlack() throws Exception {

        // Set but unusable, so it does not fall back to the policy color.
        final Policy policy = new Policy();
        policy.getConfig().getPdf().setRedactionColor("blue");

        Assertions.assertArrayEquals(BLACK, colorFor(policy, "chartreuse"));
        Assertions.assertArrayEquals(BLACK, colorFor(policy, "#ff88"));
        Assertions.assertArrayEquals(BLACK, colorFor(policy, "#gggggg"));

    }

    @Test
    void theSchemaNamedColorsAreAllSupported() throws Exception {

        for (final String name : List.of("black", "white", "red", "orange", "yellow", "green", "blue", "gray")) {

            final float[] components = colorFor(new Policy(), name);

            Assertions.assertEquals(3, components.length, name);

            for (final float component : components) {
                Assertions.assertTrue(component >= 0 && component <= 1,
                        name + " has a component outside the 0 to 1 range: " + component);
            }

            if (!"black".equals(name)) {
                Assertions.assertFalse(java.util.Arrays.equals(BLACK, components), name + " resolved to black");
            }

        }

    }

    @Test
    void anUppercaseNameIsAccepted() throws Exception {
        Assertions.assertArrayEquals(RED, colorFor(new Policy(), "RED"));
    }

}
