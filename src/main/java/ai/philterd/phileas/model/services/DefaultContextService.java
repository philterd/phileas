package ai.philterd.phileas.model.services;

import java.util.HashMap;
import java.util.Map;

public class DefaultContextService implements ContextService {

    private final Map<String, String> context;

    public DefaultContextService() {
        this.context = new HashMap<>();
    }

    @Override
    public boolean containsToken(String token) {
        return context.containsKey(token);
    }

    @Override
    public boolean containsReplacement(String replacement) {
        return context.containsValue(replacement);
    }

    @Override
    public String getReplacement(String token) {
        return context.get(token);
    }

    @Override
    public void putReplacement(String token, String replacement) {
        context.put(token, replacement);
    }

}
