package com.mtnfog.phileas.model.services;

import com.mtnfog.phileas.model.profile.FilterProfile;
import com.mtnfog.phileas.model.responses.FilterResponse;

/**
 * Processes a certain type of document.
 */
public interface DocumentProcessor {

    FilterResponse process(FilterProfile filterProfile, String context, String documentId, String input) throws Exception;

}
