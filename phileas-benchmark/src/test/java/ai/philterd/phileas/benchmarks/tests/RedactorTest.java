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

package ai.philterd.phileas.benchmarks.tests;

import ai.philterd.phileas.benchmarks.Redactor;
import ai.philterd.phileas.model.responses.FilterResponse;
import org.junit.jupiter.api.Test;

import static com.mscharhag.oleaster.matcher.Matchers.expect;

/**
 * Functional tests for redactor implementations.
 */
public class RedactorTest {

    @Test
    public void maskAllTest() throws Exception {
        Redactor r = new Redactor("mask_all");
        FilterResponse fr = r.filter("the payment method is 4532613702852251 visa or 1Lbcfr7sAHTD9CgdQo3HTMTkV8LK4ZnX71 BTC from user rik@resurfacd.io.");
        expect(fr.explanation().appliedSpans().size()).toEqual(3);
        expect(fr.explanation().identifiedSpans().size()).toEqual(3);
        expect(fr.explanation().identifiedSpans().get(0).getFilterType().toString()).toEqual("bitcoin-address");
        expect(fr.explanation().identifiedSpans().get(0).getText()).toEqual("1Lbcfr7sAHTD9CgdQo3HTMTkV8LK4ZnX71");
        expect(fr.explanation().identifiedSpans().get(1).getFilterType().toString()).toEqual("credit-card");
        expect(fr.explanation().identifiedSpans().get(1).getText()).toEqual("4532613702852251");
        expect(fr.explanation().identifiedSpans().get(2).getFilterType().toString()).toEqual("email-address");
        expect(fr.explanation().identifiedSpans().get(2).getText()).toEqual("rik@resurfacd.io");
        expect(fr.filteredText()).toEqual("the payment method is **************** visa or ********************************** BTC from user ****************.");
    }

    @Test
    public void maskBankRoutingNumbersTest() throws Exception {
        Redactor r = new Redactor("mask_bank_routing_numbers");
        FilterResponse fr = r.filter("111000038 is the routing number of the Federal Reserve Bank in Minneapolis");
        expect(fr.explanation().appliedSpans().size()).toEqual(1);
        expect(fr.explanation().identifiedSpans().size()).toEqual(1);
        expect(fr.explanation().identifiedSpans().get(0).getConfidence()).toEqual(0.95);
        expect(fr.explanation().identifiedSpans().get(0).getFilterType().toString()).toEqual("bank-routing-number");
        expect(fr.explanation().identifiedSpans().get(0).getText()).toEqual("111000038");
        expect(fr.filteredText()).toEqual("********* is the routing number of the Federal Reserve Bank in Minneapolis");
    }

    @Test
    public void maskBitcoinAddressesTest() throws Exception {
        Redactor r = new Redactor("mask_bitcoin_addresses");
        FilterResponse fr = r.filter("the payment method is 1Lbcfr7sAHTD9CgdQo3HTMTkV8LK4ZnX71 BTC from user rik@resurfacd.io.");
        expect(fr.explanation().appliedSpans().size()).toEqual(1);
        expect(fr.explanation().identifiedSpans().size()).toEqual(1);
        expect(fr.explanation().identifiedSpans().get(0).getConfidence()).toEqual(0.9);
        expect(fr.explanation().identifiedSpans().get(0).getFilterType().toString()).toEqual("bitcoin-address");
        expect(fr.explanation().identifiedSpans().get(0).getText()).toEqual("1Lbcfr7sAHTD9CgdQo3HTMTkV8LK4ZnX71");
        expect(fr.filteredText()).toEqual("the payment method is ********************************** BTC from user rik@resurfacd.io.");
    }

