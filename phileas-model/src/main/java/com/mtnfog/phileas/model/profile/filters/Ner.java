package com.mtnfog.phileas.model.profile.filters;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.mtnfog.phileas.model.profile.filters.strategies.ai.NerFilterStrategy;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Ner extends AbstractFilter {

    @SerializedName("nerFilterStrategies")
    @Expose
    private List<NerFilterStrategy> nerFilterStrategies;

    @SerializedName("removePunctuation")
    @Expose
    private boolean removePunctuation = false;

    @SerializedName("thresholds")
    @Expose
    private Map<String, Double> thresholds = new LinkedHashMap<>();

    public List<NerFilterStrategy> getNerStrategies() {
        return nerFilterStrategies;
    }

    public void setNerStrategies(List<NerFilterStrategy> nerFilterStrategies) {
        this.nerFilterStrategies = nerFilterStrategies;
    }

    public boolean isRemovePunctuation() {
        return removePunctuation;
    }

    public void setRemovePunctuation(boolean removePunctuation) {
        this.removePunctuation = removePunctuation;
    }

    public Map<String, Double> getThresholds() {
        return thresholds;
    }

    public void setThresholds(Map<String, Double> thresholds) {
        this.thresholds = thresholds;
    }

}