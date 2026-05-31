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
package ai.philterd.phileas.services.context;

import java.util.function.Supplier;

public interface ContextService {

    boolean containsToken(final String token);

    boolean containsReplacement(final String replacement);

    String getReplacement(final String token);

    void putReplacement(final String token, final String replacement, final String filterType);

    /**
     * Atomically returns the replacement for a token, generating and storing one via
     * <code>replacementSupplier</code> only if the token has not been seen before. Unlike a
     * separate {@link #containsToken(String)}/{@link #getReplacement(String)}/{@link
     * #putReplacement(String, String, String)} sequence, this is safe under concurrent access:
     * two threads requesting the same token in the same context are guaranteed to receive the
     * same replacement (the supplier is invoked at most once per token), preserving the
     * CONTEXT-scope guarantee that a given token is anonymized consistently.
     *
     * @param token The token to look up or generate a replacement for.
     * @param filterType The {@link ai.philterd.phileas.model.filtering.FilterType} of the token.
     * @param replacementSupplier Supplies a freshly generated replacement when the token is absent.
     * @return The existing or newly generated replacement. Never <code>null</code> unless the
     *         supplier returns <code>null</code>.
     */
    String computeReplacementIfAbsent(final String token, final String filterType, final Supplier<String> replacementSupplier);

}
