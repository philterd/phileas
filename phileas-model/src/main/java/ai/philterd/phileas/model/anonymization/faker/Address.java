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
package ai.philterd.phileas.model.anonymization.faker;

public class Address {
    private final Faker faker;

    protected Address(Faker faker) {
        this.faker = faker;
    }

    public String streetName() {
        return faker.fakeValuesService().resolve("address.street_name", this, faker);
    }

    public String streetAddressNumber() {
        return String.valueOf(faker.random().nextInt(1000));
    }

    public String streetAddress() {
        return faker.fakeValuesService().resolve("address.street_address", this, faker);
    }

    public String streetAddress(boolean includeSecondary) {
        String streetAddress = faker.fakeValuesService().resolve("address.street_address", this, faker);
        if (includeSecondary) {
            streetAddress = streetAddress + " " + secondaryAddress();
        }
        return streetAddress;
    }

    public String secondaryAddress() {
        return faker.numerify(faker.fakeValuesService().resolve("address.secondary_address", this,faker));
    }

    public String zipCode() {
        return faker.bothify(faker.fakeValuesService().resolve("address.postcode", this,faker));
    }

    public String postcode() {
        return faker.bothify(faker.fakeValuesService().resolve("address.postcode", this,faker));
    }

    public String zipCodeByState(String stateAbbr) {
        return faker.fakeValuesService().resolve("address.postcode_by_state." + stateAbbr, this, faker);
    }

    public String countyByZipCode(String postCode) {
        return faker.fakeValuesService().resolve("address.county_by_postcode." + postCode, this, faker);
    }

    public String streetSuffix() {
        return faker.fakeValuesService().resolve("address.street_suffix", this, faker);
    }

    public String streetPrefix() {
        return faker.fakeValuesService().resolve("address.street_prefix", this, faker);
    }

    public String citySuffix() {
        return faker.fakeValuesService().resolve("address.city_suffix", this, faker);
    }

    public String cityPrefix() {
        return faker.fakeValuesService().resolve("address.city_prefix", this, faker);
    }

    public String city() {
        return faker.fakeValuesService().resolve("address.city", this, faker);
    }

    public String cityName() {
        return faker.fakeValuesService().resolve("address.city_name", this, faker);
    }

    public String state() {
        return faker.fakeValuesService().resolve("address.state", this, faker);
    }

    public String stateAbbr() {
        return faker.fakeValuesService().resolve("address.state_abbr", this, faker);
    }

    public String firstName() {
        return faker.name().firstName();
    }

    public String lastName() {
        return faker.name().lastName();
    }

    public String latitude() {
        return String.format("%.8g", (faker.random().nextDouble() * 180) - 90);
    }

    public String longitude() {
        return String.format("%.8g", (faker.random().nextDouble() * 360) - 180);
    }

    public String timeZone() {
        return faker.fakeValuesService().resolve("address.time_zone", this, faker);
    }

    public String country() {
        return faker.fakeValuesService().resolve("address.country", this, faker);
    }

    public String countryCode() {
        return faker.fakeValuesService().resolve("address.country_code", this, faker);
    }

    public String buildingNumber() {
        return faker.numerify(faker.fakeValuesService().resolve("address.building_number", this, faker));
    }

    public String fullAddress() {
        return faker.fakeValuesService().resolve("address.full_address", this, faker);
    }
}
