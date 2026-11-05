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

    public AbstractFilterResult(final String context, final Explanation explanation, final long tokens, final List<IncrementalRedaction> incrementalRedactions) {
        this.context = context;
        this.explanation = explanation;
        this.tokens = tokens;
        this.incrementalRedactions = incrementalRedactions;
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

}
