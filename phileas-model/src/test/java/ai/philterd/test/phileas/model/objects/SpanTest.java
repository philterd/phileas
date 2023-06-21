/*
 *     Copyright 2023 Philerd, LLC @ https://www.philterd.ai
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
package ai.philterd.test.phileas.model.objects;

import ai.philterd.phileas.model.enums.FilterType;
import ai.philterd.phileas.model.objects.Span;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class SpanTest {

    private static final Logger LOGGER = LogManager.getLogger(SpanTest.class);

    @Test
    public void equalsContract() {
        EqualsVerifier.forClass(Span.class).suppress(Warning.NONFINAL_FIELDS).verify();
    }

    @Test
    public void cloneTest() {

        Span span1 = Span.make(1, 6, FilterType.PERSON, "context", "document", 1.0,  "test", "***", "salt",  false, new String[0]);
        Span span2 = span1.copy();

        Assertions.assertTrue(span1.equals(span2));

    }

    @Test
    public void shiftSpansTest1() {

        Span span1 = Span.make(1, 6, FilterType.PERSON, "context", "document", 1.0, "test", "***", "salt",  false, new String[0]);
        Span span2 = Span.make(8, 12, FilterType.PERSON, "context", "document", 1.0, "test", "***", "salt",  false, new String[0]);
        Span span3 = Span.make(14, 20, FilterType.PERSON, "context", "document", 1.0, "test", "***", "salt",  false, new String[0]);

        final List<Span> spans = Arrays.asList(span1, span2, span3);
        final List<Span> shiftedSpans = Span.shiftSpans(4, span1, spans);

        Assertions.assertEquals(2, shiftedSpans.size());
        Assertions.assertEquals(12, shiftedSpans.get(0).getCharacterStart());
        Assertions.assertEquals(16, shiftedSpans.get(0).getCharacterEnd());
        Assertions.assertEquals(18, shiftedSpans.get(1).getCharacterStart());
        Assertions.assertEquals(24, shiftedSpans.get(1).getCharacterEnd());

    }

    @Test
    public void shiftSpansTest2() {

        Span span1 = Span.make(1, 6, FilterType.PERSON, "context", "document", 1.0, "test", "***", "salt",  false, new String[0]);

        final List<Span> spans = Arrays.asList(span1);
        final List<Span> shiftedSpans = Span.shiftSpans(4, span1, spans);

        Assertions.assertEquals(0, shiftedSpans.size());

    }

    @Test
    public void doesIndexStartSpanTest1() {

        Span span1 = Span.make(1, 6, FilterType.PERSON, "context", "document", 1.0, "test", "***", "salt",  false, new String[0]);
        Span span2 = Span.make(8, 12, FilterType.PERSON, "context", "document", 1.0, "test", "***", "salt",  false, new String[0]);

        List<Span> spans = Arrays.asList(span1, span2);

        Span span = Span.doesIndexStartSpan(8, spans);

        Assertions.assertNotNull(span);
        Assertions.assertEquals(span2, span);

    }

    @Test
    public void doesIndexStartSpanTest2() {

        Span span1 = Span.make(1, 6, FilterType.PERSON, "context", "document", 1.0, "test", "***", "salt",  false, new String[0]);
        Span span2 = Span.make(8, 12, FilterType.PERSON, "context", "document", 1.0, "test", "***", "salt",  false, new String[0]);

        List<Span> spans = Arrays.asList(span1, span2);

        Span span = Span.doesIndexStartSpan(1, spans);

        Assertions.assertNotNull(span);
        Assertions.assertEquals(span1, span);

    }

    @Test
    public void doesIndexStartSpanTest3() {

        Span span1 = Span.make(1, 6, FilterType.PERSON, "context", "document", 1.0, "test", "***", "salt",  false, new String[0]);
        Span span2 = Span.make(8, 12, FilterType.PERSON, "context", "document", 1.0, "test", "***", "salt",  false, new String[0]);

        List<Span> spans = Arrays.asList(span1, span2);

        Span span = Span.doesIndexStartSpan(4, spans);

        Assertions.assertNull(span);

    }

    @Test
    public void ignored1() {

        List<Span> spans = new LinkedList<>();
        spans.add(Span.make(1, 5, FilterType.PERSON, "context", "document", 1.0, "test", "***", "salt",  false, new String[0]));
        spans.add(Span.make(2, 12, FilterType.PERSON, "context", "document", 1.0, "test", "***", "salt",  true, new String[0]));

        List<Span> nonIgnoredSpans = Span.dropIgnoredSpans(spans);

        showSpans(nonIgnoredSpans);

        Assertions.assertEquals(1, nonIgnoredSpans.size());
        Assertions.assertEquals(1, nonIgnoredSpans.get(0).getCharacterStart());

    }

    @Test
    public void ignored2() {

        List<Span> spans = new LinkedList<>();
        spans.add(Span.make(1, 5, FilterType.PERSON, "context", "document", 1.0, "test", "***", "salt",  false, new String[0]));
        spans.add(Span.make(2, 12, FilterType.PERSON, "context", "document", 1.0, "test", "***", "salt",  false, new String[0]));

        List<Span> nonIgnoredSpans = Span.dropIgnoredSpans(spans);

        showSpans(nonIgnoredSpans);

        Assertions.assertEquals(2, nonIgnoredSpans.size());
        Assertions.assertEquals(1, nonIgnoredSpans.get(0).getCharacterStart());
        Assertions.assertEquals(2, nonIgnoredSpans.get(1).getCharacterStart());

    }

    @Test
    public void ignored3() {

        List<Span> spans = new LinkedList<>();
        spans.add(Span.make(1, 5, FilterType.PERSON, "context", "document", 1.0, "test", "***", "salt",  true, new String[0]));
        spans.add(Span.make(2, 12, FilterType.PERSON, "context", "document", 1.0, "test", "***", "salt",  true, new String[0]));

        List<Span> nonIgnoredSpans = Span.dropIgnoredSpans(spans);

        showSpans(nonIgnoredSpans);

        Assertions.assertEquals(0, nonIgnoredSpans.size());

    }

    @Test
    public void overlapping1() {

        List<Span> spans = new LinkedList<>();
        spans.add(Span.make(1, 5, FilterType.PERSON, "context", "document", 1.0, "test", "***", "salt",  false, new String[0]));
        spans.add(Span.make(2, 12, FilterType.PERSON, "context", "document", 1.0, "test", "***", "salt",  false, new String[0]));

        List<Span> nonOverlappingSpans = Span.dropOverlappingSpans(spans);

        showSpans(nonOverlappingSpans);

        Assertions.assertEquals(1, nonOverlappingSpans.size());
        Assertions.assertEquals(2, nonOverlappingSpans.get(0).getCharacterStart());
        Assertions.assertEquals(12, nonOverlappingSpans.get(0).getCharacterEnd());

    }

    @Test
    public void overlapping2() {

        List<Span> spans = new LinkedList<>();
        spans.add(Span.make(2, 12, FilterType.PERSON, "context", "document", 0.5, "test", "***", "salt",  false, new String[0]));
        spans.add(Span.make(2, 12, FilterType.PERSON, "context", "document", 1.0, "test", "***", "salt",  false, new String[0]));

        List<Span> nonOverlappingSpans = Span.dropOverlappingSpans(spans);

        Assertions.assertEquals(1, nonOverlappingSpans.size());
        Assertions.assertEquals(2, nonOverlappingSpans.get(0).getCharacterStart());
        Assertions.assertEquals(12, nonOverlappingSpans.get(0).getCharacterEnd());
        Assertions.assertEquals(1.0, nonOverlappingSpans.get(0).getConfidence(), 0);

    }

    @Test
    public void overlapping3() {

        List<Span> spans = new LinkedList<>();
        spans.add(Span.make(2, 12, FilterType.PERSON, "context", "document", 0.5, "test", "***", "salt",  false, new String[0]));
        spans.add(Span.make(14, 20, FilterType.PERSON, "context", "document", 1.0, "test", "***", "salt",  false, new String[0]));

        List<Span> nonOverlappingSpans = Span.dropOverlappingSpans(spans);

        Assertions.assertEquals(2, nonOverlappingSpans.size());

    }

    @Test
    public void overlapping4() {

        List<Span> spans = new LinkedList<>();
        spans.add(Span.make(2, 12, FilterType.PERSON, "context", "document", 0.5, "test", "***", "salt",  false, new String[0]));

        List<Span> nonOverlappingSpans = Span.dropOverlappingSpans(spans);

        Assertions.assertEquals(1, nonOverlappingSpans.size());

    }

    @Test
    public void overlapping5() {

        List<Span> spans = new LinkedList<>();
        spans.add(Span.make(7, 17, FilterType.PERSON, "context", "document", 0.5, "test", "***", "salt",  false, new String[0]));
        spans.add(Span.make(0, 17, FilterType.PERSON, "context", "document", 1.0, "test", "***", "salt",  false, new String[0]));

        List<Span> nonOverlappingSpans = Span.dropOverlappingSpans(spans);

        Assertions.assertEquals(1, nonOverlappingSpans.size());
        Assertions.assertEquals(0, nonOverlappingSpans.get(0).getCharacterStart());
        Assertions.assertEquals(17, nonOverlappingSpans.get(0).getCharacterEnd());
        Assertions.assertEquals(1.0, nonOverlappingSpans.get(0).getConfidence(), 0);

    }

    @Test
    public void overlapping6() {

        // Duplicate spans should be dropped in favor of the one that appears in the list first.

        final List<Span> spans = new LinkedList<>();
        spans.add(Span.make(7, 17, FilterType.ZIP_CODE, "context", "document", 1.0, "test", "***", "salt",  false, new String[0]));
        spans.add(Span.make(7, 17, FilterType.IDENTIFIER, "context", "document", 1.0, "test", "***", "salt",  false, new String[0]));

        final List<Span> nonOverlappingSpans = Span.dropOverlappingSpans(spans);

        showSpans(nonOverlappingSpans);

        Assertions.assertEquals(1, nonOverlappingSpans.size());
        Assertions.assertEquals(7, nonOverlappingSpans.get(0).getCharacterStart());
        Assertions.assertEquals(17, nonOverlappingSpans.get(0).getCharacterEnd());
        Assertions.assertEquals(FilterType.IDENTIFIER, nonOverlappingSpans.get(0).getFilterType());

    }

    @Test
    public void overlapping7() {

        // Duplicate spans should be dropped in favor of the one that appears in the list first.

        final List<Span> spans = new LinkedList<>();
        spans.add(Span.make(7, 17, FilterType.ZIP_CODE, "context", "document", 1.0, "test", "***", "salt",  false, new String[0]));
        spans.add(Span.make(10, 17, FilterType.IDENTIFIER, "context", "document", 1.0, "test", "***", "salt",  false, new String[0]));
        spans.add(Span.make(13, 17, FilterType.IDENTIFIER, "context", "document", 1.0, "test", "***", "salt",  false, new String[0]));

        final List<Span> nonOverlappingSpans = Span.dropOverlappingSpans(spans);
        //final List<Span> nonOverlappingSpans2 = Span.dropOverlappingSpans(nonOverlappingSpans);

        showSpans(nonOverlappingSpans);

        Assertions.assertEquals(1, nonOverlappingSpans.size());
        Assertions.assertEquals(7, nonOverlappingSpans.get(0).getCharacterStart());
        Assertions.assertEquals(17, nonOverlappingSpans.get(0).getCharacterEnd());
        Assertions.assertEquals(FilterType.ZIP_CODE, nonOverlappingSpans.get(0).getFilterType());

    }

    @Test
    public void overlapping8() {

        final List<Span> spans = new LinkedList<>();
        spans.add(Span.make(10, 38, FilterType.PHYSICIAN_NAME, "context", "document", 1.0, "test", "***", "salt",  false, new String[0]));
        spans.add(Span.make(20, 38, FilterType.PHYSICIAN_NAME, "context", "document", 1.0, "test", "***", "salt",  false, new String[0]));
        spans.add(Span.make(24, 38, FilterType.PHYSICIAN_NAME, "context", "document", 1.0, "test", "***", "salt",  false, new String[0]));
        spans.add(Span.make(29, 38, FilterType.PHYSICIAN_NAME, "context", "document", 1.0, "test", "***", "salt",  false, new String[0]));

        final List<Span> nonOverlappingSpans = Span.dropOverlappingSpans(spans);

        showSpans(nonOverlappingSpans);

        Assertions.assertEquals(1, nonOverlappingSpans.size());
        Assertions.assertEquals(10, nonOverlappingSpans.get(0).getCharacterStart());
        Assertions.assertEquals(38, nonOverlappingSpans.get(0).getCharacterEnd());
        Assertions.assertEquals(FilterType.PHYSICIAN_NAME, nonOverlappingSpans.get(0).getFilterType());

    }

    @Test
    public void overlapping9() {

        final List<Span> spans = new LinkedList<>();
        spans.add(Span.make(0, 6, FilterType.PHYSICIAN_NAME, "context", "document", 1.0, "test", "***", "salt",  false, new String[0]));
        spans.add(Span.make(0, 12, FilterType.PHYSICIAN_NAME, "context", "document", 1.0, "test", "***", "salt",  false, new String[0]));
        spans.add(Span.make(0, 18, FilterType.PHYSICIAN_NAME, "context", "document", 1.0, "test", "***", "salt",  false, new String[0]));

        final List<Span> nonOverlappingSpans = Span.dropOverlappingSpans(spans);

        showSpans(nonOverlappingSpans);

        Assertions.assertEquals(1, nonOverlappingSpans.size());
        Assertions.assertEquals(0, nonOverlappingSpans.get(0).getCharacterStart());
        Assertions.assertEquals(18, nonOverlappingSpans.get(0).getCharacterEnd());
        Assertions.assertEquals(FilterType.PHYSICIAN_NAME, nonOverlappingSpans.get(0).getFilterType());

    }

    @Test
    public void getIdenticalSpans1() {

        final Span span1 = Span.make(7, 17, FilterType.ZIP_CODE, "context", "document", 1.0, "test", "***", "salt",  false, new String[0]);

        final List<Span> spans = new LinkedList<>();
        spans.add(span1);
        spans.add(Span.make(7, 17, FilterType.ZIP_CODE, "context", "document", 1.0, "test", "***", "salt",  false, new String[0]));
        spans.add(Span.make(7, 17, FilterType.IDENTIFIER, "context", "document", 1.0, "test", "***", "salt",  false, new String[0]));
        spans.add(Span.make(4, 19, FilterType.IDENTIFIER, "context", "document", 1.0, "test", "***", "salt",  false, new String[0]));
        spans.add(Span.make(22, 25, FilterType.IDENTIFIER, "context", "document", 1.0, "test", "***", "salt",  false, new String[0]));

        final List<Span> identicalSpans = Span.getIdenticalSpans(span1, spans);

        Assertions.assertEquals(1, identicalSpans.size());

    }

    @Test
    public void getIdenticalSpans2() {

        final Span span1 = Span.make(7, 17, FilterType.ZIP_CODE, "context", "document", 1.0, "test", "***", "salt",  false, new String[0]);

        final List<Span> spans = new LinkedList<>();
        spans.add(span1);
        spans.add(Span.make(7, 17, FilterType.IDENTIFIER, "context", "document", 1.0, "test", "***", "salt",  false, new String[0]));
        spans.add(Span.make(4, 19, FilterType.IDENTIFIER, "context", "document", 1.0, "test", "***", "salt",  false, new String[0]));
        spans.add(Span.make(22, 25, FilterType.IDENTIFIER, "context", "document", 1.0, "test", "***", "salt",  false, new String[0]));
        spans.add(Span.make(7, 17, FilterType.URL, "context", "document", 1.0, "test", "***", "salt",  false, new String[0]));

        final List<Span> identicalSpans = Span.getIdenticalSpans(span1, spans);

        Assertions.assertEquals(2, identicalSpans.size());

    }

    @Test
    public void getIdenticalSpans3() {

        final Span span1 = Span.make(7, 17, FilterType.ZIP_CODE, "context", "document", 1.0, "test", "***", "salt",  false, new String[0]);

        final List<Span> spans = new LinkedList<>();
        spans.add(span1);
        spans.add(Span.make(7, 17, FilterType.ZIP_CODE, "context", "document", 1.0, "test", "***", "salt",  false, new String[0]));
        spans.add(Span.make(7, 17, FilterType.IDENTIFIER, "context", "document", 1.0, "test", "***", "salt",  false, new String[0]));
        spans.add(Span.make(4, 19, FilterType.IDENTIFIER, "context", "document", 1.0, "test", "***", "salt",  false, new String[0]));
        spans.add(Span.make(22, 25, FilterType.IDENTIFIER, "context", "document", 1.0, "test", "***", "salt",  false, new String[0]));
        spans.add(Span.make(7, 17, FilterType.URL, "context", "document", 1.0, "test", "***", "salt",  false, new String[0]));
        spans.add(Span.make(22, 25, FilterType.AGE, "context", "document", 1.0, "test", "***", "salt",  false, new String[0]));

        final List<Span> identicalSpans = Span.getIdenticalSpans(span1, spans);

        Assertions.assertEquals(2, identicalSpans.size());

    }

    @Test
    public void adjacent1() {

        final Span span1 = Span.make(7, 11, FilterType.ZIP_CODE, "context", "document", 1.0, "test", "***", "salt",  false, new String[0]);
        final Span span2 = Span.make(13, 17, FilterType.ZIP_CODE, "context", "document", 1.0, "qwer", "***", "salt",  false, new String[0]);
        final String text = "asdfbf test qwer asdf";

        final boolean adjacent = Span.areSpansAdjacent(span1, span2, text);

        Assertions.assertTrue(adjacent);

    }

    @Test
    public void adjacent2() {

        final Span span1 = Span.make(7, 11, FilterType.ZIP_CODE, "context", "document", 1.0, "test", "***", "salt",  false, new String[0]);
        final Span span2 = Span.make(12, 16, FilterType.ZIP_CODE, "context", "document", 1.0, "qwer", "***", "salt",  false, new String[0]);
        final String text = "asdfbf testqwer asdf";

        final boolean adjacent = Span.areSpansAdjacent(span1, span2, text);

        Assertions.assertTrue(adjacent);

    }

    @Test
    public void adjacent3() {

        final Span span1 = Span.make(7, 11, FilterType.ZIP_CODE, "context", "document", 1.0, "test", "***", "salt",  false, new String[0]);
        final Span span2 = Span.make(15, 16, FilterType.ZIP_CODE, "context", "document", 1.0, "qwer", "***", "salt",  false, new String[0]);
        final String text = "asdfbf test   qwer asdf";

        final boolean adjacent = Span.areSpansAdjacent(span1, span2, text);

        Assertions.assertTrue(adjacent);

    }

    @Test
    public void adjacent4() {

        final Span span1 = Span.make(7, 11, FilterType.ZIP_CODE, "context", "document", 1.0, "test", "***", "salt",  false, new String[0]);
        final Span span2 = Span.make(15, 16, FilterType.ZIP_CODE, "context", "document", 1.0, "qwer", "***", "salt",  false, new String[0]);
        final String text = "asdfbf test f  qwer asdf";

        final boolean adjacent = Span.areSpansAdjacent(span1, span2, text);

        Assertions.assertFalse(adjacent);

    }

    @Test
    public void adjacent5() {

        final Span span1 = Span.make(7, 11, FilterType.ZIP_CODE, "context", "document", 1.0, "test", "***", "salt",  false, new String[0]);
        final Span span2 = Span.make(15, 16, FilterType.ZIP_CODE, "context", "document", 1.0, "qwer", "***", "salt",  false, new String[0]);
        final String text = "asdfbf test .  qwer asdf";

        final boolean adjacent = Span.areSpansAdjacent(span1, span2, text);

        Assertions.assertFalse(adjacent);

    }

    @Test
    public void adjacent6() {

        final Span span1 = Span.make(7, 11, FilterType.ZIP_CODE, "context", "document", 1.0, "test", "***", "salt",  false, new String[0]);
        final Span span2 = Span.make(15, 16, FilterType.ZIP_CODE, "context", "document", 1.0, "qwer", "***", "salt",  false, new String[0]);
        final String text = "asdfbf test    qwer asdf";

        final boolean adjacent = Span.areSpansAdjacent(span2, span1, text);

        Assertions.assertFalse(adjacent);

    }

    @Test
    public void adjacent7() {

        final Span span1 = Span.make(0, 5, FilterType.ZIP_CODE, "context", "document", 1.0, "Smith", "***", "salt",  false, new String[0]);
        final Span span2 = Span.make(7, 11, FilterType.ZIP_CODE, "context", "document", 1.0, "John", "***", "salt",  false, new String[0]);
        final String text = "Smith, John D asdf";

        final boolean adjacent = Span.areSpansAdjacent(span1, span2, text);

        Assertions.assertTrue(adjacent);

    }

    @Test
    public void lapps1() throws IOException {

        final File file = new File("src/test/resources/lapps/lapps1.json");
        final String input = FileUtils.readFileToString(file, Charset.defaultCharset());

        final List<Span> spans = Span.fromLappsJson(input);

        Assertions.assertEquals(2, spans.size());

    }

    private void showSpans(List<Span> spans) {

        for(Span span : spans) {
            LOGGER.info(span.toString());
        }

    }

}
