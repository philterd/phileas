/*
 *     Copyright 2023 Philterd, LLC @ https://www.philterd.ai
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
package ai.philterd.phileas.model.enums;

/**
 * A filter type.
 */
public enum FilterType {

    // TODO: Are Lucene dictionaries "deterministic"? Yes, if no fuzziness.

    AGE("age", true),
    BANK_ROUTING_NUMBER("bank-routing-number", true),
    BITCOIN_ADDRESS("bitcoin-address", true),
    CURRENCY("currency", true),
    CREDIT_CARD("credit-card", true),
    DRIVERS_LICENSE_NUMBER("drivers-license-number", true),
    LOCATION_CITY("city", false),
    LOCATION_STATE("state", false),
    LOCATION_COUNTY("county", false),
    DATE("date", true),
    EMAIL_ADDRESS("email-address", true),
    FIRST_NAME("first-name", false),
    HOSPITAL("hospital", false),
    HOSPITAL_ABBREVIATION("hospital-abbreviation", false),
    IBAN_CODE("iban-code", true),
    IDENTIFIER("id", true),
    IP_ADDRESS("ip-address", true),
    MAC_ADDRESS("mac-address", true),
    PASSPORT_NUMBER("passport-number", true),
    PERSON("person", false),
    PERSON_V2("person-v2", false),
    PERSON_V3("person-v3", false),
    PHONE_NUMBER("phone-number", true),
    PHONE_NUMBER_EXTENSION("phone-number-extension", true),
    PHYSICIAN_NAME("physician-name", true),
    SECTION("section", true),
    SSN("ssn", true),
    STATE_ABBREVIATION("state-abbreviation", false),
    STREET_ADDRESS("street-address", true),
    SURNAME("surname", false),
    TRACKING_NUMBER("tracking-number", true),
    URL("url", true),
    VIN("vin", true),
    ZIP_CODE("zip-code", true),
    CUSTOM_DICTIONARY("custom-dictionary", false);

    private String type;
    private boolean deterministic;

    FilterType(String type, boolean deterministic) {
        this.type = type;
        this.deterministic = deterministic;
    }

    public String getType() {
        return type;
    }

    public boolean isDeterministic() {
        return deterministic;
    }

    @Override
    public String toString() {
        return type;
    }

}
