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
import ai.philterd.phileas.model.filtering.Filtered;
import ai.philterd.phileas.model.filtering.Span;
import ai.philterd.phileas.services.filters.regex.CueWordFilter;
import ai.philterd.phileas.services.filters.regex.MedicalRecordNumberFilter;
import ai.philterd.phileas.services.strategies.rules.MedicalRecordNumberFilterStrategy;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

// All medical record numbers in these tests are invented.
public class MedicalRecordNumberFilterTest extends AbstractFilterTest {

    private MedicalRecordNumberFilter filter(final List<String> patterns, final List<String> cueWords,
                                             final boolean defaultCueWords) {

        final FilterConfiguration filterConfiguration = new FilterConfiguration.FilterConfigurationBuilder()
                .withStrategies(List.of(new MedicalRecordNumberFilterStrategy()))
                .withWindowSize(windowSize)
                .build();

        return new MedicalRecordNumberFilter(filterConfiguration, patterns, cueWords, defaultCueWords);

    }

    private MedicalRecordNumberFilter filter() {
        return filter(null, null, true);
    }

    private List<Span> spans(final MedicalRecordNumberFilter filter, final String text) throws Exception {
        return filter.filter(contextService, getPolicy(), "context", PIECE, text).getSpans();
    }

    @Test
    public void aNumberRightAfterACueWordIsMatchedAndTheCueWordIsNot() throws Exception {

        final Filtered filtered = filter().filter(contextService, getPolicy(), "context", PIECE, "MRN: 00482913");
        Assertions.assertEquals(1, filtered.getSpans().size());
        Assertions.assertTrue(checkSpan(filtered.getSpans().get(0), 5, 13, FilterType.MEDICAL_RECORD_NUMBER));
        Assertions.assertEquals(CueWordFilter.YIELDS_TO, filtered.getSpans().get(0).getYieldsTo());

    }

    @Test
    public void aNumberAFewWordsAfterACueWordIsMatched() throws Exception {

        final List<Span> spans = spans(filter(), "MRN on file: 00482913.");
        Assertions.assertEquals(1, spans.size());
        Assertions.assertEquals("00482913", spans.get(0).getText());

    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "MRN 00482913                        | 00482913",
            "mrn: 00482913                       | 00482913",
            "MR# 7731-0042                       | 7731-0042",
            "MR #: A1234567                      | A1234567",
            "MRN: A12345678901                   | A12345678901",
            "Med Rec # 00482913                  | 00482913",
            "Med Rec#00482913                    | 00482913",
            "Medical Record #: 00482913          | 00482913",
            "Medical Record No: 00482913         | 00482913",
            "Medical Record No. 00482913         | 00482913",
            "Medical record number 00482913      | 00482913",
            "(Chart #: CH-104422)                | CH-104422",
            "Chart Number: 104422                | 104422"
    })
    public void eachDefaultCueWordFormIsMatched(final String text, final String expected) throws Exception {

        final List<Span> spans = spans(filter(), text);
        Assertions.assertEquals(1, spans.size(), text);
        Assertions.assertEquals(expected, spans.get(0).getText(), text);

    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Account 00482913",                                      // No cue word.
            "Patient ID: 00482913",                                  // Not a default cue word.
            "00482913 is the MRN",                                   // The cue word comes after the value.
            "MRN was written on the intake form as 00482913",        // The cue word is outside the window.
            "MRN: ABCDEFGH",                                         // No digit.
            "MRN: A1234",                                            // Too short.
            "MRN: A123456789012",                                    // Too long.
            "MRNS 00482913",                                         // Not the whole word.
            "Chartered 00482913"                                     // Not the whole word.
    })
    public void valuesWithoutACueWordBeforeThemAreNotMatched(final String text) throws Exception {
        Assertions.assertEquals(0, spans(filter(), text).size(), text);
    }

    @Test
    public void patternsReplaceTheDefaultPattern() throws Exception {

        final MedicalRecordNumberFilter filter = filter(List.of("\\b[A-Z]{2}[0-9]{6}\\b"), null, true);

        Assertions.assertEquals(1, spans(filter, "MRN: AB123456").size());
        Assertions.assertEquals(0, spans(filter, "MRN: 00482913").size());

    }

    @Test
    public void cueWordsAddToTheDefaults() throws Exception {

        final MedicalRecordNumberFilter filter = filter(null, List.of("patient ID"), true);

        Assertions.assertEquals(1, spans(filter, "Patient ID: 00482913").size());
        Assertions.assertEquals(1, spans(filter, "MRN: 00482913").size());

    }

    @Test
    public void cueWordsReplaceTheDefaultsWhenDefaultCueWordsIsFalse() throws Exception {

        final MedicalRecordNumberFilter filter = filter(null, List.of("patient ID"), false);

        Assertions.assertEquals(1, spans(filter, "Patient ID: 00482913").size());
        Assertions.assertEquals(0, spans(filter, "MRN: 00482913").size());

    }

    @Test
    public void defaultCueWordsFalseWithNoCueWordsIsRefused() {

        Assertions.assertThrows(IllegalArgumentException.class, () -> filter(null, null, false));
        Assertions.assertThrows(IllegalArgumentException.class, () -> filter(null, List.of(), false));

    }

}