    @Test
    public void maskCreditCardsTest() throws Exception {
        Redactor r = new Redactor("mask_credit_cards");
        FilterResponse fr = r.filter("the payment method is 4532613702852251 visa from user rik@resurfacd.io.");
        expect(fr.explanation().appliedSpans().size()).toEqual(1);
        expect(fr.explanation().identifiedSpans().size()).toEqual(1);
        expect(fr.explanation().identifiedSpans().get(0).getConfidence()).toEqual(0.9);
        expect(fr.explanation().identifiedSpans().get(0).getFilterType().toString()).toEqual("credit-card");
        expect(fr.explanation().identifiedSpans().get(0).getText()).toEqual("4532613702852251");
        expect(fr.filteredText()).toEqual("the payment method is **************** visa from user rik@resurfacd.io.");
    }

    @Test
    public void maskDriversLicensesTest() throws Exception {
        Redactor r = new Redactor("mask_drivers_licenses");
        FilterResponse fr = r.filter("the license number is 94-33-0101 from Colorado");
        expect(fr.explanation().appliedSpans().size()).toEqual(1);
        expect(fr.explanation().identifiedSpans().size()).toEqual(1);
        expect(fr.explanation().identifiedSpans().get(0).getConfidence()).toEqual(0.5);  // todo low?
        expect(fr.explanation().identifiedSpans().get(0).getFilterType().toString()).toEqual("drivers-license-number");
        expect(fr.explanation().identifiedSpans().get(0).getText()).toEqual("0101");
        expect(fr.filteredText()).toEqual("the license number is 94-33-**** from Colorado"); // todo not completely masked
    }

    @Test
    public void maskEmailAddressesTest() throws Exception {
        Redactor r = new Redactor("mask_email_addresses");
        FilterResponse fr = r.filter("the payment method is 4532613702852251 visa from user rik@resurfacd.io.");
        expect(fr.explanation().appliedSpans().size()).toEqual(1);
        expect(fr.explanation().identifiedSpans().size()).toEqual(1);
        expect(fr.explanation().identifiedSpans().get(0).getConfidence()).toEqual(0.9);
        expect(fr.explanation().identifiedSpans().get(0).getFilterType().toString()).toEqual("email-address");
        expect(fr.explanation().identifiedSpans().get(0).getText()).toEqual("rik@resurfacd.io");
        expect(fr.filteredText()).toEqual("the payment method is 4532613702852251 visa from user ****************.");
    }

    @Test
    public void maskIbanCodesTest() throws Exception {
        Redactor r = new Redactor("mask_iban_codes");
        FilterResponse fr = r.filter("the iban code for Germany is DE89 3704 0044 0532 0130 00");
        expect(fr.explanation().appliedSpans().size()).toEqual(1);
        expect(fr.explanation().identifiedSpans().size()).toEqual(1);
        expect(fr.explanation().identifiedSpans().get(0).getConfidence()).toEqual(0.9);
        expect(fr.explanation().identifiedSpans().get(0).getFilterType().toString()).toEqual("iban-code");
        expect(fr.explanation().identifiedSpans().get(0).getText()).toEqual("DE89 3704 0044 0532 0130 00");
        expect(fr.filteredText()).toEqual("the iban code for Germany is ***************************");
    }

    @Test
    public void maskIpAddressesTest() throws Exception {
        Redactor r = new Redactor("mask_ip_addresses");
        FilterResponse fr = r.filter("the private ip address for my machine is 192.158.1.38");
        expect(fr.explanation().appliedSpans().size()).toEqual(1);
        expect(fr.explanation().identifiedSpans().size()).toEqual(1);
        expect(fr.explanation().identifiedSpans().get(0).getConfidence()).toEqual(0.9);
        expect(fr.explanation().identifiedSpans().get(0).getFilterType().toString()).toEqual("ip-address");
        expect(fr.explanation().identifiedSpans().get(0).getText()).toEqual("192.158.1.38");
        expect(fr.filteredText()).toEqual("the private ip address for my machine is ************");
    }

    @Test
    public void maskPassportNumbersTest() throws Exception {
        Redactor r = new Redactor("mask_passport_numbers");
        FilterResponse fr = r.filter("my passport number is 05954348 (not really)"); // todo not working with my real passport number
        expect(fr.explanation().appliedSpans().size()).toEqual(1);
        expect(fr.explanation().identifiedSpans().size()).toEqual(1);
        expect(fr.explanation().identifiedSpans().get(0).getConfidence()).toEqual(0.9);
        expect(fr.explanation().identifiedSpans().get(0).getFilterType().toString()).toEqual("passport-number");
        expect(fr.explanation().identifiedSpans().get(0).getText()).toEqual("05954348");
        expect(fr.filteredText()).toEqual("my passport number is ******** (not really)");
    }

