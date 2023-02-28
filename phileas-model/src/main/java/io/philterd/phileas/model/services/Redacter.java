package io.philterd.phileas.model.services;

import io.philterd.phileas.model.enums.MimeType;

import java.io.IOException;

/**
 * Redacts a document.
 */
public interface Redacter {

    byte[] process(byte[] document, MimeType outputType) throws IOException;

}
