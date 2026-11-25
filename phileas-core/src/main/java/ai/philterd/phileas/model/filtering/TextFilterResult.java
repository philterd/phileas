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
import java.util.LinkedList;
import java.util.List;

/**
 * Response to a filter operation.
 */
public class TextFilterResult extends AbstractFilterResult {

    private static final Logger LOGGER = LogManager.getLogger(TextFilterResult.class);

    private final String filteredText;
    private final int piece;

    // Where the redaction put its replacements in filteredText, so the leftover check can leave them out.
    private transient List<ReplacedRange> replacedRanges = List.of();

    public TextFilterResult(String filteredText, String context, int piece,
                            Explanation explanation, List<IncrementalRedaction> incrementalRedactions,
                            long tokens) {
        this(filteredText, context, piece, explanation, incrementalRedactions, tokens, false);
    }

    public TextFilterResult(String filteredText, String context, int piece,
                            Explanation explanation, List<IncrementalRedaction> incrementalRedactions,
                            long tokens, boolean reviewSampled) {
        this(filteredText, context, piece, explanation, incrementalRedactions, tokens, reviewSampled, null);
    }

    public TextFilterResult(String filteredText, String context, int piece,
                            Explanation explanation, List<IncrementalRedaction> incrementalRedactions,
                            long tokens, boolean reviewSampled, LeftoverCheckResult leftoverCheck) {
        super(context, explanation, tokens, incrementalRedactions, reviewSampled, leftoverCheck);

        this.filteredText = filteredText;
        this.piece = piece;

    }

    /**
     * Combine multiple {@link TextFilterResult} objects into a single {@link TextFilterResult}.
     * The list of {@link TextFilterResult} objects must be in order from first to last.
     *
     * @param filterResponse A list of {@link TextFilterResult} objects to combine.
     *                        Objects must be in order from first to last.
     * @param context         The context for the returned {@link TextFilterResult}.
     * @return A single, combined {@link TextFilterResult}.
     */
    public static TextFilterResult combine(List<TextFilterResult> filterResponse, final String context, String separator) {

        LOGGER.debug("Combining {} filter responses", filterResponse.size());

        // Combine the results into a single filterResponse object.
        final StringBuilder filteredText = new StringBuilder();
        final List<Span> appliedSpans = new LinkedList<>();
        final List<Span> identifiedSpans = new LinkedList<>();

        // Order the filter responses by piece number, lowest to greatest.
        final List<TextFilterResult> sortedFilterResponse =
                filterResponse.stream().sorted(Comparator.comparing(TextFilterResult::getPiece)).toList();

        // Tracks the document offset for each piece so the span locations can be adjusted.
        int documentOffset = 0;

        final List<IncrementalRedaction> combinedIncrementalRedactions = new ArrayList<>();
        final List<ReplacedRange> replacedRanges = new ArrayList<>();
        long tokens = 0;

        // Loop over each filter response and build the combined filter response.
        for (final TextFilterResult textFilterResult : sortedFilterResponse) {

            // The text is the filtered text plus the separator.
            final String pieceFilteredText = textFilterResult.getFilteredText() + separator;

            // Append the filtered text.
            filteredText.append(pieceFilteredText);

            // Adjust the character offsets when combining.
            appliedSpans.addAll(Span.shiftSpans(documentOffset, textFilterResult.getExplanation().appliedSpans()));
            identifiedSpans.addAll(Span.shiftSpans(documentOffset, textFilterResult.getExplanation().identifiedSpans()));

            for (final ReplacedRange range : textFilterResult.getReplacedRanges()) {
                replacedRanges.add(range.shift(documentOffset));
            }

            // Adjust the document offset.
            documentOffset += pieceFilteredText.length();

            // Combine the incremental redactions.
            combinedIncrementalRedactions.addAll(textFilterResult.getIncrementalRedactions());

            // Sum the tokens.
            tokens += textFilterResult.getTokens();

        }

        final String combined = filteredText.toString();
        final String trimmed = combined.trim();

        // Trimming the start moves everything after it.
        final int leading = combined.indexOf(trimmed);
        final List<ReplacedRange> trimmedRanges = new ArrayList<>();
        for (final ReplacedRange range : replacedRanges) {
            trimmedRanges.add(range.shift(-leading));
        }

        // Return the newly built FilterResponse.
        final TextFilterResult combinedResult = new TextFilterResult(trimmed, context, 0,
                new Explanation(appliedSpans, identifiedSpans), combinedIncrementalRedactions, tokens);
        combinedResult.setReplacedRanges(trimmedRanges);
        return combinedResult;

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

    /**
     * Where the redaction put its replacements in {@link #getFilteredText()}. Not serialized.
     * @return The ranges, ordered by start.
     */
    public List<ReplacedRange> getReplacedRanges() {
        return replacedRanges;
    }

    /**
     * Sets where the redaction put its replacements in {@link #getFilteredText()}.
     * @param replacedRanges The ranges.
     */
    public void setReplacedRanges(final List<ReplacedRange> replacedRanges) {
        this.replacedRanges = replacedRanges == null ? List.of() : List.copyOf(replacedRanges);
    }

}
