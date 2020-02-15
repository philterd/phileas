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

    @SerializedName("crypto")
    @Expose
    private Crypto crypto;

    @SerializedName("identifiers")
    @Expose
    private Identifiers identifiers;

    @SerializedName("ignored")
    @Expose
    private List<Ignored> ignored;

    @SerializedName("structured")
    @Expose
    private Structured structured;

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

    public Crypto getCrypto() {
        return crypto;
    }

    public void setCrypto(Crypto crypto) {
        this.crypto = crypto;
    }

    public Structured getStructured() {
        return structured;
    }

    public void setStructured(Structured structured) {
        this.structured = structured;
    }

}