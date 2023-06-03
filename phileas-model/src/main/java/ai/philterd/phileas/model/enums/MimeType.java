package ai.philterd.phileas.model.enums;

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
     * application/pdf
     */
    APPLICATION_PDF("application/pdf"),

    /**
     * image/jpeg
     */
    IMAGE_JPEG("image/jpeg"),

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
