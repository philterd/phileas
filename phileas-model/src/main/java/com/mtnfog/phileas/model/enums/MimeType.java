package com.mtnfog.phileas.model.enums;

/**
 * A mime type
 */
public enum MimeType {

    /**
     * text/plain
     */
    TEXT_PLAIN("text/plain"),

    /**
     * text/html
     */
    TEXT_HTML("text/html"),

    /**
     * application/fhir+json
     */
    APPLICATION_FHIRJSON("application/fhir+json");

    private String value;

    MimeType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }

}
