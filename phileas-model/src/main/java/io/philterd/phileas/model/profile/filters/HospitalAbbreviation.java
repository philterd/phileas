package io.philterd.phileas.model.profile.filters;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import io.philterd.phileas.model.enums.SensitivityLevel;
import io.philterd.phileas.model.profile.filters.strategies.dynamic.HospitalAbbreviationFilterStrategy;

import java.util.List;

public class HospitalAbbreviation extends AbstractFilter {

    @SerializedName("hospitalAbbreviationFilterStrategies")
    @Expose
    private List<HospitalAbbreviationFilterStrategy> hospitalAbbreviationFilterStrategies;

    @SerializedName("sensitivity")
    @Expose
    private String sensitivity = SensitivityLevel.MEDIUM.getName();

    @SerializedName("capitalized")
    @Expose
    private boolean capitalized = false;

    public List<HospitalAbbreviationFilterStrategy> getHospitalAbbreviationFilterStrategies() {
        return hospitalAbbreviationFilterStrategies;
    }

    public void setHospitalAbbreviationFilterStrategies(List<HospitalAbbreviationFilterStrategy> hospitalAbbreviationFilterStrategies) {
        this.hospitalAbbreviationFilterStrategies = hospitalAbbreviationFilterStrategies;
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