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

import ai.philterd.phileas.model.filtering.Cue;
import ai.philterd.phileas.services.filters.regex.CueWords;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

public class CueWordsTest {

    private static final CueWords CUE_WORDS = new CueWords(List.of("member ID", "ID", "serial number"), null, true);

    private static String nearest(final String text, final String value) {
        final Cue cue = CUE_WORDS.nearest(text, text.indexOf(value), 5);
        return cue == null ? null : text.substring(cue.start(), cue.end());
    }

    @Test
    public void theNearestCueWordIsFound() {
        Assertions.assertEquals("ID:", nearest("Serial Number 1234 ID: 5678", "5678"));
        Assertions.assertEquals("Serial Number", nearest("Serial Number 1234", "1234"));
    }

    @Test
    public void ofCueWordsEndingTogetherTheLongerIsNearer() {
        Assertions.assertEquals("Member ID:", nearest("Member ID: 5678", "5678"));
        Assertions.assertTrue(new Cue(0, 10).isNearerThan(new Cue(7, 10)));
        Assertions.assertFalse(new Cue(7, 10).isNearerThan(new Cue(0, 10)));
    }

    @Test
    public void aCueWordEndingLaterIsNearer() {
        Assertions.assertTrue(new Cue(20, 23).isNearerThan(new Cue(0, 13)));
        Assertions.assertFalse(new Cue(0, 13).isNearerThan(new Cue(20, 23)));
        Assertions.assertFalse(new Cue(0, 13).isNearerThan(new Cue(0, 13)));
    }

    @Test
    public void offsetsHoldWhenLowerCasingChangesTheTextsLength() {
        // "İ" lower-cases to two characters.
        Assertions.assertEquals("ID:", nearest("İİİ ID: 5678", "5678"));
    }

    @Test
    public void noCueWordIsNull() {
        Assertions.assertNull(nearest("Account 5678", "5678"));
        Assertions.assertNull(nearest("one two three four five six ID seven eight nine ten eleven 5678", "5678"));
    }

}
