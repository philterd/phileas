package com.mtnfog.phileas.model.objects;

public class Candidate {

    private String filterProfileName;
    private String context;
    private String documentId;
    private String token;
    private double initialConfidence;
    private FilterPattern filterPattern;

    public Candidate(String filterProfileName, String context, String documentId, String token, double initialConfidence) {

        this.filterProfileName = filterProfileName;
        this.context = context;
        this.documentId = documentId;
        this.token = token;
        this.initialConfidence = initialConfidence;

    }

    public Candidate(String filterProfileName, String context, String documentId, String token, double initialConfidence, FilterPattern filterPattern) {

        this.filterProfileName = filterProfileName;
        this.context = context;
        this.documentId = documentId;
        this.token = token;
        this.initialConfidence = initialConfidence;
        this.filterPattern = filterPattern;

    }

    public String getFilterProfileName() {
        return filterProfileName;
    }

    public void setFilterProfileName(String filterProfileName) {
        this.filterProfileName = filterProfileName;
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

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public double getInitialConfidence() {
        return initialConfidence;
    }

    public void setInitialConfidence(double initialConfidence) {
        this.initialConfidence = initialConfidence;
    }

    public FilterPattern getFilterPattern() {
        return filterPattern;
    }

    public void setFilterPattern(FilterPattern filterPattern) {
        this.filterPattern = filterPattern;
    }

}