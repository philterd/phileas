/*
 *     Copyright 2026 Philterd, LLC @ https://www.philterd.ai
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
package ai.philterd.phileas.services.disambiguation;

import ai.philterd.phileas.model.filtering.FilterType;
import ai.philterd.phileas.model.filtering.Span;
import ai.philterd.phileas.services.disambiguation.vector.VectorService;

import java.util.List;

/**
 * A {@link SpanDisambiguationService} that does nothing: used when span disambiguation is disabled.
 * It lets callers invoke disambiguation unconditionally (the {@link SpanDisambiguationServiceFactory}
 * decides whether the real implementation or this one is used), so the pipeline no longer needs an
 * "is it enabled?" branch around the call.
 */
public class NoOpSpanDisambiguationService implements SpanDisambiguationService {

    @Override
    public void hashAndInsert(final VectorService vectorService, final String context, final Span span) {
        // Nothing to learn when disambiguation is disabled.
    }

    @Override
    public FilterType disambiguate(final VectorService vectorService, final String context, final List<FilterType> filterTypes, final Span ambiguousSpan) {
        // No information to disambiguate with; keep the first candidate.
        return filterTypes.get(0);
    }

    @Override
    public List<Span> disambiguate(final VectorService vectorService, final String context, final List<Span> spans) {
        // Pass the spans through untouched.
        return spans;
    }

}
