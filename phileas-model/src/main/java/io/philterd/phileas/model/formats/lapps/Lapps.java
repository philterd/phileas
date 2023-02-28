package io.philterd.phileas.model.formats.lapps;

import java.util.List;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Lapps {

    public static final String NAMED_ENTITY = "http://vocab.lappsgrid.org/NamedEntity";

    @SerializedName("@context")
    @Expose
    private String context;

    @SerializedName("metadata")
    @Expose
    private Metadata metadata;

    @SerializedName("text")
    @Expose
    private Text text;

    @SerializedName("views")
    @Expose
    private List<View> views = null;

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public Metadata getMetadata() {
        return metadata;
    }

    public void setMetadata(Metadata metadata) {
        this.metadata = metadata;
    }

    public Text getText() {
        return text;
    }

    public void setText(Text text) {
        this.text = text;
    }

    public List<View> getViews() {
        return views;
    }

    public void setViews(List<View> views) {
        this.views = views;
    }

}