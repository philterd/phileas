/*
 * Copyright 2026 Philterd, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ai.philterd.phileas.data.generators;

import ai.philterd.phileas.data.DataGenerator;

import java.io.IOException;
import java.util.Random;

public class EmailAddressGenerator extends AbstractGenerator<String> {
    private final DataGenerator.Generator<String> firstNames;
    private final DataGenerator.Generator<String> surnames;
    private final Random random;
    private final String[] domains;
    private static final String[] DEFAULT_DOMAINS = {"gmail.com", "yahoo.com", "hotmail.com", "outlook.com", "example.com"};

    public EmailAddressGenerator() throws IOException {
        this(new Random());
    }

    public EmailAddressGenerator(final Random random) throws IOException {
        this.random = random;
        this.firstNames = new FirstNameGenerator(loadNames("/first-names.txt"), random);
        this.surnames = new SurnameGenerator(loadNames("/surnames.txt"), random);
        this.domains = DEFAULT_DOMAINS;
    }

    public EmailAddressGenerator(final DataGenerator.Generator<String> firstNames, final DataGenerator.Generator<String> surnames, final Random random) {
        this(firstNames, surnames, random, DEFAULT_DOMAINS);
    }

    public EmailAddressGenerator(final DataGenerator.Generator<String> firstNames, final DataGenerator.Generator<String> surnames, final Random random, final String[] domains) {
        this.firstNames = firstNames;
        this.surnames = surnames;
        this.random = random;
        this.domains = domains;
    }

    @Override
    public String random() {
        final String firstName = firstNames.random().toLowerCase();
        final String surname = surnames.random().toLowerCase();
        final String digits = String.format("%03d", random.nextInt(1000));
        final String domain = domains[random.nextInt(domains.length)];
        return firstName + "." + surname + digits + "@" + domain;
    }

    @Override
    public long poolSize() {
        return firstNames.poolSize() * surnames.poolSize() * 1000L * domains.length;
    }

}
