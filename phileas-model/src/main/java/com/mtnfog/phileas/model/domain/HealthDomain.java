package com.mtnfog.phileas.model.domain;

import com.mtnfog.phileas.model.profile.Ignored;
import com.mtnfog.phileas.model.profile.filters.AbstractFilter;
import com.mtnfog.phileas.model.profile.filters.PhysicianName;
import com.mtnfog.phileas.model.profile.filters.strategies.rules.PhysicianNameFilterStrategy;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class HealthDomain extends Domain {

    private static HealthDomain legalDomain;

    public static Domain getInstance() {

        if(legalDomain == null) {
            legalDomain = new HealthDomain();
        }

        return legalDomain;

    }

    @Override
    public Ignored getIgnored() {

        final Ignored ignored = new Ignored();

        ignored.setName("health-domain");
        ignored.setTerms(Arrays.asList("Doctor", "Physician"));

        return ignored;

    }

    @Override
    public List<AbstractFilter> getFilters() {

        final List<AbstractFilter> filters = new LinkedList<>();

        final List<PhysicianNameFilterStrategy> physicianNameFilterStrategies = new LinkedList<>();
        physicianNameFilterStrategies.add(new PhysicianNameFilterStrategy());

        final PhysicianName physicianName = new PhysicianName();
        physicianName.setPhysicianNameFilterStrategies(physicianNameFilterStrategies);

        filters.add(physicianName);

        return filters;

    }

}
