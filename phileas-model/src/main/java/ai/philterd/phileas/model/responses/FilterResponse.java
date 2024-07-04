/*
 *     Copyright 2024 Philterd, LLC @ https://www.philterd.ai
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
package ai.philterd.phileas.model.responses;

import ai.philterd.phileas.model.objects.Explanation;
import ai.philterd.phileas.model.objects.Span;
import com.google.gson.Gson;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Response to a filter operation.
 */
public record FilterResponse(String filteredText, String context, String documentId, int piece,
                             Explanation explanation, Map<String, String> attributes) {

    /**
     * Combine multiple {@link FilterResponse} objects into a single {@link FilterResponse}.
     * The list of {@link FilterResponse} objects must be in order from first to last.
     *
     * @param filterResponses A list of {@link FilterResponse} objects to combine.
     *                        Objects must be in order from first to last.
     * @param context         The context for the returned {@link FilterResponse}.
     * @param documentId      The document ID for the returned {@link FilterResponse}.
     * @return A single, combined {@link FilterResponse}.
     */
    public static FilterResponse combine(List<FilterResponse> filterResponses, String context, String documentId, String separator) {

        // Combine the results into a single filterResponse object.
        final StringBuilder filteredText = new StringBuilder();
        final List<Span> appliedSpans = new LinkedList<>();
        final List<Span> identifiedSpans = new LinkedList<>();

        // Order the filter responses by piece number, lowest to greatest.
        final List<FilterResponse> sortedFilterResponses =
                filterResponses.stream().sorted(Comparator.comparing(FilterResponse::piece)).toList();

        // Tracks the document offset for each piece so the span locations can be adjusted.
        int documentOffset = 0;

        // The attributes for each filterResponse should actually be identical.
        final Map<String, String> combinedAttributes = new HashMap<>();

        // Loop over each filter response and build the combined filter response.
        for (final FilterResponse filterResponse : sortedFilterResponses) {

            // The text is the filtered text plus the separator.
            final String pieceFilteredText = filterResponse.filteredText() + separator;

            // Append the filtered text.
            filteredText.append(pieceFilteredText);

            // Adjust the character offsets when combining.
            appliedSpans.addAll(Span.shiftSpans(documentOffset, filterResponse.explanation().appliedSpans()));
            identifiedSpans.addAll(Span.shiftSpans(documentOffset, filterResponse.explanation().identifiedSpans()));

            // Adjust the document offset.
            documentOffset += pieceFilteredText.length();

            // Combine the attributes (they should be the same anyway since attributes are on the document-level.
            combinedAttributes.putAll(filterResponse.attributes());

        }

        // Return the newly built FilterResponse.
        return new FilterResponse(filteredText.toString().trim(), context, documentId, 0,
                new Explanation(appliedSpans, identifiedSpans), combinedAttributes);

    }

    @Override
    public String toString() {

        final Gson gson = new Gson();
        return gson.toJson(this);

    }

}
