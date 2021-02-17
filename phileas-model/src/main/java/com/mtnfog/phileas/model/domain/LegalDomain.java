package com.mtnfog.phileas.model.domain;

import com.mtnfog.phileas.model.filter.Filter;
import com.mtnfog.phileas.model.profile.Ignored;
import com.mtnfog.phileas.model.profile.filters.AbstractFilter;
import com.mtnfog.phileas.model.profile.filters.PhysicianName;
import com.mtnfog.phileas.model.profile.filters.strategies.rules.PhysicianNameFilterStrategy;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class LegalDomain extends Domain {

    private static LegalDomain legalDomain;

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
