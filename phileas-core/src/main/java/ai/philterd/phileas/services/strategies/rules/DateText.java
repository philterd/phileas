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
package ai.philterd.phileas.services.strategies.rules;

import java.time.LocalDate;

/** A date read from its text, which can be written again in the same form. */
interface DateText {

    /** Whether the date has a day. A month and year alone does not. */
    boolean hasDay();

    /** The date. A month and year alone is the first of the month. */
    LocalDate date();

    /** The year as written: two digits for a two-digit year. */
    String yearText();

    /** Writes another date in this date's form. */
    String format(LocalDate other);

}
