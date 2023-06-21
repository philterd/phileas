/*
 *     Copyright 2023 Philterd, LLC @ https://www.philterd.ai
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
 * if it ends with a space.
 */
public class TrailingSpacePostFilter extends PostFilter {

    public static TrailingSpacePostFilter trailingSpacePostFilter;

    public static TrailingSpacePostFilter getInstance() {

        if(trailingSpacePostFilter == null) {
            trailingSpacePostFilter = new TrailingSpacePostFilter();
        }

        return trailingSpacePostFilter;

    }

    private TrailingSpacePostFilter() {
        // This is a singleton class.
    }

    @Override
    protected PostFilterResult process(String text, Span span) {

        if(span.getText().endsWith(" ")) {

            // Modify the span to remove the period from the span.
            span.setText(StringUtils.substring(span.getText(), 0, span.getText().length() - 1));
            span.setCharacterEnd(span.getCharacterEnd() - 1);

        }

        while(span.getText().endsWith(" ")) {
            span = process(text, span).getSpan();
        }

        return new PostFilterResult(span, false);

    }

}
