package com.mtnfog.phileas.services.disambiguation;

import java.util.HashSet;
import java.util.Set;

public class SpanVector {

    private Set<Integer> vectorIndexes;

    public SpanVector() {
        this.vectorIndexes = new HashSet<>();
    }

    public Set<Integer> getVectorIndexes() {
        return vectorIndexes;
    }

    public void setVectorIndexes(Set<Integer> vectorIndexes) {
        this.vectorIndexes = vectorIndexes;
    }

}
