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
package ai.philterd.phileas.services;

import ai.philterd.phileas.policy.Policy;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Map;
import java.util.Set;

/**
 * Checks every file a policy names, wherever it names it, when the policy is loaded and before any filter is built:
 * {@code files} (custom dictionaries and the top-level {@code ignored}), a filter's {@code ignoredFiles}, and a
 * strategy's {@code mappingFiles}. The top-level {@code ignored[].files} is otherwise read only when a document is
 * filtered, so without this a policy naming a file it may not read would load and then fail.
 */
final class PolicyFileValidator {

    private static final Gson GSON = new Gson();
    private static final Set<String> FILE_FIELDS = Set.of("files", "ignoredFiles", "mappingFiles");

    private final PolicyFiles policyFiles;

    PolicyFileValidator(final PolicyFiles policyFiles) {
        this.policyFiles = policyFiles;
    }

    /**
     * @throws ai.philterd.phileas.policy.InvalidPolicyFileException if the policy names a file it may not read.
     */
    void validate(final Policy policy) {
        walk(GSON.toJsonTree(policy), "");
    }

    private void walk(final JsonElement element, final String location) {
        if (element instanceof JsonObject object) {
            for (final Map.Entry<String, JsonElement> entry : object.entrySet()) {
                final String field = location.isEmpty() ? entry.getKey() : location + "." + entry.getKey();
                if (FILE_FIELDS.contains(entry.getKey()) && entry.getValue().isJsonArray()) {
                    for (final JsonElement path : entry.getValue().getAsJsonArray()) {
                        if (path.isJsonPrimitive()) {
                            policyFiles.check(field, path.getAsString());
                        }
                    }
                } else {
                    walk(entry.getValue(), field);
                }
            }
        } else if (element != null && element.isJsonArray()) {
            int index = 0;
            for (final JsonElement item : element.getAsJsonArray()) {
                walk(item, location + "[" + index++ + "]");
            }
        }
    }

}
