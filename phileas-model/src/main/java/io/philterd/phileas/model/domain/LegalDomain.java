package io.philterd.phileas.model.domain;

import io.philterd.phileas.model.profile.Ignored;
import io.philterd.phileas.model.profile.filters.AbstractFilter;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * A {@link Domain} for legal.
 */
public class LegalDomain extends Domain {

    private static LegalDomain legalDomain;

    /**
     * Get an instance of the {@link LegalDomain} class.
     * @return An instance of the {@link LegalDomain} class.
     */
    public static Domain getInstance() {

        if(legalDomain == null) {
            legalDomain = new LegalDomain();
        }

        return legalDomain;

    }

    @Override
    public Ignored getIgnored() {

        final Ignored ignored = new Ignored();

        ignored.setName("legal-domain");
        ignored.setTerms(Arrays.asList("Defendant", "Plaintiff", "Court"));

        return ignored;

    }

    @Override
    public List<AbstractFilter> getFilters() {

        final List<AbstractFilter> filters = new LinkedList<>();

        // TODO: Determine what filters are appropriate.

        return filters;

    }

}
