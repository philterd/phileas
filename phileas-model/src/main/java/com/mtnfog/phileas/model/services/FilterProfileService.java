package com.mtnfog.phileas.model.services;

import com.mtnfog.phileas.model.profile.FilterProfile;

import java.io.IOException;
import java.util.Map;

public interface FilterProfileService {

    String getFilterProfile(String filterProfileName) throws IOException;

    Map<String, FilterProfile> getAll() throws IOException;

}
