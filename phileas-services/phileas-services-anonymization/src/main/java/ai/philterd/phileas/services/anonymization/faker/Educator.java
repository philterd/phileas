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
package ai.philterd.phileas.services.anonymization.faker;

public class Educator {
    private final ai.philterd.phileas.services.anonymization.faker.Faker faker;

    protected Educator(Faker faker) {
        this.faker = faker;
    }

    // TODO - move these all out to en.yml by default. 
    public String university() {
        return faker.fakeValuesService().resolve("educator.name", this, faker) 
                + " " 
                + faker.fakeValuesService().resolve("educator.tertiary.type", this, faker);
    }

    public String course() {
        return faker.fakeValuesService().resolve("educator.tertiary.degree.type", this, faker)
                + " "
                + faker.fakeValuesService().resolve("educator.tertiary.degree.subject", this, faker);
    }

    public String secondarySchool() {
        return faker.fakeValuesService().resolve("educator.name", this, faker)
                + " "
                + faker.fakeValuesService().resolve("educator.secondary", this, faker);
    }

    public String campus() {
        return faker.fakeValuesService().resolve("educator.name", this, faker) + " Campus";
    }

}
