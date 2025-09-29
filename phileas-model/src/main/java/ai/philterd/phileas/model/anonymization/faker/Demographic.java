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

public class Demographic {

    private final Faker faker;

    protected Demographic(Faker faker) {
        this.faker = faker;
    }

    public String race() {
        return faker.fakeValuesService().fetchString("demographic.race");
    }

    public String educationalAttainment() {
        return faker.fakeValuesService().fetchString("demographic.educational_attainment");
    }

    public String demonym() {
        return faker.fakeValuesService().fetchString("demographic.demonym");
    }

    public String sex() {
        return faker.fakeValuesService().fetchString("demographic.sex");
    }

    public String maritalStatus() {
        return faker.fakeValuesService().fetchString("demographic.marital_status");
    }
}
