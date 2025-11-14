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
package ai.philterd.phileas.model.filtering;

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
public class TextFilterResult extends AbstractFilterResult {

    private static final Logger LOGGER = LogManager.getLogger(TextFilterResult.class);

    private final String filteredText;
    private final int piece;

    public TextFilterResult(String filteredText, String context, int piece,
                            Explanation explanation, List<IncrementalRedaction> incrementalRedactions,
                            long tokens) {
        super(context, explanation, tokens, incrementalRedactions);

        this.filteredText = filteredText;
        this.piece = piece;

    }

    /**
     * Combine multiple {@link TextFilterResult} objects into a single {@link TextFilterResult}.
     * The list of {@link TextFilterResult} objects must be in order from first to last.
     *
     * @param filterRespons A list of {@link TextFilterResult} objects to combine.
     *                        Objects must be in order from first to last.
     * @param context         The context for the returned {@link TextFilterResult}.
     * @return A single, combined {@link TextFilterResult}.
     */
    public static TextFilterResult combine(List<TextFilterResult> filterRespons, final String context, String separator) {

        LOGGER.debug("Combining {} filter responses", filterRespons.size());

        // Combine the results into a single filterResponse object.
        final StringBuilder filteredText = new StringBuilder();
        final List<Span> appliedSpans = new LinkedList<>();
        final List<Span> identifiedSpans = new LinkedList<>();

        // Order the filter responses by piece number, lowest to greatest.
        final List<TextFilterResult> sortedFilterRespons =
                filterRespons.stream().sorted(Comparator.comparing(TextFilterResult::getPiece)).toList();

        // Tracks the document offset for each piece so the span locations can be adjusted.
        int documentOffset = 0;

        // The attributes for each filterResponse should actually be identical.
        final Map<String, String> combinedAttributes = new HashMap<>();

        final List<IncrementalRedaction> combinedIncrementalRedactions = new ArrayList<>();
        long tokens = 0;

        // Loop over each filter response and build the combined filter response.
        for (final TextFilterResult textFilterResult : sortedFilterRespons) {

            // The text is the filtered text plus the separator.
            final String pieceFilteredText = textFilterResult.getFilteredText() + separator;

            // Append the filtered text.
            filteredText.append(pieceFilteredText);

            // Adjust the character offsets when combining.
            appliedSpans.addAll(Span.shiftSpans(documentOffset, textFilterResult.getExplanation().appliedSpans()));
            identifiedSpans.addAll(Span.shiftSpans(documentOffset, textFilterResult.getExplanation().identifiedSpans()));

            // Adjust the document offset.
            documentOffset += pieceFilteredText.length();

            // Combine the incremental redactions.
            combinedIncrementalRedactions.addAll(textFilterResult.getIncrementalRedactions());

            // Sum the tokens.
            tokens += textFilterResult.getTokens();

        }

        // Return the newly built FilterResponse.
        return new TextFilterResult(filteredText.toString().trim(), context, 0,
                new Explanation(appliedSpans, identifiedSpans), combinedIncrementalRedactions, tokens);

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

    public int getPiece() {
        return piece;
    }

}
