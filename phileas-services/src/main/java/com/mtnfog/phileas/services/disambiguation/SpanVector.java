package com.mtnfog.phileas.services.disambiguation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SpanVector {

    private Map<Integer, Integer> vectorIndexes;

    public SpanVector() {
        this.vectorIndexes = new HashMap<>();
    }

    public Map<Integer, Integer> getVectorIndexes() {
        return vectorIndexes;
    }

    public void setVectorIndexes(Map<Integer, Integer> vectorIndexes) {
        this.vectorIndexes = vectorIndexes;
    }

}
