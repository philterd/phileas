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

import org.apache.commons.lang3.StringUtils;

public class Name {
    
    private final ai.philterd.phileas.services.anonymization.faker.Faker faker;

    /**
     * Internal constructor, not to be used by clients.  Instances of {@link Name} should be accessed via 
     * {@link ai.philterd.phileas.services.anonymization.faker.Faker#name()}.
     */
    public Name(Faker faker) {
        this.faker = faker;
    }

    /**
     * <p>
     *      A multipart name composed of an optional prefix, a firstname and a lastname
     *      or other possible variances based on locale.  Examples:
     *      <ul>
     *          <li>James Jones Jr.</li>
     *          <li>Julie Johnson</li>
     *      </ul>
     * </p>
     * @return a random name with given and family names and an optional suffix.
     */
    public String name() {
        return faker.fakeValuesService().resolve("name.name", this, faker);
    }

    /**
     * <p>
     *      A multipart name composed of an optional prefix, a given and family name,
     *      another 'firstname' for the middle name and an optional suffix such as Jr. 
     *      Examples:
     *      <ul>
     *          <li>Mrs. Ella Geraldine Fitzgerald</li>
     *          <li>Jason Tom Sawyer Jr.</li>
     *          <li>Helen Jessica Troy</li>
     *      </ul>
     * </p>
     * @return a random name with a middle name component with optional prefix and suffix
     */
    public String nameWithMiddle() {
        return faker.fakeValuesService().resolve("name.name_with_middle", this, faker);
    }

    /**
     * <p>Returns the same value as {@link #name()}</p>
     * @see Name#name() 
     */
    public String fullName() {
        return name();
    }

    /**
     * <p>Returns a random 'given' name such as Aaliyah, Aaron, Abagail or Abbey</p>
     * @return a 'given' name such as Aaliyah, Aaron, Abagail or Abbey
     */
    public String firstName() {
        return faker.fakeValuesService().resolve("name.first_name", this, faker);
    }

    /**
     * <p>Returns a random last name such as Smith, Jones or Baldwin</p>
     * @return a random last name such as Smith, Jones or Baldwin
     */
    public String lastName() {
        return faker.fakeValuesService().resolve("name.last_name", this, faker);
    }

    /**
     * <p>Returns a name prefix such as Mr., Mrs., Ms., Miss, or Dr.</p>
     * @return a name prefix such as Mr., Mrs., Ms., Miss, or Dr.
     */
    public String prefix() {
        return faker.fakeValuesService().resolve("name.prefix", this, faker);
    }

    /**
     * <p>Returns a name suffix such as Jr., Sr., I, II, III, IV, V, MD, DDS, PhD or DVM</p>
     * @return a name suffix such as Jr., Sr., I, II, III, IV, V, MD, DDS, PhD or DVM
     */
    public String suffix() {
        return faker.fakeValuesService().resolve("name.suffix", this, faker);
    }

    /**
     * <p>
     *     A three part title composed of a descriptor level and job.  Some examples are :
     *     <ul>
     *         <li>(template) {descriptor} {level} {job}</li>
     *         <li>Lead Solutions Specialist</li>
     *         <li>National Marketing Manager</li>
     *         <li>Central Response Liaison</li>
     *     </ul>
     * </p>
     * @return a random three part job title
     */
    public String title() {
        return StringUtils.join(new String[] {
            faker.fakeValuesService().resolve("name.title.descriptor", this, faker), 
            faker.fakeValuesService().resolve("name.title.level", this, faker), 
            faker.fakeValuesService().resolve("name.title.job", this, faker) }, " ");
    }

    /**
     * <p>
     *     A lowercase username composed of the first_name and last_name joined with a '.'. Some examples are:
     *     <ul>
     *         <li>(template) {@link #firstName()}.{@link #lastName()}</li>
     *         <li>jim.jones</li>
     *         <li>jason.leigh</li>
     *         <li>tracy.jordan</li>
     *     </ul>
     * </p>
     * @return a random two part user name.
     * @see Name#firstName() 
     * @see Name#lastName()
     */
    public String username() {

        String username = StringUtils.join(
                firstName().replaceAll("'", "").toLowerCase(),
                ".",
                lastName().replaceAll("'", "").toLowerCase()
        );

        return StringUtils.deleteWhitespace(username);
    }
    
    /**
     * <p>Returns a blood group such as O−, O+, A-, A+, B-, B+, AB-, AB+</p>
     * @return a blood group such as O−, O+, A-, A+, B-, B+, AB-, AB+
     */
    public String bloodGroup() {
        return faker.fakeValuesService().resolve("name.blood_group", this, faker);
    }
}
