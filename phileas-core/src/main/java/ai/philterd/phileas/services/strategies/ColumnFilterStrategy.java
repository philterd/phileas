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
package ai.philterd.phileas.services.strategies;

import ai.philterd.phileas.model.filtering.FilterPattern;
import ai.philterd.phileas.model.filtering.FilterType;
import ai.philterd.phileas.model.filtering.Replacement;
import ai.philterd.phileas.policy.Crypto;
import ai.philterd.phileas.policy.FPE;
import ai.philterd.phileas.policy.Policy;
import ai.philterd.phileas.services.anonymization.AnonymizationService;
import ai.philterd.phileas.services.context.ContextService;
import org.apache.commons.lang3.StringUtils;

/**
 * A strategy that replaces a whole table cell, from a {@code tabular} column rule's {@code strategies} or a
 * sheet's {@code unlistedStrategies}. It takes no condition, since a whole-column rule replaces every cell, and
 * {@code %t} in its {@code redactionFormat} is {@code full-column}.
 */
public class ColumnFilterStrategy extends StandardFilterStrategy {

    /** The classification of a whole-column replacement, and what {@code %t} expands to. */
    public static final String CLASSIFICATION = "full-column";

    /** A strategy with the defaults: {@code REDACT}. */
    public static ColumnFilterStrategy redact() {
        return new ColumnFilterStrategy();
    }

    @Override
    public FilterType getFilterType() {
        return FilterType.OTHER;
    }

    @Override
    public boolean evaluateCondition(final Policy policy, final String context, final String token,
                                     final String[] window, final String condition, final double confidence) {
        // A column strategy has no condition; one is refused when the policy loads.
        return true;
    }

    @Override
    public Replacement getReplacement(final ContextService contextService, final String classification,
                                      final String context, final String token, final String[] window,
                                      final Crypto crypto, final FPE fpe,
                                      final AnonymizationService anonymizationService,
                                      final FilterPattern filterPattern) throws Exception {
        return getStandardReplacement(contextService, classification, token, crypto, fpe, anonymizationService,
                FilterType.OTHER);
    }

    @Override
    protected String getRedactedToken(final String token, final String label, final FilterType filterType) {
        return getValueOrDefault(redactionFormat, DEFAULT_REDACTION)
                .replace("%t", CLASSIFICATION)
                .replace("%l", StringUtils.defaultString(label))
                .replace("%v", token);
    }

}
