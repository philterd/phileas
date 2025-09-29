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

public class Medical {

    private final Faker faker;

    protected Medical(Faker faker) {
        this.faker = faker;
    }

    public String medicineName() {
        return faker.fakeValuesService().resolve("medical.medicine_name", this, faker);
    }

    public String diseaseName() {
        return faker.fakeValuesService().resolve("medical.disease_name", this, faker);
    }

    public String hospitalName() {
        return faker.fakeValuesService().resolve("medical.hospital_name", this, faker);
    }

    public String symptoms() {
        return faker.fakeValuesService().resolve("medical.symptoms", this, faker);
    }
}
