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
package ai.philterd.phileas.model.filtering;

import java.util.List;

public abstract class AbstractFilterResult {

    protected final String context;
    protected final Explanation explanation;
    protected final long tokens;
    protected final transient List<IncrementalRedaction> incrementalRedactions;
    protected final boolean reviewSampled;

    // Null when the policy's mode did not call for the check, so a result without it serializes as before.
    protected final LeftoverCheckResult leftoverCheck;

    public AbstractFilterResult(final String context, final Explanation explanation, final long tokens, final List<IncrementalRedaction> incrementalRedactions) {
        this(context, explanation, tokens, incrementalRedactions, false);
    }

    public AbstractFilterResult(final String context, final Explanation explanation, final long tokens,
                                final List<IncrementalRedaction> incrementalRedactions, final boolean reviewSampled) {
        this(context, explanation, tokens, incrementalRedactions, reviewSampled, null);
    }

    public AbstractFilterResult(final String context, final Explanation explanation, final long tokens,
                                final List<IncrementalRedaction> incrementalRedactions, final boolean reviewSampled,
                                final LeftoverCheckResult leftoverCheck) {
        this.context = context;
        this.explanation = explanation;
        this.tokens = tokens;
        this.incrementalRedactions = incrementalRedactions;
        this.reviewSampled = reviewSampled;
        this.leftoverCheck = leftoverCheck;
    }

    public String getContext() {
        return context;
    }

    public Explanation getExplanation() {
        return explanation;
    }

    public long getTokens() {
        return tokens;
    }

    public List<IncrementalRedaction> getIncrementalRedactions() {
        return incrementalRedactions;
    }

    /**
     * Whether the request was selected for human spot review by the policy's {@code config.review.samplingRate}.
     * Selection does not change the redacted output.
     * @return <code>true</code> if the request was selected for review.
     */
    public boolean isReviewSampled() {
        return reviewSampled;
    }

    /**
     * The outcome of the policy's {@code config.review.leftoverCheck}: whether it ran, and what it found.
     * The check does not change the redacted output.
     * @return The {@link LeftoverCheckResult}; when the check was not called for, one that was not performed.
     */
    public LeftoverCheckResult getLeftoverCheck() {
        return leftoverCheck == null ? LeftoverCheckResult.notPerformed(LeftoverCheckResult.Reason.NOT_REQUESTED) : leftoverCheck;
    }

}
