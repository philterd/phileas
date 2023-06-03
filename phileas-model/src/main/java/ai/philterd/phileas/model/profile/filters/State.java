package ai.philterd.phileas.model.profile.filters;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import ai.philterd.phileas.model.enums.SensitivityLevel;
import ai.philterd.phileas.model.profile.filters.strategies.dynamic.StateFilterStrategy;

import java.util.List;

public class State extends AbstractFilter {

    @SerializedName("stateFilterStrategies")
    @Expose
    private List<StateFilterStrategy> stateFilterStrategies;

    @SerializedName("sensitivity")
    @Expose
    private String sensitivity = SensitivityLevel.MEDIUM.getName();

    @SerializedName("capitalized")
    @Expose
    private boolean capitalized = false;

    public List<StateFilterStrategy> getStateFilterStrategies() {
        return stateFilterStrategies;
    }

    public void setStateFilterStrategies(List<StateFilterStrategy> stateFilterStrategies) {
        this.stateFilterStrategies = stateFilterStrategies;
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