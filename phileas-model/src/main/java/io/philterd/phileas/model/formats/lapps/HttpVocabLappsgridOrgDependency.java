package io.philterd.phileas.model.formats.lapps;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class HttpVocabLappsgridOrgDependency {

    @SerializedName("producer")
    @Expose
    private String producer;

    @SerializedName("type")
    @Expose
    private String type;

    public String getProducer() {
        return producer;
    }

    public void setProducer(String producer) {
        this.producer = producer;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

}