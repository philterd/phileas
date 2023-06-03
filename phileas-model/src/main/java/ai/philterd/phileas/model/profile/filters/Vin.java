package ai.philterd.phileas.model.profile.filters;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import ai.philterd.phileas.model.profile.filters.strategies.rules.VinFilterStrategy;

import java.util.List;

public class Vin extends AbstractFilter {

    @SerializedName("vinFilterStrategies")
    @Expose
    private List<VinFilterStrategy> vinFilterStrategies;

    public List<VinFilterStrategy> getVinFilterStrategies() {
        return vinFilterStrategies;
    }

    public void setVinFilterStrategies(List<VinFilterStrategy> vinFilterStrategies) {
        this.vinFilterStrategies = vinFilterStrategies;
    }

}