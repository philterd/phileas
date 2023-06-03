package ai.philterd.phileas.model.services;

import ai.philterd.phileas.model.filter.Filter;
import ai.philterd.phileas.model.profile.FilterProfile;
import ai.philterd.phileas.model.responses.FilterResponse;

import java.util.List;

/**
 * Processes a certain type of document.
 */
public interface DocumentProcessor {

    FilterResponse process(FilterProfile filterProfile, List<Filter> filters, List<PostFilter> postFilters, String context, String documentId, int piece, String input) throws Exception;

}
