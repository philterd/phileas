package ai.philterd.phileas.model.profile;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class PostFilters {

    @SerializedName("removeTrailingPeriods")
    @Expose
    private boolean removeTrailingPeriods = true;

    @SerializedName("removeTrailingSpaces")
    @Expose
    private boolean removeTrailingSpaces = true;

    @SerializedName("removeTrailingNewLines")
    @Expose
    private boolean removeTrailingNewLines = true;

    public boolean isRemoveTrailingPeriods() {
        return removeTrailingPeriods;
    }

    public void setRemoveTrailingPeriods(boolean removeTrailingPeriods) {
        this.removeTrailingPeriods = removeTrailingPeriods;
    }

    public boolean isRemoveTrailingSpaces() {
        return removeTrailingSpaces;
    }

    public void setRemoveTrailingSpaces(boolean removeTrailingSpaces) {
        this.removeTrailingSpaces = removeTrailingSpaces;
    }

    public boolean isRemoveTrailingNewLines() {
        return removeTrailingNewLines;
    }

    public void setRemoveTrailingNewLines(boolean removeTrailingNewLines) {
        this.removeTrailingNewLines = removeTrailingNewLines;
    }

}
