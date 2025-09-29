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

public class Country {
    private final Faker faker;
    private final String flagUrl;

    protected Country(Faker faker) {
        this.faker = faker;
        this.flagUrl = "http://flags.fmcdn.net/data/flags/w580/";
    }

    public String flag() {
        return flagUrl + faker.fakeValuesService().resolve("country.code2", this, faker) + ".png";
    }

    public String countryCode2() {
        return faker.fakeValuesService().resolve("country.code2", this, faker);
    }

    public String countryCode3() {
        return faker.fakeValuesService().resolve("country.code3", this, faker);
    }

    public String capital() {
        return faker.fakeValuesService().resolve("country.capital", this, faker);
    }

    public String currency() {
        return faker.fakeValuesService().resolve("country.currency", this, faker);
    }

    public String currencyCode() {
        return faker.fakeValuesService().resolve("country.currency_code", this, faker);
    }

    public String name() {
        return faker.fakeValuesService().resolve("country.name", this, faker);
    }

}
