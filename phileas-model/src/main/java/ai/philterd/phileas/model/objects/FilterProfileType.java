package ai.philterd.phileas.model.objects;

public enum FilterProfileType {

    JSON("json"),
    YML("yml");

    private String fileExtension;

    FilterProfileType(String fileExtension) {
        this.fileExtension = fileExtension;
    }

    public String getFileExtension() {
        return fileExtension;
    }

}
