package com.mtnfog.phileas.model.services;

import java.io.IOException;
import java.util.List;

/**
 * Extracts texts from documents.
 */
public interface TextExtractor {

    /**
     * Extracts lines of text from a document.
     * @param document A byte array document.
     * @return A list of lines of text from the document.
     * @throws IOException Thrown if the lines cannot be extracted.
     */
    List<String> getLines(byte[] document) throws IOException;

}
