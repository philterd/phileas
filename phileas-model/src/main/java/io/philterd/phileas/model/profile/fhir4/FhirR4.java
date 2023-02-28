package io.philterd.phileas.model.profile.fhir4;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class FhirR4 {

    @SerializedName("fhirItems")
    @Expose
    private List<FhirItem> fhirItems;

    public List<FhirItem> getFhirItems() {
        return fhirItems;
    }

    public void setFhirItems(List<FhirItem> fhirItems) {
        this.fhirItems = fhirItems;
    }

}
