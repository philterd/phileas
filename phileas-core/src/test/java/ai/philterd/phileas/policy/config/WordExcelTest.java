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

public class WordExcelTest {

    private static Policy load(final String config) {
        return new Gson().fromJson("{\"config\":" + config + "}", Policy.class);
    }

    @Test
    public void wordDefaultsWhenAbsent() {
        final Word word = new Gson().fromJson("{}", Policy.class).getConfig().getWord();
        Assertions.assertEquals(Word.RedactOrKeep.REDACT, word.getHeadersFooters());
        Assertions.assertEquals(Word.RemoveOrRedact.REMOVE, word.getComments());
        Assertions.assertEquals(Word.AcceptOrRedact.ACCEPT, word.getTrackedChanges());
        Assertions.assertEquals(Word.RemoveOrRedact.REMOVE, word.getHiddenText());
        Assertions.assertEquals(Word.RedactOrKeep.REDACT, word.getCharts());
        Assertions.assertEquals(Word.RemoveOrKeep.REMOVE, word.getUninspectableObjects());
        Assertions.assertTrue(word.isRemoveDocumentProperties());
        Assertions.assertFalse(word.isHighlight());
    }

    @Test
    public void excelDefaultsWhenAbsent() {
        final Excel excel = new Gson().fromJson("{}", Policy.class).getConfig().getExcel();
        Assertions.assertEquals(Word.RedactOrKeep.REDACT, excel.getPrintHeadersFooters());
        Assertions.assertEquals(Word.RedactOrKeep.REDACT, excel.getCharts());
        Assertions.assertEquals(Word.RedactOrKeep.REDACT, excel.getFormulaCaches());
        Assertions.assertEquals(Word.RedactOrKeep.REDACT, excel.getPivotCaches());
        Assertions.assertEquals(Word.RemoveOrKeep.REMOVE, excel.getUninspectableObjects());
        Assertions.assertTrue(excel.isRemoveDocumentProperties());
    }

    @Test
    public void readsEveryWordSetting() {
        final Word word = load("{\"word\":{\"headersFooters\":\"keep\",\"comments\":\"redact\",\"trackedChanges\":\"redact\","
                + "\"hiddenText\":\"redact\",\"charts\":\"keep\",\"uninspectableObjects\":\"keep\","
                + "\"removeDocumentProperties\":false,\"highlight\":true}}").getConfig().getWord();
        Assertions.assertEquals(Word.RedactOrKeep.KEEP, word.getHeadersFooters());
        Assertions.assertEquals(Word.RemoveOrRedact.REDACT, word.getComments());
        Assertions.assertEquals(Word.AcceptOrRedact.REDACT, word.getTrackedChanges());
        Assertions.assertEquals(Word.RemoveOrRedact.REDACT, word.getHiddenText());
        Assertions.assertEquals(Word.RedactOrKeep.KEEP, word.getCharts());
        Assertions.assertEquals(Word.RemoveOrKeep.KEEP, word.getUninspectableObjects());
        Assertions.assertFalse(word.isRemoveDocumentProperties());
        Assertions.assertTrue(word.isHighlight());
    }

    @Test
    public void readsEveryExcelSetting() {
        final Excel excel = load("{\"excel\":{\"printHeadersFooters\":\"keep\",\"charts\":\"keep\",\"formulaCaches\":\"keep\","
                + "\"pivotCaches\":\"keep\",\"uninspectableObjects\":\"keep\",\"removeDocumentProperties\":false}}")
                .getConfig().getExcel();
        Assertions.assertEquals(Word.RedactOrKeep.KEEP, excel.getPrintHeadersFooters());
        Assertions.assertEquals(Word.RedactOrKeep.KEEP, excel.getCharts());
        Assertions.assertEquals(Word.RedactOrKeep.KEEP, excel.getFormulaCaches());
        Assertions.assertEquals(Word.RedactOrKeep.KEEP, excel.getPivotCaches());
        Assertions.assertEquals(Word.RemoveOrKeep.KEEP, excel.getUninspectableObjects());
        Assertions.assertFalse(excel.isRemoveDocumentProperties());
    }

    @Test
    public void roundTrips() {
        final Policy policy = load("{\"word\":{\"comments\":\"redact\"},\"excel\":{\"pivotCaches\":\"keep\"}}");
        final JsonObject config = JsonParser.parseString(new Gson().toJson(policy)).getAsJsonObject().getAsJsonObject("config");
        Assertions.assertEquals("redact", config.getAsJsonObject("word").get("comments").getAsString());
        Assertions.assertEquals("accept", config.getAsJsonObject("word").get("trackedChanges").getAsString());
        Assertions.assertEquals("keep", config.getAsJsonObject("excel").get("pivotCaches").getAsString());
        Assertions.assertTrue(config.getAsJsonObject("excel").get("removeDocumentProperties").getAsBoolean());
        Assertions.assertEquals(new Gson().toJson(policy), new Gson().toJson(new Gson().fromJson(new Gson().toJson(policy), Policy.class)));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"word\":{\"comments\":\"keep\"}}",
            "{\"word\":{\"comments\":\"REMOVE\"}}",
            "{\"word\":{\"trackedChanges\":\"reject\"}}",
            "{\"word\":{\"hiddenText\":true}}",
            "{\"word\":{\"headersFooters\":\"remove\"}}",
            "{\"word\":{\"highlight\":\"yes\"}}",
            "{\"word\":{\"removeDocumentProperties\":1}}",
            "{\"word\":{\"other\":\"redact\"}}",
            "{\"word\":[]}",
            "{\"excel\":{\"formulaCaches\":\"remove\"}}",
            "{\"excel\":{\"uninspectableObjects\":\"redact\"}}",
            "{\"excel\":{\"comments\":\"remove\"}}",
            "{\"excel\":{\"removeDocumentProperties\":\"false\"}}",
            "{\"excel\":\"keep\"}"
    })
    public void refusesAnInvalidSetting(final String config) {
        Assertions.assertThrows(JsonParseException.class, () -> load(config));
    }

}
