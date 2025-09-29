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

public class Job {

    private final Faker faker;

    public Job(final Faker faker) {
        this.faker = faker;
    }

    public String field() {
        return faker.fakeValuesService().resolve("job.field", this, faker);
    }

    public String seniority() {
        return faker.fakeValuesService().resolve("job.seniority", this, faker);
    }

    public String position() {
        return faker.fakeValuesService().resolve("job.position", this, faker);
    }

    public String keySkills() {
        return faker.fakeValuesService().resolve("job.key_skills", this, faker);
    }

    public String title() {
        return faker.fakeValuesService().resolve("job.title", this, faker);
    }
}
