package ai.philterd.phileas.services.profiles.cache;

import ai.philterd.phileas.model.services.FilterProfileCacheService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InMemoryFilterProfileCacheService implements FilterProfileCacheService {

    private Map<String, String> cache;

    public InMemoryFilterProfileCacheService() {
        this.cache = new HashMap<>();
    }

    @Override
    public List<String> get() throws IOException {
        return new ArrayList<>(cache.keySet());
    }

    @Override
    public String get(String filterProfileName) throws IOException {
        return cache.get(filterProfileName);
    }

    @Override
    public Map<String, String> getAll() throws IOException {
        return cache;
    }

    @Override
    public void insert(String filterProfileName, String filterProfile) {
        cache.put(filterProfileName, filterProfile);
    }

    @Override
    public void remove(String filterProfileName) {
        cache.remove(filterProfileName);
    }

    @Override
    public void clear() throws IOException {
        cache.clear();
    }

}
