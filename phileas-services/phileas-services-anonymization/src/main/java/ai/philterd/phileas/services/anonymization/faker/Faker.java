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

import ai.philterd.phileas.services.anonymization.faker.service.FakeValuesService;
import ai.philterd.phileas.services.anonymization.faker.service.RandomService;

import java.util.Locale;
import java.util.Random;

/**
 * Provides utility methods for generating fake strings, such as names, phone
 * numbers, addresses. generate random strings with given patterns
 *
 * @author ren
 */
public class Faker {
    private final RandomService randomService;
    private final FakeValuesService fakeValuesService;

    private final Lorem lorem;
    private final Name name;
    private final ai.philterd.phileas.services.anonymization.faker.Number number;
    private final Internet internet;
    private final ai.philterd.phileas.services.anonymization.faker.PhoneNumber phoneNumber;
    private final Address address;
    private final Business business;
    private final ai.philterd.phileas.services.anonymization.faker.Commerce commerce;
    private final ai.philterd.phileas.services.anonymization.faker.Country country;
    private final ai.philterd.phileas.services.anonymization.faker.Currency currency;
    private final ai.philterd.phileas.services.anonymization.faker.Company company;
    private final Crypto crypto;
    private final IdNumber idNumber;
    private final Options options;
    private final Code code;
    private final ai.philterd.phileas.services.anonymization.faker.Finance finance;
    private final DateAndTime dateAndTime;
    private final ai.philterd.phileas.services.anonymization.faker.Demographic demographic;
    private final Educator educator;
    private final Bool bool;
    private final ai.philterd.phileas.services.anonymization.faker.File file;
    private final ai.philterd.phileas.services.anonymization.faker.Job job;
    private final Medical medical;
    private final ai.philterd.phileas.services.anonymization.faker.Nation nation;
    private final Disease disease;
    private final Barcode barcode;

    public Faker() {
        this(Locale.ENGLISH);
    }

    public Faker(Locale locale) {
        this(locale, (Random)null);
    }

    public Faker(Random random) {
        this(Locale.ENGLISH, random);
    }

    public Faker(Locale locale, Random random) {
        this(locale, new RandomService(random));
    }

    public Faker(Locale locale, RandomService randomService) {
        this(new FakeValuesService(locale, randomService), randomService);
    }

    public Faker(FakeValuesService fakeValuesService, RandomService random) {
        this.randomService = random;
        this.fakeValuesService = fakeValuesService;

        this.lorem = new Lorem(this);
        this.name = new Name(this);
        this.number = new ai.philterd.phileas.services.anonymization.faker.Number(this);
        this.internet = new Internet(this);
        this.phoneNumber = new ai.philterd.phileas.services.anonymization.faker.PhoneNumber(this);
        this.address = new Address(this);
        this.business = new Business(this);
        this.idNumber = new IdNumber(this);
        this.company = new ai.philterd.phileas.services.anonymization.faker.Company(this);
        this.crypto = new Crypto(this);
        this.commerce = new ai.philterd.phileas.services.anonymization.faker.Commerce(this);
        this.currency = new ai.philterd.phileas.services.anonymization.faker.Currency(this);
        this.options = new Options(this);
        this.code = new Code(this);
        this.file = new ai.philterd.phileas.services.anonymization.faker.File(this);
        this.finance = new ai.philterd.phileas.services.anonymization.faker.Finance(this);
        this.dateAndTime = new DateAndTime(this);
        this.demographic = new ai.philterd.phileas.services.anonymization.faker.Demographic(this);
        this.educator = new Educator(this);
        this.bool = new Bool(this);
        this.job = new ai.philterd.phileas.services.anonymization.faker.Job(this);
        this.medical = new Medical(this);
        this.country = new ai.philterd.phileas.services.anonymization.faker.Country(this);
        this.nation = new ai.philterd.phileas.services.anonymization.faker.Nation(this);
        this.disease = new Disease(this);
        this.barcode = new Barcode(this);
    }

    /**
     * Constructs Faker instance with default argument.
     *
     * @return {@link Faker#Faker()}
     */
    public static Faker instance() {
        return new Faker();
    }

    /**
     * Constructs Faker instance with provided {@link Locale}.
     *
     * @param locale - {@link Locale}
     * @return {@link Faker#Faker(Locale)}
     */
    public static Faker instance(Locale locale) {
        return new Faker(locale);
    }

