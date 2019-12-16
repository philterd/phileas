package com.mtnfog.phileas.model.profile.filters;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.mtnfog.phileas.model.profile.filters.strategies.rules.DateFilterStrategy;

import java.util.List;

public class Date extends AbstractFilter {

    @SerializedName("dateFilterStrategies")
    @Expose
    private List<DateFilterStrategy> dateFilterStrategies;

    public List<DateFilterStrategy> getDateFilterStrategies() {
        return dateFilterStrategies;
    }

    public void setDateFilterStrategies(List<DateFilterStrategy> dateFilterStrategies) {
        this.dateFilterStrategies = dateFilterStrategies;
    }
    
}