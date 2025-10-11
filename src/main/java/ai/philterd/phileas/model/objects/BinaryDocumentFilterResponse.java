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

import com.google.gson.Gson;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.List;

public final class BinaryDocumentFilterResponse {

    private transient final byte[] document;
	private final String context;
    private final Explanation explanation;
    private final long tokens;
    private final transient List<IncrementalRedaction> incrementalRedactions;

    public BinaryDocumentFilterResponse(byte[] document, String context, Explanation explanation, long tokens, List<IncrementalRedaction> incrementalRedactions) {

        this.document = document;
        this.context = context;
        this.explanation = explanation;
        this.tokens = tokens;
        this.incrementalRedactions = incrementalRedactions;

    }

    @Override
    public int hashCode() {

        return new HashCodeBuilder(17, 37).
                append(document).
                append(context).
                append(explanation).
                append(tokens).
                toHashCode();

    }

    @Override
    public String toString() {

        final Gson gson = new Gson();
        return gson.toJson(this);

    }

    @Override
    public boolean equals(Object o) {

        return EqualsBuilder.reflectionEquals(this, o);

    }

    public byte[] getDocument() {
        return document;
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
