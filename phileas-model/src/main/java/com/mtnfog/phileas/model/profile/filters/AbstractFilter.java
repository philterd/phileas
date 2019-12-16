package com.mtnfog.phileas.model.profile.filters;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.Collections;
import java.util.Set;

public abstract class AbstractFilter {

    @SerializedName("ignored")
    @Expose
    protected Set<String> ignored = Collections.emptySet();

    public void setIgnored(Set<String> ignored) {
        this.ignored = ignored;
    }

    public Set<String> getIgnored() {
        return ignored;
    }

}
