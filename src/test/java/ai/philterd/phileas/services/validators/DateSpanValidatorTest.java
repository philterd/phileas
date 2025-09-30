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

import ai.philterd.phileas.model.objects.Span;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class DateSpanValidatorTest {

    @Test
    public void test1() {

        // https://stackoverflow.com/a/27454146/1428388
        // https://stackoverflow.com/a/29014580/1428388

        final Span span = new Span();
        span.setPattern("MM-dd-uuuu");
        span.setText("05-20-2020");

        final SpanValidator spanValidator = DateSpanValidator.getInstance();
        Assertions.assertTrue(spanValidator.validate(span));

    }

    @Test
    public void test2() {

        final Span span = new Span();
        span.setPattern("MM-dd-uuuu");
        span.setText("15-20-2020");

        final SpanValidator spanValidator = DateSpanValidator.getInstance();
        Assertions.assertFalse(spanValidator.validate(span));

    }

}
