package com.mtnfog.phileas.model.profile.filters;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.mtnfog.phileas.model.enums.SensitivityLevel;
import com.mtnfog.phileas.model.profile.filters.strategies.custom.CustomDictionaryFilterStrategy;

import java.util.Collections;
import java.util.List;

public class CustomDictionary extends AbstractFilter {

    @SerializedName("classification")
    @Expose
    private String classification;

    @SerializedName("terms")
    @Expose
    private List<String> terms = Collections.emptyList();

    @SerializedName("files")
    @Expose
    private List<String> files = Collections.emptyList();

    @SerializedName("fuzzy")
    @Expose
    private boolean fuzzy = false;

    @SerializedName("sensitivity")
    @Expose
    private String sensitivity = SensitivityLevel.MEDIUM.getName();

    @SerializedName("customFilterStrategies")
    @Expose
    private List<CustomDictionaryFilterStrategy> customDictionaryFilterStrategies;

    public List<String> getTerms() {
        return terms;
    }

    public void setTerms(List<String> terms) {
        this.terms = terms;
    }

    public List<CustomDictionaryFilterStrategy> getCustomDictionaryFilterStrategies() {
        return customDictionaryFilterStrategies;
    }

    public void setCustomDictionaryFilterStrategies(List<CustomDictionaryFilterStrategy> customDictionaryFilterStrategies) {
        this.customDictionaryFilterStrategies = customDictionaryFilterStrategies;
    }

    public String getSensitivity() {
        return sensitivity;
    }

    public void setSensitivity(String sensitivity) {
        this.sensitivity = sensitivity;
    }

    public List<String> getFiles() {
        return files;
    }

    public void setFiles(List<String> files) {
        this.files = files;
    }

    public boolean isFuzzy() {
        return fuzzy;
    }

    public void setFuzzy(boolean fuzzy) {
        this.fuzzy = fuzzy;
    }

    public String getClassification() {
        return classification;
    }

    public void setClassification(String classification) {
        this.classification = classification;
    }

}
