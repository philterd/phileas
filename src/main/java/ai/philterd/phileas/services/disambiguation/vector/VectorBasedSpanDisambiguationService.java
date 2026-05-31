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
package ai.philterd.phileas.services.disambiguation.vector;

import ai.philterd.phileas.PhileasConfiguration;
import ai.philterd.phileas.model.filtering.FilterType;
import ai.philterd.phileas.model.filtering.Span;
import ai.philterd.phileas.services.disambiguation.AbstractSpanDisambiguationService;
import ai.philterd.phileas.services.disambiguation.SpanDisambiguationService;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Implementation of {@link SpanDisambiguationService} that uses vectors
 * to determine which filter type a span is most similar to.
 */
public class VectorBasedSpanDisambiguationService extends AbstractSpanDisambiguationService implements SpanDisambiguationService {

    private static final Logger LOGGER = LogManager.getLogger(VectorBasedSpanDisambiguationService.class);

    /**
     * Initializes the service.
     * @param phileasConfiguration The {@link PhileasConfiguration} used to configure the service.
     * @param vectorService A {@link VectorService}.
     */
    public VectorBasedSpanDisambiguationService(final PhileasConfiguration phileasConfiguration, final VectorService vectorService) {
        super(phileasConfiguration, vectorService);
    }

    @Override
    public void hashAndInsert(String context, Span span) {

        final double[] hashes = hash(span);
        vectorService.hashAndInsert(context, hashes, span, vectorSize);

    }

    @Override
    public List<Span> disambiguate(final String context, final List<Span> spans) {

        final Set<Span> disambiguatedSpans = new LinkedHashSet<>();

        for(final Span span : spans) {

            // Find the spans that compete with this one (same location, different filter type).
            final List<Span> identicalSpans = getCompetingSpans(span, spans);

            // Only continue if there is a competing span.
            if(!identicalSpans.isEmpty()) {

                // Get the list of candidate filter types: this span's own type plus the types of
                // the competing identical spans. The span's own type must be included or it could
                // never win the disambiguation.
                final Set<FilterType> candidateTypes = new LinkedHashSet<>();
                candidateTypes.add(span.getFilterType());
                identicalSpans.stream().map(Span::getFilterType).forEach(candidateTypes::add);
                final List<FilterType> filterTypes = new LinkedList<>(candidateTypes);

                // Get the filter type of the disambiguated span.
                // The "ambiguous span" is any of the spans in the list since they only differ by filter type.
                final FilterType disambiguatedFilterType = disambiguate(context, filterTypes, span);

                // Update the filter type on the span.
                span.setFilterType(disambiguatedFilterType);

                // Add the span to the disambiguated list.
                disambiguatedSpans.add(span);

            } else {

                // This span is unambiguous: exactly one filter type claimed this text. That makes
                // it a confident training example, so record its context vector under its filter
                // type. This is what lets disambiguation improve over time, since future ambiguous
                // spans are compared against these accumulated vectors.
                hashAndInsert(context, span);

                disambiguatedSpans.add(span);

            }

        }

        return new LinkedList<>(disambiguatedSpans);

    }

    /**
     * Returns the spans that compete with the given span for disambiguation: spans covering the
     * same location but assigned a different filter type. Unlike {@link Span#getIdenticalSpans},
     * confidence is intentionally not compared, since competing filters routinely assign different
     * confidences to the same text and those are exactly the cases disambiguation must resolve.
     * Scoped to this service so the shared {@link Span} matching semantics are not altered.
     */
    private List<Span> getCompetingSpans(final Span span, final List<Span> spans) {

        final Set<Span> competing = new LinkedHashSet<>();

        for(final Span other : spans) {

            if(other.getCharacterStart() == span.getCharacterStart()
                    && other.getCharacterEnd() == span.getCharacterEnd()
                    && other.getFilterType() != span.getFilterType()
                    && other.getLowerLeftX() == span.getLowerLeftX()
                    && other.getLowerLeftY() == span.getLowerLeftY()
                    && other.getUpperRightX() == span.getUpperRightX()
                    && other.getUpperRightY() == span.getUpperRightY()
                    && other.getLineNumber() == span.getLineNumber()
                    && other.getPageNumber() == span.getPageNumber()
                    && other.getParagraphNumber() == span.getParagraphNumber()
                    && !other.equals(span)) {

                competing.add(other);

            }

        }

        return new LinkedList<>(competing);

    }

