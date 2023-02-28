package io.philterd.phileas.model.objects;

public enum DocumentType {

    SUBPOENA("SUBPOENA"),
    UNKNOWN("UNKNOWN");

    private String name;

    DocumentType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }

}
