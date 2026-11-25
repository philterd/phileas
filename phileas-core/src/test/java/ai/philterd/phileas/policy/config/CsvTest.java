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

import ai.philterd.phileas.policy.Policy;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.charset.StandardCharsets;

public class CsvTest {

    private static Csv csv(final String json) {
        return new Gson().fromJson("{\"config\":{\"csv\":" + json + "}}", Policy.class).getConfig().getCsv();
    }

    @Test
    public void defaultsWhenAbsent() {
        final Csv csv = new Gson().fromJson("{}", Policy.class).getConfig().getCsv();
        Assertions.assertEquals(",", csv.getDelimiter());
        Assertions.assertEquals("\"", csv.getQuote());
        Assertions.assertEquals("UTF-8", csv.getEncoding());
        Assertions.assertEquals(StandardCharsets.UTF_8, csv.getCharset());
    }

    @Test
    public void readsTheSettings() {
        final Csv csv = csv("{\"delimiter\":\";\",\"quote\":\"'\",\"encoding\":\"ISO-8859-1\"}");
        Assertions.assertEquals(";", csv.getDelimiter());
        Assertions.assertEquals("'", csv.getQuote());
        Assertions.assertEquals(StandardCharsets.ISO_8859_1, csv.getCharset());
    }

    @Test
    public void unsetSettingsKeepTheirDefaults() {
        final Csv csv = csv("{\"delimiter\":\"\\t\"}");
        Assertions.assertEquals("\t", csv.getDelimiter());
        Assertions.assertEquals("\"", csv.getQuote());
        Assertions.assertEquals("UTF-8", csv.getEncoding());
    }

    @Test
    public void roundTrips() {
        final Policy policy = new Gson().fromJson("{\"config\":{\"csv\":{\"delimiter\":\";\"}}}", Policy.class);
        final JsonObject csv = JsonParser.parseString(new Gson().toJson(policy)).getAsJsonObject()
                .getAsJsonObject("config").getAsJsonObject("csv");
        Assertions.assertEquals(";", csv.get("delimiter").getAsString());
        Assertions.assertEquals("\"", csv.get("quote").getAsString());
        Assertions.assertEquals("UTF-8", csv.get("encoding").getAsString());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"delimiter\":\"\"}",
            "{\"delimiter\":\";;\"}",
            "{\"delimiter\":\"\\n\"}",
            "{\"quote\":\"\\r\"}",
            "{\"delimiter\":1}",
            "{\"delimiter\":\"\\\"\"}",
            "{\"delimiter\":\";\",\"quote\":\";\"}",
            "{\"encoding\":\"\"}",
            "{\"encoding\":\"NOT-A-CHARSET\"}",
            "{\"encoding\":\"bad name!\"}",
            "{\"other\":\",\"}",
            "\",\""
    })
    public void refusesAnInvalidSetting(final String json) {
        Assertions.assertThrows(JsonParseException.class, () -> csv(json));
    }

}
