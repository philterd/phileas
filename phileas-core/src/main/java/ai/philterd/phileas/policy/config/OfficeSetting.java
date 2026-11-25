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
package ai.philterd.phileas.policy.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.util.Locale;
import java.util.StringJoiner;

/** A value of a {@code config.word} or {@code config.excel} setting, written in the policy in lower case. */
public interface OfficeSetting {

    /** The enum constant's name. */
    String name();

    /** The value as the policy writes it. */
    default String value() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** Reads a setting's value, refusing one outside its list. Values are case-sensitive. */
    static <E extends Enum<E> & OfficeSetting> E of(final Class<E> type, final JsonElement json, final String owner,
                                                   final String key) {
        if (json.isJsonPrimitive() && json.getAsJsonPrimitive().isString()) {
            for (final E constant : type.getEnumConstants()) {
                if (constant.value().equals(json.getAsString())) {
                    return constant;
                }
            }
        }
        final StringJoiner values = new StringJoiner(" and ");
        for (final E constant : type.getEnumConstants()) {
            values.add(constant.value());
        }
        throw new JsonParseException("The " + owner + " " + key + " " + json + " is not valid. Valid values are "
                + values + ".");
    }

    /** Reads a boolean setting. */
    static boolean bool(final JsonElement json, final String owner, final String key) {
        if (!json.isJsonPrimitive() || !json.getAsJsonPrimitive().isBoolean()) {
            throw new JsonParseException("The " + owner + " " + key + " must be true or false.");
        }
        return json.getAsBoolean();
    }

    /** The settings object. */
    static JsonObject object(final JsonElement json, final String owner) {
        if (!json.isJsonObject()) {
            throw new JsonParseException("The " + owner + " must be an object.");
        }
        return json.getAsJsonObject();
    }

}
