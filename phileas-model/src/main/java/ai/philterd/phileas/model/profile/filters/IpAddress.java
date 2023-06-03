package ai.philterd.phileas.model.profile.filters;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import ai.philterd.phileas.model.profile.filters.strategies.rules.IpAddressFilterStrategy;

import java.util.List;

public class IpAddress extends AbstractFilter {

    @SerializedName("ipAddressFilterStrategies")
    @Expose
    private List<IpAddressFilterStrategy> ipAddressFilterStrategies;

    public List<IpAddressFilterStrategy> getIpAddressFilterStrategies() {
        return ipAddressFilterStrategies;
    }

    public void setIpAddressFilterStrategies(List<IpAddressFilterStrategy> ipAddressFilterStrategies) {
        this.ipAddressFilterStrategies = ipAddressFilterStrategies;
    }

}