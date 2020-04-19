package com.mtnfog.phileas.model.services;

import com.mtnfog.phileas.model.filter.Filter;
import com.mtnfog.phileas.model.profile.FilterProfile;
import com.mtnfog.phileas.model.responses.FilterResponse;

import java.util.List;

/**
 * Processes a certain type of document.
 */
public interface DocumentProcessor {

    FilterResponse process(FilterProfile filterProfile, List<Filter> filters, List<PostFilter> postFilters, String context, String documentId, String input) throws Exception;

}
