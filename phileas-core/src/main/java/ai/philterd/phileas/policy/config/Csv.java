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

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.util.Map;

/**
 * The policy's {@code config.csv}: how a CSV document is read. Column rules for CSV are in the policy's
 * top-level {@code tabular} section. No effect on Excel workbooks.
 */
public class Csv {

    public static final String DEFAULT_DELIMITER = ",";
    public static final String DEFAULT_QUOTE = "\"";
    public static final String DEFAULT_ENCODING = "UTF-8";

    @SerializedName("delimiter")
    @Expose
    private String delimiter = DEFAULT_DELIMITER;

    @SerializedName("quote")
    @Expose
    private String quote = DEFAULT_QUOTE;

    @SerializedName("encoding")
    @Expose
    private String encoding = DEFAULT_ENCODING;

    /** The field delimiter, one character. Default {@code ,}. */
    public String getDelimiter() {
        return delimiter;
    }

    public void setDelimiter(final String delimiter) {
        this.delimiter = delimiter;
    }

    /** The quote character around fields that hold the delimiter, a quote, or a line break. Default {@code "}. */
    public String getQuote() {
        return quote;
    }

    public void setQuote(final String quote) {
        this.quote = quote;
    }

    /** The character encoding the document is read in, by its IANA name. Default {@code UTF-8}. */
    public String getEncoding() {
        return encoding;
    }

    public void setEncoding(final String encoding) {
        this.encoding = encoding;
    }

    /** The encoding as a {@link Charset}. */
    public Charset getCharset() {
        return Charset.forName(encoding);
    }

    /**
     * Reads {@code csv} strictly, as the PhiSQL compilers check it: unknown keys, a delimiter or quote that is not
     * one character or is a line break, a delimiter equal to the quote, and an encoding this runtime cannot read
     * are refused.
     */
    public static class Adapter implements JsonDeserializer<Csv>, JsonSerializer<Csv> {

        @Override
        public Csv deserialize(final JsonElement json, final Type typeOfT,
                               final JsonDeserializationContext context) throws JsonParseException {

            if (json == null || json.isJsonNull()) {
                return null;
            }

            if (!json.isJsonObject()) {
                throw new JsonParseException("The csv must be an object.");
            }

            final Csv csv = new Csv();

            for (final Map.Entry<String, JsonElement> entry : json.getAsJsonObject().entrySet()) {

                final JsonElement value = entry.getValue();

                switch (entry.getKey()) {
                    case "delimiter" -> csv.setDelimiter(character(value, "delimiter"));
                    case "quote" -> csv.setQuote(character(value, "quote"));
                    case "encoding" -> csv.setEncoding(encoding(value));
                    default -> throw new JsonParseException("The csv has an unknown key '" + entry.getKey()
                            + "'. Valid keys are delimiter, quote, and encoding.");
                }

            }

            if (csv.getDelimiter().equals(csv.getQuote())) {
                throw new JsonParseException("The csv delimiter and quote must be different characters.");
            }

            return csv;

        }

        private static String character(final JsonElement json, final String name) {
            if (json.isJsonPrimitive() && json.getAsJsonPrimitive().isString()) {
                final String s = json.getAsString();
                if (s.codePointCount(0, s.length()) == 1 && !s.equals("\r") && !s.equals("\n")) {
                    return s;
                }
            }
            throw new JsonParseException("The csv " + name + " must be a single character other than a line break.");
        }

        private static String encoding(final JsonElement json) {
            if (!json.isJsonPrimitive() || !json.getAsJsonPrimitive().isString() || json.getAsString().isBlank()) {
                throw new JsonParseException("The csv encoding must be a non-empty string.");
            }
            final String encoding = json.getAsString();
            try {
                if (Charset.isSupported(encoding)) {
                    return encoding;
                }
            } catch (final IllegalArgumentException e) {
                // An illegal charset name is refused below like an unsupported one.
            }
            throw new JsonParseException("The csv encoding '" + encoding + "' is not a character encoding this "
                    + "runtime can read.");
        }

        @Override
        public JsonElement serialize(final Csv csv, final Type typeOfSrc, final JsonSerializationContext context) {
            final JsonObject object = new JsonObject();
            object.addProperty("delimiter", csv.getDelimiter());
            object.addProperty("quote", csv.getQuote());
            object.addProperty("encoding", csv.getEncoding());
            return object;
        }

    }

}
