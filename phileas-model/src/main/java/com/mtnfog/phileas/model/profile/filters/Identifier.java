package com.mtnfog.phileas.model.profile.filters;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.mtnfog.phileas.model.profile.filters.strategies.rules.IdentifierFilterStrategy;

import java.util.List;

public class Identifier {

    /**
     * The default regex pattern to use if none is provided in the filter profile.
     */
    public static final String DEFAULT_IDENTIFIER_REGEX = "\\b[A-Z0-9_-]{4,}\\b";

    @SerializedName("identifierFilterStrategies")
    @Expose
    private List<IdentifierFilterStrategy> identifierFilterStrategies;

    @SerializedName("pattern")
    @Expose
    private String pattern = DEFAULT_IDENTIFIER_REGEX;

    @SerializedName("caseSensitive")
    @Expose
    private boolean caseSensitive = true;

    @SerializedName("name")
    @Expose
    private String name;

    public List<IdentifierFilterStrategy> getIdentifierFilterStrategies() {
        return identifierFilterStrategies;
    }

    public void setIdentifierFilterStrategies(List<IdentifierFilterStrategy> identifierFilterStrategies) {
        this.identifierFilterStrategies = identifierFilterStrategies;
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public boolean isCaseSensitive() {
        return caseSensitive;
    }

    public void setCaseSensitive(boolean caseSensitive) {
        this.caseSensitive = caseSensitive;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}