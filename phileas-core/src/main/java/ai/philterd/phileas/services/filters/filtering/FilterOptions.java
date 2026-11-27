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
package ai.philterd.phileas.services.filters.filtering;

/**
 * What the caller asks of one filter call, beyond the policy. Immutable.
 */
public final class FilterOptions {

    /** What a binary document filter call returns. */
    public enum Output {

        /** The redacted document, in the format the call asks for. */
        DOCUMENT,

        /** The document's redacted plain text, in labeled segments, in place of the redacted document. */
        TEXT

    }

    private static final FilterOptions DEFAULTS = new FilterOptions(false, false, Output.DOCUMENT, false);

    private final boolean leftoverCheckRequested;
    private final boolean leftoverCheckMatchedText;
    private final Output output;
    private final boolean textMetadata;

    private FilterOptions(final boolean leftoverCheckRequested, final boolean leftoverCheckMatchedText,
                          final Output output, final boolean textMetadata) {
        this.leftoverCheckRequested = leftoverCheckRequested;
        this.leftoverCheckMatchedText = leftoverCheckMatchedText;
        this.output = output;
        this.textMetadata = textMetadata;
    }

    /**
     * Nothing beyond the policy.
     * @return The default {@link FilterOptions}.
     */
    public static FilterOptions defaults() {
        return DEFAULTS;
    }

    /**
     * Asks for the policy's leftover check on this call. It runs when the policy's
     * {@code config.review.leftoverCheck.mode} is {@code onRequest}; with {@code always} it runs regardless,
     * and with {@code off} it never runs.
     * @param requested Whether to ask for the check.
     * @return Options with the request set.
     */
    public FilterOptions withLeftoverCheck(final boolean requested) {
        return new FilterOptions(requested, leftoverCheckMatchedText, output, textMetadata);
    }

    /**
     * Includes the matched text in each leftover finding, for an application that shows the findings to the
     * user it redacted for. The text is held in memory only: it is never serialized with the result.
     * @param matchedText Whether to include the matched text.
     * @return Options with the setting.
     */
    public FilterOptions withLeftoverCheckMatchedText(final boolean matchedText) {
        return new FilterOptions(leftoverCheckRequested, matchedText, output, textMetadata);
    }

    /**
     * Chooses what a binary document filter call returns: the redacted document (the default) or the document's
     * redacted plain text, from the same detection. Every binary document service supports both. A call for text
     * ignores the output {@link ai.philterd.phileas.model.filtering.MimeType}.
     * @param output The {@link Output}.
     * @return Options with the output set.
     */
    public FilterOptions withOutput(final Output output) {
        if (output == null) {
            throw new IllegalArgumentException("The output must not be null.");
        }
        return new FilterOptions(leftoverCheckRequested, leftoverCheckMatchedText, output, textMetadata);
    }

    /**
     * Includes the document's metadata, redacted, as a segment of the text output. It is left out by default.
     * Has no effect when the output is the document.
     * @param include Whether to include the metadata.
     * @return Options with the setting.
     */
    public FilterOptions withTextMetadata(final boolean include) {
        return new FilterOptions(leftoverCheckRequested, leftoverCheckMatchedText, output, include);
    }

    public boolean isLeftoverCheckRequested() {
        return leftoverCheckRequested;
    }

    public boolean isLeftoverCheckMatchedText() {
        return leftoverCheckMatchedText;
    }

    public Output getOutput() {
        return output;
    }

    public boolean isTextOutput() {
        return output == Output.TEXT;
    }

    public boolean isTextMetadata() {
        return textMetadata;
    }

}
