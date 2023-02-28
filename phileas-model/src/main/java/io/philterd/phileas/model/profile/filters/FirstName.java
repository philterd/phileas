package io.philterd.phileas.model.profile.filters;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import io.philterd.phileas.model.enums.SensitivityLevel;
import io.philterd.phileas.model.profile.filters.strategies.dynamic.FirstNameFilterStrategy;

import java.util.List;

public class FirstName extends AbstractFilter {

    @SerializedName("firstNameFilterStrategies")
    @Expose
    private List<FirstNameFilterStrategy> firstNameFilterStrategies;

    @SerializedName("sensitivity")
    @Expose
    private String sensitivity = SensitivityLevel.MEDIUM.getName();

    @SerializedName("capitalized")
    @Expose
    private boolean capitalized = false;

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

    public boolean isCapitalized() {
        return capitalized;
    }

    public void setCapitalized(boolean capitalized) {
        this.capitalized = capitalized;
    }

}