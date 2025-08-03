/*
 *     Copyright 2025 Philterd, LLC @ https://www.philterd.ai
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
import ai.philterd.phileas.model.objects.IncrementalRedaction;
import ai.philterd.phileas.model.objects.Span;
import com.google.gson.Gson;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Response to a filter operation.
 */
public class FilterResponse {

    private static final Logger LOGGER = LogManager.getLogger(FilterResponse.class);

    private final String filteredText;
    private final String context;
    private final String documentId;
    private final int piece;
    private final Explanation explanation;
    private final Map<String, String> attributes;
    private final List<IncrementalRedaction> incrementalRedactions;

    public FilterResponse(String filteredText, String context, String documentId, int piece,
                          Explanation explanation, Map<String, String> attributes, List<IncrementalRedaction> incrementalRedactions) {

        this.filteredText = filteredText;
        this.context = context;
        this.documentId = documentId;
        this.piece = piece;
        this.explanation = explanation;
        this.attributes = attributes;
        this.incrementalRedactions = incrementalRedactions;

    }

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

        LOGGER.info("Combining {} filter responses for document ID: {}", filterResponses.size(), documentId);

        // Combine the results into a single filterResponse object.
        final StringBuilder filteredText = new StringBuilder();
        final List<Span> appliedSpans = new LinkedList<>();
        final List<Span> identifiedSpans = new LinkedList<>();

        // Order the filter responses by piece number, lowest to greatest.
        final List<FilterResponse> sortedFilterResponses =
                filterResponses.stream().sorted(Comparator.comparing(FilterResponse::getPiece)).toList();

        // Tracks the document offset for each piece so the span locations can be adjusted.
        int documentOffset = 0;

        // The attributes for each filterResponse should actually be identical.
        final Map<String, String> combinedAttributes = new HashMap<>();

        final List<IncrementalRedaction> combinedIncrementalRedactions = new ArrayList<>();

        // Loop over each filter response and build the combined filter response.
        for (final FilterResponse filterResponse : sortedFilterResponses) {

            // The text is the filtered text plus the separator.
            final String pieceFilteredText = filterResponse.getFilteredText() + separator;

            // Append the filtered text.
            filteredText.append(pieceFilteredText);

            // Adjust the character offsets when combining.
            appliedSpans.addAll(Span.shiftSpans(documentOffset, filterResponse.getExplanation().appliedSpans()));
            identifiedSpans.addAll(Span.shiftSpans(documentOffset, filterResponse.getExplanation().identifiedSpans()));

            // Adjust the document offset.
            documentOffset += pieceFilteredText.length();

            // Combine the attributes (they should be the same anyway since attributes are on the document-level.
            combinedAttributes.putAll(filterResponse.getAttributes());

            // Combine the incremental redactions.
            combinedIncrementalRedactions.addAll(filterResponse.getIncrementalRedactions());

        }

        // Return the newly built FilterResponse.
        return new FilterResponse(filteredText.toString().trim(), context, documentId, 0,
                new Explanation(appliedSpans, identifiedSpans), combinedAttributes, combinedIncrementalRedactions);

    }

    @Override
    public String toString() {

        final Gson gson = new Gson();
        return gson.toJson(this);

    }

    @Override
    public final boolean equals(Object o) {
        return EqualsBuilder.reflectionEquals(this, o);
    }

    @Override
    public final int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    public String getFilteredText() {
        return filteredText;
    }

    public String getContext() {
        return context;
    }

    public String getDocumentId() {
        return documentId;
    }

    public int getPiece() {
        return piece;
    }

    public Explanation getExplanation() {
        return explanation;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public List<IncrementalRedaction> getIncrementalRedactions() {
        return incrementalRedactions;
    }

}
