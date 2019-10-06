package com.mtnfog.phileas.model.profile.filters;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.mtnfog.phileas.model.enums.SensitivityLevel;
import com.mtnfog.phileas.model.profile.filters.strategies.dynamic.FirstNameFilterStrategy;

import java.util.List;

public class FirstName {

    @SerializedName("firstNameFilterStrategies")
    @Expose
    private List<FirstNameFilterStrategy> firstNameFilterStrategies;

    @SerializedName("sensitivity")
    @Expose
    private String sensitivity = SensitivityLevel.MEDIUM.getName();

    public List<FirstNameFilterStrategy> getFirstNameFilterStrategies() {
        return firstNameFilterStrategies;
    }

    public void setFirstNameFilterStrategies(List<FirstNameFilterStrategy> firstNameFilterStrategies) {
        this.firstNameFilterStrategies = firstNameFilterStrategies;
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