package com.mtnfog.phileas.services.registry;

import com.mtnfog.phileas.model.services.FilterProfileService;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class ProxyFilterProfileService implements FilterProfileService {

    private FilterProfileService filterProfileService;

    public ProxyFilterProfileService(FilterProfileService filterProfileService) {
        this.filterProfileService = filterProfileService;
    }

    @Override
    public List<String> get() throws IOException {
        return filterProfileService.get();
    }

    @Override
    public String get(String filterProfileName) throws IOException {
        return filterProfileService.get(filterProfileName);
    }

    @Override
    public Map<String, String> getAll() throws IOException {
        return filterProfileService.getAll();
    }

    @Override
    public void save(String filterProfileJson) throws IOException {
        filterProfileService.save(filterProfileJson);
    }

    @Override
    public void delete(String filterProfileName) throws IOException {
        filterProfileService.delete(filterProfileName);
    }

}
