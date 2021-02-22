package com.mtnfog.phileas.model.domain;

import com.mtnfog.phileas.model.profile.Ignored;
import com.mtnfog.phileas.model.profile.filters.AbstractFilter;

import java.util.List;

/**
 * Represents an industry domain in which Philter can be used.
 * A domain provides convenience filter profile configuration
 * options.
 */
public abstract class Domain {

    // These are the domain names given in the filter profile
    // used to trigger the activation of the respective domain.
    public static final String DOMAIN_LEGAL = "legal";
    public static final String DOMAIN_HEALTH = "health";

    /**
     * The terms that should be ignored in the domain.
     * These terms will be ignored globally.
     * @return A list of {@link Ignored} terms.
     */
    public abstract Ignored getIgnored();

    /**
     * A list of {@link AbstractFilter} that should be used for the
     * domain.
     *
     * These filters will only be applied if the same
     * filter is not already defined in the filter profile. By enabling
     * a domain and then still adding the filter to the filter profile
     * the user can override this filter (can disable it, customize it, etc.)
     * @return A list of {@link AbstractFilter}.
     */
    public abstract List<AbstractFilter> getFilters();

}
