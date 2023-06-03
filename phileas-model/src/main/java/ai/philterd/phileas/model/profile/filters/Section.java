package ai.philterd.phileas.model.profile.filters;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import ai.philterd.phileas.model.profile.filters.strategies.rules.SectionFilterStrategy;

import java.util.List;

public class Section extends AbstractFilter {

    @SerializedName("sectionFilterStrategies")
    @Expose
    private List<SectionFilterStrategy> sectionFilterStrategies;

    @SerializedName("startPattern")
    @Expose
    private String startPattern;

    @SerializedName("endPattern")
    @Expose
    private String endPattern;

    public List<SectionFilterStrategy> getSectionFilterStrategies() {
        return sectionFilterStrategies;
    }

    public void setSectionFilterStrategies(List<SectionFilterStrategy> sectionFilterStrategies) {
        this.sectionFilterStrategies = sectionFilterStrategies;
    }

    public String getStartPattern() {
        return startPattern;
    }

    public void setStartPattern(String startPattern) {
        this.startPattern = startPattern;
    }

    public String getEndPattern() {
        return endPattern;
    }

    public void setEndPattern(String endPattern) {
        this.endPattern = endPattern;
    }

}