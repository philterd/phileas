package com.mtnfog.phileas.services.filters.regex;

import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.filter.rules.regex.RegexFilter;
import com.mtnfog.phileas.model.objects.Analyzer;
import com.mtnfog.phileas.model.objects.FilterPattern;
import com.mtnfog.phileas.model.objects.FilterResult;
import com.mtnfog.phileas.model.objects.Span;
import com.mtnfog.phileas.model.profile.Crypto;
import com.mtnfog.phileas.model.profile.FilterProfile;
import com.mtnfog.phileas.model.profile.filters.strategies.AbstractFilterStrategy;
import com.mtnfog.phileas.model.services.AlertService;
import com.mtnfog.phileas.model.services.AnonymizationService;

import java.util.*;
import java.util.regex.Pattern;

public class DriversLicenseFilter extends RegexFilter {

    private static final HashMap<String, Pattern> driversLicensePatterns = new HashMap<>();

    public DriversLicenseFilter(List<? extends AbstractFilterStrategy> strategies, AnonymizationService anonymizationService, AlertService alertService, Set<String> ignored, Crypto crypto, int windowSize) {
        super(FilterType.DRIVERS_LICENSE_NUMBER, strategies, anonymizationService, alertService, ignored, crypto, windowSize);

        // https://ntsi.com/drivers-license-format/
        // https://www.mvrdecoder.com/content/drvlicformats.aspx

        driversLicensePatterns.put("ALABAMA", Pattern.compile("\\b[0-9]{8}\\b"));        // https://www.mvtrip.alabama.gov/News/ViewArticle/444
        driversLicensePatterns.put("ALASKA", Pattern.compile("\\b[0-9]{7}\\b"));
        driversLicensePatterns.put("ARIZONA", Pattern.compile("\\b([A-Z][0-9]{8})|([0-9]{9})\\b"));
        driversLicensePatterns.put("ARKANSAS", Pattern.compile("\\b[9][0-9]{7}\\b"));
        driversLicensePatterns.put("CALIFORNIA", Pattern.compile("\\b[A-Z][0-9]{7}\\b"));
        driversLicensePatterns.put("COLORADO", Pattern.compile("\\b[0-9]{2}-[0-9]{3}[0-9]{4}\\b"));
        driversLicensePatterns.put("CONNECTICUT", Pattern.compile("\\b[0-9]{9}\\b"));
        driversLicensePatterns.put("DELAWARE", Pattern.compile("\\b[0-9]{7}\\b"));
        driversLicensePatterns.put("DISTRICT OF COLUMBIA", Pattern.compile("\\b[0-9]{7}\\b"));
        driversLicensePatterns.put("FLORIDA", Pattern.compile("\\b[A-Z][0-9]{3}-[0-9]{3}-[0-9]{2}-[0-9]{3}\\b"));
        driversLicensePatterns.put("GEORGIA", Pattern.compile("\\b[0-9]{9}\\b"));
        driversLicensePatterns.put("HAWAII", Pattern.compile("\\b[0-9]{8}\\b"));
        driversLicensePatterns.put("IDAHO", Pattern.compile("\\b[A-Z]{2}[0-9]{6}[A-Z]\\b"));
        driversLicensePatterns.put("ILLINOIS", Pattern.compile("\\b[A-Z][0-9]{11}\\b"));
        driversLicensePatterns.put("INDIANA", Pattern.compile("\\b[0-9]{4}-[0-9]{2}-[0-9]{4}\\b"));
        driversLicensePatterns.put("IOWA", Pattern.compile("\\b([0-9]{9})|[0-9]{3}[A-Z]{2}[0-9]{4}\\b"));
        driversLicensePatterns.put("KANSAS", Pattern.compile("\\b[Kk][0-9]{2}-[0-9]{2}-[0-9]{4}\\b"));
        driversLicensePatterns.put("KENTUCKY", Pattern.compile("\\b[A-Z][0-9]{2}-[0-9]{3}-[0-9]{3}\\b"));
        driversLicensePatterns.put("LOUISIANA", Pattern.compile("\\b[00][0-9]{7}\\b"));
        driversLicensePatterns.put("MAINE", Pattern.compile("\\b([0-9]{7})|[0-9]{7}[Xx]\\b"));
        driversLicensePatterns.put("MARYLAND", Pattern.compile("\\b[A-Z]-[0-9]{3}-[0-9]{3}-[0-9]{3}-[0-9]{3}\\b"));
        driversLicensePatterns.put("MASSACHUSETTS", Pattern.compile("\\b[Ss][1][0-9]{8}\\b"));
        driversLicensePatterns.put("MICHIGAN", Pattern.compile("\\b[A-Z][0-9]{12}\\b"));
        driversLicensePatterns.put("MINNESOTA", Pattern.compile("\\b[A-Z][0-9]{12}\\b"));
        driversLicensePatterns.put("MISSISSIPPI", Pattern.compile("\\b[0-9]{9}\\b"));
        driversLicensePatterns.put("MISSOURI", Pattern.compile("\\b([A-Z][0-9]{5,9})\\b"));
        driversLicensePatterns.put("MONTANA", Pattern.compile("\\b[0-9]{13}\\b"));
        driversLicensePatterns.put("NEBRASKA", Pattern.compile("\\b[A-Z][0-9]{3,8}\\b"));
        driversLicensePatterns.put("NEVADA", Pattern.compile("\\b[0-9]{10}\\b"));
        driversLicensePatterns.put("NEW HAMPSHIRE", Pattern.compile("\\b[0-9]{2}[A-Z]{3}[0-9]{5}\\b"));
        driversLicensePatterns.put("NEW JERSEY", Pattern.compile("\\b[A-Z][0-9]{4}-[0-9]{5}-[0-9]{5}\\b"));
        driversLicensePatterns.put("NEW MEXICO", Pattern.compile("\\b[0-9]{9}\\b"));
        driversLicensePatterns.put("NEW YORK", Pattern.compile("\\b([0-9]{9})|([0-9]{3}-0-9]{3}-0-9]{3})\\b"));
        driversLicensePatterns.put("NORTH CAROLINA", Pattern.compile("\\b[0-9]{8,12}\\b"));
        driversLicensePatterns.put("NORTH DAKOTA", Pattern.compile("\\b([A-Z]{3}[0-9]{6})|([A-Z]{3}-[0-9]{2}-[0-9]{4})\\b"));
        driversLicensePatterns.put("OHIO", Pattern.compile("\\b[A-Z]{2}[0-9}{6}]\\b"));
        driversLicensePatterns.put("OKLAHOMA", Pattern.compile("\\b([A-Z][0-9]{8})|([A-Z][0-9]{9})\\b"));
        driversLicensePatterns.put("OREGON", Pattern.compile("\\b[0-9]{7}\\b"));
        driversLicensePatterns.put("PENNSYLVANIA", Pattern.compile("\\b([0-9]{8})|([0-9]{2}[ ][0-9]{3}[ ][0-9]{3})\\b"));
        driversLicensePatterns.put("RHODE ISLAND", Pattern.compile("\\b[0-9]{7}\\b"));
        driversLicensePatterns.put("SOUTH CAROLINA", Pattern.compile("\\b[0-9]{9}\\b"));
        driversLicensePatterns.put("SOUTH DAKOTA", Pattern.compile("\\b[0-9]{8}\\b"));
        driversLicensePatterns.put("TENNESSEE", Pattern.compile("\\b[0-9]{8,9}\\b"));
        driversLicensePatterns.put("TEXAS", Pattern.compile("\\b[0-9]{8}\\b"));
        driversLicensePatterns.put("UTAH", Pattern.compile("\\b[0-9]{4,10}\\b"));
        driversLicensePatterns.put("VERMONT", Pattern.compile("\\b([0-9]{8})|([0-9]{7}[A-Z])\\b"));
        driversLicensePatterns.put("VIRGINIA", Pattern.compile("\\b[A-Z][0-9]{2}-[0-9]{2}-[0-9]{4}\\b"));
        driversLicensePatterns.put("WASHINGTON", Pattern.compile("\\b[A-Z0-9]{12}\\b"));
        driversLicensePatterns.put("WEST VIRGINIA", Pattern.compile("\\b([0-9]{7})|([A-Z][0-9]{6})\\b"));
        driversLicensePatterns.put("WISCONSIN", Pattern.compile("\\b[A-Z][0-9]{13}\\b"));
        driversLicensePatterns.put("WYOMING", Pattern.compile("\\b([0-9]{9})|([0-9]{6}-[0-9]{3})\\b"));

        this.contextualTerms = new HashSet<>();
        this.contextualTerms.add("license");
        this.contextualTerms.add("drivers");

        final List<FilterPattern> filterPatterns = new LinkedList<>();

        // Create a filter pattern for each state's regex.
        for(final String state : driversLicensePatterns.keySet()) {
            filterPatterns.add(new FilterPattern.FilterPatternBuilder(driversLicensePatterns.get(state), 0.50).withClassification(state).build());
        }

        this.analyzer = new Analyzer(contextualTerms, filterPatterns);

    }

    @Override
    public FilterResult filter(FilterProfile filterProfile, String context, String documentId, int piece, String input) throws Exception {

        final List<Span> spans = findSpans(filterProfile, analyzer, input, context, documentId);

        return new FilterResult(context, documentId, Span.dropOverlappingSpans(spans));

    }

}
