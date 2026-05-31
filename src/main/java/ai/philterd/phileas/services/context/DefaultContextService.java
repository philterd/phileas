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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class DefaultContextService implements ContextService {

    // A single context service instance is shared across all filters and across documents
    // processed concurrently, so the backing map must be thread-safe. ConcurrentHashMap also
    // provides the atomic compute-if-absent used to keep CONTEXT-scope replacements consistent
    // under concurrent access.
    private final Map<String, String> context = new ConcurrentHashMap<>();

    @Override
    public boolean containsToken(String token) {
        return context.containsKey(token);
    }

    @Override
    public boolean containsReplacement(String replacement) {
        return context.containsValue(replacement);
    }

    @Override
    public String getReplacement(String token) {
        return context.get(token);
    }

    @Override
    public void putReplacement(String token, String replacement, final String filterType) {
        context.put(token, replacement);
    }

    @Override
    public String computeReplacementIfAbsent(final String token, final String filterType, final Supplier<String> replacementSupplier) {
        return context.computeIfAbsent(token, t -> replacementSupplier.get());
    }

}
