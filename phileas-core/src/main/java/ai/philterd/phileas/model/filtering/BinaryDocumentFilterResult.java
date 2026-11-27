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

import com.google.gson.Gson;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.LinkedHashSet;
import java.util.List;

public final class BinaryDocumentFilterResult extends AbstractFilterResult {

    private transient final byte[] document;

    // Null unless the document is an email message.
    private final EmailReport email;

    // Null unless the call asked for text. Transient, as the document is, so the text does not reach a log.
    private transient final RedactedText text;

    // Null when there are none, so a result without warnings serializes as before.
    private final List<RedactionWarning> warnings;

    public BinaryDocumentFilterResult(byte[] document, String context, Explanation explanation, long tokens, List<IncrementalRedaction> incrementalRedactions) {
        this(document, context, explanation, tokens, incrementalRedactions, false);
    }

    public BinaryDocumentFilterResult(byte[] document, String context, Explanation explanation, long tokens,
                                      List<IncrementalRedaction> incrementalRedactions, boolean reviewSampled) {
        this(document, context, explanation, tokens, incrementalRedactions, reviewSampled, null);
    }

    public BinaryDocumentFilterResult(byte[] document, String context, Explanation explanation, long tokens,
                                      List<IncrementalRedaction> incrementalRedactions, boolean reviewSampled,
                                      LeftoverCheckResult leftoverCheck) {
        this(document, context, explanation, tokens, incrementalRedactions, reviewSampled, leftoverCheck, null);
    }

    public BinaryDocumentFilterResult(byte[] document, String context, Explanation explanation, long tokens,
                                      List<IncrementalRedaction> incrementalRedactions, boolean reviewSampled,
                                      LeftoverCheckResult leftoverCheck, EmailReport email) {
        this(document, null, context, explanation, tokens, incrementalRedactions, reviewSampled, leftoverCheck, email);
    }

    /**
     * A result holding the redacted document or the document's redacted text.
     * @param document The redacted document, or {@code null} when the call asked for text.
     * @param text The redacted text, or {@code null} when the call asked for the document.
     */
    public BinaryDocumentFilterResult(byte[] document, RedactedText text, String context, Explanation explanation,
                                      long tokens, List<IncrementalRedaction> incrementalRedactions,
                                      boolean reviewSampled, LeftoverCheckResult leftoverCheck, EmailReport email) {
        super(context, explanation, tokens, incrementalRedactions, reviewSampled, leftoverCheck);

        this.document = document;
        this.text = text;
        this.email = email;
        this.warnings = null;

    }

    @Override
    public int hashCode() {

        return new HashCodeBuilder(17, 37).
                append(document).
                append(context).
                append(explanation).
                append(tokens).
                append(reviewSampled).
                append(leftoverCheck).
                append(email).
                append(text).
                append(warnings).
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

    private BinaryDocumentFilterResult(final BinaryDocumentFilterResult result, final List<RedactionWarning> warnings) {
        super(result.context, result.explanation, result.tokens, result.incrementalRedactions, result.reviewSampled,
                result.leftoverCheck);
        this.document = result.document;
        this.text = result.text;
        this.email = result.email;
        this.warnings = warnings == null || warnings.isEmpty() ? null : List.copyOf(new LinkedHashSet<>(warnings));
    }

    /**
     * This result with warnings about the policy, such as column rules that matched no column. Repeated warnings
     * are reported once.
     * @param warnings The warnings.
     * @return A copy of this result with the warnings.
     */
    public BinaryDocumentFilterResult withWarnings(final List<RedactionWarning> warnings) {
        return new BinaryDocumentFilterResult(this, warnings);
    }

    /**
     * Warnings about the policy for this document, such as a column rule whose header matches no column. Each holds
     * only values from the policy. Serialized as {@code warnings}, which is left out when there are none.
     * @return The warnings, empty when there are none.
     */
    public List<RedactionWarning> getWarnings() {
        return warnings == null ? List.of() : warnings;
    }

    /**
     * The redacted document.
     * @return The document, or {@code null} when the call asked for text.
     */
    public byte[] getDocument() {
        return document;
    }

    /**
     * The document's redacted plain text, when the call asked for it with
     * {@code FilterOptions.withOutput(Output.TEXT)}.
     * @return The {@link RedactedText}, or {@code null} when the call asked for the document.
     */
    public RedactedText getText() {
        return text;
    }

    /**
     * For an email message, what was left uninspected or taken out beyond the policy's text redaction.
     * @return The {@link EmailReport}, or {@code null} when the document is not an email message.
     */
    public EmailReport getEmail() {
        return email;
    }

}
