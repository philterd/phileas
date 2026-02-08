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
package ai.philterd.phileas.services.anonymization;

public enum AnonymizationMethod {

    REALISTIC("realistic"),
    FROM_LIST("from_list"),
    UUID("uuid");

    private final String value;

    AnonymizationMethod(String value) {
        this.value = value;
    }

    public static AnonymizationMethod fromString(String value) {

        if(value == null) {
            // Default to UUID if null value.
            return UUID;
        }

        if(value.equalsIgnoreCase(REALISTIC.getValue())) {
            return REALISTIC;
        } else if(value.equalsIgnoreCase(FROM_LIST.getValue())) {
            return FROM_LIST;
        } else if(value.equalsIgnoreCase(UUID.getValue())) {
            return UUID;
        } else {
            // Default to UUID if invalid value.
            return UUID;
        }

    }

    @Override
    public String toString() {
        return value;
    }

    public String getValue() {
        return value;
    }

}
