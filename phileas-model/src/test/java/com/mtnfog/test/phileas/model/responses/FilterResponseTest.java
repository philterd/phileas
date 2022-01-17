package com.mtnfog.test.phileas.model.responses;

import com.mtnfog.phileas.model.enums.FilterType;
import com.mtnfog.phileas.model.objects.Explanation;
import com.mtnfog.phileas.model.objects.Span;
import com.mtnfog.phileas.model.responses.FilterResponse;
import com.mtnfog.phileas.model.services.SplitService;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FilterResponseTest {

    private static final Logger LOGGER = LogManager.getLogger(FilterResponseTest.class);

    @Test
    public void combine1() {

        final String input = "George Washington was president." + System.lineSeparator() + "Another president was Abraham Lincoln.";
        final String separator = System.lineSeparator();

        final Span span1 = Span.make(0, 17, FilterType.PERSON, "context", "docid", 1.0, "George Washington", "{{{REDACTED-person}}}", null, false, new String[]{});
        final List<Span> identifiedSpans1 = Arrays.asList(span1);
        final List<Span> appliedSpans1 = Arrays.asList(span1);
        final Explanation explanation1 = new Explanation(identifiedSpans1, appliedSpans1);
        final String filtered1 = "{{{REDACTED-person}}} was president.";
        final FilterResponse filterResponse1 = new FilterResponse(filtered1, "context", "docid", 0, explanation1);

        final Span span2 = Span.make(22, 37, FilterType.PERSON, "context", "docid", 1.0, "Abraham Lincoln", "{{{REDACTED-person}}}", null, false, new String[]{});
        final List<Span> identifiedSpans2 = Arrays.asList(span2);
        final List<Span> appliedSpans2 = Arrays.asList(span2);
        final Explanation explanation2 = new Explanation(identifiedSpans2, appliedSpans2);
        final String filtered2 = "Another president was {{{REDACTED-person}}}.";
        final FilterResponse filterResponse2 = new FilterResponse(filtered2, "context", "docid", 1, explanation2);

        final FilterResponse combined = FilterResponse.combine(Arrays.asList(filterResponse1, filterResponse2), "context", "docid", separator);
        LOGGER.info(combined.getFilteredText());

        assertEquals(2, combined.getExplanation().getIdentifiedSpans().size());
        assertEquals(2, combined.getExplanation().getAppliedSpans().size());

        showSpans(combined.getExplanation().getIdentifiedSpans());

        assertEquals(filtered1 + separator + filtered2, combined.getFilteredText());

    }

    @Test
    public void equalsContract() {
        EqualsVerifier.forClass(FilterResponse.class).verify();
    }

    private void showSpans(List<Span> spans) {

        for(Span span : spans) {
            LOGGER.info(span.toString());
        }

    }

}
