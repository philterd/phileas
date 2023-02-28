package io.philterd.phileas.model.profile.filters;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import io.philterd.phileas.model.profile.filters.strategies.rules.UrlFilterStrategy;

import java.util.List;

public class Url extends AbstractFilter {

    @SerializedName("urlFilterStrategies")
    @Expose
    private List<UrlFilterStrategy> urlFilterStrategies;

    @SerializedName("requireHttpWwwPrefix")
    @Expose
    private boolean requireHttpWwwPrefix = true;

    public List<UrlFilterStrategy> getUrlFilterStrategies() {
        return urlFilterStrategies;
    }

    public void setUrlFilterStrategies(List<UrlFilterStrategy> urlFilterStrategies) {
        this.urlFilterStrategies = urlFilterStrategies;
    }

    public boolean isRequireHttpWwwPrefix() {
        return requireHttpWwwPrefix;
    }

    public void setRequireHttpWwwPrefix(boolean requireHttpWwwPrefix) {
        this.requireHttpWwwPrefix = requireHttpWwwPrefix;
    }

}