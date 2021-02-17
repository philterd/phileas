package com.mtnfog.phileas.model.domain;

import com.mtnfog.phileas.model.profile.Ignored;

import java.util.Arrays;

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

}
