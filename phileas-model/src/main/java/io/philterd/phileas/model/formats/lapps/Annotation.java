package io.philterd.phileas.model.formats.lapps;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Annotation {

    @SerializedName("id")
    @Expose
    private String id;

    @SerializedName("start")
    @Expose
    private Integer start;

    @SerializedName("end")
    @Expose
    private Integer end;

    @SerializedName("@type")
    @Expose
    private String type;

    @SerializedName("features")
    @Expose
    private Features features;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Integer getStart() {
        return start;
    }

    public void setStart(Integer start) {
        this.start = start;
    }

    public Integer getEnd() {
        return end;
    }

    public void setEnd(Integer end) {
        this.end = end;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Features getFeatures() {
        return features;
    }

    public void setFeatures(Features features) {
        this.features = features;
    }

}