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
package ai.philterd.phileas.services.split;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

public class SplitOverlapTest {

    @Test
    public void eachPieceCarriesTheTailOfThePreviousOne() {

        final String input = "line one\nline two\nline three";
        final List<TextSplit> splits = new NewLineSplitService().splitWithOverlap(input, 4).orElseThrow();

        Assertions.assertEquals(3, splits.size());

        // The first piece has nothing before it to overlap with.
        Assertions.assertEquals("line one", splits.get(0).text());
        Assertions.assertEquals(0, splits.get(0).offset());

        // Each later piece starts four characters earlier in the input.
        Assertions.assertEquals(5, splits.get(1).offset());
        Assertions.assertEquals("one\nline two", splits.get(1).text());
        Assertions.assertEquals(14, splits.get(2).offset());
        Assertions.assertEquals("two\nline three", splits.get(2).text());

    }

    @Test
    public void offsetsLocateEachPieceInTheInput() {

        final String input = "line one\nline two\nline three";

        for(final TextSplit split : new NewLineSplitService().splitWithOverlap(input, 4).orElseThrow()) {
            Assertions.assertEquals(split.text(), input.substring(split.offset(), split.offset() + split.text().length()));
        }

    }

    @Test
    public void noOverlapLeavesPiecesContiguous() {

        final String input = "line one\nline two";
        final List<TextSplit> splits = new NewLineSplitService().splitWithOverlap(input, 0).orElseThrow();

        Assertions.assertEquals("line one", splits.get(0).text());
        Assertions.assertEquals("line two", splits.get(1).text());
        Assertions.assertEquals(9, splits.get(1).offset());
        Assertions.assertEquals(input, splits.get(0).text() + "\n" + splits.get(1).text());

    }

    @Test
    public void anOverlapLongerThanTheInputIsClamped() {

        final String input = "line one\nline two";
        final List<TextSplit> splits = new NewLineSplitService().splitWithOverlap(input, 500).orElseThrow();

        Assertions.assertEquals(0, splits.get(1).offset());
        Assertions.assertEquals(input, splits.get(1).text());

    }

    @Test
    public void duplicateLinesAreEachLocatedAtTheirOwnPosition() {

        // Identical pieces must not all resolve to the first match.
        final String input = "row\nrow\nrow";
        final List<TextSplit> splits = new NewLineSplitService().splitWithOverlap(input, 2).orElseThrow();

        Assertions.assertEquals(3, splits.size());
        Assertions.assertEquals(0, splits.get(0).offset());
        Assertions.assertEquals(2, splits.get(1).offset());
        Assertions.assertEquals(6, splits.get(2).offset());

    }

    @Test
    public void sentencePiecesThatAreVerbatimGetAnOverlap() {

        final String input = "Ada lives here. The ssn is 123-45-6789. Bob lives there.";
        final List<TextSplit> splits = new CharacterCountSplitService(20).splitWithOverlap(input, 6).orElseThrow();

        Assertions.assertTrue(splits.size() > 1);
        assertEveryPieceIsVerbatim(input, splits);
        Assertions.assertTrue(splits.get(1).text().startsWith("here."), splits.get(1).text());

    }

    @Test
    public void everyPieceIsVerbatimAtItsOffsetForEachSplitter() {

        final String input = "Ada lives here. The ssn is 123-45-6789. Bob lives there. The date was May 22, 1999.";

        for(final SplitService splitService : List.of(new NewLineSplitService(),
                new LineWidthSplitService(25), new CharacterCountSplitService(25))) {

            splitService.splitWithOverlap(input, 8).ifPresent(splits -> assertEveryPieceIsVerbatim(input, splits));

        }

    }

    @Test
    public void offsetsNeverGoBackwards() {

        final String input = "one one one one one one";
        final List<TextSplit> splits = new LineWidthSplitService(8).splitWithOverlap(input, 4).orElseThrow();

        for(int i = 1; i < splits.size(); i++) {
            Assertions.assertTrue(splits.get(i).offset() >= splits.get(i - 1).offset(),
                    "offsets must be in document order: " + splits);
        }

    }

    @Test
    public void aNegativeOverlapIsTreatedAsNone() {

        final String input = "line one\nline two";
        final List<TextSplit> splits = new NewLineSplitService().splitWithOverlap(input, -50).orElseThrow();

        Assertions.assertEquals("line two", splits.get(1).text());
        Assertions.assertEquals(9, splits.get(1).offset());

    }

    @Test
    public void blankInputProducesNoPieces() {

        Assertions.assertTrue(new CharacterCountSplitService(20).splitWithOverlap("   ", 5).orElseThrow().isEmpty());

    }

    @Test
    public void anInputThatDoesNotSplitIsOnePieceAtZero() {

        final String input = "just one line";
        final List<TextSplit> splits = new NewLineSplitService().splitWithOverlap(input, 5).orElseThrow();

        Assertions.assertEquals(1, splits.size());
        Assertions.assertEquals(input, splits.get(0).text());
        Assertions.assertEquals(0, splits.get(0).offset());

    }

    @Test
    public void surroundingWhitespaceDoesNotShiftOffsets() {

        // The splitter trims each piece, so the offsets must skip the whitespace, not include it.
        final String input = "   line one   \n   line two   ";
        final List<TextSplit> splits = new NewLineSplitService().splitWithOverlap(input, 3).orElseThrow();

        assertEveryPieceIsVerbatim(input, splits);
        Assertions.assertEquals(3, splits.get(0).offset());

    }

    private void assertEveryPieceIsVerbatim(final String input, final List<TextSplit> splits) {

        for(final TextSplit split : splits) {
            Assertions.assertTrue(split.offset() >= 0 && split.offset() + split.text().length() <= input.length(),
                    "piece is out of bounds: " + split);
            Assertions.assertEquals(split.text(),
                    input.substring(split.offset(), split.offset() + split.text().length()),
                    "piece is not verbatim at its offset: " + split);
        }

    }

    @Test
    public void piecesThatAreNotVerbatimInTheInputCannotBeLocated() {

        // The sentence splitter joins sentences with a space, so a piece covering a newline-separated
        // pair is not a substring of the input. Locating it by search could match the identical text
        // later in the document, so no overlap is offered at all.
        final String input = "Ada lives here.\nThe ssn is 123-45-6789. Ada lives here. The ssn is 123-45-6789.";

        Assertions.assertTrue(new CharacterCountSplitService(45).splitWithOverlap(input, 5).isEmpty());

    }

    @Test
    public void aWrappedPieceCarriesTheTailOfThePreviousOne() {

        // The line-width splitter wraps on spaces, so a value containing a space can be cut in two.
        final String input = "the date was May 22, 1999 in the record";
        final List<TextSplit> splits = new LineWidthSplitService(20).splitWithOverlap(input, 12).orElseThrow();

        Assertions.assertTrue(splits.size() > 1, "expected the input to be split");

        // Whatever the wrap points are, the seam value is whole in one of the pieces.
        Assertions.assertTrue(splits.stream().anyMatch(s -> s.text().contains("May 22, 1999")),
                "expected a piece to contain the whole date: " + splits);

    }

}
