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
package ai.philterd.phileas.services.postfilters;

import ai.philterd.phileas.model.objects.PostFilterResult;
import ai.philterd.phileas.model.objects.Span;
import ai.philterd.phileas.model.services.PostFilter;
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
    protected PostFilterResult process(final String text, Span span) {

        if(span.getText().endsWith(System.lineSeparator())) {

            // Modify the span to remove the new line from the span.
            span.setText(StringUtils.chomp(span.getText()));
            span.setCharacterEnd(span.getCharacterEnd() - System.lineSeparator().length());

        }

        while(span.getText().endsWith(System.lineSeparator())) {
            span = process(text, span).span();
        }

        return new PostFilterResult(span, false);

    }

}
