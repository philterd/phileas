package com.mtnfog.phileas.services.filters.regex;

import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.filter.rules.regex.RegexFilter;
import com.mtnfog.phileas.model.objects.Analyzer;
import com.mtnfog.phileas.model.objects.FilterPattern;
import com.mtnfog.phileas.model.objects.Span;
import com.mtnfog.phileas.model.profile.Crypto;
import com.mtnfog.phileas.model.profile.FilterProfile;
import com.mtnfog.phileas.model.profile.filters.strategies.AbstractFilterStrategy;
import com.mtnfog.phileas.model.services.AnonymizationService;

import java.io.Serializable;
import java.util.*;
import java.util.regex.Pattern;

public class DriversLicenseFilter extends RegexFilter implements Serializable {

    private static final HashMap<String, Pattern> DRIVERS_LICENSES_REGEX = new HashMap<>();

    public DriversLicenseFilter(List<? extends AbstractFilterStrategy> strategies, AnonymizationService anonymizationService, Set<String> ignored, Crypto crypto, int windowSize) {
        super(FilterType.DRIVERS_LICENSE_NUMBER, strategies, anonymizationService, ignored, crypto, windowSize);

        // https://ntsi.com/drivers-license-format/
        // https://www.mvrdecoder.com/content/drvlicformats.aspx

        DRIVERS_LICENSES_REGEX.put("ALABAMA", Pattern.compile("\\b[0-9]{8}\\b"));        // https://www.mvtrip.alabama.gov/News/ViewArticle/444
        DRIVERS_LICENSES_REGEX.put("ALASKA", Pattern.compile("\\b[0-9]{7}\\b"));
        DRIVERS_LICENSES_REGEX.put("ARIZONA", Pattern.compile("\\b([A-Z][0-9]{8})|([0-9]{9})\\b"));
        DRIVERS_LICENSES_REGEX.put("ARKANSAS", Pattern.compile("\\b[9][0-9]{7}\\b"));
        DRIVERS_LICENSES_REGEX.put("CALIFORNIA", Pattern.compile("\\b[A-Z][0-9]{7}\\b"));
        DRIVERS_LICENSES_REGEX.put("COLORADO", Pattern.compile("\\b[0-9]{2}-[0-9]{3}[0-9]{4}\\b"));
        DRIVERS_LICENSES_REGEX.put("CONNECTICUT", Pattern.compile("\\b[0-9]{9}\\b"));
        DRIVERS_LICENSES_REGEX.put("DELAWARE", Pattern.compile("\\b[0-9]{7}\\b"));
        DRIVERS_LICENSES_REGEX.put("DISTRICT OF COLUMBIA", Pattern.compile("\\b[0-9]{7}\\b"));
        DRIVERS_LICENSES_REGEX.put("FLORIDA", Pattern.compile("\\b[A-Z][0-9]{3}-[0-9]{3}-[0-9]{2}-[0-9]{3}\\b"));
        DRIVERS_LICENSES_REGEX.put("GEORGIA", Pattern.compile("\\b[0-9]{9}\\b"));
        DRIVERS_LICENSES_REGEX.put("HAWAII", Pattern.compile("\\b[0-9]{8}\\b"));
        DRIVERS_LICENSES_REGEX.put("IDAHO", Pattern.compile("\\b[A-Z]{2}[0-9]{6}[A-Z]\\b"));
        DRIVERS_LICENSES_REGEX.put("ILLINOIS", Pattern.compile("\\b[A-Z][0-9]{11}\\b"));
        DRIVERS_LICENSES_REGEX.put("INDIANA", Pattern.compile("\\b[0-9]{4}-[0-9]{2}-[0-9]{4}\\b"));
        DRIVERS_LICENSES_REGEX.put("IOWA", Pattern.compile("\\b([0-9]{9})|[0-9]{3}[A-Z]{2}[0-9]{4}\\b"));
        DRIVERS_LICENSES_REGEX.put("KANSAS", Pattern.compile("\\b[Kk][0-9]{2}-[0-9]{2}-[0-9]{4}\\b"));
        DRIVERS_LICENSES_REGEX.put("KENTUCKY", Pattern.compile("\\b[A-Z][0-9]{2}-[0-9]{3}-[0-9]{3}\\b"));
        DRIVERS_LICENSES_REGEX.put("LOUISIANA", Pattern.compile("\\b[00][0-9]{7}\\b"));
        DRIVERS_LICENSES_REGEX.put("MAINE", Pattern.compile("\\b([0-9]{7})|[0-9]{7}[Xx]\\b"));
        DRIVERS_LICENSES_REGEX.put("MARYLAND", Pattern.compile("\\b[A-Z]-[0-9]{3}-[0-9]{3}-[0-9]{3}-[0-9]{3}\\b"));
        DRIVERS_LICENSES_REGEX.put("MASSACHUSETTS", Pattern.compile("\\b[Ss][1][0-9]{8}\\b"));
        DRIVERS_LICENSES_REGEX.put("MICHIGAN", Pattern.compile("\\b[A-Z][0-9]{12}\\b"));
        DRIVERS_LICENSES_REGEX.put("MINNESOTA", Pattern.compile("\\b[A-Z][0-9]{12}\\b"));
        DRIVERS_LICENSES_REGEX.put("MISSISSIPPI", Pattern.compile("\\b[0-9]{9}\\b"));
        DRIVERS_LICENSES_REGEX.put("MISSOURI", Pattern.compile("\\b([A-Z][0-9]{5,9})\\b"));
        DRIVERS_LICENSES_REGEX.put("MONTANA", Pattern.compile("\\b[0-9]{13}\\b"));
        DRIVERS_LICENSES_REGEX.put("NEBRASKA", Pattern.compile("\\b[A-Z][0-9]{3,8}\\b"));
        DRIVERS_LICENSES_REGEX.put("NEVADA", Pattern.compile("\\b[0-9]{10}\\b"));
        DRIVERS_LICENSES_REGEX.put("NEW HAMPSHIRE", Pattern.compile("\\b[0-9]{2}[A-Z]{3}[0-9]{5}\\b"));
        DRIVERS_LICENSES_REGEX.put("NEW JERSEY", Pattern.compile("\\b[A-Z][0-9]{4}-[0-9]{5}-[0-9]{5}\\b"));
        DRIVERS_LICENSES_REGEX.put("NEW MEXICO", Pattern.compile("\\b[0-9]{9}\\b"));
        DRIVERS_LICENSES_REGEX.put("NEW YORK", Pattern.compile("\\b([0-9]{9})|([0-9]{3}-0-9]{3}-0-9]{3})\\b"));
        DRIVERS_LICENSES_REGEX.put("NORTH CAROLINA", Pattern.compile("\\b[0-9]{8,12}\\b"));
        DRIVERS_LICENSES_REGEX.put("NORTH DAKOTA", Pattern.compile("\\b([A-Z]{3}[0-9]{6})|([A-Z]{3}-[0-9]{2}-[0-9]{4})\\b"));
        DRIVERS_LICENSES_REGEX.put("OHIO", Pattern.compile("\\b[A-Z]{2}[0-9}{6}]\\b"));
        DRIVERS_LICENSES_REGEX.put("OKLAHOMA", Pattern.compile("\\b([A-Z][0-9]{8})|([A-Z][0-9]{9})\\b"));
        DRIVERS_LICENSES_REGEX.put("OREGON", Pattern.compile("\\b[0-9]{7}\\b"));
        DRIVERS_LICENSES_REGEX.put("PENNSYLVANIA", Pattern.compile("\\b([0-9]{8})|([0-9]{2}[ ][0-9]{3}[ ][0-9]{3})\\b"));
        DRIVERS_LICENSES_REGEX.put("RHODE ISLAND", Pattern.compile("\\b[0-9]{7}\\b"));
        DRIVERS_LICENSES_REGEX.put("SOUTH CAROLINA", Pattern.compile("\\b[0-9]{9}\\b"));
        DRIVERS_LICENSES_REGEX.put("SOUTH DAKOTA", Pattern.compile("\\b[0-9]{8}\\b"));
        DRIVERS_LICENSES_REGEX.put("TENNESSEE", Pattern.compile("\\b[0-9]{8,9}\\b"));
        DRIVERS_LICENSES_REGEX.put("TEXAS", Pattern.compile("\\b[0-9]{8}\\b"));
        DRIVERS_LICENSES_REGEX.put("UTAH", Pattern.compile("\\b[0-9]{4,10}\\b"));
        DRIVERS_LICENSES_REGEX.put("VERMONT", Pattern.compile("\\b([0-9]{8})|([0-9]{7}[A-Z])\\b"));
        DRIVERS_LICENSES_REGEX.put("VIRGINIA", Pattern.compile("\\b[A-Z][0-9]{2}-[0-9]{2}-[0-9]{4}\\b"));
        DRIVERS_LICENSES_REGEX.put("WASHINGTON", Pattern.compile("\\b[A-Z0-9]{12}\\b"));
        DRIVERS_LICENSES_REGEX.put("WEST VIRGINIA", Pattern.compile("\\b([0-9]{7})|([A-Z][0-9]{6})\\b"));
        DRIVERS_LICENSES_REGEX.put("WISCONSIN", Pattern.compile("\\b[A-Z][0-9]{13}\\b"));
        DRIVERS_LICENSES_REGEX.put("WYOMING", Pattern.compile("\\b([0-9]{9})|([0-9]{6}-[0-9]{3})\\b"));

        this.contextualTerms = new HashSet<>(){{
            add("license");
            add("drivers");
        }};

        final List<FilterPattern> filterPatterns = new LinkedList<>();

        for(final String state : DRIVERS_LICENSES_REGEX.keySet()) {

            // TODO: How to include the state so it is part of the span?
            filterPatterns.add(new FilterPattern(DRIVERS_LICENSES_REGEX.get(state), 0.50));

        }

        this.analyzer = new Analyzer(contextualTerms, filterPatterns);

    }

    @Override
    public List<Span> filter(FilterProfile filterProfile, String context, String documentId, String input) throws Exception {

        final List<Span> spans = findSpans(filterProfile, analyzer, input, context, documentId);

        return Span.dropOverlappingSpans(spans);

    }

}
