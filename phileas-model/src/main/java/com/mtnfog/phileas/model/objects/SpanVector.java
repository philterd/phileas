package com.mtnfog.phileas.model.objects;

import com.google.gson.Gson;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class SpanVector implements Serializable {

    private Map<Integer, Integer> vectorIndexes;
    private transient Gson gson;

    public SpanVector() {
        this.vectorIndexes = new HashMap<>();
        this.gson = new Gson();
    }

    @Override
    public String toString() {
        return gson.toJson(this);
    }

    public Map<Integer, Integer> getVectorIndexes() {
        return vectorIndexes;
    }

    public void setVectorIndexes(Map<Integer, Integer> vectorIndexes) {
        this.vectorIndexes = vectorIndexes;
    }

}
