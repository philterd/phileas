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
package ai.philterd.phileas.model.serializers;

import com.google.gson.*;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Type;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlaceholderDeserializer implements JsonDeserializer<String> {

    final Pattern pattern = Pattern.compile("\\$\\{([A-Z0-9_]+)\\}", Pattern.CASE_INSENSITIVE);

    @Override
    public String deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {

        final String value = jsonElement.getAsString().trim();
        final String deserialized;

        // PHL-233: Substitute values in the string.

        final Matcher matcher = pattern.matcher(value);
        boolean matches = matcher.matches();

        if(matches) {

            final String placeholder = matcher.group(1);

            // See if it's an environment variable.
            if(System.getenv().containsKey(placeholder)) {

                deserialized = System.getenv(placeholder);

            } else {

                // Not an environment variable so just return the value.
                deserialized = value;

            }

        } else {

            // It's not a placeholder value.
            deserialized = value;

        }

        return deserialized;

    }

}