    @Override
    public FilterType disambiguate(final String context, final List<FilterType> filterTypes, final Span ambiguousSpan) {

        // Build the vector for the ambiguous span from its surrounding context window.
        final double[] ambiguousSpanVector = hash(ambiguousSpan);
        LOGGER.debug("Ambiguous: {}", StringUtils.leftPad(Arrays.toString(ambiguousSpanVector), 20));

        FilterType bestFilterType = null;
        double bestSimilarity = Double.NEGATIVE_INFINITY;

        // Compare the ambiguous span's vector against the accumulated vector for each candidate
        // filter type. The candidate with the highest cosine similarity wins.
        for(final FilterType filterType : filterTypes) {

            // Reconstruct the accumulated vector for this filter type in this context.
            final Map<Double, Double> vectorRepresentation = vectorService.getVectorRepresentation(context, filterType);

            final double[] filterTypeVector = new double[vectorSize];
            for(final Map.Entry<Double, Double> entry : vectorRepresentation.entrySet()) {
                filterTypeVector[entry.getKey().intValue()] = entry.getValue();
            }

            // Cosine similarity between the candidate's learned vector and the ambiguous span's
            // vector. A NaN result (one side had no signal, e.g. cold start or no token overlap)
            // is treated as zero so it never outranks a candidate with real overlap.
            final double similarity = cosineSimilarity(filterTypeVector, ambiguousSpanVector);
            final double score = Double.isNaN(similarity) ? 0.0 : similarity;
            LOGGER.debug("Filter Type {}: {}", StringUtils.rightPad(filterType.name(), 20), score);

            // Strictly-greater keeps the first candidate on ties, so the decision is deterministic
            // for a given candidate ordering even before any training has accumulated.
            if(score > bestSimilarity) {
                bestSimilarity = score;
                bestFilterType = filterType;
            }

        }

        // Cold start / no signal: fall back to the first candidate so the result is deterministic
        // rather than undefined.
        if(bestFilterType == null) {
            bestFilterType = filterTypes.get(0);
        }

        return bestFilterType;

    }

    private double[] hash(Span span) {

        final double[] vector = new double[vectorSize];

        final String[] window = span.getWindow();

        for(final String token : window) {

            // Lowercase the token and remove any whitespace.
            final String lowerCasedToken = token.toLowerCase().trim();

            // Ignore stop words?
            if(ignoreStopWords && stopwords.contains(lowerCasedToken)) {

                // Ignore it because it is a stop word.

            } else {

                // Hash the lower-cased token so casing does not split the signal: "Phone" at the
                // start of a sentence and "phone" mid-sentence must land on the same vector index,
                // otherwise the same context word fails to reinforce the learned vector. This also
                // keeps hashing consistent with the (case-insensitive) stop word check above.
                final int hash = hashToken(lowerCasedToken);

                // We're only looking for what the window has. How many of each token is irrelevant.
                // TODO: But is it irrelevant though? If a word occurs more often than others
                // it is probably more indicative of the type than a word that only occurs once.
                vector[hash] = 1;

            }

        }

        return vector;

    }

    public static double cosineSimilarity(final double[] vectorA, final double[] vectorB) {

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < vectorA.length; i++) {
            dotProduct += vectorA[i] * vectorB[i];
            normA += Math.pow(vectorA[i], 2);
            normB += Math.pow(vectorB[i], 2);
        }

        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));

    }

}
