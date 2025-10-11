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
package ai.philterd.phileas.model.objects;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

/**
 * Response to a filter operation.
 */
public class ApplyResponse {

    private static final Logger LOGGER = LogManager.getLogger(ApplyResponse.class);

    private final String filteredText;
    private final long tokens;
    private final List<IncrementalRedaction> incrementalRedactions;

    public ApplyResponse(final String filteredText, final List<IncrementalRedaction> incrementalRedactions, final long tokens) {

        this.filteredText = filteredText;
        this.incrementalRedactions = incrementalRedactions;
        this.tokens = tokens;

    }

    @Override
    public String toString() {
        return filteredText;
    }

    @Override
    public final boolean equals(Object o) {
        return EqualsBuilder.reflectionEquals(this, o);
    }

    @Override
    public final int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    public String getFilteredText() {
        return filteredText;
    }

    public List<IncrementalRedaction> getIncrementalRedactions() {
        return incrementalRedactions;
    }

    public long getTokens() {
        return tokens;
    }

}
