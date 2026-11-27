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

import java.util.List;
import java.util.stream.Collectors;

/**
 * A document's redacted plain text, as labeled segments in document order, from the same detection the
 * document's redaction uses.
 */
public final class RedactedText {

    private final List<TextSegment> segments;

    public RedactedText(final List<TextSegment> segments) {
        this.segments = List.copyOf(segments);
    }

    public List<TextSegment> getSegments() {
        return segments;
    }

    /**
     * The text of the segments that have text, joined with a separator.
     * @param separator The separator, such as a blank line.
     * @return The joined text.
     */
    public String join(final String separator) {
        return segments.stream().filter(segment -> segment.getStatus() == TextSegment.Status.TEXT)
                .map(TextSegment::getText).collect(Collectors.joining(separator));
    }

    /**
     * The labels of the segments with no extractable text, such as scanned PDF pages.
     * @return The labels, in document order.
     */
    public List<String> getSegmentsWithoutText() {
        return segments.stream().filter(segment -> segment.getStatus() == TextSegment.Status.NO_TEXT)
                .map(TextSegment::getLabel).toList();
    }

    @Override
    public boolean equals(final Object o) {
        return o instanceof RedactedText other && segments.equals(other.segments);
    }

    @Override
    public int hashCode() {
        return segments.hashCode();
    }

    @Override
    public String toString() {
        return segments.toString();
    }

}
