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

import ai.philterd.phileas.policy.Policy;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

public class EmailConfigTest {

    private static Policy load(final String email) {
        return new Gson().fromJson("{\"config\":{\"email\":" + email + "}}", Policy.class);
    }

    @Test
    public void defaultsRemoveAttachmentsAndInlineImagesWhenAbsent() {

        final Email email = new Gson().fromJson("{}", Policy.class).getConfig().getEmail();

        Assertions.assertTrue(email.getHeaders().isRemoveTechnical());
        Assertions.assertTrue(email.getHeaders().isRemoveIdentity());
        Assertions.assertFalse(email.getHeaders().isRemoveDate());
        Assertions.assertFalse(email.getHeaders().isRemoveSubject());
        Assertions.assertEquals(Email.Attachments.REMOVE, email.getAttachments());
        Assertions.assertEquals(Email.InlineImages.REMOVE, email.getInlineImages());

    }

    @Test
    public void unsetSettingsKeepTheirDefaults() {

        final Email email = load("{\"headers\":{\"removeDate\":true},\"attachments\":\"redact\"}").getConfig().getEmail();

        Assertions.assertTrue(email.getHeaders().isRemoveTechnical());
        Assertions.assertTrue(email.getHeaders().isRemoveDate());
        Assertions.assertEquals(Email.Attachments.REDACT, email.getAttachments());
        Assertions.assertEquals(Email.InlineImages.REMOVE, email.getInlineImages());

    }

    @Test
    public void roundTrips() {

        final String json = "{\"headers\":{\"removeTechnical\":false,\"removeIdentity\":false,\"removeDate\":true,"
                + "\"removeSubject\":true},\"attachments\":\"keep\",\"inlineImages\":\"keep\"}";

        final JsonObject written = JsonParser.parseString(new Gson().toJson(load(json)))
                .getAsJsonObject().getAsJsonObject("config").getAsJsonObject("email");

        Assertions.assertEquals(JsonParser.parseString(json), written);

    }

    @Test
    public void refusesWhatTheSchemaDoesNotAllow() {

        for (final String email : List.of(
                "\"keep\"",
                "{\"attachments\":\"Keep\"}",
                "{\"attachments\":\"delete\"}",
                "{\"attachments\":true}",
                "{\"inlineImages\":\"redact\"}",
                "{\"headers\":{\"removeDate\":\"true\"}}",
                "{\"headers\":{\"removeBcc\":true}}",
                "{\"headers\":true}",
                "{\"removeDate\":true}")) {
            Assertions.assertThrows(JsonParseException.class, () -> load(email), email);
        }

    }

    @Test
    public void compilesFromPhiSql() throws Exception {

        final Policy policy = Policy.fromPhiSQL("CONFIGURE EMAIL (headers = (removeTechnical = FALSE, removeIdentity = TRUE, "
                + "removeDate = TRUE, removeSubject = TRUE), attachments = 'redact', inlineImages = 'remove');");
        final Email email = policy.getConfig().getEmail();

        Assertions.assertFalse(email.getHeaders().isRemoveTechnical());
        Assertions.assertTrue(email.getHeaders().isRemoveDate());
        Assertions.assertTrue(email.getHeaders().isRemoveSubject());
        Assertions.assertEquals(Email.Attachments.REDACT, email.getAttachments());
        Assertions.assertEquals(Email.InlineImages.REMOVE, email.getInlineImages());

    }

}
