package ai.philterd.phileas.model.profile;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import ai.philterd.phileas.model.profile.fhir4.FhirR4;

public class Structured {

    @SerializedName("fhir_v4")
    @Expose
    private FhirR4 fhirR4;

    public FhirR4 getFhirR4() {
        return fhirR4;
    }

    public void setFhirR4(FhirR4 fhirR4) {
        this.fhirR4 = fhirR4;
    }

}
