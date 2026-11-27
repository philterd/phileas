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

import ai.philterd.phileas.model.filtering.RedactionWarning;
import ai.philterd.phileas.policy.Tabular;
import ai.philterd.phileas.services.strategies.ColumnFilterStrategy;

import java.util.List;

/** A policy's column rules for a workbook, with what applying them needs. */
public interface ColumnRules {

    /** The policy's {@code tabular} section. */
    Tabular tabular();

    /**
     * The filter a {@code filter} rule's cells are scanned with.
     * @param identifierKeys The rule's {@code identifiers} keys.
     * @return The filter.
     */
    TextFilter filter(List<String> identifierKeys);

    /**
     * The replacement for a whole cell.
     * @param strategies A {@code redact} rule's strategies, or a sheet's unlisted strategies.
     * @param value The cell's text.
     * @return The replacement.
     */
    String replace(List<ColumnFilterStrategy> strategies, String value);

    /**
     * Receives a warning about the rules from a workbook embedded in the document being redacted, such as a header
     * that matches no column, so the call can report it. The default drops it.
     * @param warning The warning, marked as embedded.
     */
    default void warn(final RedactionWarning warning) {
    }

}