    /**
     * Constructs Faker instance with provided {@link Random}.
     *
     * @param random - {@link Random}
     * @return {@link Faker#Faker(Random)}
     */
    public static Faker instance(Random random) {
        return new Faker(random);
    }

    /**
     * Constructs Faker instance with provided {@link Locale} and {@link Random}.
     *
     * @param locale - {@link Locale}
     * @param random - {@link Random}
     * @return {@link Faker#Faker(Locale, Random)}
     */
    public static Faker instance(Locale locale, Random random) {
        return new Faker(locale, random);
    }

    /**
     * Returns a string with the '#' characters in the parameter replaced with random digits between 0-9 inclusive.
     * <p>
     * For example, the string "ABC##EFG" could be replaced with a string like "ABC99EFG".
     *
     * @param numberString
     * @return
     */
    public String numerify(String numberString) {
        return fakeValuesService.numerify(numberString);
    }

    /**
     * Returns a string with the '?' characters in the parameter replaced with random alphabetic
     * characters.
     * <p>
     * For example, the string "12??34" could be replaced with a string like "12AB34".
     *
     * @param letterString
     * @return
     */
    public String letterify(String letterString) {
        return fakeValuesService.letterify(letterString);
    }

    /**
     * Returns a string with the '?' characters in the parameter replaced with random alphabetic
     * characters.
     * <p>
     * For example, the string "12??34" could be replaced with a string like "12AB34".
     *
     * @param letterString
     * @param isUpper
     * @return
     */
    public String letterify(String letterString, boolean isUpper) {
        return fakeValuesService.letterify(letterString, isUpper);
    }

    /**
     * Applies both a {@link #numerify(String)} and a {@link #letterify(String)}
     * over the incoming string.
     *
     * @param string
     * @return
     */
    public String bothify(String string) {
        return fakeValuesService.bothify(string);
    }

    /**
     * Applies both a {@link #numerify(String)} and a {@link #letterify(String)}
     * over the incoming string.
     *
     * @param string
     * @param isUpper
     * @return
     */
    public String bothify(String string, boolean isUpper) {
        return fakeValuesService.bothify(string, isUpper);
    }

    /**
     * Generates a String that matches the given regular expression.
     */
    public String regexify(String regex) {
        return fakeValuesService.regexify(regex);
    }

    public RandomService random() {
        return this.randomService;
    }

    public Currency currency() {
        return currency;

    }

    FakeValuesService fakeValuesService() {
        return this.fakeValuesService;
    }

    public Name name() {
        return name;
    }

    public Number number() {
        return number;
    }

    public Internet internet() {
        return internet;
    }

    public PhoneNumber phoneNumber() {
        return phoneNumber;
    }

    public Lorem lorem() {
        return lorem;
    }

    public Address address() {
        return address;
    }

    public Business business() {
        return business;
    }

    public Commerce commerce() {
        return commerce;
    }

    public Company company() {
        return company;
    }

    public Crypto crypto() {
        return crypto;
    }

    public IdNumber idNumber() {
        return idNumber;
    }

    public Options options() {
        return options;
    }

    public Code code() {
        return code;
    }

    public File file() {
        return file;
    }

    public Finance finance() {
        return finance;
    }

    public DateAndTime date() {
        return dateAndTime;
    }

    public Demographic demographic() {
        return demographic;
    }

    public Educator educator() {
        return educator;
    }

    public Bool bool() {
        return bool;
    }

    public Job job() {
        return job;
    }

    public Medical medical() {
        return medical;
    }

    public Country country() {
        return country;
    }

    public Nation nation() {
        return nation;
    }

    public Disease disease() {return disease; }

    public Barcode barcode() { return barcode; }

    public String resolve(String key) {
        return this.fakeValuesService.resolve(key, this, this);
    }

    /**
     * Allows the evaluation of native YML expressions to allow you to build your own.
     * <p>
     * The following are valid expressions:
     * <ul>
     * <li>#{regexify '(a|b){2,3}'}</li>
     * <li>#{regexify '\\.\\*\\?\\+'}</li>
     * <li>#{bothify '????','false'}</li>
     * <li>#{Name.first_name} #{Name.first_name} #{Name.last_name}</li>
     * <li>#{number.number_between '1','10'}</li>
     * </ul>
     *
     * @param expression (see examples above)
     * @return the evaluated string expression
     * @throws RuntimeException if unable to evaluate the expression
     */
    public String expression(String expression) {
        return this.fakeValuesService.expression(expression, this);
    }
}
