package com.mtnfog.phileas.model.filter.dynamic;

import com.mtnfog.phileas.model.objects.Span;
import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.filter.Filter;
import com.mtnfog.phileas.model.profile.FilterProfile;
import com.mtnfog.phileas.model.services.AnonymizationService;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;

public abstract class DynamicFilter extends Filter implements Serializable {

    /**
     * Filters the input text.
     * @param filterProfile The {@link FilterProfile} to use.
     * @param context The context.
     * @param documentId An ID uniquely identifying the document.
     * @param text The input text.
     * @return A list of {@link Span spans}.
     */
    public abstract List<Span> filter(FilterProfile filterProfile, String context, String documentId, String text) throws IOException;

    /**
     * Creates a new dynamic filter.
     * @param filterType The {@link FilterType type} of the filter.
     * @param anonymizationService The {@link AnonymizationService} for this filter.
     */
    public DynamicFilter(FilterType filterType, AnonymizationService anonymizationService) {
        super(filterType, anonymizationService);
    }

    /**
     * Gets the count of occurrences of items in the input.
     * @param filterProfile The {@link FilterProfile} to use for the filtering.
     * @param text The input text.
     * @return The count of occurrences of items in the input.
     */
    public int getOccurrences(FilterProfile filterProfile, String text) throws IOException {
        return filter(filterProfile, "none", "none", text).size();
    }

}
