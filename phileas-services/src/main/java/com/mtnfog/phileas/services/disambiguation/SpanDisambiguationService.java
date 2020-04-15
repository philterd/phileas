package com.mtnfog.phileas.services.disambiguation;

import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.objects.Span;
import com.mtnfog.phileas.model.services.DisambiguationService;
import org.apache.commons.lang3.RandomStringUtils;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class SpanDisambiguationService implements DisambiguationService {

    private static final int VECTOR_SIZE = 8; //2^18;

    private Map<FilterType, SpanVector> vectors;

    public SpanDisambiguationService() {

        this.vectors = new HashMap<>();

        for(final FilterType filterType : FilterType.values()) {

            vectors.put(filterType, new SpanVector());

        }

    }

    @Override
    public void hashAndInsert(Span span) {

        final String[] window = span.getWindow();

        for(final String token : window) {

            // TODO: Hash the token with an actual algorithm.
            final int hash = Math.abs(token.hashCode() % VECTOR_SIZE);

            System.out.println(token + ": hash = " + hash);

            // Insert it into the vector.
            vectors.get(span.getFilterType()).getVectorIndexes().add(hash);

        }

    }

    private int[] hash(Span span) {

        final int[] vector = new int[VECTOR_SIZE];

        final String[] window = span.getWindow();

        for(final String token : window) {

            // TODO: Hash the token with an actual algorithm.
            final int hash = Math.abs(token.hashCode() % VECTOR_SIZE);

            vector[hash] = 1;

        }

        return vector;

    }

    @Override
    public FilterType disambiguate(Span span1, Span span2, Span ambiguousSpan) {

        // Get the vector representations.
        final Set<Integer> span1VectorRepresentation = vectors.get(span1.getFilterType()).getVectorIndexes();
        final Set<Integer> span2VectorRepresentation = vectors.get(span2.getFilterType()).getVectorIndexes();

        // Build the ambiguousSpanVectorRepresentation from the ambiguousSpan.
        final int[] ambiguousSpanVectorRepresentation = hash(ambiguousSpan);

        // Create vectors from the span1 representation.
        final double[] span1Vector = new double[VECTOR_SIZE];
        for(int d : span1VectorRepresentation) {
            span1Vector[d] = 5;
        }
        System.out.println("span1: " + Arrays.toString(span1Vector));

        // Create vectors from the span2 representation.
        final double[] span2Vector = new double[VECTOR_SIZE];
        for(int d : span2VectorRepresentation) {
            span2Vector[d] = 5;
        }
        System.out.println("span2: " + Arrays.toString(span2Vector));

        // Create vectors from the ambiguousSpan representation.
        final double[] ambiguousSpanVector = new double[VECTOR_SIZE];
        for(int d : ambiguousSpanVectorRepresentation) {
            ambiguousSpanVector[d] = 5;
        }
        System.out.println("ambiguous: " + Arrays.toString(ambiguousSpanVector));

        // Get the distance for each of the vectors.
        final double span1VectorDistance = cosineSimilarity(span1Vector, ambiguousSpanVector);
        final double span2VectorDistance = cosineSimilarity(span2Vector, ambiguousSpanVector);

        final DecimalFormat df = new DecimalFormat("0.000000000000000");
        System.out.println("span1VectorDistance: " + df.format(span1VectorDistance));
        System.out.println("span2VectorDistance: " + df.format(span2VectorDistance));

        // The span with the smallest distance wins.
        if(span1VectorDistance > span2VectorDistance) {
            return span2.getFilterType();
        }

        return span1.getFilterType();

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
