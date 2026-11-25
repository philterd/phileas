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

    private static final FilterOptions DEFAULTS = new FilterOptions(false, false);

    private final boolean leftoverCheckRequested;
    private final boolean leftoverCheckMatchedText;

    private FilterOptions(final boolean leftoverCheckRequested, final boolean leftoverCheckMatchedText) {
        this.leftoverCheckRequested = leftoverCheckRequested;
        this.leftoverCheckMatchedText = leftoverCheckMatchedText;
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
        return new FilterOptions(requested, leftoverCheckMatchedText);
    }

    /**
     * Includes the matched text in each leftover finding, for an application that shows the findings to the
     * user it redacted for. The text is held in memory only: it is never serialized with the result.
     * @param matchedText Whether to include the matched text.
     * @return Options with the setting.
     */
    public FilterOptions withLeftoverCheckMatchedText(final boolean matchedText) {
        return new FilterOptions(leftoverCheckRequested, matchedText);
    }

    public boolean isLeftoverCheckRequested() {
        return leftoverCheckRequested;
    }

    public boolean isLeftoverCheckMatchedText() {
        return leftoverCheckMatchedText;
    }

}
