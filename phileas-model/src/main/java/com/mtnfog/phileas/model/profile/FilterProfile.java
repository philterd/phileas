package com.mtnfog.phileas.model.profile;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

public class FilterProfile {

    private static final Logger LOGGER = LogManager.getLogger(FilterProfile.class);

    @SerializedName("name")
    @Expose
    private String name;

    @SerializedName("identifiers")
    @Expose
    private Identifiers identifiers;

    @SerializedName("ignored")
    @Expose
    private List<Ignored> ignored;

    @SerializedName("removePunctuation")
    @Expose
    private boolean removePunctuation = false;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Identifiers getIdentifiers() {
        return identifiers;
    }

    public void setIdentifiers(Identifiers identifiers) {
        this.identifiers = identifiers;
    }

    public List<Ignored> getIgnored() {
        return ignored;
    }

    public void setIgnored(List<Ignored> ignored) {
        this.ignored = ignored;
    }

    public boolean isRemovePunctuation() {
        return removePunctuation;
    }

    public void setRemovePunctuation(boolean removePunctuation) {
        this.removePunctuation = removePunctuation;
    }

}