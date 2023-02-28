package io.philterd.phileas.model.profile.filters;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import io.philterd.phileas.model.profile.filters.strategies.rules.PhysicianNameFilterStrategy;

import java.util.List;

public class PhysicianName extends AbstractFilter {

    @SerializedName("physicianNameFilterStrategies")
    @Expose
    private List<PhysicianNameFilterStrategy> physicianNameFilterStrategies;

    public List<PhysicianNameFilterStrategy> getPhysicianNameFilterStrategies() {
        return physicianNameFilterStrategies;
    }

    public void setPhysicianNameFilterStrategies(List<PhysicianNameFilterStrategy> physicianNameFilterStrategies) {
        this.physicianNameFilterStrategies = physicianNameFilterStrategies;
    }

}