    @Test
    public void maskPhoneNumbersTest() throws Exception {
        Redactor r = new Redactor("mask_phone_numbers");
        FilterResponse fr = r.filter("call me at 1-800-123-5678 x3321");
        expect(fr.explanation().appliedSpans().size()).toEqual(1);
        expect(fr.explanation().identifiedSpans().size()).toEqual(1);
        expect(fr.explanation().identifiedSpans().get(0).getConfidence()).toEqual(0.75);
        expect(fr.explanation().identifiedSpans().get(0).getFilterType().toString()).toEqual("phone-number");
        expect(fr.explanation().identifiedSpans().get(0).getText()).toEqual("1-800-123-5678 x3321");
        expect(fr.filteredText()).toEqual("call me at ********************");
    }

    @Test
    public void maskSSNsTest() throws Exception {
        Redactor r = new Redactor("mask_ssns");
        FilterResponse fr = r.filter("my ssn is 123-45-7027, not really");
        expect(fr.explanation().appliedSpans().size()).toEqual(1);
        expect(fr.explanation().identifiedSpans().size()).toEqual(1);
        expect(fr.explanation().identifiedSpans().get(0).getConfidence()).toEqual(0.9);
        expect(fr.explanation().identifiedSpans().get(0).getFilterType().toString()).toEqual("ssn");
        expect(fr.explanation().identifiedSpans().get(0).getText()).toEqual("123-45-7027");
        expect(fr.filteredText()).toEqual("my ssn is ***********, not really");
    }

    @Test
    public void maskTrackingNumbersTest() throws Exception {
        Redactor r = new Redactor("mask_tracking_numbers");
        FilterResponse fr = r.filter("the UPS tracking number for your order is 1z1234567890123456");
        expect(fr.explanation().appliedSpans().size()).toEqual(1);
        expect(fr.explanation().identifiedSpans().size()).toEqual(1);
        expect(fr.explanation().identifiedSpans().get(0).getConfidence()).toEqual(0.9);
        expect(fr.explanation().identifiedSpans().get(0).getFilterType().toString()).toEqual("tracking-number");
        expect(fr.explanation().identifiedSpans().get(0).getText()).toEqual("1z1234567890123456");
        expect(fr.filteredText()).toEqual("the UPS tracking number for your order is ******************");
    }

    @Test
    public void maskVehicleNumbersTest() throws Exception {
        Redactor r = new Redactor("mask_vehicle_numbers");
        FilterResponse fr = r.filter("my fake car's vin is WVWHP7AN0CE516562");
        expect(fr.explanation().appliedSpans().size()).toEqual(1);
        expect(fr.explanation().identifiedSpans().size()).toEqual(1);
        expect(fr.explanation().identifiedSpans().get(0).getConfidence()).toEqual(0.9);
        expect(fr.explanation().identifiedSpans().get(0).getFilterType().toString()).toEqual("vin");
        expect(fr.explanation().identifiedSpans().get(0).getText()).toEqual("WVWHP7AN0CE516562");
        expect(fr.filteredText()).toEqual("my fake car's vin is *****************");
    }

    @Test
    public void skipAllTest() throws Exception {
        Redactor r = new Redactor("mask_none");
        String value = "the payment method is 4532613702852251 visa or 1Lbcfr7sAHTD9CgdQo3HTMTkV8LK4ZnX71 BTC from user rik@resurfacd.io.";
        FilterResponse fr = r.filter(value);
        expect(fr.explanation().appliedSpans().size()).toEqual(0);
        expect(fr.explanation().identifiedSpans().size()).toEqual(0);
        expect(fr.filteredText()).toEqual(value);
    }

}
