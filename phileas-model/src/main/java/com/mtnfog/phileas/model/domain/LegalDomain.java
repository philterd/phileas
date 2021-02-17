package com.mtnfog.phileas.model.domain;

import com.mtnfog.phileas.model.profile.Ignored;

import java.util.Arrays;

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

}
