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
package ai.philterd.phileas.filters;

import ai.philterd.phileas.model.filtering.FilterType;
import ai.philterd.phileas.model.filtering.Filtered;
import ai.philterd.phileas.model.filtering.Replacement;
import ai.philterd.phileas.model.filtering.Span;
import ai.philterd.phileas.policy.Policy;
import ai.philterd.phileas.services.context.ContextService;
import ai.philterd.phileas.services.strategies.ColumnFilterStrategy;

import java.util.List;

/**
 * Replaces a whole table cell with a {@code tabular} column rule's strategies, without detection. Built by
 * {@link ai.philterd.phileas.services.FilterPolicyLoader#getColumnFilter} so its strategies are wired as
 * a detecting filter's are: encryption keys, anonymization, mappings, and generators.
 */
public class ColumnFilter extends Filter {

    public ColumnFilter(final FilterConfiguration filterConfiguration) {
        super(FilterType.OTHER, filterConfiguration);
    }

    /** The whole input as one span, replaced by the strategies. */
    @Override
    public Filtered filter(final ContextService contextService, final Policy policy, final String context,
                           final int piece, final String input) throws Exception {
        if (input == null || input.isEmpty()) {
            return new Filtered(context, piece, List.of());
        }
        final Replacement replacement = replace(contextService, policy, context, input);
        final Span span = Span.make(0, input.length(), FilterType.OTHER, context, 1.0, input,
                replacement.getReplacement(), replacement.getSalt(), false, true, new String[0], priority);
        span.setClassification(ColumnFilterStrategy.CLASSIFICATION);
        return new Filtered(context, piece, List.of(span));
    }

    /**
     * The replacement for a whole cell value.
     * @param contextService The context service, for a {@code CONTEXT} replacement scope.
     * @param policy The policy being applied.
     * @param context The redaction context.
     * @param value The cell value.
     * @return The replacement.
     * @throws Exception Thrown if the strategy cannot produce a replacement.
     */
    public Replacement replace(final ContextService contextService, final Policy policy, final String context,
                               final String value) throws Exception {
        return getReplacement(contextService, policy, context, value, new String[0], 1.0,
                ColumnFilterStrategy.CLASSIFICATION, null);
    }

}
