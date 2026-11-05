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
package ai.philterd.phileas.policy.filters;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Gson deserializer for a policy field that accepts either a single JSON string or an array of
 * strings, normalizing both shapes to a {@code List<String>}.
 */
public class StringOrArrayListDeserializer implements JsonDeserializer<List<String>> {

    @Override
    public List<String> deserialize(final JsonElement jsonElement, final Type type,
                                    final JsonDeserializationContext context) throws JsonParseException {

        final List<String> values = new ArrayList<>();

        if (jsonElement.isJsonArray()) {
            final JsonArray array = jsonElement.getAsJsonArray();
            for (final JsonElement element : array) {
                values.add(element.getAsString());
            }
        } else {
            values.add(jsonElement.getAsString());
        }

        return values;

    }

}
