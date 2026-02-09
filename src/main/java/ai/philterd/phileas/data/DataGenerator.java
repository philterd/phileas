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

/**
 * Interface for generating fake PII data.
 */
public interface DataGenerator {

    /**
     * Interface for a generator that can provide a random value and its pool size.
     * @param <T> The type of data generated.
     */
    interface Generator<T> {
        
        /**
         * Generates a random value.
         * @return A random value.
         */
        T random();

        /**
         * Gets the maximum number of unique values that can be generated.
         * @return The maximum pool size.
         */
        long poolSize();

    }

    /**
     * Gets a generator for first names.
     * @return A generator for first names.
     */
    Generator<String> firstNames();

    /**
     * Gets a generator for surnames.
     * @return A generator for surnames.
     */
    Generator<String> surnames();

    /**
     * Gets a generator for full names.
     * @return A generator for full names.
     */
    Generator<String> fullNames();

    /**
     * Gets a generator for Social Security Numbers (SSN).
     * @return A generator for Social Security Numbers (SSN).
     */
    Generator<String> ssn();

    /**
     * Gets a generator for phone numbers.
     * @return A generator for phone numbers.
     */
    Generator<String> phoneNumbers();

    /**
     * Gets a generator for email addresses.
     * @return A generator for email addresses.
     */
    Generator<String> emailAddresses();

    /**
     * Gets a generator for ages.
     * @return A generator for ages.
     */
    Generator<Integer> age();

    /**
     * Gets a generator for bank routing numbers.
     * @return A generator for bank routing numbers.
     */
    Generator<String> bankRoutingNumbers();

    /**
     * Gets a generator for credit card numbers.
     * @return A generator for credit card numbers.
     */
    Generator<String> creditCardNumbers();

    /**
     * Gets a generator for dates.
     * @return A generator for dates.
     */
    Generator<String> dates();

    /**
     * Gets a generator for IBANs.
     * @return A generator for IBANs.
     */
    Generator<String> iban();

    /**
     * Gets a generator for IP addresses.
     * @return A generator for IP addresses.
     */
    Generator<String> ipAddresses();

    /**
     * Gets a generator for MAC addresses.
     * @return A generator for MAC addresses.
     */
    Generator<String> macAddresses();

    /**
     * Gets a generator for passport numbers.
     * @return A generator for passport numbers.
     */
    Generator<String> passportNumbers();

    /**
     * Gets a generator for states.
     * @return A generator for states.
     */
    Generator<String> states();

    /**
     * Gets a generator for state abbreviations.
     * @return A generator for state abbreviations.
     */
    Generator<String> stateAbbreviations();

    /**
     * Gets a generator for zip codes.
     * @return A generator for zip codes.
     */
    Generator<String> zipCodes();

    /**
     * Gets a generator for Bitcoin addresses.
     * @return A generator for Bitcoin addresses.
     */
    Generator<String> bitcoinAddresses();

    /**
     * Gets a generator for Vehicle Identification Numbers (VIN).
     * @return A generator for Vehicle Identification Numbers (VIN).
     */
    Generator<String> vin();

    /**
     * Gets a generator for URLs.
     * @return A generator for URLs.
     */
    Generator<String> urls();

    /**
     * Gets a generator for driver's license numbers.
     * @return A generator for driver's license numbers.
     */
    Generator<String> driversLicenseNumbers();

    /**
     * Gets a generator for hospital names.
     * @return A generator for hospital names.
     */
    Generator<String> hospitals();

    /**
     * Gets a generator for hospital abbreviations.
     * @return A generator for hospital abbreviations.
     */
    Generator<String> hospitalAbbreviations();

    /**
     * Gets a generator for tracking numbers.
     * @return A generator for tracking numbers.
     */
    Generator<String> trackingNumbers();

    /**
     * Gets a generator for cities.
     * @return A generator for cities.
     */
    Generator<String> cities();

    /**
     * Gets a generator for counties.
     * @return A generator for counties.
     */
    Generator<String> counties();

    /**
     * Gets a generator for custom IDs matching a pattern.
     * @param pattern The pattern to use for generation.
     * @return A generator for custom IDs.
     */
    Generator<String> customId(String pattern);

    /**
     * Gets a generator for dates with a custom format.
     * @param pattern The date pattern to use.
     * @return A generator for dates.
     */
    Generator<String> dates(String pattern);

}
