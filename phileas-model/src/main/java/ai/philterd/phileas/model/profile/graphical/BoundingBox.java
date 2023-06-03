package ai.philterd.phileas.model.profile.graphical;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import ai.philterd.phileas.model.enums.MimeType;
import ai.philterd.phileas.model.profile.filters.AbstractFilter;

public class BoundingBox extends AbstractFilter {

    @SerializedName("color")
    @Expose
    private String color;

    @SerializedName("mimeType")
    @Expose
    private String mimeType = MimeType.APPLICATION_PDF.toString();

    @SerializedName("x")
    @Expose
    private float x;

    @SerializedName("y")
    @Expose
    private float y;

    @SerializedName("w")
    @Expose
    private float w;

    @SerializedName("h")
    @Expose
    private float h;

    @SerializedName("page")
    @Expose
    private int page = 1;

    public float getX() {
        return x;
    }

    public void setX(float x) {
        this.x = x;
    }

    public float getY() {
        return y;
    }

    public void setY(float y) {
        this.y = y;
    }

    public float getW() {
        return w;
    }

    public void setW(float w) {
        this.w = w;
    }

    public float getH() {
        return h;
    }

    public void setH(float h) {
        this.h = h;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

}