package io.philterd.phileas.model.responses;

public class ImageFilterResponse {

    private byte[] image;
    private int redactions;

    public ImageFilterResponse(byte[] image, int redactions) {
        this.image = image;
        this.redactions = redactions;
    }

    public byte[] getImage() {
        return image;
    }

    public void setImage(byte[] image) {
        this.image = image;
    }

    public int getRedactions() {
        return redactions;
    }

    public void setRedactions(int redactions) {
        this.redactions = redactions;
    }

}
