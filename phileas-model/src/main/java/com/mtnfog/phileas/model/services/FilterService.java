package com.mtnfog.phileas.model.services;

import com.mtnfog.phileas.model.objects.Span;
import com.mtnfog.phileas.model.responses.FilterResponse;

import java.io.IOException;
import java.util.List;

public interface FilterService {

    FilterResponse filter(String filterProfileName, String context, String input) throws IOException;

    List<Span> replacements(String documentId);

}
