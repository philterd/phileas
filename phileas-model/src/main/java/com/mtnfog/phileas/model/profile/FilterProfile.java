package com.mtnfog.phileas.model.profile;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FilterProfile {

    private static final Logger LOGGER = LogManager.getLogger(FilterProfile.class);

    @SerializedName("name")
    @Expose
    private String name;

    @SerializedName("identifiers")
    @Expose
    private Identifiers identifiers;

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

}