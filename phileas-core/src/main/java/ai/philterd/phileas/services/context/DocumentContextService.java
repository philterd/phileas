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
package ai.philterd.phileas.services.context;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * The context service for one filter call: the caller's {@link ContextService}, which {@code CONTEXT}-scoped
 * replacements use, and a map kept for the call only, which {@code DOCUMENT}-scoped replacements use. The map is
 * never written to the caller's service, so document replacements never reach a shared or stored context, and two
 * documents filtered in the same context get independent replacements.
 */
public final class DocumentContextService implements ContextService {

    private final ContextService delegate;
    private final Map<String, String> document = new ConcurrentHashMap<>();

    private DocumentContextService(final ContextService delegate) {
        this.delegate = delegate;
    }

    /**
     * The context service for a filter call. A service already made for the call is returned as it is, so the
     * parts of one document filtered by nested calls, such as an email's attachments, share one map.
     * @param contextService The caller's context service.
     * @return The context service for the call.
     */
    public static ContextService forDocument(final ContextService contextService) {
        return contextService instanceof DocumentContextService ? contextService : new DocumentContextService(contextService);
    }

    /**
     * The document map of a filter call's context service.
     * @param contextService A context service.
     * @return The {@link DocumentContextService}, or {@code null} when the context service was not made for a
     *         filter call, as when a filter is used on its own.
     */
    public static DocumentContextService of(final ContextService contextService) {
        return contextService instanceof DocumentContextService document ? document : null;
    }

    /**
     * Whether a context service has a caller's context behind it: {@code false} for {@code null}, and for a filter
     * call's context service made from {@code null}.
     * @param contextService A context service.
     * @return {@code true} if {@code CONTEXT}-scoped replacements can be kept in it.
     */
    public static boolean hasContext(final ContextService contextService) {
        return contextService instanceof DocumentContextService document ? document.delegate != null : contextService != null;
    }

    /**
     * The replacement for a token in this document, made by the supplier the first time the token is seen. A
     * supplier that returns {@code null} stores nothing.
     * @param token The detected value.
     * @param replacementSupplier Makes the replacement.
     * @return The replacement.
     */
    public String computeDocumentReplacementIfAbsent(final String token, final Supplier<String> replacementSupplier) {
        return document.computeIfAbsent(token, t -> replacementSupplier.get());
    }

    /**
     * Stores a token's replacement in this document.
     * @param token The detected value.
     * @param replacement The replacement.
     */
    public void putDocumentReplacement(final String token, final String replacement) {
        document.put(token, replacement);
    }

    @Override
    public boolean containsToken(final String token) {
        return delegate.containsToken(token);
    }

    @Override
    public boolean containsReplacement(final String replacement) {
        return delegate.containsReplacement(replacement);
    }

    @Override
    public String getReplacement(final String token) {
        return delegate.getReplacement(token);
    }

    @Override
    public void putReplacement(final String token, final String replacement, final String filterType) {
        delegate.putReplacement(token, replacement, filterType);
    }

    @Override
    public String computeReplacementIfAbsent(final String token, final String filterType,
                                             final Supplier<String> replacementSupplier) {
        return delegate.computeReplacementIfAbsent(token, filterType, replacementSupplier);
    }

    @Override
    public String computeValueIfAbsent(final String key, final String filterType, final Supplier<String> valueSupplier) {
        return delegate.computeValueIfAbsent(key, filterType, valueSupplier);
    }

}
