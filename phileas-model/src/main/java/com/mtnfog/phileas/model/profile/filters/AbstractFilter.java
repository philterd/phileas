package com.mtnfog.phileas.model.profile.filters;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.mtnfog.phileas.model.profile.IgnoredPattern;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public abstract class AbstractFilter {

    @SerializedName("enabled")
    @Expose
    protected boolean enabled = true;

    @SerializedName("ignored")
    @Expose
    protected Set<String> ignored = Collections.emptySet();

    @SerializedName("ignoredFiles")
    @Expose
    protected Set<String> ignoredFiles = Collections.emptySet();

    @SerializedName("ignoredPatterns")
    @Expose
    protected List<IgnoredPattern> ignoredPatterns = Collections.emptyList();

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setIgnored(Set<String> ignored) {
        this.ignored = ignored;
    }

    public Set<String> getIgnored() {
        return ignored;
    }

    public List<IgnoredPattern> getIgnoredPatterns() {
        return ignoredPatterns;
    }

    public void setIgnoredPatterns(List<IgnoredPattern> ignoredPatterns) {
        this.ignoredPatterns = ignoredPatterns;
    }

    public void setIgnoredFiles(Set<String> ignoredFiles) {
        this.ignoredFiles = ignoredFiles;
    }

    public Set<String> getIgnoredFiles() {
        return ignoredFiles;
    }

}
