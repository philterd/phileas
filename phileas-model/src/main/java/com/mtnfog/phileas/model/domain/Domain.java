package com.mtnfog.phileas.model.domain;

import com.mtnfog.phileas.model.profile.Ignored;
import com.mtnfog.phileas.model.profile.filters.AbstractFilter;

import java.util.List;

public abstract class Domain {

    public static final String DOMAIN_LEGAL = "legal";
    public static final String DOMAIN_HEALTH = "health";

    public abstract Ignored getIgnored();
    public abstract List<AbstractFilter> getFilters();

}
