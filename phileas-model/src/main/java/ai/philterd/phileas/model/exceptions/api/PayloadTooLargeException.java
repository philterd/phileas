package ai.philterd.phileas.model.exceptions.api;

/**
 * This exception corresponds to HTTP response 413 Payload Too Large.
 * 
 * @author Mountain Fog, Inc.
 *
 */
public final class PayloadTooLargeException extends RuntimeException {

	private static final long serialVersionUID = 8498236096061129077L;

	public PayloadTooLargeException(String message) {
		super(message);
	}
	
}
