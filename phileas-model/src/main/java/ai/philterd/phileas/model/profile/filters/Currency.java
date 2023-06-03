package ai.philterd.phileas.model.profile.filters;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import ai.philterd.phileas.model.profile.filters.strategies.rules.CurrencyFilterStrategy;

import java.util.List;

public class Currency extends AbstractFilter {

    @SerializedName("currencyFilterStrategies")
    @Expose
    private List<CurrencyFilterStrategy> currencyFilterStrategies;

    public List<CurrencyFilterStrategy> getCurrencyFilterStrategies() {
        return currencyFilterStrategies;
    }

    public void setCurrencyFilterStrategies(List<CurrencyFilterStrategy> currencyFilterStrategies) {
        this.currencyFilterStrategies = currencyFilterStrategies;
    }

}