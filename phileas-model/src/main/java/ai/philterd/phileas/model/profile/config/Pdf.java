package ai.philterd.phileas.model.profile.config;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Pdf {

    @SerializedName("enabled")
    @Expose
    private String redactionColor = "black";

    public String getRedactionColor() {
        return redactionColor;
    }

    public void setRedactionColor(String redactionColor) {
        this.redactionColor = redactionColor;
    }

}
