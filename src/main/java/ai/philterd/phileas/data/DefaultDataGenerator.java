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
package ai.philterd.phileas.data;

import ai.philterd.phileas.data.generators.AbstractGenerator;
import ai.philterd.phileas.data.generators.AgeGenerator;
import ai.philterd.phileas.data.generators.BankRoutingNumberGenerator;
import ai.philterd.phileas.data.generators.BitcoinAddressGenerator;
import ai.philterd.phileas.data.generators.CityGenerator;
import ai.philterd.phileas.data.generators.CountyGenerator;
import ai.philterd.phileas.data.generators.CreditCardNumberGenerator;
import ai.philterd.phileas.data.generators.CustomIdGenerator;
import ai.philterd.phileas.data.generators.DateGenerator;
import ai.philterd.phileas.data.generators.DriversLicenseGenerator;
import ai.philterd.phileas.data.generators.EmailAddressGenerator;
import ai.philterd.phileas.data.generators.FirstNameGenerator;
import ai.philterd.phileas.data.generators.FullNameGenerator;
import ai.philterd.phileas.data.generators.HospitalGenerator;
import ai.philterd.phileas.data.generators.IBANGenerator;
import ai.philterd.phileas.data.generators.IPAddressGenerator;
import ai.philterd.phileas.data.generators.MACAddressGenerator;
import ai.philterd.phileas.data.generators.PassportNumberGenerator;
import ai.philterd.phileas.data.generators.PhoneNumberGenerator;
import ai.philterd.phileas.data.generators.SSNGenerator;
import ai.philterd.phileas.data.generators.StateAbbreviationGenerator;
import ai.philterd.phileas.data.generators.StateGenerator;
import ai.philterd.phileas.data.generators.StreetAddressGenerator;
import ai.philterd.phileas.data.generators.SurnameGenerator;
import ai.philterd.phileas.data.generators.TrackingNumberGenerator;
import ai.philterd.phileas.data.generators.URLGenerator;
import ai.philterd.phileas.data.generators.VINGenerator;
import ai.philterd.phileas.data.generators.ZipCodeGenerator;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.List;
import java.util.Random;

/**
 * Default implementation of {@link DataGenerator}.
 */
public class DefaultDataGenerator extends AbstractGenerator<Object> implements DataGenerator {

    private final List<String> firstNamesList;
    private final List<String> surnamesList;
    private final Random random;

    /**
     * Generator for first names.
     */
    public final Generator<String> firstNames;

    /**
     * Generator for surnames.
     */
    public final Generator<String> surnames;

    /**
     * Generator for full names.
     */
    public final Generator<String> fullNames;

    /**
     * Generator for SSNs.
     */
    public final Generator<String> ssn;

    /**
     * Generator for phone numbers.
     */
    public final Generator<String> phoneNumbers;

    /**
     * Generator for email addresses.
     */
    public final Generator<String> emailAddresses;

    /**
     * Generator for age.
     */
    public final Generator<Integer> age;

    /**
     * Generator for bank routing numbers.
     */
    public final Generator<String> bankRoutingNumbers;

    /**
     * Generator for credit card numbers.
     */
    public final Generator<String> creditCardNumbers;

    /**
     * Generator for dates.
     */
    public final Generator<String> dates;

    /**
     * Generator for IBANs.
     */
    public final Generator<String> iban;

    /**
     * Generator for IP addresses.
     */
    public final Generator<String> ipAddresses;

    /**
     * Generator for MAC addresses.
     */
    public final Generator<String> macAddresses;

    /**
     * Generator for passport numbers.
     */
    public final Generator<String> passportNumbers;

    /**
     * Generator for states.
     */
    public final Generator<String> states;

    /**
     * Generator for state abbreviations.
     */
    public final Generator<String> stateAbbreviations;

    /**
     * Generator for zip codes.
     */
    public final Generator<String> zipCodes;

    /**
     * Generator for Bitcoin addresses.
     */
    public final Generator<String> bitcoinAddresses;

    /**
     * Generator for VINs.
     */
    public final Generator<String> vin;

    /**
     * Generator for URLs.
     */
    public final Generator<String> urls;

    /**
     * Generator for driver's license numbers.
     */
    public final Generator<String> driversLicenseNumbers;

    /**
     * Generator for hospitals.
     */
    public final Generator<String> hospitals;

    /**
     * Generator for tracking numbers.
     */
    public final Generator<String> trackingNumbers;

    /**
     * Generator for cities.
     */
    public final Generator<String> cities;

