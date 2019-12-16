package com.mtnfog.phileas.model.profile.filters;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.mtnfog.phileas.model.profile.filters.strategies.ai.NerFilterStrategy;

import java.util.List;

public class Ner extends AbstractFilter {

    @SerializedName("nerFilterStrategies")
    @Expose
    private List<NerFilterStrategy> nerFilterStrategies;

    public List<NerFilterStrategy> getNerStrategies() {
        return nerFilterStrategies;
    }

    public void setNerStrategies(List<NerFilterStrategy> nerFilterStrategies) {
        this.nerFilterStrategies = nerFilterStrategies;
    }

}