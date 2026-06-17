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

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Map;

/**
 * Optional post-match validation for the {@code identifier} filter. A regex match is kept
 * only if the named validator passes, so a generic identifier can reject format-valid but
 * checksum-invalid values (for example a SIN that fails the Luhn check).
 *
 * <p>The redaction policy schema allows this to be written either as a bare string (the
 * validator name) or as an object with a {@code name} and validator-specific {@code params}.
 * Both forms deserialize to this class via {@link ValidatorAdapter}. No executable code is
 * carried in the policy; the name selects a built-in validator.</p>
 */
public class Validator {

    private final String name;
    private final Map<String, Object> params;

    public Validator(final String name) {
        this(name, null);
    }

    public Validator(final String name, final Map<String, Object> params) {
        this.name = name;
        this.params = params;
    }

    public String getName() {
        return name;
    }

    public Map<String, Object> getParams() {
        return params;
    }

    /**
     * Deserializes the schema's {@code oneOf} (string or {name, params} object) into a
     * {@link Validator}, and serializes back to the compact string form when there are no
     * params.
     */
    public static class ValidatorAdapter implements JsonDeserializer<Validator>, JsonSerializer<Validator> {

        @Override
        public Validator deserialize(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context) throws JsonParseException {

            if (json.isJsonPrimitive()) {
                // Compact form: "validator": "luhn"
                return new Validator(json.getAsString());
            }

            if (json.isJsonObject()) {
                // Object form: "validator": { "name": "luhn", "params": { ... } }
                final JsonObject object = json.getAsJsonObject();

                if (!object.has("name")) {
                    throw new JsonParseException("An identifier validator object must have a 'name'.");
                }

                final String name = object.get("name").getAsString();

                Map<String, Object> params = null;
                if (object.has("params") && object.get("params").isJsonObject()) {
                    params = context.deserialize(object.get("params"), new TypeToken<Map<String, Object>>() {}.getType());
                }

                return new Validator(name, params);
            }

            throw new JsonParseException("An identifier validator must be a string or an object with a 'name'.");

        }

        @Override
        public JsonElement serialize(final Validator validator, final Type typeOfSrc, final JsonSerializationContext context) {

            if (validator.getParams() == null || validator.getParams().isEmpty()) {
                return new JsonPrimitive(validator.getName());
            }

            final JsonObject object = new JsonObject();
            object.addProperty("name", validator.getName());
            object.add("params", context.serialize(validator.getParams()));
            return object;

        }

    }

}
