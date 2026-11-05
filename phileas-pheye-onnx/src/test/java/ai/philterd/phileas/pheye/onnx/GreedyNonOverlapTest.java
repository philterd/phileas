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
package ai.philterd.phileas.pheye.onnx;

import ai.philterd.phileas.pheye.onnx.LocalPhEyeDetector.Candidate;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link LocalPhEyeDetector#greedyNonOverlap}, the decode step that turns scored
 * candidate spans into the final non-overlapping selection. Word spans are inclusive ranges; the
 * full pipeline only exercises this through the model, so these pin the selection logic directly.
 */
class GreedyNonOverlapTest {

    private static Candidate span(final int startWord, final int endWord, final double score) {
        return new Candidate(startWord, endWord, 0, score);
    }

    private static List<int[]> ranges(final List<Candidate> candidates) {
        final List<int[]> ranges = new ArrayList<>();
        for (final Candidate c : candidates) {
            ranges.add(new int[]{c.startWord(), c.endWord()});
        }
        return ranges;
    }

    private static void assertRange(final int[] range, final int start, final int end) {
        assertEquals(start, range[0]);
        assertEquals(end, range[1]);
    }

    @Test
    void emptyInputReturnsEmpty() {
        assertTrue(LocalPhEyeDetector.greedyNonOverlap(new ArrayList<>()).isEmpty());
    }

    @Test
    void keepsNonOverlappingSpansSortedByStartWord() {
        // Provide them out of order; the result must be ordered by start word, not by score.
        final List<Candidate> result = LocalPhEyeDetector.greedyNonOverlap(new ArrayList<>(List.of(
                span(4, 5, 0.7),
                span(0, 1, 0.9))));

        assertEquals(2, result.size());
        assertRange(ranges(result).get(0), 0, 1);
        assertRange(ranges(result).get(1), 4, 5);
    }

    @Test
    void keepsAdjacentButNonOverlappingSpans() {
        // Words 0 and 1 touch but do not overlap (end of one is before start of the next).
        final List<Candidate> result = LocalPhEyeDetector.greedyNonOverlap(new ArrayList<>(List.of(
                span(0, 0, 0.9),
                span(1, 1, 0.8))));

        assertEquals(2, result.size());
        assertRange(ranges(result).get(0), 0, 0);
        assertRange(ranges(result).get(1), 1, 1);
    }

    @Test
    void dropsLowerScoringOverlappingSpan() {
        // (0,1) and (1,2) share word 1; the higher-scoring one wins.
        final List<Candidate> result = LocalPhEyeDetector.greedyNonOverlap(new ArrayList<>(List.of(
                span(0, 1, 0.9),
                span(1, 2, 0.5))));

        assertEquals(1, result.size());
        assertRange(ranges(result).get(0), 0, 1);
    }

    @Test
    void keepsHigherScoringSmallerSpanOverContainingSpan() {
        // The narrower span scores higher than the wider one that contains it; keep the narrower.
        final List<Candidate> result = LocalPhEyeDetector.greedyNonOverlap(new ArrayList<>(List.of(
                span(0, 2, 0.4),
                span(1, 1, 0.95))));

        assertEquals(1, result.size());
        assertRange(ranges(result).get(0), 1, 1);
    }

    @Test
    void resolvesContainedSpanToHigherScoringContainer() {
        // The parity scenario: a wider span and the single word it contains; the higher score wins.
        final List<Candidate> result = LocalPhEyeDetector.greedyNonOverlap(new ArrayList<>(List.of(
                span(0, 1, 0.99),
                span(0, 0, 0.8))));

        assertEquals(1, result.size());
        assertRange(ranges(result).get(0), 0, 1);
    }

    @Test
    void resolvesThreeWayOverlapToSingleHighestThenKeepsADisjointSpan() {
        // (0,2) overlaps both (0,0) and (1,1); (4,4) is disjoint and survives alongside the winner.
        final List<Candidate> result = LocalPhEyeDetector.greedyNonOverlap(new ArrayList<>(List.of(
                span(0, 0, 0.6),
                span(0, 2, 0.9),
                span(1, 1, 0.7),
                span(4, 4, 0.55))));

        assertEquals(2, result.size());
        assertRange(ranges(result).get(0), 0, 2);
        assertRange(ranges(result).get(1), 4, 4);
    }

}
