package com.mtnfog.phileas.model.services;

import com.mtnfog.phileas.model.enums.MimeType;

import java.io.IOException;

/**
 * Redacts a document.
 */
public interface Redacter {

    byte[] process(byte[] document, MimeType outputType) throws IOException;

}
