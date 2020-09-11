package com.mtnfog.phileas.model.services;

import com.mtnfog.phileas.configuration.PhileasConfiguration;
import com.mtnfog.phileas.model.enums.MimeType;
import com.mtnfog.phileas.model.objects.Span;
import com.mtnfog.phileas.model.responses.DetectResponse;
import com.mtnfog.phileas.model.responses.FilterResponse;

import java.io.IOException;
import java.util.List;

/**
 * Interface for implementing filter services.
 */
public interface FilterService {

    /**
     * Filter text.
     * @param filterProfileName The name of the filter profile.
     * @param context The context.
     * @param documentId A document ID. Provide <code>null</code> for a document ID to be generated.
     * @param input The input text.
     * @param mimeType The {@link MimeType}.
     * @return A {@link FilterResponse}.
     * @throws Exception Thrown if the text cannot be filtered.
     */
    FilterResponse filter(String filterProfileName, String context, String documentId, String input, MimeType mimeType) throws Exception;

    /**
     * Detect the possible types of sensitive information in text.
     * @param input The input text.
     * @param mimeType The {@link MimeType}.
     * @return A {@link DetectResponse}.
     */
    DetectResponse detect(String input, MimeType mimeType) throws Exception;

    /**
     * Get the replacement spans for a document.
     * @param documentId The document ID.
     * @return A list of {@link Span}.
     * @throws IOException Thrown if the replacements cannot be retrieved.
     */
    List<Span> replacements(String documentId) throws IOException;

    /**
     * Gets the filter profile service being used.
     * @return A {@link FilterProfileService}.
     */
    FilterProfileService getFilterProfileService();

    /**
     * Returns the alert service being used.
     * @return An {@link AlertService}.
     */
    AlertService getAlertService();

}
