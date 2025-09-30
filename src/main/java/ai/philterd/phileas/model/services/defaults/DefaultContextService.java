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
package ai.philterd.phileas.model.services.defaults;

import ai.philterd.phileas.model.services.ContextService;

import java.util.HashMap;
import java.util.Map;

public class DefaultContextService implements ContextService {

    private final Map<String, String> context;

    public DefaultContextService() {
        this.context = new HashMap<>();
    }

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
    public void putReplacement(String token, String replacement) {
        context.put(token, replacement);
    }

}
