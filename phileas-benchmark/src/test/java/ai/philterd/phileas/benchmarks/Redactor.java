/*
 *     Copyright 2024 Philterd, LLC @ https://www.philterd.ai
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

package ai.philterd.phileas.benchmarks;

import ai.philterd.phileas.model.configuration.PhileasConfiguration;
import ai.philterd.phileas.model.enums.MimeType;
import ai.philterd.phileas.model.policy.Identifiers;
import ai.philterd.phileas.model.policy.Policy;
import ai.philterd.phileas.model.policy.filters.BankRoutingNumber;
import ai.philterd.phileas.model.policy.filters.BitcoinAddress;
import ai.philterd.phileas.model.policy.filters.CreditCard;
import ai.philterd.phileas.model.policy.filters.DriversLicense;
import ai.philterd.phileas.model.policy.filters.EmailAddress;
import ai.philterd.phileas.model.policy.filters.IbanCode;
import ai.philterd.phileas.model.policy.filters.IpAddress;
import ai.philterd.phileas.model.policy.filters.PassportNumber;
import ai.philterd.phileas.model.policy.filters.PhoneNumber;
import ai.philterd.phileas.model.policy.filters.Ssn;
import ai.philterd.phileas.model.policy.filters.TrackingNumber;
import ai.philterd.phileas.model.policy.filters.Vin;
import ai.philterd.phileas.model.policy.filters.strategies.AbstractFilterStrategy;
import ai.philterd.phileas.model.policy.filters.strategies.rules.BankRoutingNumberFilterStrategy;
import ai.philterd.phileas.model.policy.filters.strategies.rules.BitcoinAddressFilterStrategy;
import ai.philterd.phileas.model.policy.filters.strategies.rules.CreditCardFilterStrategy;
import ai.philterd.phileas.model.policy.filters.strategies.rules.DriversLicenseFilterStrategy;
import ai.philterd.phileas.model.policy.filters.strategies.rules.EmailAddressFilterStrategy;
import ai.philterd.phileas.model.policy.filters.strategies.rules.IbanCodeFilterStrategy;
import ai.philterd.phileas.model.policy.filters.strategies.rules.IpAddressFilterStrategy;
import ai.philterd.phileas.model.policy.filters.strategies.rules.PassportNumberFilterStrategy;
import ai.philterd.phileas.model.policy.filters.strategies.rules.PhoneNumberFilterStrategy;
import ai.philterd.phileas.model.policy.filters.strategies.rules.SsnFilterStrategy;
import ai.philterd.phileas.model.policy.filters.strategies.rules.TrackingNumberFilterStrategy;
import ai.philterd.phileas.model.policy.filters.strategies.rules.VinFilterStrategy;
import ai.philterd.phileas.model.responses.FilterResponse;
import ai.philterd.phileas.services.PhileasFilterService;

import java.util.List;
import java.util.Properties;

/**
 * Single-threaded redactor using Phileas PII engine.
 */
public class Redactor {

