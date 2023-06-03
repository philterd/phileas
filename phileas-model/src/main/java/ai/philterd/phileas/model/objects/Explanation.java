package ai.philterd.phileas.model.objects;

import java.util.List;

/**
 * Contains the spans that were identified and the spans that were applied.
 * Provides insight into how the filters executed.
 */
public class Explanation {

    private List<Span> appliedSpans;
    private List<Span> identifiedSpans;

    public Explanation(List<Span> appliedSpans, List<Span> identifiedSpans) {
        this.appliedSpans = appliedSpans;
        this.identifiedSpans = identifiedSpans;
    }

    public List<Span> getAppliedSpans() {
        return appliedSpans;
    }

    public List<Span> getIdentifiedSpans() {
        return identifiedSpans;
    }

}
