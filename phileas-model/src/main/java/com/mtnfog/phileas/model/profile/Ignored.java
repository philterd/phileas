package com.mtnfog.phileas.model.profile;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.Collections;
import java.util.List;

public class Ignored {

    @SerializedName("name")
    @Expose
    private String name;

    @SerializedName("terms")
    @Expose
    private List<String> terms = Collections.emptyList();

    @SerializedName("files")
    @Expose
    private List<String> files = Collections.emptyList();

    @SerializedName("caseSensitive")
    @Expose
    private boolean caseSensitive = false;

    public Ignored() {

    }

    public Ignored(String name, List<String> terms, List<String> files, boolean caseSensitive) {
        this.name = name;
        this.terms = terms;
        this.files = files;
        this.caseSensitive = caseSensitive;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getTerms() {
        return terms;
    }

    public void setTerms(List<String> terms) {
        this.terms = terms;
    }

    public boolean isCaseSensitive() {
        return caseSensitive;
    }

    public void setCaseSensitive(boolean caseSensitive) {
        this.caseSensitive = caseSensitive;
    }

    public List<String> getFiles() {
        return files;
    }

    public void setFiles(List<String> files) {
        this.files = files;
    }

}