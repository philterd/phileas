package io.philterd.phileas.model.profile.filters;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import io.philterd.phileas.model.enums.SensitivityLevel;
import io.philterd.phileas.model.profile.filters.strategies.dynamic.CountyFilterStrategy;

import java.util.List;

public class County extends AbstractFilter {

    @SerializedName("countyFilterStrategies")
    @Expose
    private List<CountyFilterStrategy> countyFilterStrategies;

    @SerializedName("sensitivity")
    @Expose
    private String sensitivity = SensitivityLevel.MEDIUM.getName();

    @SerializedName("capitalized")
    @Expose
    private boolean capitalized = false;

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

    public boolean isCapitalized() {
        return capitalized;
    }

    public void setCapitalized(boolean capitalized) {
        this.capitalized = capitalized;
    }
}