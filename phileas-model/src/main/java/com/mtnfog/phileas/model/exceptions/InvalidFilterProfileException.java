package com.mtnfog.phileas.model.exceptions;

/**
 * Thrown when the requested filter profile does not exist.
 */
public class InvalidFilterProfileException extends RuntimeException {

    public InvalidFilterProfileException(String message) {
        super(message);
    }

}
