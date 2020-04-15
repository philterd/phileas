package com.mtnfog.phileas.model.services;

import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.objects.Span;

public interface DisambiguationService {

    void hashAndInsert(String context, Span span);
    FilterType disambiguate(String context, Span span1, Span span2, Span ambiguousSpan);

}
