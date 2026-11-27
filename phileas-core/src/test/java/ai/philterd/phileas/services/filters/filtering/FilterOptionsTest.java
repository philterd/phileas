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
package ai.philterd.phileas.services.filters.filtering;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class FilterOptionsTest {

    @Test
    public void theDocumentIsTheDefaultOutputWithoutMetadata() {
        Assertions.assertEquals(FilterOptions.Output.DOCUMENT, FilterOptions.defaults().getOutput());
        Assertions.assertFalse(FilterOptions.defaults().isTextOutput());
        Assertions.assertFalse(FilterOptions.defaults().isTextMetadata());
    }

    @Test
    public void eachSettingKeepsTheOthers() {

        final FilterOptions options = FilterOptions.defaults().withLeftoverCheck(true).withLeftoverCheckMatchedText(true)
                .withOutput(FilterOptions.Output.TEXT).withTextMetadata(true);

        Assertions.assertTrue(options.isLeftoverCheckRequested());
        Assertions.assertTrue(options.isLeftoverCheckMatchedText());
        Assertions.assertTrue(options.isTextOutput());
        Assertions.assertTrue(options.isTextMetadata());

        final FilterOptions after = options.withLeftoverCheck(false).withLeftoverCheckMatchedText(false);
        Assertions.assertTrue(after.isTextOutput());
        Assertions.assertTrue(after.isTextMetadata());

        // The defaults are not changed.
        Assertions.assertFalse(FilterOptions.defaults().isTextOutput());

    }

    @Test
    public void refusesANullOutput() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> FilterOptions.defaults().withOutput(null));
    }

}
