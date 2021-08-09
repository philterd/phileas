package com.mtnfog.phileas.model.serializers;

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
