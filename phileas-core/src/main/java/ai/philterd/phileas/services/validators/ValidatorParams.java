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
package ai.philterd.phileas.services.validators;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Helpers for reading the optional {@code params} of a parameterized identifier validator (the
 * object form of the {@code validator} field). Values come from a permissive JSON deserialization,
 * so this normalizes them defensively.
 */
final class ValidatorParams {

    private ValidatorParams() {
        // Static utility.
    }

    /**
     * @return the named parameter as a string, or {@code null} if absent.
     */
    static String string(final Map<String, Object> params, final String key) {

        if (params == null) {
            return null;
        }

        final Object value = params.get(key);
        return value == null ? null : value.toString();

    }

    /**
     * Reads a string-to-string substitution map parameter, with keys and values upper-cased to
     * match upper-cased input. Returns {@code defaultValue} when the parameter is absent or not a
     * map.
     */
    static Map<String, String> stringMap(final Map<String, Object> params, final String key, final Map<String, String> defaultValue) {

        if (params == null) {
            return defaultValue;
        }

        final Object value = params.get(key);
        if (!(value instanceof Map)) {
            return defaultValue;
        }

        final Map<?, ?> raw = (Map<?, ?>) value;
        final Map<String, String> result = new HashMap<>();
        for (final Map.Entry<?, ?> entry : raw.entrySet()) {
            result.put(
                    String.valueOf(entry.getKey()).toUpperCase(Locale.ROOT),
                    String.valueOf(entry.getValue()).toUpperCase(Locale.ROOT));
        }

        return result;

    }

}
