package com.mtnfog.phileas.model.profile.filters;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.mtnfog.phileas.model.enums.SensitivityLevel;
import com.mtnfog.phileas.model.profile.filters.strategies.dynamic.HospitalFilterStrategy;

import java.util.List;

public class Hospital extends AbstractFilter {

    @SerializedName("hospitalFilterStrategies")
    @Expose
    private List<HospitalFilterStrategy> hospitalFilterStrategies;

    @SerializedName("sensitivity")
    @Expose
    private String sensitivity = SensitivityLevel.MEDIUM.getName();

    public List<HospitalFilterStrategy> getHospitalFilterStrategies() {
        return hospitalFilterStrategies;
    }

    public void setHospitalFilterStrategies(List<HospitalFilterStrategy> hospitalFilterStrategies) {
        this.hospitalFilterStrategies = hospitalFilterStrategies;
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