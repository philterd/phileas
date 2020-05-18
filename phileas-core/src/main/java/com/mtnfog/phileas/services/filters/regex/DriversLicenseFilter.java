package com.mtnfog.phileas.services.filters.regex;

import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.filter.rules.regex.RegexFilter;
import com.mtnfog.phileas.model.objects.Span;
import com.mtnfog.phileas.model.profile.Crypto;
import com.mtnfog.phileas.model.profile.FilterProfile;
import com.mtnfog.phileas.model.profile.filters.strategies.AbstractFilterStrategy;
import com.mtnfog.phileas.model.services.AnonymizationService;
import org.redisson.misc.Hash;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public class DriversLicenseFilter extends RegexFilter implements Serializable {

    private static final HashMap<String, Pattern> DRIVERS_LICENSE = new HashMap<>();

    private static final Pattern BITCOIN_ADDRESS_REGEX = Pattern.compile("\\b\\b[13][a-km-zA-HJ-NP-Z1-9]{25,34}\\b", Pattern.CASE_INSENSITIVE);

    public DriversLicenseFilter(List<? extends AbstractFilterStrategy> strategies, AnonymizationService anonymizationService, Set<String> ignored, Crypto crypto, int windowSize) {
        super(FilterType.BITCOIN_ADDRESS, strategies, anonymizationService, ignored, crypto, windowSize);

        // https://ntsi.com/drivers-license-format/
        // https://www.mvrdecoder.com/content/drvlicformats.aspx

        DRIVERS_LICENSE.put("ALABAMA", Pattern.compile("\\b[0-9]{8}\\b"));        // https://www.mvtrip.alabama.gov/News/ViewArticle/444
        DRIVERS_LICENSE.put("ALASKA", Pattern.compile("\\b[0-9]{7}\\b"));
        DRIVERS_LICENSE.put("ARIZONA", Pattern.compile("\\b([A-Z][0-9]{8})|([0-9]{9})\\b"));
        DRIVERS_LICENSE.put("ARKANSAS", Pattern.compile("\\b[9][0-9]{7}\\b"));
        DRIVERS_LICENSE.put("CALIFORNIA", Pattern.compile("\\b[A-Z][0-9]{7}\\b"));
        DRIVERS_LICENSE.put("COLORADO", Pattern.compile("\\b[0-9]{2}-[0-9]{3}[0-9]{4}\\b"));
        DRIVERS_LICENSE.put("CONNECTICUT", Pattern.compile("\\b[0-9]{9}\\b"));
        DRIVERS_LICENSE.put("DELAWARE", Pattern.compile("\\b[0-9]{7}\\b"));
        DRIVERS_LICENSE.put("DISTRICT OF COLUMBIA", Pattern.compile("\\b[0-9]{7}\\b"));
        DRIVERS_LICENSE.put("FLORIDA", Pattern.compile("\\b[A-Z][0-9]{3}-[0-9]{3}-[0-9]{2}-[0-9]{3}\\b"));
        DRIVERS_LICENSE.put("GEORGIA", Pattern.compile("\\b[0-9]{9}\\b"));
        DRIVERS_LICENSE.put("HAWAII", Pattern.compile("\\b[0-9]{8}\\b"));
        DRIVERS_LICENSE.put("IDAHO", Pattern.compile("\\b[A-Z]{2}[0-9]{6}[A-Z]\\b"));
        DRIVERS_LICENSE.put("ILLINOIS", Pattern.compile("\\b[A-Z][0-9]{11}\\b"));
        DRIVERS_LICENSE.put("INDIANA", Pattern.compile("\\b[0-9]{4}-[0-9]{2}-[0-9]{4}\\b"));
        DRIVERS_LICENSE.put("IOWA", Pattern.compile("\\b([0-9]{9})|[0-9]{3}[A-Z]{2}[0-9]{4}\\b"));
        DRIVERS_LICENSE.put("KANSAS", Pattern.compile("\\b[Kk][0-9]{2}-[0-9]{2}-[0-9]{4}\\b"));
        DRIVERS_LICENSE.put("KENTUCKY", Pattern.compile("\\b[A-Z][0-9]{2}-[0-9]{3}-[0-9]{3}\\b"));
        DRIVERS_LICENSE.put("LOUISIANA", Pattern.compile("\\b[00][0-9]{7}\\b"));
        DRIVERS_LICENSE.put("MAINE", Pattern.compile("\\b([0-9]{7})|[0-9]{7}[Xx]\\b"));
        DRIVERS_LICENSE.put("MARYLAND", Pattern.compile("\\b[A-Z]-[0-9]{3}-[0-9]{3}-[0-9]{3}-[0-9]{3}\\b"));
        DRIVERS_LICENSE.put("MASSACHUSETTS", Pattern.compile("\\b[Ss][1][0-9]{8}\\b"));
        DRIVERS_LICENSE.put("MICHIGAN", Pattern.compile("\\b[A-Z][0-9]{12}\\b"));
        DRIVERS_LICENSE.put("MINNESOTA", Pattern.compile("\\b[A-Z][0-9]{12}\\b"));
        DRIVERS_LICENSE.put("MISSISSIPPI", Pattern.compile("\\b[0-9]{9}\\b"));
        DRIVERS_LICENSE.put("MISSOURI", Pattern.compile("\\b([A-Z][0-9]{5,9})\\b"));
        DRIVERS_LICENSE.put("MONTANA", Pattern.compile("\\b[0-9]{13}\\b"));
        DRIVERS_LICENSE.put("NEBRASKA", Pattern.compile("\\b[A-Z][0-9]{3,8}\\b"));
        DRIVERS_LICENSE.put("NEVADA", Pattern.compile("\\b[0-9]{10}\\b"));
        DRIVERS_LICENSE.put("NEW HAMPSHIRE", Pattern.compile("\\b[0-9]{2}[A-Z]{3}[0-9]{5}\\b"));
        DRIVERS_LICENSE.put("NEW JERSEY", Pattern.compile("\\b[A-Z][0-9]{4}-[0-9]{5}-[0-9]{5}\\b"));
        DRIVERS_LICENSE.put("NEW MEXICO", Pattern.compile("\\b[0-9]{9}\\b"));
        DRIVERS_LICENSE.put("NEW YORK", Pattern.compile("\\b([0-9]{9})|([0-9]{3}-0-9]{3}-0-9]{3})\\b"));
        DRIVERS_LICENSE.put("NORTH CAROLINA", Pattern.compile("\\b[0-9]{8,12}\\b"));
        DRIVERS_LICENSE.put("NORTH DAKOTA", Pattern.compile("\\b([A-Z]{3}[0-9]{6})|([A-Z]{3}-[0-9]{2}-[0-9]{4})\\b"));
        DRIVERS_LICENSE.put("OHIO", Pattern.compile("\\b[A-Z]{2}[0-9}{6}]\\b"));
        DRIVERS_LICENSE.put("OKLAHOMA", Pattern.compile("\\b([A-Z][0-9]{8})|([A-Z][0-9]{9})\\b"));
        DRIVERS_LICENSE.put("OREGON", Pattern.compile("\\b[0-9]{7}\\b"));
        DRIVERS_LICENSE.put("PENNSYLVANIA", Pattern.compile("\\b([0-9]{8})|([0-9]{2}[ ][0-9]{3}[ ][0-9]{3})\\b"));
        DRIVERS_LICENSE.put("RHODE ISLAND", Pattern.compile("\\b[0-9]{7}\\b"));
        DRIVERS_LICENSE.put("SOUTH CAROLINA", Pattern.compile("\\b[0-9]{9}\\b"));
        DRIVERS_LICENSE.put("SOUTH DAKOTA", Pattern.compile("\\b[0-9]{8}\\b"));
        DRIVERS_LICENSE.put("TENNESSEE", Pattern.compile("\\b[0-9]{8,9}\\b"));
        DRIVERS_LICENSE.put("TEXAS", Pattern.compile("\\b[0-9]{8}\\b"));
        DRIVERS_LICENSE.put("UTAH", Pattern.compile("\\b[0-9]{4,10}\\b"));
        DRIVERS_LICENSE.put("VERMONT", Pattern.compile("\\b([0-9]{8})|([0-9]{7}[A-Z])\\b"));
        DRIVERS_LICENSE.put("VIRGINIA", Pattern.compile("\\b[A-Z][0-9]{2}-[0-9]{2}-[0-9]{4}\\b"));
        DRIVERS_LICENSE.put("WASHINGTON", Pattern.compile("\\b[A-Z0-9]{12}\\b"));
        DRIVERS_LICENSE.put("WEST VIRGINIA", Pattern.compile("\\b([0-9]{7})|([A-Z][0-9]{6})\\b"));
        DRIVERS_LICENSE.put("WISCONSIN", Pattern.compile("\\b[A-Z][0-9]{13}\\b"));
        DRIVERS_LICENSE.put("WYOMING", Pattern.compile("\\b([0-9]{9})|([0-9]{6}-[0-9]{3})\\b"));


    }

    @Override
    public List<Span> filter(FilterProfile filterProfile, String context, String documentId, String input) throws Exception {

        return findSpans(filterProfile, BITCOIN_ADDRESS_REGEX, input, context, documentId);

    }

}
