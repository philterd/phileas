package com.mtnfog.phileas.model.profile.filters;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.mtnfog.phileas.model.enums.SensitivityLevel;
import com.mtnfog.phileas.model.profile.filters.strategies.dynamic.CityFilterStrategy;

import java.util.List;

public class City extends AbstractFilter {

    @SerializedName("cityFilterStrategies")
    @Expose
    private List<CityFilterStrategy> cityFilterStrategies;

    @SerializedName("sensitivity")
    @Expose
    private String sensitivity = SensitivityLevel.MEDIUM.getName();

    @SerializedName("capitalized")
    @Expose
    private boolean capitalized = false;

    public List<CityFilterStrategy> getCityFilterStrategies() {
        return cityFilterStrategies;
    }

    public void setCityFilterStrategies(List<CityFilterStrategy> cityFilterStrategies) {
        this.cityFilterStrategies = cityFilterStrategies;
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