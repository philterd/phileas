package com.mtnfog.phileas.model.objects;

import com.mtnfog.phileas.model.enums.FilterType;

public class Entity {

    private int characterStart;
    private int characterEnd;
    private FilterType filterType = FilterType.PERSON;
    private String context;
    private String documentId;
    private String text;
    private double confidence;

    public Entity(int characterStart, int characterEnd, FilterType filterType, String context, String documentId, String text, double confidence) {

        this.characterStart = characterStart;
        this.characterEnd = characterEnd;
        this.filterType = filterType;
        this.context = context;
        this.documentId = documentId;
        this.text = text;
        this.confidence = confidence;

    }

    public int getCharacterStart() {
        return characterStart;
    }

    public int getCharacterEnd() {
        return characterEnd;
    }

    public FilterType getFilterType() {
        return filterType;
    }

    public String getContext() {
        return context;
    }

    public String getDocumentId() {
        return documentId;
    }

    public String getText() {
        return text;
    }

    public double getConfidence() {
        return confidence;
    }

}
