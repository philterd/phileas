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
import ai.philterd.phileas.model.filtering.Span;
import ai.philterd.phileas.services.filters.regex.CueWordFilter;
import ai.philterd.phileas.services.filters.regex.DeviceIdentifierFilter;
import ai.philterd.phileas.services.strategies.rules.DeviceIdentifierFilterStrategy;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

// The UDIs are the examples in the PhiSQL detection contract. Serial numbers are invented.
public class DeviceIdentifierFilterTest extends AbstractFilterTest {

    private static final String GS1_UDI = "(01)00812345000016(17)270128(21)SN0042";

    private DeviceIdentifierFilter filter(final List<String> patterns) {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new DeviceIdentifierFilterStrategy()))
                .withWindowSize(windowSize)
                .build();

        return new DeviceIdentifierFilter(filterConfiguration, patterns, null, true);

    }

    private List<Span> spans(final DeviceIdentifierFilter filter, final String text) throws Exception {
        return filter.filter(contextService, getPolicy(), "context", PIECE, text).getSpans();
    }

    @Test
    public void aGs1UdiIsMatchedWholeWithNoCueWord() throws Exception {

        final List<Span> spans = spans(filter(null), "implanted " + GS1_UDI + " today");
        Assertions.assertEquals(1, spans.size());
        Assertions.assertTrue(checkSpan(spans.get(0), 10, 10 + GS1_UDI.length(), FilterType.DEVICE_IDENTIFIER));
        Assertions.assertTrue(spans.get(0).getYieldsTo().isEmpty());

    }

    @Test
    public void aGs1UdiWithOnlyItsGtinIsMatched() throws Exception {

        final List<Span> spans = spans(filter(null), "UDI (01)00812345000016.");
        Assertions.assertEquals(1, spans.size());
        Assertions.assertEquals("(01)00812345000016", spans.get(0).getText());

    }

    @Test
    public void aGs1UdiWithAWrongCheckDigitIsNotMatched() throws Exception {
        Assertions.assertEquals(0, spans(filter(null), "implanted (01)00812345000017(17)270128(21)SN0042 today").size());
    }

    @Test
    public void aHibccUdiIsMatchedOnlyWithAValidCheckCharacter() throws Exception {

        final List<Span> spans = spans(filter(null), "label +H123ABC01C on the box");
        Assertions.assertEquals(1, spans.size());
        Assertions.assertEquals("+H123ABC01C", spans.get(0).getText());

        Assertions.assertEquals(0, spans(filter(null), "label +H123ABC01D on the box").size());

    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "S/N: SN0042                 | SN0042",
            "SN 4X2-0091                 | 4X2-0091",
            "Serial No. 4X2-0091         | 4X2-0091",
            "Serial No: 4X2-0091         | 4X2-0091",
            "serial number ABC123        | ABC123",
            "Serial #4X2-0091            | 4X2-0091",
            "Device ID: PM-77231         | PM-77231"
    })
    public void aSerialNumberAfterACueWordIsMatched(final String text, final String expected) throws Exception {

        final List<Span> spans = spans(filter(null), text);
        Assertions.assertEquals(1, spans.size(), text);
        Assertions.assertEquals(expected, spans.get(0).getText(), text);
        Assertions.assertEquals(CueWordFilter.YIELDS_TO, spans.get(0).getYieldsTo(), text);

    }

    @ParameterizedTest
    @ValueSource(strings = {
            "SNOW 4X2-0091",          // Not the whole cue word.
            "lot 4X2-0091",           // No cue word.
            "S/N: ABCDEF",            // No digit.
            "S/N: A12"                // Too short.
    })
    public void aSerialNumberWithoutACueWordIsNotMatched(final String text) throws Exception {
        Assertions.assertEquals(0, spans(filter(null), text).size(), text);
    }

    @Test
    public void aSerialNumberPatternInsideAUdiGivesWayToTheUdi() throws Exception {

        final List<Span> spans = spans(filter(null), "S/N " + GS1_UDI);
        Assertions.assertEquals(1, spans.size());
        Assertions.assertEquals(GS1_UDI, spans.get(0).getText());

    }

    @Test
    public void patternsReplaceTheSerialNumberPatternButNotTheUdiChecks() throws Exception {

        final DeviceIdentifierFilter filter = filter(List.of("\\b[0-9]{8}\\b"));

        Assertions.assertEquals(0, spans(filter, "S/N: SN0042").size());
        Assertions.assertEquals(1, spans(filter, "S/N: 12345678").size());
        Assertions.assertEquals(1, spans(filter, "implanted " + GS1_UDI).size());

    }

    @Test
    public void checkCharacters() {

        Assertions.assertTrue(DeviceIdentifierFilter.isValidGtin("00812345000016"));
        Assertions.assertFalse(DeviceIdentifierFilter.isValidGtin("00812345000017"));
        Assertions.assertTrue(DeviceIdentifierFilter.isValidHibcc("+H123ABC01C"));
        Assertions.assertFalse(DeviceIdentifierFilter.isValidHibcc("+H123ABC01D"));
        Assertions.assertTrue(DeviceIdentifierFilter.isUdi(GS1_UDI));
        Assertions.assertFalse(DeviceIdentifierFilter.isUdi("SN0042"));

    }

}
