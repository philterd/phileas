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
package ai.philterd.phileas.policy.config;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;
import java.util.Map;

/**
 * The policy's {@code config.review.leftoverCheck}: a second detection pass over the redacted output that
 * reports anything still detectable, without changing the output.
 */
public class LeftoverCheck {

    /** When the check runs. */
    public enum Mode {

        /** The check never runs. */
        OFF("off"),

        /** The check runs only when the caller asks for it on that call. */
        ON_REQUEST("onRequest"),

        /** The check runs on every call. A caller cannot turn it off. */
        ALWAYS("always");

        private final String value;

        Mode(final String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        static Mode of(final String value) {
            for (final Mode mode : values()) {
                if (mode.value.equals(value)) {
                    return mode;
                }
            }
            throw new JsonParseException("The review leftoverCheck mode '" + value
                    + "' is not valid. Valid values are off, onRequest, and always.");
        }

    }

    /** What the check detects with. */
    public enum Using {

        /** Every filter the policy defines plus every other built-in identifier at its defaults. */
        BROAD("broad"),

        /** Only the filters the policy defines. */
        SAME_POLICY("samePolicy");

        private final String value;

        Using(final String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        static Using of(final String value) {
            for (final Using using : values()) {
                if (using.value.equals(value)) {
                    return using;
                }
            }
            throw new JsonParseException("The review leftoverCheck using '" + value
                    + "' is not valid. Valid values are broad and samePolicy.");
        }

    }

    private Mode mode = Mode.OFF;
    private Using using = Using.BROAD;

    public LeftoverCheck() {
    }

    public LeftoverCheck(final Mode mode, final Using using) {
        setMode(mode);
        setUsing(using);
    }

    public Mode getMode() {
        return mode;
    }

    public void setMode(final Mode mode) {
        this.mode = mode == null ? Mode.OFF : mode;
    }

    public Using getUsing() {
        return using;
    }

    public void setUsing(final Using using) {
        this.using = using == null ? Using.BROAD : using;
    }

    /**
     * Reads {@code leftoverCheck} strictly, so a policy that is not what the schema allows fails to load rather
     * than silently running without the check: it must be an object whose only keys are {@code mode} and
     * {@code using}, each a string from its list. Values are case-sensitive.
     */
    public static class Adapter implements JsonDeserializer<LeftoverCheck>, JsonSerializer<LeftoverCheck> {

        @Override
        public LeftoverCheck deserialize(final JsonElement json, final Type typeOfT,
                                         final JsonDeserializationContext context) throws JsonParseException {

            if (json == null || json.isJsonNull()) {
                return null;
            }

            if (!json.isJsonObject()) {
                throw new JsonParseException("The review leftoverCheck must be an object with mode and using.");
            }

            final LeftoverCheck leftoverCheck = new LeftoverCheck();

            for (final Map.Entry<String, JsonElement> entry : json.getAsJsonObject().entrySet()) {

                final JsonElement value = entry.getValue();

                if (!"mode".equals(entry.getKey()) && !"using".equals(entry.getKey())) {
                    throw new JsonParseException("The review leftoverCheck has an unknown key '" + entry.getKey()
                            + "'. Valid keys are mode and using.");
                }

                if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
                    throw new JsonParseException("The review leftoverCheck " + entry.getKey() + " must be a string.");
                }

                if ("mode".equals(entry.getKey())) {
                    leftoverCheck.setMode(Mode.of(value.getAsString()));
                } else {
                    leftoverCheck.setUsing(Using.of(value.getAsString()));
                }

            }

            return leftoverCheck;

        }

        @Override
        public JsonElement serialize(final LeftoverCheck leftoverCheck, final Type typeOfSrc,
                                     final JsonSerializationContext context) {

            final JsonObject object = new JsonObject();
            object.addProperty("mode", leftoverCheck.getMode().getValue());
            object.addProperty("using", leftoverCheck.getUsing().getValue());
            return object;

        }

    }

}
