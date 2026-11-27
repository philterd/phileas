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
package ai.philterd.phileas.services.ooxml;

import ai.philterd.phileas.model.filtering.RedactedTextBuilder;
import ai.philterd.phileas.model.filtering.TextFilterResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A Word or Excel document's redacted text as labeled segments, built line by line as the redaction walks the
 * document.
 */
public final class OfficeRedactedText extends RedactedTextBuilder {

    /**
     * Adds the metadata segment: one "key: value" line per property, redacted together with {@code filter}.
     * @param properties The properties, in order. None gives an empty segment.
     * @param filter The policy's filter.
     * @param captured Where to record the metadata's redactions, as content outside a paragraph or cell.
     * @param order The redaction order.
     */
    void metadata(final Map<String, String> properties, final TextFilter filter,
                  final List<OfficeRedactionSpan> captured, final OfficeSpanMath.Order order) {
        ensure(METADATA_LABEL);
        final List<String> lines = new ArrayList<>();
        for (final Map.Entry<String, String> property : properties.entrySet()) {
            lines.add(property.getKey() + ": " + flatten(property.getValue()));
        }
        final String original = String.join(LINE_SEPARATOR, lines);
        if (original.isEmpty()) {
            return;
        }
        final TextFilterResult result = filter.apply(original);
        final OfficeSpanMath.Applied applied = OfficeSpanMath.asWritten(result, original);
        OfficeSpanMath.capture(result, original, -1, null, captured, order);
        line(METADATA_LABEL, applied.text(), applied.replacedRanges());
    }

}
