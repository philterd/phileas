package ai.philterd.phileas.model.profile.filters;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import ai.philterd.phileas.model.profile.filters.strategies.rules.PassportNumberFilterStrategy;

import java.util.List;

public class PassportNumber extends AbstractFilter {

    @SerializedName("passportNumberFilterStrategies")
    @Expose
    private List<PassportNumberFilterStrategy> passportNumberFilterStrategies;

    public List<PassportNumberFilterStrategy> getPassportNumberFilterStrategies() {
        return passportNumberFilterStrategies;
    }

    public void setPassportNumberFilterStrategies(List<PassportNumberFilterStrategy> passportNumberFilterStrategies) {
        this.passportNumberFilterStrategies = passportNumberFilterStrategies;
    }

}