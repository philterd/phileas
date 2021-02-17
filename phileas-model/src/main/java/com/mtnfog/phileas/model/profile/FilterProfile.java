package com.mtnfog.phileas.model.profile;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class FilterProfile {

    public static final String DOMAIN_LEGAL = "legal";
    public static final String DOMAIN_HEALTH = "health";

    @SerializedName("name")
    @Expose
    private String name;

    @SerializedName("domain")
    @Expose
    private String domain;

    @SerializedName("crypto")
    @Expose
    private Crypto crypto;

    @SerializedName("identifiers")
    @Expose
    private Identifiers identifiers = new Identifiers();

    @SerializedName("ignored")
    @Expose
    private List<Ignored> ignored;

    @SerializedName("ignoredPatterns")
    @Expose
    private List<IgnoredPattern> ignoredPatterns;

    @SerializedName("structured")
    @Expose
    private Structured structured;

    @SerializedName("config")
    @Expose
    private Config config = new Config();

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

    public List<IgnoredPattern> getIgnoredPatterns() {
        return ignoredPatterns;
    }

    public void setIgnoredPatterns(List<IgnoredPattern> ignoredPatterns) {
        this.ignoredPatterns = ignoredPatterns;
    }

    public Config getConfig() {
        return config;
    }

    public void setConfig(Config config) {
        this.config = config;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

}