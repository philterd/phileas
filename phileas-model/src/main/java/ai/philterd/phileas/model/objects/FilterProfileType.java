package ai.philterd.phileas.model.objects;

public enum FilterProfileType {

    JSON(".json"),
    YML(".yml");

    private String fileExtension;

    @Override
    public String toString() {
        return fileExtension;
    }

    FilterProfileType(String fileExtension) {
        this.fileExtension = fileExtension;
    }

    public String getFileExtension() {
        return fileExtension;
    }

}
