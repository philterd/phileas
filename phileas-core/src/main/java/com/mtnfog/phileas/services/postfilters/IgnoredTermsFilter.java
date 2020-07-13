package com.mtnfog.phileas.services.postfilters;

import com.mtnfog.phileas.model.objects.PostFilterResult;
import com.mtnfog.phileas.model.objects.Span;
import com.mtnfog.phileas.model.profile.Ignored;
import com.mtnfog.phileas.model.services.PostFilter;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Implementation of {@link PostFilter} that removes identified
 * tokens found in an ignore list.
 */
public class IgnoredTermsFilter extends PostFilter {

    private static final Logger LOGGER = LogManager.getLogger(IgnoredTermsFilter.class);

    private Set<String> ignoredTerms = new HashSet<>();
    private Ignored ignored;

    public IgnoredTermsFilter(final Ignored ignored) throws IOException {

        this.ignored = ignored;

        // Read the ignored terms from the files.
        final Set<String> ignoredTermsFromFiles = new HashSet<>();
        for(final String file : ignored.getFiles()) {
            ignoredTermsFromFiles.addAll(FileUtils.readLines(new File(file), Charset.defaultCharset()));
        }

        if(ignored.isCaseSensitive()) {

            ignoredTerms.addAll(ignored.getTerms());
            ignoredTerms.addAll(ignoredTermsFromFiles);

        } else {

            LOGGER.debug("Ignore terms is not case sensitive.");

            // Not case-sensitive. Lowercase everything before adding.
            ignoredTerms.addAll(ignored.getTerms().stream()
                    .map(String::toLowerCase)
                    .collect(Collectors.toList()));

            ignoredTerms.addAll(ignoredTermsFromFiles.stream()
                    .map(String::toLowerCase)
                    .collect(Collectors.toList()));

        }

        LOGGER.info("Added {} terms to ignore.", ignoredTerms.size());

    }

    @Override
    protected PostFilterResult process(String text, Span span) {

        String spanText = span.getText(text);

        if(!ignored.isCaseSensitive()) {
            spanText = spanText.toLowerCase();
        }

        // Look in the ignore lists to see if this token should be ignored.
        // TODO: A bloom filter would provide better performance for long lists of ignored terms.
        final boolean ignored = ignoredTerms.contains(spanText);

        // Return false if allowed; true if ignored.
        return new PostFilterResult(span, ignored);

    }

}
