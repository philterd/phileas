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
package ai.philterd.phileas.policy.filters;

import com.google.gson.Gson;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ValidatorTest {

    private final Gson gson = new Gson();

    @Test
    public void deserializesStringForm() {
        final Identifier identifier = gson.fromJson("{\"validator\": \"luhn\"}", Identifier.class);
        Assertions.assertNotNull(identifier.getValidator());
        Assertions.assertEquals("luhn", identifier.getValidator().getName());
        Assertions.assertNull(identifier.getValidator().getParams());
    }

    @Test
    public void deserializesObjectForm() {
        final Identifier identifier = gson.fromJson("{\"validator\": {\"name\": \"luhn\"}}", Identifier.class);
        Assertions.assertNotNull(identifier.getValidator());
        Assertions.assertEquals("luhn", identifier.getValidator().getName());
    }

    @Test
    public void deserializesObjectFormWithParams() {
        final Identifier identifier = gson.fromJson("{\"validator\": {\"name\": \"mod11\", \"params\": {\"variant\": \"cpf\"}}}", Identifier.class);
        Assertions.assertEquals("mod11", identifier.getValidator().getName());
        Assertions.assertNotNull(identifier.getValidator().getParams());
        Assertions.assertEquals("cpf", identifier.getValidator().getParams().get("variant"));
    }

    @Test
    public void absentValidatorIsNull() {
        final Identifier identifier = gson.fromJson("{\"pattern\": \"\\\\d+\"}", Identifier.class);
        Assertions.assertNull(identifier.getValidator());
    }

    @Test
    public void roundTripsStringForm() {
        final Identifier identifier = gson.fromJson("{\"validator\": \"luhn\"}", Identifier.class);
        // A params-free validator serializes back to the compact string form.
        Assertions.assertTrue(gson.toJson(identifier).contains("\"validator\":\"luhn\""));
    }

}
