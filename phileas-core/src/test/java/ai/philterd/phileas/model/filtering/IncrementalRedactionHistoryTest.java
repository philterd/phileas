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
package ai.philterd.phileas.model.filtering;

import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

public class IncrementalRedactionHistoryTest {

    private static Span span(final int start, final int end, final String text, final String replacement) {
        return Span.make(start, end, FilterType.SSN, "context", 0.9, text, replacement, "", false, true, new String[0], 0);
    }

    @Test
    public void textAfterAppliesTheReplacementsUpToTheStep() {

        final String input = "a 111 b 222 c";
        final IncrementalRedactionHistory history = new IncrementalRedactionHistory(input,
                List.of(span(2, 5, "111", "[X]"), span(8, 11, "222", "[YY]")));

        Assertions.assertEquals(2, history.size());
        Assertions.assertEquals("a [X] b 222 c", history.textAfter(0));
        Assertions.assertEquals("a [X] b [YY] c", history.textAfter(1));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> history.textAfter(2));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> history.textAfter(-1));

    }

    @Test
    public void laterChangesToASpanDoNotChangeTheHistory() {

        final Span span = span(2, 5, "111", "[X]");
        final IncrementalRedactionHistory history = new IncrementalRedactionHistory("a 111 b", List.of(span));
        span.setReplacement("changed");

        Assertions.assertEquals("a [X] b", history.textAfter(0));

    }

    @Test
    public void spansMustBeInOrderAndNotOverlap() {

        final List<Span> outOfOrder = List.of(span(8, 11, "222", "[Y]"), span(2, 5, "111", "[X]"));
        final List<Span> overlapping = List.of(span(2, 6, "111 ", "[X]"), span(5, 11, " b 222", "[Y]"));

        Assertions.assertThrows(IllegalArgumentException.class, () -> new IncrementalRedactionHistory("a 111 b 222 c", outOfOrder));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new IncrementalRedactionHistory("a 111 b 222 c", overlapping));

    }

    @Test
    public void redactionBuiltFromAHistoryReturnsItsStepsText() {

        final Span first = span(2, 5, "111", "[X]");
        final Span second = span(8, 11, "222", "[YY]");
        final IncrementalRedactionHistory history = new IncrementalRedactionHistory("a 111 b 222 c", List.of(first, second));
        final IncrementalRedaction redaction = new IncrementalRedaction(DigestUtils.sha256Hex("a [X] b [YY] c"), second, history, 1);

        Assertions.assertEquals("a [X] b [YY] c", redaction.getIncrementallyRedactedText());
        Assertions.assertEquals(DigestUtils.sha256Hex(redaction.getIncrementallyRedactedText()), redaction.getHash());
        Assertions.assertSame(second, redaction.getSpan());
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> new IncrementalRedaction("hash", second, history, 2));

    }

    @Test
    public void redactionGivenItsTextReturnsIt() {

        final IncrementalRedaction redaction = new IncrementalRedaction("hash", span(0, 1, "a", "b"), "text");

        Assertions.assertEquals("text", redaction.getIncrementallyRedactedText());

    }

}
