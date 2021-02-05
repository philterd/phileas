package com.mtnfog.phileas.services.postfilters;

import com.mtnfog.phileas.model.objects.PostFilterResult;
import com.mtnfog.phileas.model.objects.Span;
import com.mtnfog.phileas.model.services.PostFilter;
import org.apache.commons.lang3.StringUtils;

/**
 * Implementation of {@link PostFilter} that modifies the span
 * if it ends with a new line.
 */
public class TrailingNewLinePostFilter extends PostFilter {

    public static TrailingNewLinePostFilter trailingNewLinePostFilter;

    public static TrailingNewLinePostFilter getInstance() {

        if(trailingNewLinePostFilter == null) {
            trailingNewLinePostFilter = new TrailingNewLinePostFilter();
        }

        return trailingNewLinePostFilter;

    }

    private TrailingNewLinePostFilter() {
        // This is a singleton class.
    }

    @Override
    protected PostFilterResult process(String text, Span span) {

        if(span.getText().endsWith(System.lineSeparator())) {

            // Modify the span to remove the period from the span.
            span.setText(StringUtils.substring(span.getText(), 0, span.getText().length() - 1));
            span.setCharacterEnd(span.getCharacterEnd() - 1);

        }

        while(span.getText().endsWith("\n")) {
            span = process(text, span).getSpan();
        }

        return new PostFilterResult(span, false);

    }

}
