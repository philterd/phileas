package com.mtnfog.phileas.model.formats.lapps;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Contains {

    @SerializedName("http://vocab.lappsgrid.org/NamedEntity")
    @Expose
    private HttpVocabLappsgridOrgNamedEntity httpVocabLappsgridOrgNamedEntity;

    @SerializedName("http://vocab.lappsgrid.org/Dependency")
    @Expose
    private HttpVocabLappsgridOrgDependency httpVocabLappsgridOrgDependency;

    public HttpVocabLappsgridOrgNamedEntity getHttpVocabLappsgridOrgNamedEntity() {
        return httpVocabLappsgridOrgNamedEntity;
    }

    public void setHttpVocabLappsgridOrgNamedEntity(HttpVocabLappsgridOrgNamedEntity httpVocabLappsgridOrgNamedEntity) {
        this.httpVocabLappsgridOrgNamedEntity = httpVocabLappsgridOrgNamedEntity;
    }

    public HttpVocabLappsgridOrgDependency getHttpVocabLappsgridOrgDependency() {
        return httpVocabLappsgridOrgDependency;
    }

    public void setHttpVocabLappsgridOrgDependency(HttpVocabLappsgridOrgDependency httpVocabLappsgridOrgDependency) {
        this.httpVocabLappsgridOrgDependency = httpVocabLappsgridOrgDependency;
    }

}