package com.mtnfog.phileas.model.profile.filters;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.mtnfog.phileas.model.enums.SensitivityLevel;
import com.mtnfog.phileas.model.profile.filters.strategies.dynamic.SurnameFilterStrategy;

import java.util.List;

public class Surname extends AbstractFilter {

    @SerializedName("surnameFilterStrategies")
    @Expose
    private List<SurnameFilterStrategy> surnameFilterStrategies;

    @SerializedName("sensitivity")
    @Expose
    private String sensitivity = SensitivityLevel.MEDIUM.getName();

    @SerializedName("capitalized")
    @Expose
    private boolean capitalized = false;

    public List<SurnameFilterStrategy> getSurnameFilterStrategies() {
        return surnameFilterStrategies;
    }

    public void setSurnameFilterStrategies(List<SurnameFilterStrategy> surnameFilterStrategies) {
        this.surnameFilterStrategies = surnameFilterStrategies;
    }

    public SensitivityLevel getSensitivityLevel() {
        return SensitivityLevel.fromName(sensitivity);
    }

    public String getSensitivity() {
        return sensitivity;
    }

    public void setSensitivity(String sensitivity) {
        this.sensitivity = sensitivity;
    }

    public boolean isCapitalized() {
        return capitalized;
    }

    public void setCapitalized(boolean capitalized) {
        this.capitalized = capitalized;
    }

}