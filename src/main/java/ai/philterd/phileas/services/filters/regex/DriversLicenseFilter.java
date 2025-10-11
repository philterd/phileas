/*
 *     Copyright 2025 Philterd, LLC @ https://www.philterd.ai
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ai.philterd.phileas.services.filters.regex;

import ai.philterd.phileas.model.enums.FilterType;
import ai.philterd.phileas.filters.FilterConfiguration;
import ai.philterd.phileas.filters.rules.regex.RegexFilter;
import ai.philterd.phileas.services.Analyzer;
import ai.philterd.phileas.model.objects.FilterPattern;
import ai.philterd.phileas.model.objects.FilterResult;
import ai.philterd.phileas.model.objects.Span;
import ai.philterd.phileas.policy.Policy;

import java.util.*;
import java.util.regex.Pattern;

public class DriversLicenseFilter extends RegexFilter {

    private static final HashMap<String, Pattern> driversLicensePatterns = new HashMap<>();

    public DriversLicenseFilter(FilterConfiguration filterConfiguration) {
        super(FilterType.DRIVERS_LICENSE_NUMBER, filterConfiguration);

        // https://ntsi.com/drivers-license-format/
        // https://www.mvrdecoder.com/content/drvlicformats.aspx

        driversLicensePatterns.put("ALABAMA", Pattern.compile("\\b[\\d]{8}\\b"));        // https://www.mvtrip.alabama.gov/News/ViewArticle/444
        driversLicensePatterns.put("ALASKA", Pattern.compile("\\b[\\d]{7}\\b"));
        driversLicensePatterns.put("ARIZONA", Pattern.compile("\\b([A-Z][\\d]{8})|([\\d]{9})\\b"));
        driversLicensePatterns.put("ARKANSAS", Pattern.compile("\\b[9][\\d]{7}\\b"));
        driversLicensePatterns.put("CALIFORNIA", Pattern.compile("\\b[A-Z][\\d]{7}\\b"));
        driversLicensePatterns.put("COLORADO", Pattern.compile("\\b[\\d]{2}-[\\d]{3}[\\d]{4}\\b"));
        driversLicensePatterns.put("CONNECTICUT", Pattern.compile("\\b[\\d]{9}\\b"));
        driversLicensePatterns.put("DELAWARE", Pattern.compile("\\b[\\d]{7}\\b"));
        driversLicensePatterns.put("DISTRICT OF COLUMBIA", Pattern.compile("\\b[\\d]{7}\\b"));
        driversLicensePatterns.put("FLORIDA", Pattern.compile("\\b[A-Z][\\d]{3}-[\\d]{3}-[\\d]{2}-[\\d]{3}\\b"));
        driversLicensePatterns.put("GEORGIA", Pattern.compile("\\b[\\d]{9}\\b"));
        driversLicensePatterns.put("HAWAII", Pattern.compile("\\b[\\d]{8}\\b"));
        driversLicensePatterns.put("IDAHO", Pattern.compile("\\b[A-Z]{2}[\\d]{6}[A-Z]\\b"));
        driversLicensePatterns.put("ILLINOIS", Pattern.compile("\\b[A-Z][\\d]{11}\\b"));
        driversLicensePatterns.put("INDIANA", Pattern.compile("\\b[\\d]{4}-[\\d]{2}-[\\d]{4}\\b"));
        driversLicensePatterns.put("IOWA", Pattern.compile("\\b([\\d]{9})|[\\d]{3}[A-Z]{2}[\\d]{4}\\b"));
        driversLicensePatterns.put("KANSAS", Pattern.compile("\\b[Kk][\\d]{2}-[\\d]{2}-[\\d]{4}\\b"));
        driversLicensePatterns.put("KENTUCKY", Pattern.compile("\\b[A-Z][\\d]{2}-[\\d]{3}-[\\d]{3}\\b"));
        driversLicensePatterns.put("LOUISIANA", Pattern.compile("\\b[00][\\d]{7}\\b"));
        driversLicensePatterns.put("MAINE", Pattern.compile("\\b([\\d]{7})|[\\d]{7}[Xx]\\b"));
        driversLicensePatterns.put("MARYLAND", Pattern.compile("\\b[A-Z]-[\\d]{3}-[\\d]{3}-[\\d]{3}-[\\d]{3}\\b"));
        driversLicensePatterns.put("MASSACHUSETTS", Pattern.compile("\\b[Ss][1][\\d]{8}\\b"));
        driversLicensePatterns.put("MICHIGAN", Pattern.compile("\\b[A-Z][\\d]{12}\\b"));
        driversLicensePatterns.put("MINNESOTA", Pattern.compile("\\b[A-Z][\\d]{12}\\b"));
        driversLicensePatterns.put("MISSISSIPPI", Pattern.compile("\\b[\\d]{9}\\b"));
        driversLicensePatterns.put("MISSOURI", Pattern.compile("\\b([A-Z][\\d]{5,9})\\b"));
        driversLicensePatterns.put("MONTANA", Pattern.compile("\\b[\\d]{13}\\b"));
        driversLicensePatterns.put("NEBRASKA", Pattern.compile("\\b[A-Z][\\d]{3,8}\\b"));
        driversLicensePatterns.put("NEVADA", Pattern.compile("\\b[\\d]{10}\\b"));
        driversLicensePatterns.put("NEW HAMPSHIRE", Pattern.compile("\\b[\\d]{2}[A-Z]{3}[\\d]{5}\\b"));
        driversLicensePatterns.put("NEW JERSEY", Pattern.compile("\\b[A-Z][\\d]{4}-[\\d]{5}-[\\d]{5}\\b"));
        driversLicensePatterns.put("NEW MEXICO", Pattern.compile("\\b[\\d]{9}\\b"));
        driversLicensePatterns.put("NEW YORK", Pattern.compile("\\b([\\d]{9})|([\\d]{3}-\\d]{3}-\\d]{3})\\b"));
        driversLicensePatterns.put("NORTH CAROLINA", Pattern.compile("\\b[\\d]{8,12}\\b"));
        driversLicensePatterns.put("NORTH DAKOTA", Pattern.compile("\\b([A-Z]{3}[\\d]{6})|([A-Z]{3}-[\\d]{2}-[\\d]{4})\\b"));
        driversLicensePatterns.put("OHIO", Pattern.compile("\\b[A-Z]{2}[\\d}{6}]\\b"));
        driversLicensePatterns.put("OKLAHOMA", Pattern.compile("\\b([A-Z][\\d]{8})|([A-Z][\\d]{9})\\b"));
        driversLicensePatterns.put("OREGON", Pattern.compile("\\b[\\d]{7}\\b"));
        driversLicensePatterns.put("PENNSYLVANIA", Pattern.compile("\\b([\\d]{8})|([\\d]{2}[ ][\\d]{3}[ ][\\d]{3})\\b"));
        driversLicensePatterns.put("RHODE ISLAND", Pattern.compile("\\b[\\d]{7}\\b"));
        driversLicensePatterns.put("SOUTH CAROLINA", Pattern.compile("\\b[\\d]{9}\\b"));
        driversLicensePatterns.put("SOUTH DAKOTA", Pattern.compile("\\b[\\d]{8}\\b"));
        driversLicensePatterns.put("TENNESSEE", Pattern.compile("\\b[\\d]{8,9}\\b"));
        driversLicensePatterns.put("TEXAS", Pattern.compile("\\b[\\d]{8}\\b"));
        driversLicensePatterns.put("UTAH", Pattern.compile("\\b[\\d]{4,10}\\b"));
        driversLicensePatterns.put("VERMONT", Pattern.compile("\\b([\\d]{8})|([\\d]{7}[A-Z])\\b"));
        driversLicensePatterns.put("VIRGINIA", Pattern.compile("\\b[A-Z][\\d]{2}-[\\d]{2}-[\\d]{4}\\b"));
        driversLicensePatterns.put("WASHINGTON", Pattern.compile("\\b[A-Z\\d]{12}\\b"));
        driversLicensePatterns.put("WEST VIRGINIA", Pattern.compile("\\b([\\d]{7})|([A-Z][\\d]{6})\\b"));
        driversLicensePatterns.put("WISCONSIN", Pattern.compile("\\b[A-Z][\\d]{13}\\b"));
        driversLicensePatterns.put("WYOMING", Pattern.compile("\\b([\\d]{9})|([\\d]{6}-[\\d]{3})\\b"));

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
    public FilterResult filter(Policy policy, String context, int piece, String input, Map<String, String> attributes) throws Exception {

        final List<Span> spans = findSpans(policy, analyzer, input, context, attributes);

        return new FilterResult(context, Span.dropOverlappingSpans(spans));

    }

}