    /**
     * Generator for street addresses.
     */
    public final Generator<String> streetAddresses;

    /**
     * Generator for counties.
     */
    public final Generator<String> counties;

    /**
     * Creates a new DefaultDataGenerator instance.
     * @throws IOException if name data files cannot be loaded
     */
    public DefaultDataGenerator() throws IOException {
        this(new SecureRandom());
    }

    /**
     * Creates a new DefaultDataGenerator instance with a given {@link Random}.
     * @param random The {@link Random} to use.
     * @throws IOException if name data files cannot be loaded
     */
    public DefaultDataGenerator(final Random random) throws IOException {
        this.random = random;
        this.firstNamesList = loadNames("/first-names.txt");
        this.surnamesList = loadNames("/surnames.txt");

        this.firstNames = new FirstNameGenerator(firstNamesList, random);
        this.surnames = new SurnameGenerator(surnamesList, random);
        this.fullNames = new FullNameGenerator(firstNames, surnames);
        this.ssn = new SSNGenerator(random);
        this.phoneNumbers = new PhoneNumberGenerator(random);
        this.emailAddresses = new EmailAddressGenerator(firstNames, surnames, random);
        this.age = new AgeGenerator(random);
        this.bankRoutingNumbers = new BankRoutingNumberGenerator(random);
        this.creditCardNumbers = new CreditCardNumberGenerator(random);
        this.dates = new DateGenerator(random);
        this.iban = new IBANGenerator(random);
        this.ipAddresses = new IPAddressGenerator(random);
        this.macAddresses = new MACAddressGenerator(random);
        this.passportNumbers = new PassportNumberGenerator(random);
        this.states = new StateGenerator(random);
        this.stateAbbreviations = new StateAbbreviationGenerator(random);
        this.zipCodes = new ZipCodeGenerator(random);
        this.bitcoinAddresses = new BitcoinAddressGenerator(random);
        this.vin = new VINGenerator(random);
        this.urls = new URLGenerator(firstNames, random);
        this.driversLicenseNumbers = new DriversLicenseGenerator(random);
        this.hospitals = new HospitalGenerator(random);
        this.trackingNumbers = new TrackingNumberGenerator(random);
        this.cities = new CityGenerator(loadNames("/cities.txt"), random);
        this.streetAddresses = new StreetAddressGenerator(surnames, random);
        this.counties = new CountyGenerator(loadNames("/counties.txt"), random);
    }

    @Override public Generator<String> firstNames() { return firstNames; }
    @Override public Generator<String> surnames() { return surnames; }
    @Override public Generator<String> fullNames() { return fullNames; }
    @Override public Generator<String> ssn() { return ssn; }
    @Override public Generator<String> phoneNumbers() { return phoneNumbers; }
    @Override public Generator<String> emailAddresses() { return emailAddresses; }
    @Override public Generator<Integer> age() { return age; }
    @Override public Generator<String> bankRoutingNumbers() { return bankRoutingNumbers; }
    @Override public Generator<String> creditCardNumbers() { return creditCardNumbers; }
    @Override public Generator<String> dates() { return dates; }
    @Override public Generator<String> iban() { return iban; }
    @Override public Generator<String> ipAddresses() { return ipAddresses; }
    @Override public Generator<String> macAddresses() { return macAddresses; }
    @Override public Generator<String> passportNumbers() { return passportNumbers; }
    @Override public Generator<String> states() { return states; }
    @Override public Generator<String> stateAbbreviations() { return stateAbbreviations; }
    @Override public Generator<String> zipCodes() { return zipCodes; }
    @Override public Generator<String> bitcoinAddresses() { return bitcoinAddresses; }
    @Override public Generator<String> vin() { return vin; }
    @Override public Generator<String> urls() { return urls; }
    @Override public Generator<String> driversLicenseNumbers() { return driversLicenseNumbers; }
    @Override public Generator<String> hospitals() { return hospitals; }
    @Override public Generator<String> trackingNumbers() { return trackingNumbers; }
    @Override public Generator<String> streetAddresses() { return streetAddresses; }
    @Override public Generator<String> cities() { return cities; }
    @Override public Generator<String> counties() { return counties; }
    @Override public Generator<String> customId(final String pattern) { return new CustomIdGenerator(random, pattern); }
    @Override public Generator<String> dates(final String pattern) { return new DateGenerator(random, 1970, 2030, pattern); }

    @Override
    public Object random() {
        return null;
    }

    @Override
    public long poolSize() {
        return 0;
    }

}
