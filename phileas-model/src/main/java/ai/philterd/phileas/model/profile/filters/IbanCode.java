package ai.philterd.phileas.model.profile.filters;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import ai.philterd.phileas.model.profile.filters.strategies.rules.IbanCodeFilterStrategy;

import java.util.List;

public class IbanCode extends AbstractFilter {

    @SerializedName("onlyValidIBANCodes")
    @Expose
    protected boolean onlyValidIBANCodes = true;

    @SerializedName("allowSpaces")
    @Expose
    protected boolean allowSpaces = true;

    @SerializedName("ibanCodeFilterStrategies")
    @Expose
    private List<IbanCodeFilterStrategy> ibanCodeFilterStrategies;

    public List<IbanCodeFilterStrategy> getIbanCodeFilterStrategies() {
        return ibanCodeFilterStrategies;
    }

    public void setIbanCodeFilterStrategies(List<IbanCodeFilterStrategy> ibanCodeFilterStrategies) {
        this.ibanCodeFilterStrategies = ibanCodeFilterStrategies;
    }

    public boolean isOnlyValidIBANCodes() {
        return onlyValidIBANCodes;
    }

    public void setOnlyValidIBANCodes(boolean onlyValidIBANCodes) {
        this.onlyValidIBANCodes = onlyValidIBANCodes;
    }

    public boolean isAllowSpaces() {
        return allowSpaces;
    }

    public void setAllowSpaces(boolean allowSpaces) {
        this.allowSpaces = allowSpaces;
    }

}