package ai.philterd.phileas.services.postfilters;

import ai.philterd.phileas.model.objects.PostFilterResult;
import ai.philterd.phileas.model.objects.Span;
import ai.philterd.phileas.model.profile.IgnoredPattern;
import ai.philterd.phileas.model.services.PostFilter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.LinkedList;
import java.util.List;

/**
 * Implementation of {@link PostFilter} that removes tokens matching a given pattern.
 */
public class IgnoredPatternsFilter extends PostFilter {

    private static final Logger LOGGER = LogManager.getLogger(IgnoredPatternsFilter.class);

    private List<IgnoredPattern> ignoredPatterns;

    public IgnoredPatternsFilter(final List<IgnoredPattern> ignoredPatterns) {

        this.ignoredPatterns = ignoredPatterns;

    }

    @Override
    protected PostFilterResult process(String text, Span span) {

        final String spanText = span.getText(text);

        for(final IgnoredPattern pattern : ignoredPatterns) {

            final boolean ignored = spanText.matches(pattern.getPattern());

            if(ignored) {
                return new PostFilterResult(span, true);
            }

        }

        return new PostFilterResult(span, false);

    }

}