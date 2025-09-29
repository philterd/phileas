package ai.philterd.phileas.model.services;

public interface ContextService {

    boolean containsToken(final String token);

    boolean containsReplacement(final String replacement);

    String getReplacement(final String token);

    void putReplacement(final String token, final String replacement);

}
