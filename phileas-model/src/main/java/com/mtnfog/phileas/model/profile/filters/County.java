package com.mtnfog.phileas.model.profile.filters;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.mtnfog.phileas.model.enums.SensitivityLevel;
import com.mtnfog.phileas.model.profile.filters.strategies.dynamic.CountyFilterStrategy;

import java.util.List;

public class County {

    @SerializedName("countyFilterStrategies")
    @Expose
    private List<CountyFilterStrategy> countyFilterStrategies;

    @SerializedName("sensitivity")
    @Expose
    private String sensitivity = SensitivityLevel.MEDIUM.getName();

    public List<CountyFilterStrategy> getCountyFilterStrategies() {
        return countyFilterStrategies;
    }

    public void setCountyFilterStrategies(List<CountyFilterStrategy> countyFilterStrategies) {
        this.countyFilterStrategies = countyFilterStrategies;
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