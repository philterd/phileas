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

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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

            // Find the spans ambiguous to this one.
            final List<Span> identicalSpans = Span.getIdenticalSpans(span, spans);

            // Only continue if there is an identical span.
            if(!identicalSpans.isEmpty()) {

                // Get a list of all the possible filter types for this span.
                final List<FilterType> filterTypes = identicalSpans.stream().map(Span::getFilterType).collect(Collectors.toList());

                // Get the filter type of the disambiguated span.
                // The "ambiguous span" is any of the spans in the list since they only differ by filter type.
                final FilterType disambiguatedFilterType = disambiguate(context, filterTypes, span);

                // Update the filter type on the span.
                span.setFilterType(disambiguatedFilterType);

                // Add the span to the disambiguated list.
                disambiguatedSpans.add(span);

            } else {

                // This span doesn't have any identical spans so just add it.
                disambiguatedSpans.add(span);

            }

        }

        return new LinkedList<>(disambiguatedSpans);

    }

    @Override
    public FilterType disambiguate(String context, List<FilterType> filterTypes, Span ambiguousSpan) {

        // Holds all the span vectors.
        final Map<FilterType, double[]> spanVectors = new HashMap<>();

        // Loop over each filter type to determine which filter type most closely resembles the ambiguous span.
        for(final FilterType filterType : filterTypes) {

            LOGGER.debug("Getting vector representation for filter type {}", filterType.name());

            // Get the vector representations for each potential filter type.
            final Map<Double, Double> vectorRepresentation = vectorService.getVectorRepresentation(context, filterType);

            // Create vectors for the representations.
            final double[] spanVector = new double[vectorSize];
            for(final double d : vectorRepresentation.keySet()) {
                spanVector[(int) d] = vectorRepresentation.get(d);
            }

            spanVectors.put(filterType, spanVector);

        }

        // Build the ambiguousSpanVector from the ambiguousSpan.
        final double[] ambiguousSpanVector = hash(ambiguousSpan);
        LOGGER.debug("Ambiguous: {}", StringUtils.leftPad(Arrays.toString(ambiguousSpanVector), 20));

        // Map of filter type to distance from the ambiguous span.
        final Map<FilterType, Double> distances = new HashMap<>();

        // Get the distance for each of the vectors.
        for(final FilterType filterType : spanVectors.keySet()) {

            // Get the span vector for this filter type.
            final double[] spanVector = spanVectors.get(filterType);
            LOGGER.debug("Vector {}: {}", StringUtils.rightPad(filterType.name(), 20), Arrays.toString(spanVector));

            // Calculate the distance of the vector from the ambiguous span's vector.
            final double[] normalized = normalize(spanVector, ambiguousSpanVector);
            LOGGER.debug("Normalized {}: {}", StringUtils.rightPad(filterType.name(), 20), Arrays.toString(normalized));
            final double distance = cosineSimilarity(spanVector, normalized);

            // Record this distance.
            distances.put(filterType, distance);

        }

        // Just to show the output values.
        final DecimalFormat df = new DecimalFormat("0.000000000000000");
        for(final FilterType filterType : distances.keySet()) {
            LOGGER.debug("Filter Type {}: {}", filterType.name(), df.format(distances.get(filterType)));
        }

        // Get the filter type with the smallest distance (closest to 1.0) and return it.
        return Collections.max(distances.entrySet(), Map.Entry.comparingByValue()).getKey();

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

                final int hash = hashToken(token);

                // We're only looking for what the window has. How many of each token is irrelevant.
                // TODO: But is it irrelevant though? If a word occurs more often than others
                // it is probably more indicative of the type than a word that only occurs once.
                vector[hash] = 1;

            }

        }

        return vector;

    }

    private double[] normalize(double[] vector, double[] ambiguousVector) {

        final double[] normalized = new double[vectorSize];

        for(int x = 0; x < vector.length; x++) {

            if(vector[x] != 0 && ambiguousVector[x] != 0) {
                normalized[x] = vector[x];
            } else {
                normalized[x] = ambiguousVector[x];
            }

        }

        return normalized;

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
