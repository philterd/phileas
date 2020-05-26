package com.mtnfog.phileas.model.objects;

import com.google.gson.Gson;
import org.apache.commons.lang3.builder.EqualsBuilder;

import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

public class Alert implements Serializable {

    private String id;
    private String strategyId;
    private String context;
    private String documentId;
    private String filterType;
    private boolean viewed;
    private Date date;

    public Alert() {

    }

    public Alert(String strategyId, String context, String documentId, String filterType) {

        this.id = UUID.randomUUID().toString();
        this.strategyId = strategyId;
        this.context = context;
        this.documentId = documentId;
        this.filterType = filterType;
        this.viewed = false;
        this.date = new Date();

    }

    @Override
    public String toString() {

        final Gson gson = new Gson();
        return gson.toJson(this);

    }

    @Override
    public boolean equals(Object o) {
        return EqualsBuilder.reflectionEquals(this, o);
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getStrategyId() {
        return strategyId;
    }

    public void setStrategyId(String strategyId) {
        this.strategyId = strategyId;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    public String getFilterType() {
        return filterType;
    }

    public void setFilterType(String filterType) {
        this.filterType = filterType;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public boolean isViewed() {
        return viewed;
    }

    public void setViewed(boolean viewed) {
        this.viewed = viewed;
    }
}
