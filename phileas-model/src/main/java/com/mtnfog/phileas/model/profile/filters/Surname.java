package com.mtnfog.phileas.model.profile.filters;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.mtnfog.phileas.model.enums.SensitivityLevel;
import com.mtnfog.phileas.model.profile.filters.strategies.dynamic.SurnameFilterStrategy;

import java.util.List;

public class Surname {

    @SerializedName("surnameFilterStrategies")
    @Expose
    private List<SurnameFilterStrategy> surnameFilterStrategies;

    @SerializedName("sensitivity")
    @Expose
    private String sensitivity = SensitivityLevel.MEDIUM.getName();

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

}