package io.philterd.phileas.model.profile.filters;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import io.philterd.phileas.model.profile.filters.strategies.rules.DateFilterStrategy;

import java.util.List;

public class Date extends AbstractFilter {

    @SerializedName("onlyValidDates")
    @Expose
    protected boolean onlyValidDates = false;

    @SerializedName("dateFilterStrategies")
    @Expose
    private List<DateFilterStrategy> dateFilterStrategies;

    public List<DateFilterStrategy> getDateFilterStrategies() {
        return dateFilterStrategies;
    }

    public void setDateFilterStrategies(List<DateFilterStrategy> dateFilterStrategies) {
        this.dateFilterStrategies = dateFilterStrategies;
    }

    public void setOnlyValidDates(boolean onlyValidDates) {
        this.onlyValidDates = onlyValidDates;
    }

    public boolean isOnlyValidDates() {
        return onlyValidDates;
    }

}