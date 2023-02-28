package io.philterd.phileas.model.profile.filters;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import io.philterd.phileas.model.profile.filters.strategies.rules.DriversLicenseFilterStrategy;

import java.util.List;

public class DriversLicense extends AbstractFilter {

    @SerializedName("driversLicenseFilterStrategies")
    @Expose
    private List<DriversLicenseFilterStrategy> driversLicenseFilterStrategies;

    public List<DriversLicenseFilterStrategy> getDriversLicenseFilterStrategies() {
        return driversLicenseFilterStrategies;
    }

    public void setDriversLicenseFilterStrategies(List<DriversLicenseFilterStrategy> driversLicenseFilterStrategies) {
        this.driversLicenseFilterStrategies = driversLicenseFilterStrategies;
    }

}