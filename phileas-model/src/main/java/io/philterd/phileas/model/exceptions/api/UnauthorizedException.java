package io.philterd.phileas.model.exceptions.api;

/**
 * This exception corresponds to HTTP error 401 Unauthorized.
 * 
 * @author Mountain Fog, Inc.
 *
 */
public final class UnauthorizedException extends RuntimeException {

	private static final long serialVersionUID = 8498236096061129077L;
	
	public UnauthorizedException(String message) {
		super(message);
	}
	
}
