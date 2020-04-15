package com.mtnfog.phileas.services.disambiguation;

import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.objects.Span;
import com.mtnfog.phileas.model.services.DisambiguationService;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class SpanDisambiguationService implements DisambiguationService {

    // Can this vector size be increased over time as the number of documents process grows?
    // No, because it factors into the hash function.
    // Changing the size would require starting all over because the values in it would
    // no longer be valid because the hash function would have changed.
    private static final int VECTOR_SIZE = 1024; //2^18;

    private Map<String, Map<FilterType, SpanVector>> vectors;

    public SpanDisambiguationService() {

        this.vectors = new HashMap<>();

    }

    @Override
    public void hashAndInsert(String context, Span span) {

        if(vectors.get(context) == null) {

            final Map<FilterType, SpanVector> v = new HashMap<>();

            for(final FilterType filterType : FilterType.values()) {
                v.put(filterType, new SpanVector());
            }

            vectors.put(context, v);

        }

        final String[] window = span.getWindow();

        for(final String token : window) {

            // TODO: Hash the token with an actual algorithm.
            final int hash = Math.abs(token.hashCode() % VECTOR_SIZE);

            System.out.println(token + ": hash = " + hash);

            // Insert it into the vector.
            vectors.get(context).get(span.getFilterType()).getVectorIndexes().putIfAbsent(hash, 0);
            int val = vectors.get(context).get(span.getFilterType()).getVectorIndexes().get(hash) + 1;
            vectors.get(context).get(span.getFilterType()).getVectorIndexes().put(hash, val);

        }

    }

    private double[] hash(Span span) {

        System.out.println("Hashing ambiguous span");

        final double[] vector = new double[VECTOR_SIZE];

        final String[] window = span.getWindow();

        for(final String token : window) {

            // TODO: Hash the token with an actual algorithm.
            final int hash = Math.abs(token.hashCode() % VECTOR_SIZE);

            // We're only looking for what the window has.
            // How many of each token is irrelevant.
            vector[hash] = 1;

            System.out.println(token + ": hash = " + hash + ": val = " + vector[hash]);

        }

        return vector;

    }

    @Override
    public FilterType disambiguate(String context, Span span1, Span span2, Span ambiguousSpan) {

        // Get the vector representations.
        final Map<Integer, Integer> span1VectorRepresentation = vectors.get(context).get(span1.getFilterType()).getVectorIndexes();
        final Map<Integer, Integer> span2VectorRepresentation = vectors.get(context).get(span2.getFilterType()).getVectorIndexes();

        // Build the ambiguousSpnVector from the ambiguousSpan.
        final double[] ambiguousSpanVector = hash(ambiguousSpan);

        // Create vectors from the span1 representation.
        final double[] span1Vector = new double[VECTOR_SIZE];
        for(int d : span1VectorRepresentation.keySet()) {
            //span1Vector[d] = 1;
            span1Vector[d] = span1VectorRepresentation.get(d);
        }
        System.out.println("span1:     " + Arrays.toString(span1Vector));

        // Create vectors from the span2 representation.
        final double[] span2Vector = new double[VECTOR_SIZE];
        for(int d : span2VectorRepresentation.keySet()) {
            //span2Vector[d] = 1;
            span2Vector[d] = span2VectorRepresentation.get(d);
        }
        System.out.println("span2:     " + Arrays.toString(span2Vector));

        // Create vectors from the ambiguousSpan representation.
        /*final double[] ambiguousSpanVector = new double[VECTOR_SIZE];
        for(int d : ambiguousSpnVector) {
            ambiguousSpanVector[d] = 1;
        }*/
        // TODO: If the ambiguousSpanVector has a non-zero value for any index in
        System.out.println("ambiguous: " + Arrays.toString(ambiguousSpanVector));

        // Get the distance for each of the vectors.
        final double span1VectorDistance = cosineSimilarity(span1Vector, normalize(span1Vector, ambiguousSpanVector));
        final double span2VectorDistance = cosineSimilarity(span2Vector, normalize(span2Vector, ambiguousSpanVector));

        final DecimalFormat df = new DecimalFormat("0.000000000000000");
        System.out.println("span1VectorDistance: " + df.format(span1VectorDistance));
        System.out.println("span2VectorDistance: " + df.format(span2VectorDistance));

        // The span with the smallest distance wins.
        if(span1VectorDistance < span2VectorDistance) {
            return span2.getFilterType();
        }

        return span1.getFilterType();

    }

    private double[] normalize(double[] vector, double[] ambiguousVector) {

        double[] normalized = new double[VECTOR_SIZE];

        for(int x = 0; x < vector.length; x++) {

            if(vector[x] != 0 && ambiguousVector[x] != 0) {
                normalized[x] = vector[x];
            } else {
                normalized[x] = ambiguousVector[x];
            }

        }

        System.out.println("Normalized:" + Arrays.toString(normalized));

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