    public Redactor(String name) throws Exception {
        boolean all = "mask_all".equals(name);
        boolean fastest = "mask_fastest".equals(name);
        boolean valid = "mask_none".equals(name);
        Identifiers identifiers = new Identifiers();

        if (all || fastest || "mask_bank_routing_numbers".equals(name)) {
            BankRoutingNumberFilterStrategy fs = new BankRoutingNumberFilterStrategy();
            fs.setStrategy(AbstractFilterStrategy.MASK);
            fs.setMaskCharacter("*");
            fs.setMaskLength(AbstractFilterStrategy.SAME);
            BankRoutingNumber x = new BankRoutingNumber();
            x.setBankRoutingNumberFilterStrategies(List.of(fs));
            identifiers.setBankRoutingNumber(x);
            valid = true;
        }

        if (all || fastest || "mask_bitcoin_addresses".equals(name)) {
            BitcoinAddressFilterStrategy fs = new BitcoinAddressFilterStrategy();
            fs.setStrategy(AbstractFilterStrategy.MASK);
            fs.setMaskCharacter("*");
            fs.setMaskLength(AbstractFilterStrategy.SAME);
            BitcoinAddress x = new BitcoinAddress();
            x.setBitcoinFilterStrategies(List.of(fs));
            identifiers.setBitcoinAddress(x);
            valid = true;
        }

        if (all || fastest || "mask_credit_cards".equals(name)) {
            CreditCardFilterStrategy fs = new CreditCardFilterStrategy();
            fs.setStrategy(AbstractFilterStrategy.MASK);
            fs.setMaskCharacter("*");
            fs.setMaskLength(AbstractFilterStrategy.SAME);
            CreditCard x = new CreditCard();
            x.setCreditCardFilterStrategies(List.of(fs));
            identifiers.setCreditCard(x);
            valid = true;
        }

        if (all || "mask_drivers_licenses".equals(name)) {
            DriversLicenseFilterStrategy fs = new DriversLicenseFilterStrategy();
            fs.setStrategy(AbstractFilterStrategy.MASK);
            fs.setMaskCharacter("*");
            fs.setMaskLength(AbstractFilterStrategy.SAME);
            DriversLicense x = new DriversLicense();
            x.setDriversLicenseFilterStrategies(List.of(fs));
            identifiers.setDriversLicense(x);
            valid = true;
        }

        if (all || fastest || "mask_email_addresses".equals(name)) {
            EmailAddressFilterStrategy fs = new EmailAddressFilterStrategy();
            fs.setStrategy(AbstractFilterStrategy.MASK);
            fs.setMaskCharacter("*");
            fs.setMaskLength(AbstractFilterStrategy.SAME);
            EmailAddress x = new EmailAddress();
            x.setEmailAddressFilterStrategies(List.of(fs));
            identifiers.setEmailAddress(x);
            valid = true;
        }

        if (all || fastest || "mask_iban_codes".equals(name)) {
            IbanCodeFilterStrategy fs = new IbanCodeFilterStrategy();
            fs.setStrategy(AbstractFilterStrategy.MASK);
            fs.setMaskCharacter("*");
            fs.setMaskLength(AbstractFilterStrategy.SAME);
            IbanCode x = new IbanCode();
            x.setIbanCodeFilterStrategies(List.of(fs));
            identifiers.setIbanCode(x);
            valid = true;
        }

        if (all || "mask_ip_addresses".equals(name)) {
            IpAddressFilterStrategy fs = new IpAddressFilterStrategy();
            fs.setStrategy(AbstractFilterStrategy.MASK);
            fs.setMaskCharacter("*");
            fs.setMaskLength(AbstractFilterStrategy.SAME);
            IpAddress x = new IpAddress();
            x.setIpAddressFilterStrategies(List.of(fs));
            identifiers.setIpAddress(x);
            valid = true;
        }

        if (all || "mask_passport_numbers".equals(name)) {
            PassportNumberFilterStrategy fs = new PassportNumberFilterStrategy();
            fs.setStrategy(AbstractFilterStrategy.MASK);
            fs.setMaskCharacter("*");
            fs.setMaskLength(AbstractFilterStrategy.SAME);
            PassportNumber x = new PassportNumber();
            x.setPassportNumberFilterStrategies(List.of(fs));
            identifiers.setPassportNumber(x);
            valid = true;
        }

        if (all || fastest || "mask_phone_numbers".equals(name)) {
            PhoneNumberFilterStrategy fs = new PhoneNumberFilterStrategy();
            fs.setStrategy(AbstractFilterStrategy.MASK);
            fs.setMaskCharacter("*");
            fs.setMaskLength(AbstractFilterStrategy.SAME);
            PhoneNumber x = new PhoneNumber();
            x.setPhoneNumberFilterStrategies(List.of(fs));
            identifiers.setPhoneNumber(x);
            valid = true;
        }

        if (all || fastest || "mask_ssns".equals(name)) {
            SsnFilterStrategy fs = new SsnFilterStrategy();
            fs.setStrategy(AbstractFilterStrategy.MASK);
            fs.setMaskCharacter("*");
            fs.setMaskLength(AbstractFilterStrategy.SAME);
            Ssn x = new Ssn();
            x.setSsnFilterStrategies(List.of(fs));
            identifiers.setSsn(x);
            valid = true;
        }

        if (all || "mask_tracking_numbers".equals(name)) {
            TrackingNumberFilterStrategy fs = new TrackingNumberFilterStrategy();
            fs.setStrategy(AbstractFilterStrategy.MASK);
            fs.setMaskCharacter("*");
            fs.setMaskLength(AbstractFilterStrategy.SAME);
            TrackingNumber x = new TrackingNumber();
            x.setTrackingNumberFilterStrategies(List.of(fs));
            identifiers.setTrackingNumber(x);
            valid = true;
        }

        if (all || "mask_vehicle_numbers".equals(name)) {
            VinFilterStrategy fs = new VinFilterStrategy();
            fs.setStrategy(AbstractFilterStrategy.MASK);
            fs.setMaskCharacter("*");
            fs.setMaskLength(AbstractFilterStrategy.SAME);
            Vin x = new Vin();
            x.setVinFilterStrategies(List.of(fs));
            identifiers.setVin(x);
            valid = true;
        }

        // quit if name parameter didn't match
        if (!valid) throw new IllegalArgumentException("Invalid redactor: " + name);

        // create filter service
        this.policy = new Policy();
        policy.setName("default");
        policy.setIdentifiers(identifiers);
        Properties properties = new Properties();
        PhileasConfiguration configuration = new PhileasConfiguration(properties, "phileas");
        this.filterService = new PhileasFilterService(configuration);
    }

    private final PhileasFilterService filterService;
    private final Policy policy;

    public FilterResponse filter(String s) throws Exception {
        return filterService.filter(policy, "context_id", "document_id", s, MimeType.TEXT_PLAIN);
    }

}
