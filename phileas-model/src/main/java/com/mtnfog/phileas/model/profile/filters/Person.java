package com.mtnfog.phileas.model.profile.filters;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.mtnfog.phileas.model.profile.filters.strategies.ai.PersonsFilterStrategy;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Person extends AbstractFilter {

    @SerializedName("personFilterStrategies")
    @Expose
    private List<PersonsFilterStrategy> personFilterStrategies;

    @SerializedName("thresholds")
    @Expose
    private Map<String, Double> thresholds = new LinkedHashMap<>();

    @SerializedName("model")
    @Expose
    private String model = "/opt/philter/general-3.0.lens";

    @SerializedName("vocab")
    @Expose
    private String vocab = "/opt/philter/vocab.txt";

    public List<PersonsFilterStrategy> getNerStrategies() {
        return personFilterStrategies;
    }

    public void setPersonFilterStrategies(List<PersonsFilterStrategy> personFilterStrategies) {
        this.personFilterStrategies = personFilterStrategies;
    }

    public Map<String, Double> getThresholds() {
        return thresholds;
    }

    public void setThresholds(Map<String, Double> thresholds) {
        this.thresholds = thresholds;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getVocab() {
        return vocab;
    }

    public void setVocab(String vocab) {
        this.vocab = vocab;
    }

}