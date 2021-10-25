package com.mtnfog.phileas.model.profile;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.mtnfog.phileas.model.profile.graphical.BoundingBox;

import java.util.Collections;
import java.util.List;

public class Graphical {

    @SerializedName("boundingBoxes")
    @Expose
    private List<BoundingBox> boundingBoxes = Collections.emptyList();

    public List<BoundingBox> getBoundingBoxes() {
        return boundingBoxes;
    }

    public void setBoundingBoxes(List<BoundingBox> boundingBoxes) {
        this.boundingBoxes = boundingBoxes;
    }

}
