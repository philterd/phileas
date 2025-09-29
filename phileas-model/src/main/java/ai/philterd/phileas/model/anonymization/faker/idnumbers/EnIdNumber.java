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
/*
 * Copyright 2014 DiUS Computing
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ai.philterd.phileas.model.anonymization.faker.idnumbers;

import ai.philterd.phileas.model.anonymization.faker.Faker;

public class EnIdNumber {
    private static final String[] invalidSSNPatterns = {
            "0{3}-\\\\d{2}-\\\\d{4}",
            "\\d{3}-0{2}-\\d{4}",
            "\\d{3}-\\d{2}-0{4}",
            "666-\\d{2}-\\d{4}",
            "9\\d{2}-\\d{2}-\\d{4}"};

    public String getValidSsn(Faker f) {
        String ssn = f.regexify("[0-8]\\d{2}-\\d{2}-\\d{4}");

        boolean isValid = true;
        for (String invalidSSNPattern : invalidSSNPatterns) {
            if (ssn.matches(invalidSSNPattern)) {
                isValid = false;
                break;
            }
        }
        if (!isValid) {
            ssn = getValidSsn(f);
        }
        return ssn;
    }
}
