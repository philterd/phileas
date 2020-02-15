package com.mtnfog.phileas.model.profile;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class FhirR4 {

    @SerializedName("itemPaths")
    @Expose
    private List<String> itemPaths;

    public List<String> getItemPaths() {
        return itemPaths;
    }

    public void setItemPaths(List<String> itemPaths) {
        this.itemPaths = itemPaths;
    }

}
