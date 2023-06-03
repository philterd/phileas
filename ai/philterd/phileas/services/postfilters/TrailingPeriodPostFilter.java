package ai.philterd.phileas.services.postfilters;

import ai.philterd.phileas.model.enums.FilterType;
import ai.philterd.phileas.model.objects.PostFilterResult;
import ai.philterd.phileas.model.objects.Span;
import ai.philterd.phileas.model.services.PostFilter;
import org.apache.commons.lang3.StringUtils;

/**
 * Implementation of {@link PostFilter} that modifies the span
 * if it ends with a period.
 */
public class TrailingPeriodPostFilter extends PostFilter {

    public static TrailingPeriodPostFilter trailingPeriodPostFilter;

    public static TrailingPeriodPostFilter getInstance() {

        if(trailingPeriodPostFilter == null) {
            trailingPeriodPostFilter = new TrailingPeriodPostFilter();
        }

        return trailingPeriodPostFilter;

    }

    private TrailingPeriodPostFilter() {
        // This is a singleton class.
    }

    @Override
    protected PostFilterResult process(String text, Span span) {

        // A street filter can end with a period.
        if(span.getFilterType() != FilterType.STREET_ADDRESS) {

            if (span.getText().endsWith(".")) {

                // Modify the span to remove the period from the span.
                span.setText(StringUtils.substring(span.getText(), 0, span.getText().length() - 1));
                span.setCharacterEnd(span.getCharacterEnd() - 1);

            }

            while (span.getText().endsWith(".")) {
                span = process(text, span).getSpan();
            }

        }

        return new PostFilterResult(span, false);

    }

}
