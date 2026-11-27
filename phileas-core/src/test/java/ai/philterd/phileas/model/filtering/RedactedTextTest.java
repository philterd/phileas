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
package ai.philterd.phileas.model.filtering;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

public class RedactedTextTest {

    private static final RedactedText TEXT = new RedactedText(List.of(
            new TextSegment("page 1", "First.", TextSegment.Status.TEXT),
            new TextSegment("page 2", "", TextSegment.Status.NO_TEXT),
            new TextSegment("page 3", "", TextSegment.Status.NOT_SCANNED),
            new TextSegment("page 4", "Last.", TextSegment.Status.TEXT)));

    @Test
    public void joinsTheSegmentsThatHaveText() {
        Assertions.assertEquals("First.\n\nLast.", TEXT.join("\n\n"));
    }

    @Test
    public void listsOnlyTheSegmentsWithNoExtractableText() {
        Assertions.assertEquals(List.of("page 2"), TEXT.getSegmentsWithoutText());
    }

    @Test
    public void aSegmentWithoutTextHoldsNone() {
        Assertions.assertEquals("", new TextSegment("page 1", "ignored", TextSegment.Status.NOT_SCANNED).getText());
    }

    @Test
    public void aSegmentPrintsItsLabelNotItsText() {
        Assertions.assertEquals("[page 1 (TEXT), page 2 (NO_TEXT), page 3 (NOT_SCANNED), page 4 (TEXT)]", TEXT.toString());
    }

    @Test
    public void serializesStatusesInCamelCase() {
        final JsonObject json = new Gson().toJsonTree(TEXT).getAsJsonObject();
        Assertions.assertEquals("noText", json.getAsJsonArray("segments").get(1).getAsJsonObject().get("status").getAsString());
        Assertions.assertEquals("notScanned", json.getAsJsonArray("segments").get(2).getAsJsonObject().get("status").getAsString());
    }

    @Test
    public void aFindingsSegmentIsLeftOutOfJsonUnlessSet() {

        final LeftoverFinding finding = new LeftoverFinding(FilterType.SSN, "ssn", 0, 11, 1.0, 0, 1, 0, null);
        Assertions.assertFalse(new Gson().toJsonTree(finding).getAsJsonObject().has("segment"));

        final LeftoverFinding inPage = finding.inSegment("page 1");
        Assertions.assertEquals("page 1", inPage.getSegment());
        Assertions.assertEquals("page 1", new Gson().toJsonTree(inPage).getAsJsonObject().get("segment").getAsString());
        Assertions.assertEquals("page 1", inPage.inAttachment("a.pdf").getSegment());

    }

}
