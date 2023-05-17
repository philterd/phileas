package io.philterd.phileas.model.objects;

public class PdfRedactionOptions extends RedactionOptions {

    private int dpi = 150;
    private float compressionQuality = 1.0F;
    private float scale = 1.0F;

    public PdfRedactionOptions() {

    }

    public PdfRedactionOptions(int dpi, float compressionQuality, float scale) {
        this.dpi = dpi;
        this.compressionQuality = compressionQuality;
        this.scale = scale;
    }

    public int getDpi() {
        return dpi;
    }

    public void setDpi(int dpi) {
        this.dpi = dpi;
    }

    public float getCompressionQuality() {
        return compressionQuality;
    }

    public void setCompressionQuality(float compressionQuality) {
        this.compressionQuality = compressionQuality;
    }

    public float getScale() {
        return scale;
    }

    public void setScale(float scale) {
        this.scale = scale;
    }

}
