package ai.philterd.phileas.model.services;

import ai.philterd.phileas.model.objects.Span;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;

/**
 * A database store of {@link Span spans}.
 */
public interface Store extends Serializable {

    /**
     * Inserts a {@link Span span} in the database
     * @param span The {@link Span span} to store.
     */
    void insert(Span span) throws IOException;

    /**
     * Inserts many {@link Span spans} in the database
     * @param spans The {@link Span spans} to store.
     */
    void insert(List<Span> spans) throws IOException;

    /**
     * Gets a list of spans for a document ID.
     * @param documentId The document ID.
     * @return A list of matching {@link Span spans}.
     */
    List<Span> getByDocumentId(String documentId) throws IOException;

}
