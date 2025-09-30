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
package ai.philterd.phileas.services.filters.postfilters;

import ai.philterd.phileas.model.objects.PostFilterResult;
import ai.philterd.phileas.model.objects.Span;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Iterator;
import java.util.List;

/**
 * Performs post-filtering.
 */
public abstract class PostFilter {

    protected static final Logger LOGGER = LogManager.getLogger(PostFilter.class);

    protected boolean skipped = false;

    /**
     * Performs post-filtering on a list of spans.
     * @param text The input text.
     * @param span The {@link Span} to process.
     * @return <code>true</code> if the span should not be filtered.
     * <code>false</code> if the span should be filtered.
     */
    protected abstract PostFilterResult process(String text, Span span);

    public boolean skipped() {
        return skipped;
    }

    /**
     * Filters a list of spans per the implementation of <code>process</code>.
     * @param text The input text.
     * @param spans A list of {@link Span spans}.
     * @return A filtered list of {@link Span spans}.
     */
    public List<Span> filter(final String text, final List<Span> spans) {

        final Iterator<Span> i = spans.iterator();

        while (i.hasNext()) {

            final Span span = i.next();
            final PostFilterResult postFilterResult = process(text, span);

            if(postFilterResult.isPostFiltered()) {
                i.remove();
            }

        }

        return spans;

    }

}
