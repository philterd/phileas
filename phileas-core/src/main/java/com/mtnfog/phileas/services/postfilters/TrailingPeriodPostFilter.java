package com.mtnfog.phileas.services.postfilters;

import com.mtnfog.phileas.model.objects.PostFilterResult;
import com.mtnfog.phileas.model.objects.Span;
import com.mtnfog.phileas.model.services.PostFilter;
import org.apache.commons.lang3.StringUtils;

/**
 * Implementation of {@link PostFilter} that modifies the span
 * if it ends with a period.
 */
public class TrailingPeriodPostFilter extends PostFilter {

    @Override
    protected PostFilterResult process(String text, Span span) {

        if(span.getText().endsWith(".")) {

            // Modify the span to remove the period from the span.
            span.setText(StringUtils.substring(span.getText(), 0, span.getText().length() - 1));
            span.setCharacterEnd(span.getCharacterEnd() - 1);

        }

        return new PostFilterResult(span, false);

    }

}
