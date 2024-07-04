/*
 *     Copyright 2024 Philterd, LLC @ https://www.philterd.ai
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
package ai.philterd.phileas.model.responses;

import com.google.gson.Gson;
import ai.philterd.phileas.model.objects.Explanation;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public final class BinaryDocumentFilterResponse {

    private transient final byte[] document;
	private final String context;
    private final String documentId;
    private final Explanation explanation;

    public BinaryDocumentFilterResponse(byte[] document, String context, String documentId, Explanation explanation) {

        this.document = document;
        this.context = context;
        this.documentId = documentId;
        this.explanation = explanation;

    }

    @Override
    public int hashCode() {

        return new HashCodeBuilder(17, 37).
                append(document).
                append(context).
                append(documentId).
                append(explanation).
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

    public String getDocumentId() {
        return documentId;
    }

    public String getContext() {
        return context;
    }

    public Explanation getExplanation() {
        return explanation;
    }

}
