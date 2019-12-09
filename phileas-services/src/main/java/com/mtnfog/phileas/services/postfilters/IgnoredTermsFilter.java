package com.mtnfog.phileas.services.postfilters;

import com.mtnfog.phileas.model.objects.PostFilterResult;
import com.mtnfog.phileas.model.objects.Span;
import com.mtnfog.phileas.model.profile.FilterProfile;
import com.mtnfog.phileas.model.profile.Ignored;
import com.mtnfog.phileas.model.services.PostFilter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Implementation of {@link PostFilter} that removes identified
 * tokens found in an ignore list.
 */
public class IgnoredTermsFilter extends PostFilter implements Serializable {

    private static final Logger LOGGER = LogManager.getLogger(IgnoredTermsFilter.class);

    private Set<String> ignoredTerms = new HashSet<>();
    private boolean caseSensitive;

    public IgnoredTermsFilter(FilterProfile filterProfile) {
        this(filterProfile, false);
    }

    public IgnoredTermsFilter(FilterProfile filterProfile, boolean caseSensitive) {

        this.caseSensitive = caseSensitive;

        // Build the full list of ignored terms.
        for(final Ignored ignored : filterProfile.getIgnored()) {

            if(caseSensitive) {

                ignoredTerms.addAll(ignored.getTerms());

            } else {

                // Lowercase everything before adding.
                ignoredTerms.addAll(ignored.getTerms().stream()
                        .map(String::toLowerCase)
                        .collect(Collectors.toList()));

            }

        }

    }

    @Override
    protected PostFilterResult process(String text, Span span) {

        String spanText = span.getText(text);

        if(!caseSensitive) {
            spanText = spanText.toLowerCase();
        }

        // Look in the ignore lists to see if this token should be ignored.
        // TODO: A bloom filter would provide better performance for long lists of ignored terms.
        boolean ignored = ignoredTerms.contains(spanText);

        // Return true if allowed; false if ignored.
        return new PostFilterResult(ignored);

    }

}
