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
package ai.philterd.phileas.services;

import ai.philterd.phileas.PhileasConfiguration;
import ai.philterd.phileas.model.enums.MimeType;
import ai.philterd.phileas.model.objects.Span;
import ai.philterd.phileas.model.responses.FilterResponse;
import ai.philterd.phileas.model.serializers.PlaceholderDeserializer;
import ai.philterd.phileas.model.services.ContextService;
import ai.philterd.phileas.model.services.VectorService;
import ai.philterd.phileas.policy.Identifiers;
import ai.philterd.phileas.policy.Policy;
import ai.philterd.phileas.policy.filters.BitcoinAddress;
import ai.philterd.phileas.policy.filters.CreditCard;
import ai.philterd.phileas.policy.filters.CustomDictionary;
import ai.philterd.phileas.policy.filters.DriversLicense;
import ai.philterd.phileas.services.strategies.AbstractFilterStrategy;
import ai.philterd.phileas.services.strategies.custom.CustomDictionaryFilterStrategy;
import ai.philterd.phileas.services.strategies.rules.BitcoinAddressFilterStrategy;
import ai.philterd.phileas.services.strategies.rules.CreditCardFilterStrategy;
import ai.philterd.phileas.services.strategies.rules.DriversLicenseFilterStrategy;
import com.google.gson.GsonBuilder;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;

import static ai.philterd.phileas.services.EndToEndTestsHelper.getPolicy;
import static ai.philterd.phileas.services.EndToEndTestsHelper.getPolicyJustCreditCard;
import static ai.philterd.phileas.services.EndToEndTestsHelper.getPolicyJustCreditCardNotInUnixTimestamps;
import static ai.philterd.phileas.services.EndToEndTestsHelper.getPolicyJustPhoneNumber;
import static ai.philterd.phileas.services.EndToEndTestsHelper.getPolicyJustStreetAddress;
import static ai.philterd.phileas.services.EndToEndTestsHelper.getPolicyZipCodeWithIgnored;

@Disabled("Some of these tests require a running philter-ner service")
public class EndToEndTests {

    private static final Logger LOGGER = LogManager.getLogger(EndToEndTests.class);

    private final ContextService contextService = Mockito.mock(ContextService.class); 
    private final VectorService vectorService = Mockito.mock(VectorService.class);
    
    @BeforeEach
    public void before() {
        final GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(String.class, new PlaceholderDeserializer());
    }

    @Test
    public void endToEnd1() throws Exception {

        final Properties properties = new Properties();
        final PhileasConfiguration phileasConfiguration = new PhileasConfiguration(properties);

        final Policy policy = getPolicy("default");

        final PhileasFilterService service = new PhileasFilterService(phileasConfiguration, contextService, vectorService);
        final FilterResponse response = service.filter(policy, "context",  "documentid", "George Washington was president and his ssn was 123-45-6789 and he lived at 90210.", MimeType.TEXT_PLAIN);

        LOGGER.info(response.getFilteredText());

        Assertions.assertEquals("{{{REDACTED-person}}} was president and his ssn was {{{REDACTED-ssn}}} and he lived at {{{REDACTED-zip-code}}}.", response.getFilteredText());
        Assertions.assertEquals("documentid", response.getDocumentId());

    }

    @Test
    public void endToEnd2() throws Exception {

        final Properties properties = new Properties();
        final PhileasConfiguration phileasConfiguration = new PhileasConfiguration(properties);

        final Policy policy = getPolicy("default");

        final PhileasFilterService service = new PhileasFilterService(phileasConfiguration, contextService, vectorService);
        final FilterResponse response = service.filter(policy, "context",  "documentid", "My email is test@something.com and cc is 4121742025464465", MimeType.TEXT_PLAIN);

        LOGGER.info(response.getFilteredText());

        Assertions.assertEquals("My email is {{{REDACTED-email-address}}} and cc is {{{REDACTED-credit-card}}}", response.getFilteredText());
        Assertions.assertEquals("documentid", response.getDocumentId());

    }

    @Test
    public void endToEnd3() throws Exception {

        final Properties properties = new Properties();
        final PhileasConfiguration phileasConfiguration = new PhileasConfiguration(properties);

        final Policy policy = getPolicy("default");

        final PhileasFilterService service = new PhileasFilterService(phileasConfiguration, contextService, vectorService);
        final FilterResponse response = service.filter(policy, "context",  "documentid", "test@something.com is email and cc is 4121742025464465", MimeType.TEXT_PLAIN);

        LOGGER.info(response.getFilteredText());

        Assertions.assertEquals("{{{REDACTED-email-address}}} is email and cc is {{{REDACTED-credit-card}}}", response.getFilteredText());
        Assertions.assertEquals("documentid", response.getDocumentId());

    }

    @Test
    public void endToEnd4() throws Exception {

        final Properties properties = new Properties();
        final PhileasConfiguration phileasConfiguration = new PhileasConfiguration(properties);

        final Policy policy = getPolicy("default");

        final PhileasFilterService service = new PhileasFilterService(phileasConfiguration, contextService, vectorService);
        final FilterResponse response = service.filter(policy, "context",  "documentid", "test@something.com", MimeType.TEXT_PLAIN);

        LOGGER.info(response.getFilteredText());

        Assertions.assertEquals("{{{REDACTED-email-address}}}", response.getFilteredText());
        Assertions.assertEquals("documentid", response.getDocumentId());

    }

    @Test
    public void endToEnd5() throws Exception {

        final Properties properties = new Properties();
        final PhileasConfiguration phileasConfiguration = new PhileasConfiguration(properties);

        final Policy policy = getPolicy("default");

        final PhileasFilterService service = new PhileasFilterService(phileasConfiguration, contextService, vectorService);
        final FilterResponse response = service.filter(policy, "context",  "documentid", "90210", MimeType.TEXT_PLAIN);

        LOGGER.info(response.getFilteredText());

        Assertions.assertEquals("{{{REDACTED-zip-code}}}", response.getFilteredText());
        Assertions.assertEquals("documentid", response.getDocumentId());

    }

    @Test
    public void endToEnd6() throws Exception {

        final Properties properties = new Properties();
        final PhileasConfiguration phileasConfiguration = new PhileasConfiguration(properties);

        final Policy policy = getPolicy("default");

        final PhileasFilterService service = new PhileasFilterService(phileasConfiguration, contextService, vectorService);
        final FilterResponse response = service.filter(policy, "context",  "documentid", "his name was JEFF.", MimeType.TEXT_PLAIN);

        LOGGER.info(response.getFilteredText());

        Assertions.assertEquals("his name was {{{REDACTED-person}}}.", response.getFilteredText());
        Assertions.assertEquals("documentid", response.getDocumentId());

    }

    @Test
    public void endToEnd7() throws Exception {

        final Properties properties = new Properties();
        final PhileasConfiguration phileasConfiguration = new PhileasConfiguration(properties);

        final Policy policy = getPolicy("default");

        final PhileasFilterService service = new PhileasFilterService(phileasConfiguration, contextService, vectorService);
        final FilterResponse response = service.filter(policy, "context",  "documentid", "he was seen on 10-19-2020.", MimeType.TEXT_PLAIN);

        LOGGER.info(response.getFilteredText());

        Assertions.assertEquals("he was seen on 12-20-2023.", response.getFilteredText());
        Assertions.assertEquals("documentid", response.getDocumentId());

    }

    @Test
    public void endToEnd8() throws Exception {

        final Properties properties = new Properties();
        final PhileasConfiguration phileasConfiguration = new PhileasConfiguration(properties);

        final Policy policy = getPolicy("default");

        final PhileasFilterService service = new PhileasFilterService(phileasConfiguration, contextService, vectorService);
        final FilterResponse response = service.filter(policy, "context",  "documentid",
                "George Washington was president." + System.lineSeparator() + "Abraham Lincoln was president.", MimeType.TEXT_PLAIN);

        LOGGER.info(response.getFilteredText());

        // Ensure that the line separator in the input text was not removed.
        Assertions.assertTrue(response.getFilteredText().contains(System.lineSeparator()));

    }

    @Test
    public void endToEnd9() throws Exception {

        final Properties properties = new Properties();
        final PhileasConfiguration phileasConfiguration = new PhileasConfiguration(properties);

        final Policy policy = getPolicy("default");

        final PhileasFilterService service = new PhileasFilterService(phileasConfiguration, contextService, vectorService);
        final FilterResponse response = service.filter(policy, "context",  "documentid", "George Washington was president and his ssn was 123-45-6789 and he lived at 90210. The name 456 should be filtered. Jeff Smith should be ignored.", MimeType.TEXT_PLAIN);

        LOGGER.info(response.getFilteredText());

        Assertions.assertEquals("{{{REDACTED-person}}} was president and his ssn was {{{REDACTED-ssn}}} and he lived at {{{REDACTED-zip-code}}}. The name 456 should be filtered. {{{REDACTED-person}}} should be ignored.", response.getFilteredText());
        Assertions.assertEquals("documentid", response.getDocumentId());

    }

    @Test
    public void endToEnd10() throws Exception {

        final Properties properties = new Properties();
        final PhileasConfiguration phileasConfiguration = new PhileasConfiguration(properties);

        final Policy policy = getPolicy("default");

        final String input = IOUtils.toString(this.getClass().getResourceAsStream("/inputs/1.txt"), Charset.defaultCharset());

        final PhileasFilterService service = new PhileasFilterService(phileasConfiguration, contextService, vectorService);
        final FilterResponse response = service.filter(policy, "context",  "documentid", input, MimeType.TEXT_PLAIN);

        LOGGER.info(response.getFilteredText());

        showSpans(response.getExplanation().appliedSpans());

        Assertions.assertEquals("documentid", response.getDocumentId());
        Assertions.assertEquals(14, response.getExplanation().appliedSpans().size());
        Assertions.assertEquals("all dollar figures in US dollars, unless otherwise indicated VANCOUVER, {{{REDACTED-date}}} /PRNewswire/ - Equinox Gold Corp. (TSX: EQX, NYSE American: EQX) (Equinox Gold or the Company) is pleased to report its first quarter 2020 summary financial and operating results. The Company's unaudited condensed consolidated interim financial statements and related management's discussion and analysis for the three months ended {{{REDACTED-date}}} will be available for download shortly on SEDAR, on {{{REDACTED-person}}} and on the Company's website. The Company will host a conference call and webcast today commencing at 2:00 pm Vancouver time to discuss the Company's business strategy and objectives, first quarter results and activities underway at the Company's projects. Further details are provided at the end of this news release. Equinox Gold had a strong first quarter bolstered by production from our newly acquired assets and achieved record gold production and record earnings from mine operations, despite navigating challenges related to the COVID-19 pandemic, {{{REDACTED-person}}}, Chief Executive Officer. With more than $350 million of cash on hand, Equinox Gold is in a strong financial position and fully funded for its organic growth plans. Castle Mountain Phase 1 construction is 75% complete, the Los Filos expansion and Santa Luz restart projects are expected to significantly increase production over the course of 2021 and 2022 and we recently completed a positive preliminary economic assessment for development of an underground mine at Aurizona. While the temporary suspension of mining activities at our Los Filos mine in Mexico will affect Q2 production, our other mines have experienced only minimal COVID-19 disruption to date. We remain focused on achieving our growth objectives while continuing to maintain appropriate operational and safety procedures to help protect the health and economic wellbeing of our workforce and local communities. HIGHLIGHTS FOR THE THREE MONTHS ENDED {{{REDACTED-date}}}(1)n• Completed 3.5 million work hours with three lost-time injuries across all sitesn• Produced 88,951 ounces (oz) of gold and sold 82,629 oz of gold (excluding Leagold production prior to the merger close on )n• Mine cash costs of /oz(2) and all-in-sustaining costs (AISC) of /oz(2,3)n• EBITDA of (2) and adjusted EBITDA of (2,4)n• Net income of or per sharen• Adjusted net income of (2,4) or per sharen• Cash flow from operations before changes in working capital ofn• Cash and cash equivalents (unrestricted) of (more than at )n• Completed at-market merger with Leagold and concurrent financing package that included:n• Added to the GDXJ (VanEck Vectors Junior Gold Miners ETF) onn• Drew the remaining from the revolving credit facility as a COVID-19 proactive measuren• Production of 540,000 to 600,000 ounces of gold(5)n• AISC of to per oz of gold sold(2)n• Sustaining capital of and expansion capital of (5)n• Guidance to be updated when practicaln• Phase 1 construction 75% complete with first gold pour expected in Q3 2020n• Guadalupe open pit and Bermejal underground development activities continued during the quarter but were suspended at the beginning of April in compliance with Mexican COVID-19 restrictions; first ore anticipated in early 2021n• Updating study on new carbon-in-leach plant to incorporate several optimization opportunities to prepare for a project construction decision later this yearn• Commenced an update and review of costs and engineering for the resin-in-leach plant to prepare for a project construction decision later this yearn• Added to the GDX (VanEck Vectors Gold Miners ETF) onn• Received approximately from the issuance of convertible notes and common shares related to a shareholder's anti-dilution rightn• Completed a positive preliminary economic assessment (PEA) for potential development of an underground mine at Aurizona that could be operated concurrently with the existing open-pit mine, demonstrating potential for both mine life extension and increased annual gold production(6)n• Completed updated reserve and resource estimates for Aurizona and Mesquiten• Los Filos preparing to safely resume operations following the Mexico Federal Government declaration on that mining is an essential activity Equinox Gold took early precautionary measures at its mine sites and offices to proactively manage issues related to the COVID-19 pandemic. Each of the Company's operations has implemented preventive measures in collaboration with the Company's employees, contractors, local communities and governments to help ensure the health, safety and economic wellbeing of the Company's workforce and local communities. The Company has also evaluated supply chain and other risks at each operation and implemented business continuity protocols so the mines can operate as effectively as possible. The Company's operations have experienced limited disruption to date, with the exception of the Company's Los Filos mine in Mexico at which mining and development activities were suspended at the beginning of April in compliance with a Mexico Federal Government order requiring the temporary suspension of all non-essential businesses. On {{{REDACTED-date}}} the Mexico Federal Government declared mining an essential activity and allowed for the restart of operations on {{{REDACTED-date}}}. The Company is preparing to safely resume operations. Additional information regarding Equinox Gold's COVID-19 response plan, preventive measures taken to date and the potential impact on operations is available in the Q1 2020 management's discussion and analysis and on the Company's website at {{{REDACTED-url}}}. The Company drew the remaining $180 million from its $400 million revolving credit facility in late March as a proactive measure given the uncertainty of the potential effects of the COVID-19 pandemic on the Company's operations. There are no current plans to spend these funds and Equinox Gold remains in a strong financial position with more than $350 million in cash and cash equivalents (unrestricted) at the date of this news release. OPERATING AND FINANCIAL RESULTS FOR THE THREE MONTHS ENDED {{{REDACTED-date}}} SELECTED FINANCIAL RESULTS FOR THE THREE MONTHS ENDED {{{REDACTED-date}}} Additional information regarding the Company's financial results and activities underway at the Company's projects are available in the Company's Q1 2020 Financial Statements and accompanying management's discussion and analysis for the three months ended {{{REDACTED-date}}}, which will be available for download shortly on the Company's website at {{{REDACTED-url}}}, on SEDAR at {{{REDACTED-url}}} and on EDGAR at {{{REDACTED-url}}}.", response.getFilteredText().trim());

    }

    @Test
    public void endToEnd11() throws Exception {

        final Properties properties = new Properties();
        final PhileasConfiguration phileasConfiguration = new PhileasConfiguration(properties);

        final Policy policy = getPolicy("default");

        final String input = "IN THE UNITED STATES DISTRICT COURT \nEASTERN DISTRICT OF ARKANSAS \nWESTERN DIVISION \nJAMES EDWARD SMITH, \nafk/a James Edward Bridges, \nADC#103093 \nv. No. 4:14-cv-455-DPM \nPLAINTIFF \nCHARLES A. SMITH; \nMARY ANN CONLEY, \nafk/a Mary Ann Smith; and \nROBERT CASTILLOW DEFENDANTS \nORDER \nJames Smith's prose complaint must be dismissed without prejudice. \nHe hasn't paid the filing fee, moved to proceed in forma pauperis, or provided \nproof of service on any defendant. FED. R. CIV. P. 4(I); Local Rule 5.5(c)(2). \nSo Ordered. \nD.P. Marshall Jr. \nUnited States District Judge \nCase 4:14-cv-00455-DPM   Document 2   Filed 12/09/14   Page 1 of 1\n";

        final PhileasFilterService service = new PhileasFilterService(phileasConfiguration, contextService, vectorService);
        final FilterResponse response = service.filter(policy, "context",  "documentid", input, MimeType.TEXT_PLAIN);

        LOGGER.info(response.getFilteredText());

        //Assertions.assertEquals("George Washington was president and his ssn was {{{REDACTED-ssn}}} and he lived at {{{REDACTED-zip-code}}}. The name 456 should be filtered. Jeff Smith should be ignored.", response.getFilteredText());
        Assertions.assertEquals("documentid", response.getDocumentId());

    }

    @Test
    public void endToEnd12() throws Exception {

        final Properties properties = new Properties();
        final PhileasConfiguration phileasConfiguration = new PhileasConfiguration(properties);

        final Policy policy = getPolicy("default");

        final String input = IOUtils.toString(this.getClass().getResourceAsStream("/inputs/Oxford_City_unveil_merger_to_expand_their_youth_system.json.txt"), Charset.defaultCharset());

        final PhileasFilterService service = new PhileasFilterService(phileasConfiguration, contextService, vectorService);
        final FilterResponse response = service.filter(policy, "context",  "documentid", input, MimeType.TEXT_PLAIN);

        LOGGER.info(response.getFilteredText());

        showSpans(response.getExplanation().appliedSpans());

        Assertions.assertEquals("documentid", response.getDocumentId());
        Assertions.assertEquals(6, response.getExplanation().appliedSpans().size());
        Assertions.assertEquals("OXFORD City have increased the size of their junior The two already had a partnership, but the latest move will see all Ignite players from under seven to under 18 play for City from this season. In turn, Ignite will provide the coaching support, management and administration of City’s existing youth and junior teams. {{{REDACTED-person}}}, who is City’s director of football and owner of Ignite, said: Having everyone play for our flagship club will improve overall identity and strengthen our junior pathway programmes. Ignite will provide the structure around delivery, coaching and administration which will provide a professional system. Together this will create a clearer structure that will benefit everyone and create an environment in which coaches and players can achieve even better outcomes. Former Oxford United coach {{{REDACTED-person}}} is the head of City’s academy, which is now using video technology for player analysis across all age groups. He said: This will enable us to provide better coaching and provide young players with a real opportunity for progression. The aim is to see even more academy graduates represent the Oxford City first team. City’s youth set-up has had a decent track record in recent seasons. Udoka Godwin-Malife was signed by Forest Green Rovers last year, while in the existing set-up players Z{{{REDACTED-person}}}, {{{REDACTED-person}}} and {{{REDACTED-person}}} came through the ranks, as did assistant manager {{{REDACTED-person}}}.", response.getFilteredText().trim());

    }

    @Test
    public void endToEnd13() throws Exception {

        final Properties properties = new Properties();
        final PhileasConfiguration phileasConfiguration = new PhileasConfiguration(properties);

        final Policy policy = getPolicy("default");

        final String input = IOUtils.toString(this.getClass().getResourceAsStream("/inputs/Kinross_reports_strong_2020_secondquarter_results.json.txt"), Charset.defaultCharset());

        final PhileasFilterService service = new PhileasFilterService(phileasConfiguration, contextService, vectorService);
        final FilterResponse response = service.filter(policy, "context",  "documentid", input, MimeType.TEXT_PLAIN);

        LOGGER.info(response.getFilteredText());

        showSpans(response.getExplanation().appliedSpans());

        Assertions.assertEquals("documentid", response.getDocumentId());
        Assertions.assertEquals(31, response.getExplanation().appliedSpans().size());
        Assertions.assertEquals("TORONTO, {{{REDACTED-date}}} (GLOBE NEWSWIRE) -- {{{REDACTED-person}}}ross Gold Corporation (TSX: K, NYSE: KGC) today announced its results for the second-quarter ended {{{REDACTED-date}}}.n• Production1 of 571,978 attributable gold equivalent ounces (Au eq. oz.), and sales of 584,477 Au eq. oz.n• All {{{REDACTED-person}}}ross mines continued production during the quarter, as the Company’s comprehensive COVID-19 response plans mitigated operational risk and continued to help protect the health and safety of employees and host communities.n• Kin{{{REDACTED-person}}} three largest producing mines – Paracatu, {{{REDACTED-person}}} and Tasiast – delivered 63% of total production and were the lowest cost mines in the portfolio, with an average cost of sales of $596 per Au eq. oz.n• Reported net earnings2 and adjusted net earnings3 both more than doubled to $195.7 million, or $0.16 per share, and $194.0 million, or $0.15 per share, respectively, compared with Q2 2019.n• Operating cash flow of $432.8 million and adjusted operating cash flow3 of $416.9 million, a 30% and 45% increase, respectively, compared with Q2 2019.n• Production cost of sales1,3 of $725 per Au eq. oz. and all-in sustaining cost1,3 of $984 per Au eq. oz. sold, both of which are within the Company’s original annual 2020 guidance range.n• Attributable margin per Au eq. oz. sold4 increased 53% to $987 per Au eq. oz. compared with Q2 2019, outpacing the 31% increase in average realized gold price to $1,712 per Au oz. compared with Q2 2019.n• Cash and cash equivalents of $1,527.1 million and total liquidity of $2.3 billion at {{{REDACTED-date}}}, as both improved quarter-over-quarter. The Company also further improved its debt metrics, including its net debt to EBITDA ratio, and has no debt maturities until {{{REDACTED-date}}}.n• While the Company withdrew its full-year guidance as a precautionary measure given the global uncertainties caused by the pandemic, production, cost of sales per ounce, all-in sustaining cost per ounce and capital expenditures are on track to meet Kinross’ original 2020 guidance.n.• On {{{REDACTED-date}}}, Kinross announced an agreement in principle with the Government of Mauritania to enhance the parties’ partnership.n• On {{{REDACTED-date}}}, Kinross announced the results of the Lobo-Marte project pre-feasibility study in Chile, which added 6.4 million Au oz.5 to the Company’s mineral reserve estimates and increased its reserve life index by approximately 2.5 years6. CEO commentary:  J{{{REDACTED-person}}}, President and {{{REDACTED-person}}}, made the following comments in relation to 2020 second-quarter results. Kinross had a strong second quarter, as we generated robust free cash flow, more than doubled earnings year-over-year, and continued to strengthen our investment grade balance sheet. Our margins increased 53% year-over-year, well above the 31% increase in the average realized gold price. Our portfolio of mines performed well and continued production during the quarter, with our three largest producing mines – Paracatu, Kupol and Tasiast – delivering the lowest costs. We have been able to effectively manage COVID-19 impacts on our portfolio of mines during the first half of the year, as our comprehensive pandemic response plan continued to help protect the health of our employees and communities, while supporting the successful continuation of our business. Although we prudently withdrew our full-year guidance given the potential impacts of the pandemic on our operations, we continue to work towards the safe delivery of our annual targets. I would like to thank our employees around the world for their dedication, hard work and commitment to safety during these challenging times. During the quarter, we announced an agreement in principle with the Government of Mauritania that enhances our partnership and will provide further stability for the long-term success of our Tasiast mine. Earlier this month, we also announced an addition of 6.4 million ounces to our gold reserve estimates with the completion of the Lobo-Marte pre-feasibility study. This high-quality asset increases our reserve life index and further enhances optionality on our long-term development project pipeline. For the first half of the year, more than 50% of our production came from the Americas, and more than 80% from five key assets in five diverse regions. With the recent acquisition in Russia, and taking into account our track record of exploration success, we expect these assets and regions will continue to produce for at least 10 years. Summary of financial and operating results (a) Total includes 100% of Chirano production. Attributable includes Kinross' share of Chirano (90%) production (b) The definition and reconciliation of these non-GAAP measures is included on pages 14 to 19 of this news release (c) Gold equivalent ounces include silver ounces produced and sold converted to a gold equivalent based on a ratio of the average spot market prices for the commodities for each period. The ratio for the second quarter of 2020 was 104.49:1 (second quarter of 2019: 87.98:1). The ratio for the first six months of 2020 was 98.85:1 (first six months of 2019: 85.78:1) (d) Capital expenditures is as reported as Additions to property, plant and equipment on the interim condensed consolidated statement of cash flows and excludes Interest paid capitalized to property, plant and equipment. The following operating and financial results are based on 2020 second-quarter gold equivalent production. Production and cost measures are on an attributable basis: Production1: Kinross produced 571,978 attributable Au eq. oz. in Q2 2020, compared with 648,251 Au eq. oz. in Q2 2019. The decrease was mainly due to lower production at Paracatu, Round Mountain and Chirano, partially offset by higher production at Bald Mountain and Kupol. Production cost of sales1,3: Production cost of sales per Au eq. oz. was $725 for Q2 2020, compared with $663 for Q2 2019. Production cost of sales per Au oz. on a by-product basis was $707 in Q2 2020, compared with $650 in Q2 2019, based on Q2 2020 attributable gold sales of 574,299 ounces and attributable silver sales of 1,063,572 ounces. All-in sustaining cost1,3: All-in sustaining cost per Au eq. oz. sold was $984 in Q2 2020, compared with $925 in Q2 2019. All-in sustaining cost per Au oz. sold on a by-product basis was $971 in Q2 2020, compared with $918 in Q2 2019. Revenue: Revenue from metal sales increased 20% to $1,007.2 million in Q2 2020, compared with $837.8 million during the same period in 2019. Average realized gold price7: The average realized gold price in Q2 2020 increased 31% to $1,712 per ounce, compared with $1,307 per ounce in Q2 2019. Margins: Kinross’ attributable margin per Au eq. oz. sold4 increased 53% to $987 per Au eq. oz. for Q2 2020, compared with the Q2 2019 margin of $644 per Au eq. oz. sold. Operating cash flow: Adjusted operating cash flow3 for Q2 2020 increased significantly by 45% to $416.9 million, compared with $287.7 million for Q2 2019, primarily due to the increase in margins. Net operating cash flow was $432.8 million for Q2 2020, an increase of 30% compared with $333.0 million for Q2 2019. Earnings: Adjusted net earnings3 more than doubled to $194.0 million, or $0.15 per share, for Q2 2020, compared with adjusted net earnings of $79.6 million, or $0.06 per share, for Q2 2019, primarily due to the increase in margins. Reported net earnings2 also more than doubled to $195.7 million, or $0.16 per share, for Q2 2020, compared with net earnings of $71.5 million, or $0.06 per share, in Q2 2019. The increase was mainly due to higher operating earnings and a non-cash impairment reversal of $48.3 million at Lobo-Marte as a result of the addition of mineral reserves at the project in conjunction with the recently completed pre-feasibility study, partially offset by the increase in income tax expense in Q2 2020. Capital expenditures: Capital expenditures were $214.3 million for Q2 2020, compared with $275.8 million for the same period last year, primarily due to a decrease in spending at Tasiast as a result of impacts of the pandemic on stripping rates, and decreases at Bald Mountain and Round Mountain. As of {{{REDACTED-date}}}, Kinross had cash and cash equivalents of $1,527.1 million, which increased compared with $1,138.6 million at {{{REDACTED-date}}}. The quarter-over-over increase was due to free cash flow generated during Q2 2020 and the $200 million drawdown from the Tasiast project financing. The Company had additional available credit of $811.2 million as of {{{REDACTED-date}}}, and total liquidity of approximately $2.3 billion, with no scheduled debt repayments until {{{REDACTED-date}}}. The Company had total debt of approximately $2.7 billion, which includes the $750 million draw from the revolving credit facility in the first quarter and the $200 million in Tasiast project financing, and net debt8 of approximately $1.1 billion. Kinross has further improved its debt metrics, including its net debt to EBITDA ratio. The Company drew down from its revolving credit facility in {{{REDACTED-date}}} as a precautionary measure to protect against economic and business uncertainties caused by the COVID-19 pandemic. The Company repaid $250 million of the drawn amount on {{{REDACTED-date}}} given the increase in the Company’s cash and cash equivalents and its strong financial position, and does not plan to deploy the remaining funds. On {{{REDACTED-date}}}, Kinross extended the maturity date of its $300 million letter of credit guarantee facility with Export Development Canada for two years to {{{REDACTED-date}}}. All of Kinross’ mines continued production during Q2 2020, as the Company’s ongoing response to COVID-19 safeguarded the health and safety of employees and host communities and mitigated material operational impacts to the portfolio. However, COVID-19 did partially affect overall performance and productivity rates, mainly as a result of global travel constraints and the implementation of rigorous safety protocols and measures at all mines and projects. Mine-by-mine summaries for 2020 second-quarter results can be found on pages nine and 13 of this news release. Operational highlights from Q2 2020 include the following: Paracatu performed well during the quarter, with production increasing compared with Q1 2020 mainly due to higher mill throughput and grades, while cost of sales per ounce sold decreased largely as a result of favourable foreign exchange rates. Production was lower compared with Q2 2019’s record performance, as grades and recoveries decreased as planned. Cost of sales per ounce sold was higher year-over-year mainly due to the lower production, which was offset by favourable foreign exchange rates. At Round Mountain, production was lower quarter-over-quarter mainly due to fewer ounces recovered from the heap leach pads, and decreased year-over-year mainly due to lower mill grades. Cost of sales per ounce sold was higher versus Q1 2020 and Q2 2019 largely due to lower production as a result of fewer ounces from the heap leach pads, with higher maintenance and contractor costs also contributing to the increase year-over-year. Bald Mountain had good performance during the quarter, as production increased compared with Q1 2020 and Q2 2019 largely as a result of more ounces recovered from the Vantage Complex heap leach pad and higher grades. Cost of sales per ounce sold increased compared with Q1 2020 mainly due to higher cost ounces recovered from the heap leach pads, and was largely in line with Q2 2019. At Fort Knox, production increased compared with Q1 2020 primarily as a result of higher mill grades and recoveries, while cost of sales per ounce sold decreased mainly due to higher mill grades and lower energy costs. Production was largely in line year-over-year, with cost of sales per ounce sold increasing mainly due to a higher percentage of operating waste mined and higher maintenance costs, partially offset by lower energy costs. The Russia region continued its strong and consistent performance during the quarter, with production at Kupol and Dvoinoye increasing quarter-over-quarter and year-over-year, mainly due to higher gold grades. Cost of sales per ounce sold was lower compared with Q1 2020 largely as a result of favourable foreign exchange rates, and was higher versus Q2 2019 mainly due to higher royalties associated with the increase in the average realized gold price. At Tasiast, production was lower compared with Q1 2020 and Q2 2019 mainly due to the 17-day strike during the quarter and mine sequencing, which was slightly offset by higher grades. The principal impact of COVID-19 was a lower-than-planned mining rate, which resulted in deferrals of some stripping and associated capital expenditures. Production is expected to increase during the second half of the year, and, as a result, 2020 production is not expected to be materially impacted by the deferrals. Throughput performance adjusted for the impact of the strike continued to be strong, with average daily rates slightly better than the record performance in Q1 2020. Cost of sales per ounce sold increased compared with the previous quarter mainly due to the lower production and impacts from COVID-19. Cost of sales per ounce sold decreased compared with the previous year mainly due to lower fuel and overhead costs. In 2021, stripping rates and capital expenditures are expected to be higher compared to those presented in the Tasiast Technical Report as the mine makes up for the stripping deferred from 2020. A modest reduction in 2021 gold production is also expected compared to the Technical Report due to a longer-than-planned period of stockpile feed and delayed access to higher grade ore. The Company expects no impacts to Tasiast’s life of mine production, mineral reserve estimates and overall value, and was able to adjust short-term mine plans given the availability of large stockpiles at site. At Chirano, production was lower quarter-over-quarter mainly due to temporary downtime at the mill and decreased mining rates from COVID-19 impacts, both of which were slightly offset by higher grades, while cost of sales per ounce sold decreased mainly due to lower operating waste mined. Production decreased compared with the previous year mainly due to lower throughput, grades and recoveries, with cost of sales per ounce sold increasing mainly due to higher operating waste mined. The Tasiast 24k project continues to advance and remains on schedule to increase throughput capacity to 21,000 t/d by the end of 2021, and then to 24,000 t/d by mid-2023. During Q2 2020, COVID-19 impacts affected progress on power plant construction, while civil works in the processing plant, including the gravity circuit, thickener and screens, progressed well. The project team continues to explore measures to mitigate potential impacts of prolonged constraints on the global movement of people and supplies, which could affect the project schedule. However, by late June, the Company reinstated more regular rotations of expatriate staff in Mauritania, which has improved the situation. The Fort Knox Gilmore project continues to progress well and is on schedule and on budget, with the new Barnes Creek heap leach expected to be completed in Q4 2020. Stripping is advancing well and the project is now approximately 80% complete. At the Chulbatkan development project in Russia, the 2020 drill program is ramping back up after COVID-19-related challenges reduced drilling rates in the second quarter and remains on track to be completed by year-end. As of the end of Q2 2020, approximately 35,500 metres of infill, step-out and metallurgical drilling was completed, with drilling confirming the well disseminated nature of the orebody, including large lower grade intercepts, combined with pockets of high grade intercepts. In the third quarter, the drilling program will focus on further defining the high-grade zone of the known resource through additional tight-spaced drilling. The project currently has a large, near-surface estimated mineral resource, with highly continuous mineralization that is open along strike and at depth. At the La Coipa Restart project, work is ramping up after limitations on people movement challenged the project in the first quarter. Mining crews are being mobilized and fleet rebuilds are commencing in preparation for pre-stripping, which is expected to start in early 2021, with first production expected in mid-2022. The project team continues to study opportunities to optimize the mine plan and potentially extend mine life. On {{{REDACTED-date}}}, Kinross announced results for the Lobo-Marte pre-feasibility study (PFS). The project added a significant 6.4 million gold ounces5 to Kinross’ 2019 year-end probable mineral reserve estimates and increased the Company’s reserve life index by approximately 2.5 years6. The PFS estimate includes total life of mine production of approximately 4.5 million Au oz. during a 15-year mine life, and pending a positive development decision, is expected to commence production after the conclusion of mining at the La Coipa project. The long-term Lobo-Marte project provides Kinross with an excellent, organic development option that has attractive all-in sustaining costs and strong returns at the consensus long-term gold price. The project is expected to realize significant upside value and increase margins at higher gold prices without having to increase stripping or current cost estimates as the pit design would remain based on a $1,200/oz. gold price. The Company plans to commence a feasibility study later this year, with scheduled completion in Q4 2021, and will continue to prioritize balance sheet strength and disciplined capital allocation as it moves forward with the project. Exploration activities during the first half of the year continued to focus on promising targets around current operations, and areas where existing infrastructure can be leveraged, with the goal of extending mine life and adding to the Company’s mineral reserve and resource estimates. Highlights include: Kupol: During the first half of the year, exploration within the existing footprint of Russia operations were very encouraging, with positive results from the Kupol NE Extension, Kupol Deeps South, Moroshka and Providence. Exploration will continue to focus on these targets for the rest of 2020, with the goal of adding significant ounces to Kupol’s mineral reserve and resource estimates at year-end and extending mine life. Chirano: Exploration at Chirano showed promising results during the first half of the year as the Company continued to target multi-year mine life extensions. To date, a total of approximately 29,000 metres of drilling was completed at the Akwaaba, Tano, Obra and Mamnao West areas. At Obra, drilling yielded significant results and has extended the depth of high-grade mineralization. For the second half of the year, Kinross will continue to explore the underground mining potential at Obra by commencing initial works on an exploration drift to drill from the underground in order to increase accuracy and targeting. Drilling will also continue to explore the extensions of Akwaaba, Tano and Suraw, and the potential for open pit mining at Mamnao West. Round Mountain: At Round Mountain, drilling continued at Phase X, which is the northwest continuation of Phase W mineralization. Results received to date have been encouraging, as drilling has intersected significant mineralization in the upper portions within the shallow portion of Phase X to potentially optimize the pit shell design, and confirmed that mineralization extends from Phase W. Further drilling is assessing mineralization to reduce the strip ratio at Phase X. Curlew Basin Project: The 2020 Curlew exploration program has focused on areas around the historical K2 mine, which is located approximately 35 kilometres north of the Kettle River mill. The program added 162 Au koz. with grades of 8.8 g/t to Kinross’ indicated mineral resource estimates at year-end 2019, and high level engineering and economic assessment of potential mining at the Curlew Basin achieved encouraging results during the first half of the year. Exploration activities will continue to target incremental high-margin ounces proximal to and extensions of the K2 and K5 deposits by constructing a series of exploration drifts to explore the highly prospective areas. The drifts will allow for underground drilling that will test the large prospective ground at optimal drill angles and at expected lower costs. Exploration work for the second half of the year is expected to also continue at the Company’s other brownfield targets, including Fort Knox, Bald Mountain and La Coipa. As well, Kinross expects to focus on growing mineral resource estimates at Tasiast Sud in Mauritania and progressing work at district targets around Kupol-Dvoinoye in Russia. Agreement in principle with Government of Mauritania On {{{REDACTED-date}}}, Kinross reached an agreement in principle with the Government of Mauritania to resolve outstanding matters between the parties. The terms are subject to finalizing definitive agreements and provide Kinross with a 30-year exploitation license for Tasiast Sud, with expedited permitting and the possibility of early mining. The terms also provide for the reinstatement of a tax exemption on fuel duties and repayment by the Government to Kinross of outstanding VAT refunds. Kinross also volunteered to update the royalty structure for Tasiast so it is tied to the gold price, is in line with Mauritania’s current mining conventions and codes, and further aligns interests by ensuring the country receives an appropriate share of economic benefits from the Tasiast mine. 2020 Guidance The following section of the news release represents forward-looking information and users are cautioned that actual results may vary. We refer to the risks and assumptions contained in the Cautionary Statement on Forward-Looking Information on page 20 of this news release. On {{{REDACTED-date}}}, the Company made the prudent decision to withdraw its full-year guidance. Although the COVID-19 pandemic has not materially impacted Kinross’ overall business performance during the first half of the year, the pandemic continues to present the potential for further business disruptions. To date, Kinross’ ongoing and comprehensive response to the pandemic has enabled the Company to safeguard employees and local communities, help prevent the spread of COVID-19, and mitigate operational risk. The Company continues to target the safe delivery of its operating plans and is on track to meet its original 2020 guidance for production, cost of sales per ounce sold, all-in sustaining cost per ounce sold and capital expenditures. In connection with the release, Kinross will hold a conference call and audio webcast on Thursday, {{{REDACTED-date}}} at 8:00 a.m. ET followed by a question-and-answer session. Please enter the passcode: 5161488 to access the call. Canada & US toll-free – +1 (833) 968-2237; passcode: 5161488 Outside of Canada & US – +1 (825) 312-2059; passcode: 5161488 Replay (available up to 14 days after the call): Canada & US toll-free – +1 (800) 585-8367; passcode: 5161488 Outside of Canada & US – +1 (416) 621-4642; passcode: 5161488 You may also access the conference call on a listen-only basis via webcast at our website . The audio webcast will be archived on . This news release should be read in conjunction with Kinross’ 2020 second-quarter unaudited Financial Statements and Management’s Discussion and Analysis report at . Kinross’ 2020 second-quarter unaudited Financial Statements and Management’s Discussion and Analysis have been filed with Canadian securities regulators (available at ) and furnished to the U.S. Securities and Exchange Commission (available at ). Kinross shareholders may obtain a copy of the financial statements free of charge upon request to the Company. Kinross is a Canadian-based senior gold mining company with mines and projects in the United States, Brazil, Russia, Mauritania, Chile and Ghana. Kinross maintains listings on the Toronto Stock Exchange (symbol:K) and the New York Stock Exchange (symbol:KGC). (a) Tonnes of ore mined and processed represent 100% Kinross for all periods presented. (b) Due to the nature of heap leach operations, recovery rates at Maricunga and Bald Mountain cannot be accurately measured on a quarterly basis. Recovery rates at Fort Knox, Round Mountain and Tasiast represent mill recovery only. (c) The Kupol segment includes the Kupol and Dvoinoye mines. (d) Kupol silver grade and recovery were as follows: Q2 2020: 70.36 g/t, 86.1% Q1 2020: 80.02 g/t, 84.1% Q4 2019: 65.63 g/t, 84.8%; Q3 2019: 67.44 g/t, 87.8%; Q2 2019: 75.29 g/t, 84.9% (e) Gold equivalent ounces include silver ounces produced and sold converted to a gold equivalent based on the ratio of the average spot market prices for the commodities for each period. The ratios for the quarters presented are as follows: Q2 2020: 104.49:1, Q1 2020: 93.34:1, Q4 2019: 85.59:1; Q3 2019: 86.73:1; Q2 2019: 87.98:1 (f) Dvoinoye ore processed and grade were as follows: Q2 2020: 113,472, 9.55 g/t; Q1 2020: 117,502, 9.24 g/t; Q4 2019: 100,685, 9.89 g/t; Q3 2019: 113,497, 9.82 g/t; Q2 2019: 113,872, 9.24 g/t (g) Capital expenditures is as reported as Additions to property, plant and equipment on the interim condensed consolidated statement of cash flows and excludes Interest paid capitalized to property, plant and equipment. (h) nm means not meaningful. The Company has included certain non-GAAP financial measures in this document. These measures are not defined under IFRS and should not be considered in isolation. The Company believes that these measures, together with measures determined in accordance with IFRS, provide investors with an improved ability to evaluate the underlying performance of the Company. The inclusion of these measures is meant to provide additional information and should not be used as a substitute for performance measures prepared in accordance with IFRS. These measures are not necessarily standard and therefore may not be comparable to other issuers. Adjusted net earnings attributable to common shareholders and adjusted net earnings per share are non-GAAP measures which determine the performance of the Company, excluding certain impacts which the Company believes are not reflective of the Company’s underlying performance for the reporting period, such as the impact of foreign exchange gains and losses, reassessment of prior year taxes and/or taxes otherwise not related to the current period, impairment charges (reversals), gains and losses and other one-time costs related to acquisitions, dispositions and other transactions, and non-hedge derivative gains and losses. Although some of the items are recurring, the Company believes that they are not reflective of the underlying operating performance of its current business and are not necessarily indicative of future operating results. Management believes that these measures, which are used internally to assess performance and in planning and forecasting future operating results, provide investors with the ability to better evaluate underlying performance, particularly since the excluded items are typically not included in public guidance. However, adjusted net earnings and adjusted net earnings per share measures are not necessarily indicative of net earnings and earnings per share measures as determined under IFRS. The following table provides a reconciliation of net earnings to adjusted net earnings for the periods presented: (a) During the three and six months ended {{{REDACTED-date}}}, the Company recognized a non-cash reversal of impairment charge of $48.3 million related to property, plant and equipment at Lobo-Marte. (b) Includes $6.0 million of Tasiast strike costs. The Company makes reference to a non-GAAP measure for adjusted operating cash flow. Adjusted operating cash flow is defined as cash flow from operations excluding certain impacts which the Company believes are not reflective of the Company’s regular operating cash flow, and excluding changes in working capital. Working capital can be volatile due to numerous factors, including the timing of tax payments, and in the case of Kupol, a build-up of inventory due to transportation logistics. The Company uses adjusted operating cash flow internally as a measure of the underlying operating cash flow performance and future operating cash flow-generating capability of the Company. However, the adjusted operating cash flow measure is not necessarily indicative of net cash flow from operations as determined under IFRS. The following table provides a reconciliation of adjusted operating cash flow for the periods presented: Consolidated production cost of sales per gold equivalent ounce sold is a non-GAAP measure and is defined as production cost of sales as per the interim condensed consolidated financial statements divided by the total number of gold equivalent ounces sold. This measure converts the Company’s non-gold production into gold equivalent ounces and credits it to total production.  Attributable production cost of sales per gold equivalent ounce sold is a non-GAAP measure and is defined as attributable production cost of sales divided by the attributable number of gold equivalent ounces sold. This measure converts the Company’s non-gold production into gold equivalent ounces and credits it to total production. Management uses these measures to monitor and evaluate the performance of its operating properties. The following table presents a reconciliation of consolidated and attributable production cost of sales per equivalent ounce sold for the periods presented: Attributable production cost of sales per ounce sold on a by-product basis is a non-GAAP measure which calculates the Company’s non-gold production as a credit against its per ounce production costs, rather than converting its non-gold production into gold equivalent ounces and crediting it to total production, as is the case in co-product accounting. Management believes that this measure provides investors with the ability to better evaluate Kinross’ production cost of sales per ounce on a comparable basis with other major gold producers who routinely calculate their cost of sales per ounce using by-product accounting rather than co-product accounting.  The following table provides a reconciliation of attributable production cost of sales per ounce sold on a by-product basis for the periods presented: In {{{REDACTED-date}}}, the World Gold Council (WGC) published updates to its guidelines for reporting all-in sustaining costs and all-in costs to address how the costs associated with leases, after a company’s adoption of IFRS 16, should be treated. The WGC is a market development organization for the gold industry and is an association whose membership comprises leading gold mining companies including Kinross. Although the WGC is not a mining industry regulatory organization, it worked closely with its member companies to develop these non-GAAP measures. Adoption of the all-in sustaining cost and all-in cost metrics is voluntary and not necessarily standard, and therefore, these measures presented by the Company may not be comparable to similar measures presented by other issuers. The Company believes that the all-in sustaining cost and all-in cost measures complement existing measures reported by Kinross.  All-in sustaining cost includes both operating and capital costs required to sustain gold production on an ongoing basis. The value of silver sold is deducted from the total production cost of sales as it is considered residual production. Sustaining operating costs represent expenditures incurred at current operations that are considered necessary to maintain current production. Sustaining capital represents capital expenditures at existing operations comprising mine development costs and ongoing replacement of mine equipment and other capital facilities, and does not include capital expenditures for major growth projects or enhancement capital for significant infrastructure improvements at existing operations. All-in cost is comprised of all-in sustaining cost as well as operating expenditures incurred at locations with no current operation, or costs related to other non-sustaining activities, and capital expenditures for major growth projects or enhancement capital for significant infrastructure improvements at existing operations. Attributable all-in sustaining cost and all-in cost per ounce sold on a by-product basis are calculated by adjusting total production cost of sales, as reported on the interim condensed consolidated statement of operations, as follows:   The Company also assesses its all-in sustaining cost and all-in cost on a gold equivalent ounce basis. Under these non-GAAP measures, the Company’s production of silver is converted into gold equivalent ounces and credited to total production.  Attributable all-in sustaining cost and all-in cost per equivalent ounce sold are calculated by adjusting total production cost of sales, as reported on the interim condensed consolidated statement of operations, as follows: (a) The portion attributable to Chirano non-controlling interest represents the non-controlling interest (10%) in the production cost of sales for the Chirano mine. (b) Attributable includes Kinross' share of Chirano (90%) production. (c) Attributable silver revenues represents the attributable portion of metal sales realized from the production of the secondary or by-product metal (i.e. silver). Revenue from the sale of silver, which is produced as a by-product of the process used to produce gold, effectively reduces the cost of gold production. (d) General and administrative expenses is as reported on the interim condensed consolidated statement of operations, net of certain restructuring expenses. General and administrative expenses are considered sustaining costs as they are required to be absorbed on a continuing basis for the effective operation and governance of the Company. (e) Other operating expense – sustaining is calculated as Other operating expense as reported on the interim condensed consolidated statement of operations, less other operating and reclamation and remediation expenses related to non-sustaining activities as well as other items not reflective of the underlying operating performance of our business. Other operating expenses are classified as either sustaining or non-sustaining based on the type and location of the expenditure incurred. The majority of other operating expenses that are incurred at existing operations are considered costs necessary to sustain operations, and are therefore classified as sustaining. Other operating expenses incurred at locations where there is no current operation or related to other non-sustaining activities are classified as non-sustaining. (f) Reclamation and remediation - sustaining is calculated as current period accretion related to reclamation and remediation obligations plus current period amortization of the corresponding reclamation and remediation assets, and is intended to reflect the periodic cost of reclamation and remediation for currently operating mines. Reclamation and remediation costs for development projects or closed mines are excluded from this amount and classified as non-sustaining. (g) Exploration and business development – sustaining is calculated as Exploration and business development expenses as reported on the interim condensed consolidated statement of operations, less non-sustaining exploration expenses. Exploration expenses are classified as either sustaining or non-sustaining based on a determination of the type and location of the exploration expenditure. Exploration expenditures within the footprint of operating mines are considered costs required to sustain current operations and so are included in sustaining costs. Exploration expenditures focused on new ore bodies near existing mines (i.e. brownfield), new exploration projects (i.e. greenfield) or for other generative exploration activity not linked to existing mining operations are classified as non-sustaining. Business development expenses are considered sustaining costs as they are required for general operations. (h) Additions to property, plant and equipment – sustaining represents the majority of capital expenditures at existing operations including capitalized exploration costs, periodic capitalized stripping and underground mine development costs, ongoing replacement of mine equipment and other capital facilities and other capital expenditures and is calculated as total additions to property, plant and equipment (as reported on the interim condensed consolidated statements of cash flows), less capitalized interest and non-sustaining capital. Non-sustaining capital represents capital expenditures for major projects, including major capital stripping projects at existing operations that are expected to materially benefit the operation, as well as enhancement capital for significant infrastructure improvements at existing operations. Non-sustaining capital expenditures during the three and six months ended {{{REDACTED-date}}}, primarily related to major projects at Tasiast, Round Mountain and Fort Knox. Non-sustaining capital expenditures during the three and six months ended {{{REDACTED-date}}}, primarily related to major projects at Tasiast, Round Mountain, and Bald Mountain. (i) Lease payments – sustaining represents the majority of lease payments as reported on the interim condensed consolidated statements of cash flows and is made up of the principal and financing components of such cash payments, less non-sustaining lease payments. Lease payments for development projects or closed mines are classified as non-sustaining. (j) Portion attributable to Chirano non-controlling interest represents the non-controlling interest (10%) in the ounces sold from the Chirano mine. (k) Average realized gold price per ounce is a non-GAAP financial measure and is defined as gold metal sales divided by the total number of gold ounces sold. This measure is intended to enable Management to better understand the price realized in each reporting period. The realized price measure does not have any standardized definition under IFRS and should not be considered a substitute for measure of performance prepared in accordance with IFRS. 1) Unless otherwise stated, production, production costs of sales per Au eq. oz., and all-in-sustaining costs per Au eq. oz., in this news release are based on Kinross’ 90% share of Chirano production. 2) Net earnings figures in this release represent net earnings attributable to common shareholders. 3) These figures are non-GAAP financial measures and are defined and reconciled on pages 14 to 19 of this news release. 4) Attributable margin per equivalent ounce sold is a non-GAAP financial measure defined as average realized gold price per ounce less attributable production cost of sales per gold equivalent ounce sold. 5) For more information on Lobo-Marte’s mineral reserve and resource estimates, see Kinross’ news release dated {{{REDACTED-date}}} on kinross.com. 6) Calculated as estimated 2019 proven and probable gold reserves divided by 2019 gold production. 7) Average realized gold price is a non-GAAP financial measure and is defined as gold metal sales divided by the total number of gold ounces sold. 8) Net debt is a non-GAAP financial measure defined as Long-term debt and credit facilities less Cash and cash equivalents. 9) Refers to all of the currencies in the countries where the Company has mining operations, fluctuating simultaneously by 10% in the same direction, either appreciating or depreciating, taking into consideration the impact of hedging and the weighting of each currency within our consolidated cost structure.  All statements, other than statements of historical fact, contained or incorporated by reference in this news release including, but not limited to, any information as to the future financial or operating performance of Kinross, constitute ‘‘forward-looking information’’ or ‘‘forward-looking statements’’ within the meaning of certain securities laws, including the provisions of the Securities Act (Ontario) and the provisions for ‘‘safe harbor’’ under the United States Private Securities Litigation Reform Act of 1995 and are based on expectations, estimates and projections as of the date of this news release. Forward-looking statements contained in this news release, include, but are not limited to, those under the headings (or headings that include) CEO commentary, Development projects, Agreement in principle with Government of Mauritania and 2020 Guidance and include, without limitation, statements with respect to our guidance for production, production costs of sales, all-in sustaining cost and capital expenditures; the schedules and budgets for the Company’s development projects; mine life; and continuous improvement initiatives, as well as references to other possible events, the future price of gold and silver, the timing and amount of estimated future production, costs of production, capital expenditures, costs and timing of the development of projects and new deposits, estimates and the realization of such estimates (such as mineral or gold reserves and resources or mine life), success of exploration, development and mining, currency fluctuations, capital requirements, project studies, mine life extensions, government regulation permit applications and conversions, restarting suspended or disrupted operations; environmental risks and proceedings; and resolution of pending litigation. The words continue, , estimates, expects, explore, focus, forward, goal, guidance, mitigate, on budget, on schedule, on track, opportunity, option, outlook, plan, potential, progress, schedule, target, upside, or variations of or similar such words and phrases or statements that certain actions, events or results may, could, should or will be achieved, received or taken, or will occur or result and similar such expressions identify forward-looking statements. Forward-looking statements are necessarily based upon a number of estimates and assumptions that, while considered reasonable by Kinross as of the date of such statements, are inherently subject to significant business, economic and competitive uncertainties and contingencies. The estimates, models and assumptions of Kinross referenced, contained or incorporated by reference in this news release, which may prove to be incorrect, include, but are not limited to, the various assumptions set forth herein and in our MD&A for the year ended {{{REDACTED-date}}}, and the Annual Information Form dated {{{REDACTED-date}}} as well as: (1) there being no significant disruptions affecting the operations of the Company, whether due to extreme weather events (including, without limitation, excessive or lack of rainfall, in particular, the potential for further production curtailments at Paracatu resulting from insufficient rainfall and the operational challenges at Fort Knox and Bald Mountain resulting from excessive rainfall, which can impact costs and/or production) and other or related natural disasters, labour disruptions (including but not limited to strikes or workforce reductions), supply disruptions, power disruptions, damage to equipment, pit wall slides (in particular that the effects of the pit wall slides at Fort Knox and Round Mountain are consistent with the Company’s expectations) or otherwise; (2) permitting, development, operations and production from the Company’s operations and development projects being consistent with Kinross’ current expectations including, without limitation: the maintenance of existing permits and approvals and the timely receipt of all permits and authorizations necessary for the operation of the Tasiast Phase One expansion, and the development and operation of the 24k Project; operation of the SAG mill at Tasiast; land acquisitions and permitting for the construction and operation of the new tailings facility, water and power supply and continued operation of the tailings reprocessing facility at Paracatu; and the Lobo-Marte project in a manner consistent with the Company’s expectations; (3) political and legal developments in any jurisdiction in which the Company operates being consistent with its current expectations including, without limitation, the impact of any political tensions and uncertainty in the Russian Federation and Ukraine or any related sanctions and any other similar restrictions or penalties imposed, or actions taken, by any government, including but not limited to amendments to the mining laws, and potential power rationing and tailings facility regulations in Brazil, potential amendments to water laws and/or other water use restrictions and regulatory actions in Chile, new dam safety regulations, and potential amendments to minerals and mining laws and energy levies laws, and the enforcement of labour laws in Ghana, new regulations relating to work permits, potential amendments to customs and mining laws (including but not limited to amendments to the VAT) and the pending implementation of revisions to the tax code in Mauritania, the European Union’s General Data Protection Regulation or similar legislation in other jurisdictions and potential amendments to and enforcement of tax laws in Russia (including, but not limited to, the interpretation, implementation, application and enforcement of any such laws and amendments thereto), and the impact of any trade tariffs being consistent with Kinross’ current expectations; (4) the completion of studies, including optimization studies, scoping studies and pre-feasibility and feasibility studies, on the timelines currently expected and the results of those studies being consistent with Kinross’ current expectations, including the completion of the Lobo-Marte feasibility study; (5) the exchange rate between the Canadian dollar, Brazilian real, Chilean peso, Russian rouble, Mauritanian ouguiya, Ghanaian cedi and the U.S. dollar being approximately consistent with current levels; (6) certain price assumptions for gold and silver; (7) prices for diesel, natural gas, fuel oil, electricity and other key supplies being approximately consistent with the Company’s expectations; (8) production and cost of sales forecasts for the Company meeting expectations; (9) the accuracy of the current mineral reserve and mineral resource estimates of the Company (including but not limited to ore tonnage and ore grade estimates), mine plans for the Company’s mining operations, and the Company’s internal models; (10) labour and materials costs increasing on a basis consistent with Kinross’ current expectations; (11) the terms and conditions of the legal and fiscal stability agreements for the Tasiast and Chirano operations being interpreted and applied in a manner consistent with their intent and Kinross’ expectations and without material amendment or formal dispute (including without limitation the application of tax, customs and duties exemptions and royalties); (12) goodwill and/or asset impairment potential; (13) the regulatory and legislative regime regarding mining, electricity production and transmission (including rules related to power tariffs) in Brazil being consistent with Kinross’ current expectations; (14) access to capital markets, including but not limited to maintaining our current credit ratings consistent with the Company’s current expectations; (15) that the Brazilian power plants will operate in a manner consistent with our current expectations; (16) that drawdown of remaining funds under the Tasiast project financing will proceed in a manner consistent with our current expectations; (17) potential direct or indirect operational impacts resulting from infectious diseases or pandemics such as the ongoing COVID-19 pandemic; (18) the effectiveness of preventative actions and contingency plans put in place by the Company to respond to the COVID-19 pandemic, including, but not limited to, social distancing, a non-essential travel ban, business continuity plans, and efforts to mitigate supply chain disruptions; (19) changes in national and local government legislation or other government actions, particularly in response to the COVID-19 outbreak; (20) litigation, regulatory proceedings and audits, and the potential ramifications thereof, being concluded in a manner consistent with the Corporation’s expectations (including without limitation the ongoing industry-wide audit of mining companies in Ghana which includes the Corporation’s Ghanaian subsidiaries, litigation in Chile relating to the alleged damage of wetlands and the scope of any remediation plan or other environmental obligations arising therefrom, the ongoing litigation with the Russian tax authorities regarding dividend withholding tax and the ongoing Sunnyside litigation regarding potential liability under the U.S. Comprehensive Environmental Response, Compensation, and Liability Act); (21) that the Company will enter into definitive documentation with the Government of Mauritania in accordance with, and on the timeline contemplated by, the terms and conditions of the term sheet, on a basis consistent with our expectations and that the parties will perform their respective obligations thereunder on the timelines agreed; (22) that the exploitation permit for Tasiast Sud will be issued on timelines consistent with our expectations; and (23) that the benefits of the contemplated arrangements will result in increased stability at the Company’s operations in Mauritania. Known and unknown factors could cause actual results to differ materially from those projected in the forward-looking statements. Such factors include, but are not limited to: sanctions (any other similar restrictions or penalties) now or subsequently imposed, other actions taken, by, against, in respect of or otherwise impacting any jurisdiction in which the Company is domiciled or operates (including but not limited to the Russian Federation, Canada, the European Union and the United States), or any government or citizens of, persons or companies domiciled in, or the Company’s business, operations or other activities in, any such jurisdiction; reductions in the ability of the Company to transport and refine doré; fluctuations in the currency markets; fluctuations in the spot and forward price of gold or certain other commodities (such as fuel and electricity); changes in the discount rates applied to calculate the present value of net future cash flows based on country-specific real weighted average cost of capital; changes in the market valuations of peer group gold producers and the Company, and the resulting impact on market price to net asset value multiples; changes in various market variables, such as interest rates, foreign exchange rates, gold or silver prices and lease rates, or global fuel prices, that could impact the mark-to-market value of outstanding derivative instruments and ongoing payments/receipts under any financial obligations; risks arising from holding derivative instruments (such as credit risk, market liquidity risk and mark-to-market risk); changes in national and local government legislation, taxation (including but not limited to income tax, advance income tax, stamp tax, withholding tax, capital tax, tariffs, value-added or sales tax, capital outflow tax, capital gains tax, windfall or windfall profits tax, production royalties, excise tax, customs/import or export taxes/duties, asset taxes, asset transfer tax, property use or other real estate tax, together with any related fine, penalty, surcharge, or interest imposed in connection with such taxes), controls, policies and regulations; the security of personnel and assets; political or economic developments in Canada, the United States, Chile, Brazil, Russia, Mauritania, Ghana, or other countries in which Kinross does business or may carry on business; business opportunities that may be presented to, or pursued by, us; our ability to successfully integrate acquisitions and complete divestitures; operating or technical difficulties in connection with mining or development activities; employee relations; litigation or other claims against, or regulatory investigations and/or any enforcement actions, administrative orders or sanctions in respect of the Company (and/or its directors, officers, or employees) including, but not limited to, securities class action litigation in Canada and/or the United States, environmental litigation or regulatory proceedings or any investigations, enforcement actions and/or sanctions under any applicable anti-corruption, international sanctions and/or anti-money laundering laws and regulations in Canada, the United States or any other applicable jurisdiction; the speculative nature of gold exploration and development including, but not limited to, the risks of obtaining necessary licenses and permits; diminishing quantities or grades of reserves; adverse changes in our credit ratings; and contests over title to properties, particularly title to undeveloped properties. In addition, there are risks and hazards associated with the business of gold exploration, development and mining, including environmental hazards, industrial accidents, unusual or unexpected formations, pressures, cave-ins, flooding and gold bullion losses (and the risk of inadequate insurance, or the inability to obtain insurance, to cover these risks). Many of these uncertainties and contingencies can directly or indirectly affect, and could cause, Kinross’ actual results to differ materially from those expressed or implied in any forward-looking statements made by, or on behalf of, Kinross.", response.getFilteredText().trim());

    }

    @Test
    public void endToEnd14() throws Exception {

        final Properties properties = new Properties();
        final PhileasConfiguration phileasConfiguration = new PhileasConfiguration(properties);

        final Policy policy = getPolicy("default");

        final String input = IOUtils.toString(this.getClass().getResourceAsStream("/inputs/Donations_to_Black_Lives_Matter_Group_Dont_Go_to_DNC.json.txt"), Charset.defaultCharset());

        final PhileasFilterService service = new PhileasFilterService(phileasConfiguration, contextService, vectorService);
        final FilterResponse response = service.filter(policy, "context",  "documentid", input, MimeType.TEXT_PLAIN);

        LOGGER.info(response.getFilteredText());

        showSpans(response.getExplanation().appliedSpans());

        Assertions.assertEquals("documentid", response.getDocumentId());
        Assertions.assertEquals(21, response.getExplanation().appliedSpans().size());
        Assertions.assertEquals("Social media posts falsely claim donations ma{{{REDACTED-person}}} on the Black Lives Matter website go directly to the Democratic Party, because the group uses ActBlue Charities — an online fundraising platform. Donations go to the Black Lives Matter Global Network Foundation. The funds first pass through a nonprofit that sponsors the group. As the Black Lives Matter movement captures national attention, websites and social media posts are spreading a false claim about donations made to a leading Black Lives Matter organization. An Instagram {{{REDACTED-person}}} for the website WokeHub.com claimed, for example, that donations made to Black Lives Matter website go directly to the DNC and that BlackLivesMatter.com appears to be an international money laundering program used by the Democratic National Committee. A similar claim appeared on the conspiracy theory website InfoWars.com. Conservative personality {{{REDACTED-person}}} also put the claim before her more than 1 million followers on Instagram, where she claimed BLACK LIVES MATTER FUNDING GOES DIRECTLY TO WHITE DEMOCRATS AND THEIR VARIOUS INITIATIVES TO GET DEMOCRATS INTO OFFICE. But the claims — perhaps worsened by public confusion over the structure of the organization, which we’ll explain later — are based on a distortion of facts regarding the nonprofit ActBlue, which provides fundraising infrastructure to Democratic campaigns and progressive organizations. There are a number of groups that use the phrase Black Lives Matter in their name. There is a Black Lives Matter Foundation, for example, whose president confirmed to us that it is not related to the organization behind BlackLivesMatter.com — which is at the center of the viral claims. BlackLivesMatter.com is operated by an umbrella Black Lives Matter organization called the Black Lives Matter Global Network. The effort started in 2013 following the acquittal of {{{REDACTED-person}}} in the killing of Tray{{{REDACTED-person}}}. To collect donations, the website uses ActBlue Charities, a 501(c)(3) organization that specifically makes the platform available to charitable organizations. {{{REDACTED-person}}} Instagram post features a video showing that BlackLivesMatter.com uses ActBlue for its donations. It then shows an OpenSecrets.org breakdown of expenditures by ActBlue made in the 2020 cycle; the top recipients are the presidential campaigns of Sen. {{{REDACTED-person}}}, former Vice President {{{REDACTED-person}}}, and Sen. {{{REDACTED-person}}}. But the video misrepresents how ActBlue works. ActBlue isn’t itself donating money. It’s just the online platform that campaigns and groups use to solicit and collect donations. Campaign finance experts told us that it’s false to say that donations to the Black Lives Matter group are going to the Democratic National Committee, or to Democratic presidential campaigns, simply because the website uses ActBlue. ActBlue lets candidates and liberal nonprofit organizations set up their separate accounts, {{{REDACTED-person}}}, director of the nonpartisan Campaign Finance Institute, told us in a phone interview. There’s no crossing from one account to another. If you give the money to Sanders, it does not go to Biden. Malbin — a professor of political science at the University at Albany, State University of New York — said candidates and organizations use ActBlue for the ease of transactions. ActBlue has no discretion over where to direct individuals’ contributions, he said. The money you see coming through ActBlue is coming through it, not from it. A similar platform used for Republican fundraising efforts, WinRed, launched in 2019. When you donate on an ActBlue or ActBlue Charities page, the donation is earmarked for the group listed on the form, {{{REDACTED-person}}}, a spokesperson for ActBlue, told us in an email. We pass along the contribution directly to the receiving campaign or entity. We do not choose the recipient of the contribution, the donor does. In a statement to FactCheck.org, the Black Lives Matter Global Network’s managing director, Kai{{{REDACTED-person}}}, said that the claims about the money being routed to Democrats are part of an organized disinformation campaign against BLM, from actors clearly trying to blunt the growing support for this movement. All contributions to the DNC are publicly reported to the FEC and review of FEC reports will confirm there has never been any donation from this organization, she said. The Operations of the Black Lives Matter Global Network The claims seem to have flourished amid confusion over the structure of the Black Lives Matter Global Network, which is incorporated in Delaware. The ActBlue donations page on the website specifically says that the money given will benefit Black Lives Matter Global Network. Technically, the network also has structured a foundation — the Black Lives Matter Global Network Foundation — which is fiscally sponsored by a global nonprofit called Thousand Currents, according to {{{REDACTED-person}}}, director of finance and administration at Thousand Currents. The partnership between the network and the nonprofit (formerly the International Development Exchange) was announced in 2016. The nonprofit organization said it would provide fiduciary oversight, financial management, and other administrative services to BLM. Cade, the ActBlue spokesperson, confirmed that the donations made through ActBlue go to Thousand Currents for Black Lives Matter. Using a fiscal sponsorship arrangement offers a way for a cause to attract donors even when it is not yet recognized as tax-exempt under Internal Revenue Code Section 501(c)(3), according to the National Council on Nonprofits. In essence the fiscal sponsor serves as the administrative ‘home’ of the cause. Charitable contributions are given to the fiscal sponsor, which then grants them to support the cause. Black Lives Matter Global Network Foundation is an organization within Thousand Currents, de Rivera said in a phone interview, so it can accomplish its charitable purpose prior to receiving its approval from the IRS for its own 501(c)(3). De Rivera said the foundation has applied to become its own 501(c)(3). We asked the Black Lives Matter group for a copy of its Form 1023, which is used to apply for that designation, but didn’t hear back. An IRS spokesperson said copies are only publicly available if the organization is approved for 501(c)(3) status. On the claims that money is being given to Democratic campaigns, de Rivera said that Thousand Currents’ own 501(c)(3) status would be in jeopardy if charitable donations made through the organization were rerouted to political campaigns. According to the IRS, all section 501(c)(3) organizations are absolutely prohibited from directly or indirectly participating in, or intervening in, any political campaign on behalf of (or in opposition to) any candidate for elective public office. Contributions to political campaign funds or public statements of position (verbal or written) made on behalf of the organization in favor of or in opposition to any candidate for public office clearly violate the prohibition against political campaign activity. A Thousand Currents audit for 2019 shows that, as of {{{REDACTED-date}}}, the organization held nearly $3.4 million in net assets for Black Lives Matter. It had released nearly $1.8 million to the group in the last fiscal year. The audit also provides a breakdown of how the foundation (referred to as the fiscal project under a statement of functional expenses, according to de Rivera) spent that $1.8 million in buckets — including on consultants and salaries. Speaking generally about the issue of fiscal sponsorships, {{{REDACTED-person}}}, a law professor at American University who specializes in the regulation of nonprofits, told us by phone that the sponsoring organization is responsible for ensuring that the sponsored organization spends the money in a way that is consistent with the law. He said the sponsored organization does not have to file its own Forms 990. And, Leff said in an email, because ActBlue Charities is also a 501(c)(3), it is his understanding … that both ActBlue Charities and Thousand Currents have a legal obligation to make reasonable efforts to ensure that the funds they receive are spent consistent with 501(c)(3) restrictions. On {{{REDACTED-date}}}, the Black Lives Matter Global Network Foundation said in a press release that, thanks to donations, it would make $6.5 million available through grants that will be available to all chapters affiliated with it. Editor’s note: FactCheck.org is one of several organizations working with Facebook to debunk misinformation shared on social media. Our previous stories can be found here. De Rivera, Jenesha. Director of finance and administration, Thousand Currents. Phone interview with FactCheck.org. {{{REDACTED-date}}}. {{{REDACTED-person}}}, {{{REDACTED-person}}}. WinRed, new GOP donor platform, reaps impeachment windfall, rakes in millions since probe launch. Fox News. {{{REDACTED-date}}}. The Restriction of Political Campaign Intervention by Section 501(c)(3) Tax-Exempt Organizations. IRS. Accessed {{{REDACTED-date}}}.", response.getFilteredText().trim());

    }

    @Test
    public void endToEnd15() throws Exception {

        final Properties properties = new Properties();
        final PhileasConfiguration phileasConfiguration = new PhileasConfiguration(properties);

        final Policy policy = getPolicy("default");

        final String input = IOUtils.toString(this.getClass().getResourceAsStream("/inputs/Fantasy_Baseball_Winners__Losers_Sixto_Sanchez_and_Jeff_McNeil_stay_hot.json.txt"), Charset.defaultCharset());

        final PhileasFilterService service = new PhileasFilterService(phileasConfiguration, contextService, vectorService);
        final FilterResponse response = service.filter(policy, "context",  "documentid", input, MimeType.TEXT_PLAIN);

        LOGGER.info(response.getFilteredText());

        showSpans(response.getExplanation().appliedSpans());

        Assertions.assertEquals("documentid", response.getDocumentId());
        Assertions.assertEquals(32, response.getExplanation().appliedSpans().size());
        Assertions.assertEquals("For the most part, {{{REDACTED-person}}} and I have been pretty patient with struggling players on Fantasy Baseball Today. We are now getting to the point, however, where we would need to change our stance on struggling stars even in a full-length season. I know it feels like the season just started, but we're nearly seven weeks in. While you may not want to flat-out drop names like {{{REDACTED-person}}} and {{{REDACTED-person}}} so that your opponents can't have them, you can certainly bench these players. There isn't really much that suggests they're going to get much better besides their track records, and the Fantasy playoffs start next week. We don't have any more time to waste. Now is the time to make moves if you're tired of the lackluster production. We discussed slumping players who should be benched plus reviewed Tuesday's action on the Wednesday edition of the Fantasy Baseball Today podcast. Follow all our podcasts and subscribe here. Let's find out what happened on Tuesday. The latest in the world of Fantasy Baseballn• Braves placed {{{REDACTED-person}}} on the 10-day IL Tuesday, retroactive to Sept. 6, with muscle spasms in his back. He's expected to return when first eligible Sept. 16 or soon after, according to David O'Brien.n• {{{REDACTED-person}}}er suffered a fractured left index finger while trying to lay down a bunt with two strikes on Monday night against the Rockies. He was officially placed on the 10-day IL Tuesday, but {{{REDACTED-person}}} of the San Diego Union Tribune believes {{{REDACTED-person}}}er will be able to return before the end of the regular season.n• {{{REDACTED-person}}} was not in the lineup Tuesday. An MRI revealed a left-hand bruise. The team will reassess his status Wednesday.n• {{{REDACTED-person}}} is progressing well with his ankle injury and could come off the IL when first eligible Thursday.n• {{{REDACTED-person}}}, dealing with an elbow injury, was back in the lineup Tuesday as the designated hitter.n• {{{REDACTED-person}}} returned to the lineup for the A's in the first game of their doubleheader Tuesday. He had missed seven consecutive games before that.n• {{{REDACTED-person}}} is dealing with right hip tendinitis, but he'll test things out on Wednesday. He didn't rule out a return by Thursday, but his timetable likely depends on how he feels after participating in baseball activities.n• {{{REDACTED-person}}} is dealing with left wrist soreness that kept him out of the lineup for Tuesday's games against the Astrosn• {{{REDACTED-person}}} will play the field at the Blue Jays' alternate training site Tuesday. A weekend return has not been ruled out.n• {{{REDACTED-person}}} threw a live batting practice session that went well Tuesday. He can return as early as Thursday.n• Sticking with the Blue Jays, Row{{{REDACTED-person}}} left Tuesday's game and is set to undergo an MRI on his right knee on Wednesday.n• Ren{{{REDACTED-person}}} was back in the lineup for the Orioles on Tuesday. He had missed the previous two games with a hamstring injury.n• {{{REDACTED-person}}} returned for the first game of their doubleheader Tuesday and went 0-for-3 with a walk and two strikeouts.n• The Cardinals optioned OF {{{REDACTED-person}}} to their alternate training site due to his struggles.n• Recently I mentioned how awesome {{{REDACTED-person}}} has been and that his hype might get out of control heading into the 2021 season. I think the same can be said for Six{{{REDACTED-person}}} who looks like a stud to this point. He tossed another six shutout innings Tuesday against the Braves, striking out six while walking just one. He now has 25 strikeouts to just two walks over his first four starts. It's a small sample but so far he does exactly what you want from a pitcher; get whiffs, limit walks and induce ground balls. I'm starting to think he might be drafted as Top-25 starting pitcher heading into next season.n• Like last season, {{{REDACTED-person}}} got off to a slow start in the power department this year. That's not the case anymore, however, as he's now homered in three straight. Additionally, he has multiple hits in seven of his past 10 games. If you stuck with him throughout his slow start, enjoy reaping the benefits over these final few weeks.n• {{{REDACTED-person}}} is back. After a down season in 2019, he looks like the Jeffress of old. He locked down his sixth save of the season Tuesday, including the fourth consecutive save for the Chicago Cubs. He certainly seems like the go-to guy for manager {{{REDACTED-person}}}. Through his first 15 games this season, Jeffress has a 1.06 ERA and a 0.82 WHIP. In the words of Borat, very nice!n• Man, what are the Dodgers thinking? {{{REDACTED-person}}} clearly isn't himself and now it seems like they rushed him back from his blister issue. According to manager {{{REDACTED-person}}}, the plan is for B{{{REDACTED-person}}} to make his next start although there will be discussions and that nothing's off the table. Buehler last just 2.2 innings on Tuesday, allowing five runs (two earned). It's been quite a weird season for the {{{REDACTED-age}}}.n• If we're talking weird seasons, let's throw J.D. {{{REDACTED-person}}} in the mix. It looks like Martinez was showing signs of life recently and then came to a crashing halt, going 0-for-12 over his last three games. He's struggled big time against right-handed pitching this season to the tune of just a .652 OPS. Like I mentioned in the open, I think Martinez is one of those names you can bench if you have better options, especially in a three-outfielder league.n• It looks like we've hit the end of the road for {{{REDACTED-person}}}. After a nice start to the season, Dobnak has been knocked around in two of his last three starts. On Tuesday, he allowed five runs in just 2.2 innings pitched. His next start comes against the White Sox and, considering Dobnak pitches to contact, I'm not really a fan of that matchup. I'd be OK dropping him for {{{REDACTED-person}}}e or {{{REDACTED-person}}} if they're available.", response.getFilteredText().trim());

    }

    @Test
    public void endToEnd16() throws Exception {

        final Properties properties = new Properties();
        final PhileasConfiguration phileasConfiguration = new PhileasConfiguration(properties);

        final Policy policy = getPolicy("default");

        final String input = "the id is 123456.";

        final PhileasFilterService service = new PhileasFilterService(phileasConfiguration, contextService, vectorService);
        final FilterResponse response = service.filter(policy, "context",  "documentid", input, MimeType.TEXT_PLAIN);

        LOGGER.info(response.getFilteredText());

        showSpans(response.getExplanation().appliedSpans());

        Assertions.assertEquals("documentid", response.getDocumentId());
        Assertions.assertEquals(1, response.getExplanation().appliedSpans().size());
        Assertions.assertEquals("the id is {{{REDACTED-id}}}.", response.getFilteredText().trim());

    }

    @Test
    public void endToEnd17() throws Exception {

        final Properties properties = new Properties();
        final PhileasConfiguration phileasConfiguration = new PhileasConfiguration(properties);

        final Policy policy = getPolicyJustStreetAddress("streetaddress");

        final String input = "he lived at 100 main street";

        final PhileasFilterService service = new PhileasFilterService(phileasConfiguration, contextService, vectorService);
        final FilterResponse response = service.filter(policy, "context",  "documentid", input, MimeType.TEXT_PLAIN);

        LOGGER.info(response.getFilteredText());

        showSpans(response.getExplanation().appliedSpans());

        Assertions.assertEquals("documentid", response.getDocumentId());
        Assertions.assertEquals(1, response.getExplanation().appliedSpans().size());
        Assertions.assertEquals("he lived at {{{REDACTED-street-address}}}", response.getFilteredText().trim());

    }

    @Test
    public void endToEndWithOverlappingSpans() throws Exception {

        BitcoinAddressFilterStrategy bitcoinAddressFilterStrategy = new BitcoinAddressFilterStrategy();
        bitcoinAddressFilterStrategy.setStrategy(AbstractFilterStrategy.MASK);
        bitcoinAddressFilterStrategy.setMaskCharacter("*");
        bitcoinAddressFilterStrategy.setMaskLength("same");
        BitcoinAddress bitcoinAddress = new BitcoinAddress();
        bitcoinAddress.setBitcoinFilterStrategies(List.of(bitcoinAddressFilterStrategy));

        CreditCardFilterStrategy creditCardFilterStrategy = new CreditCardFilterStrategy();
        creditCardFilterStrategy.setStrategy(AbstractFilterStrategy.MASK);
        creditCardFilterStrategy.setMaskCharacter("*");
        creditCardFilterStrategy.setMaskLength("same");
        CreditCard creditCard = new CreditCard();
        creditCard.setCreditCardFilterStrategies(List.of(creditCardFilterStrategy));

        DriversLicenseFilterStrategy driversLicenseFilterStrategy = new DriversLicenseFilterStrategy();
        driversLicenseFilterStrategy.setStrategy(AbstractFilterStrategy.MASK);
        driversLicenseFilterStrategy.setMaskCharacter("*");
        driversLicenseFilterStrategy.setMaskLength("same");
        DriversLicense driversLicense = new DriversLicense();
        driversLicense.setDriversLicenseFilterStrategies(List.of(driversLicenseFilterStrategy));

        Identifiers identifiers = new Identifiers();
        identifiers.setBitcoinAddress(bitcoinAddress);
        identifiers.setCreditCard(creditCard);
        identifiers.setDriversLicense(driversLicense);

        final Policy policy = new Policy();
        policy.setName("default");
        policy.setIdentifiers(identifiers);
        Properties properties = new Properties();
        PhileasConfiguration configuration = new PhileasConfiguration(properties);

        final String input = "the payment method is 4532613702852251 visa or 1Lbcfr7sAHTD9CgdQo3HTMTkV8LK4ZnX71 BTC from user.";

        final PhileasFilterService service = new PhileasFilterService(configuration, contextService, vectorService);
        final FilterResponse response = service.filter(policy, "context",  "documentid", input, MimeType.TEXT_PLAIN);

        LOGGER.info(response.getFilteredText());

        showSpans(response.getExplanation().appliedSpans());

        Assertions.assertEquals(2, response.getExplanation().appliedSpans().size());
        Assertions.assertEquals("the payment method is **************** visa or ********************************** BTC from user.", response.getFilteredText());

        // We do NOT want driver's license to show up as a span.
        for(final Span span : response.getExplanation().appliedSpans()) {
            Assertions.assertTrue(span.getFilterType().getType().equals("bitcoin-address") || span.getFilterType().getType().equals("credit-card"));
        }

    }

    @Test
    public void endToEndJustPhoneNumbers() throws Exception {

        final Properties properties = new Properties();
        final PhileasConfiguration phileasConfiguration = new PhileasConfiguration(properties);

        final Policy policy = getPolicyJustPhoneNumber("phonenumbers");

        final String input = "his number is 123-456-7890. her number is 9999999999. her number is 102-304-5678.";

        final PhileasFilterService service = new PhileasFilterService(phileasConfiguration, contextService, vectorService);
        final FilterResponse response = service.filter(policy, "context",  "documentid", input, MimeType.TEXT_PLAIN);

        LOGGER.info(response.getFilteredText());

        LOGGER.info("Identified spans:");
        showSpans(response.getExplanation().identifiedSpans());

        LOGGER.info("Applied spans:");
        showSpans(response.getExplanation().appliedSpans());

        Assertions.assertEquals("documentid", response.getDocumentId());
        Assertions.assertEquals(1, response.getExplanation().appliedSpans().size());
        Assertions.assertEquals(3, response.getExplanation().identifiedSpans().size());
        Assertions.assertEquals("his number is {{{REDACTED-phone-number}}}. her number is 9999999999. her number is 102-304-5678.", response.getFilteredText().trim());

    }

    @Test
    public void endToEndWithPolicyAsObject() throws Exception {

        final Policy policy = getPolicyJustStreetAddress("streetaddress");

        final Properties properties = new Properties();
        final PhileasConfiguration phileasConfiguration = new PhileasConfiguration(properties);

        final String input = "he lived at 100 main street";

        final PhileasFilterService service = new PhileasFilterService(phileasConfiguration, contextService, vectorService);
        final FilterResponse response = service.filter(policy, "context",  "documentid", input, MimeType.TEXT_PLAIN);

        LOGGER.info(response.getFilteredText());

        showSpans(response.getExplanation().appliedSpans());

        Assertions.assertEquals("documentid", response.getDocumentId());
        Assertions.assertEquals(1, response.getExplanation().appliedSpans().size());
        Assertions.assertEquals("he lived at {{{REDACTED-street-address}}}", response.getFilteredText().trim());

    }
    @Test
    public void endToEndUsingCustomDictionary() throws Exception {

        final CustomDictionary customDictionary = new CustomDictionary();
        customDictionary.setCustomDictionaryFilterStrategies(List.of(new CustomDictionaryFilterStrategy()));
        customDictionary.setTerms(List.of("george", "samuel"));

        final Policy policy = new Policy();
        policy.setName("custom-dictionary");
        policy.getIdentifiers().setCustomDictionaries(List.of(customDictionary));

        final Properties properties = new Properties();
        final PhileasConfiguration phileasConfiguration = new PhileasConfiguration(properties);

        final PhileasFilterService service = new PhileasFilterService(phileasConfiguration, contextService, vectorService);
        final FilterResponse response = service.filter(policy, "context",  "documentid", "his name was samuel and george.", MimeType.TEXT_PLAIN);

        LOGGER.info(response.getFilteredText());

        Assertions.assertEquals("his name was {{{REDACTED-custom-dictionary}}} and {{{REDACTED-custom-dictionary}}}.", response.getFilteredText());
        Assertions.assertEquals("documentid", response.getDocumentId());

    }

    @Test
    public void endToEndUsingCustomDictionaryFileFuzzyDictionaryFilter() throws Exception {

        final Path temp = Files.createTempDirectory("philter");

        final String terms = IOUtils.toString(this.getClass().getResourceAsStream("/customdictionaries/terms1.txt"), Charset.defaultCharset());
        final File termsFile = Paths.get(temp.toFile().getAbsolutePath(), "terms1.txt").toFile();
        FileUtils.writeStringToFile(termsFile, terms, Charset.defaultCharset());
        LOGGER.info("Terms file written to {}", termsFile.getAbsolutePath());

        final CustomDictionary customDictionary = new CustomDictionary();
        customDictionary.setFiles(List.of(termsFile.getAbsolutePath()));
        customDictionary.setCustomDictionaryFilterStrategies(List.of(new CustomDictionaryFilterStrategy()));
        customDictionary.setClassification("names");
        customDictionary.setTerms(List.of("george"));

        final Policy policy = new Policy();
        policy.setName("custom-dictionary");
        policy.getIdentifiers().setCustomDictionaries(List.of(customDictionary));

        final Properties properties = new Properties();
        final PhileasConfiguration phileasConfiguration = new PhileasConfiguration(properties);

        final PhileasFilterService service = new PhileasFilterService(phileasConfiguration, contextService, vectorService);
        final FilterResponse response = service.filter(policy, "context",  "documentid", "his name was samuel and george.", MimeType.TEXT_PLAIN);

        LOGGER.info(response.getFilteredText());

        Assertions.assertEquals("his name was {{{REDACTED-custom-dictionary}}} and {{{REDACTED-custom-dictionary}}}.", response.getFilteredText());
        Assertions.assertEquals("documentid", response.getDocumentId());

    }

    @Test
    public void endToEndUsingCustomDictionaryFileBloomFilter() throws Exception {

        final Path temp = Files.createTempDirectory("philter");

        final String terms = IOUtils.toString(this.getClass().getResourceAsStream("/customdictionaries/terms1.txt"), Charset.defaultCharset());
        final File termsFile = Paths.get(temp.toFile().getAbsolutePath(), "terms1.txt").toFile();
        FileUtils.writeStringToFile(termsFile, terms, Charset.defaultCharset());
        LOGGER.info("Terms file written to {}", termsFile.getAbsolutePath());

        final CustomDictionary customDictionary = new CustomDictionary();
        customDictionary.setFiles(List.of(termsFile.getAbsolutePath()));
        customDictionary.setCustomDictionaryFilterStrategies(List.of(new CustomDictionaryFilterStrategy()));
        customDictionary.setClassification("names");

        final Policy policy = new Policy();
        policy.setName("custom-dictionary-bloom");
        policy.getIdentifiers().setCustomDictionaries(List.of(customDictionary));

        final Properties properties = new Properties();
        final PhileasConfiguration phileasConfiguration = new PhileasConfiguration(properties);

        final PhileasFilterService service = new PhileasFilterService(phileasConfiguration, contextService, vectorService);
        final FilterResponse response = service.filter(policy, "context",  "documentid", "his name was samuel.", MimeType.TEXT_PLAIN);

        LOGGER.info(response.getFilteredText());

        Assertions.assertEquals("his name was {{{REDACTED-custom-dictionary}}}.", response.getFilteredText());
        Assertions.assertEquals("documentid", response.getDocumentId());

    }

    @Test
    public void endToEndWithoutDocumentId() throws Exception {

        final Properties properties = new Properties();
        final PhileasConfiguration phileasConfiguration = new PhileasConfiguration(properties);

        final Policy policy = getPolicy("default");

        final PhileasFilterService service = new PhileasFilterService(phileasConfiguration, contextService, vectorService);
        final FilterResponse response = service.filter(policy, "context",  null, "his name was JEFF.", MimeType.TEXT_PLAIN);

        LOGGER.info("Generated document ID: " + response.getDocumentId());
        LOGGER.info(response.getFilteredText());

        Assertions.assertEquals("his name was {{{REDACTED-person}}}.", response.getFilteredText());
        Assertions.assertNotNull(response.getDocumentId());

    }

    @Test
    public void endToEndMultiplePolicies() throws Exception {

        final Properties properties = new Properties();
        final PhileasConfiguration phileasConfiguration = new PhileasConfiguration(properties);

        final Policy policy = getPolicyJustCreditCard("justcreditcard");

        final PhileasFilterService service = new PhileasFilterService(phileasConfiguration, contextService, vectorService);
        final FilterResponse response = service.filter(policy, "context",  "documentid", "My email is test@something.com", MimeType.TEXT_PLAIN);

        LOGGER.info(response.getFilteredText());

        Assertions.assertEquals("My email is test@something.com", response.getFilteredText());

    }

    @Test
    public void endToEndJustCreditCard() throws Exception {

        final Properties properties = new Properties();
        final PhileasConfiguration phileasConfiguration = new PhileasConfiguration(properties);

        final Policy policy = getPolicyJustCreditCard("justcreditcard");

        final PhileasFilterService service = new PhileasFilterService(phileasConfiguration, contextService, vectorService);
        final FilterResponse response = service.filter(policy, "context",  "documentid", "My cc is 4121742025464465", MimeType.TEXT_PLAIN);

        LOGGER.info(response.getFilteredText());

        Assertions.assertEquals("My cc is {{{REDACTED-credit-card}}}", response.getFilteredText());

    }

    @Test
    public void endToEndJustCreditCardInUnixTimestamp() throws Exception {

        final Properties properties = new Properties();
        final PhileasConfiguration phileasConfiguration = new PhileasConfiguration(properties);

        final Policy policy = getPolicyJustCreditCardNotInUnixTimestamps("justcreditcard");

        final PhileasFilterService service = new PhileasFilterService(phileasConfiguration, contextService, vectorService);
        final FilterResponse response = service.filter(policy, "context",  "documentid", "My cc is 1647725122227", MimeType.TEXT_PLAIN);

        LOGGER.info(response.getFilteredText());
        showSpans(response.getExplanation().identifiedSpans());
        Assertions.assertEquals("My cc is 1647725122227", response.getFilteredText());

    }

    @Test
    public void endToEndJustCreditCardWithIgnoredTerms() throws Exception {

        final Properties properties = new Properties();
        final PhileasConfiguration phileasConfiguration = new PhileasConfiguration(properties);

        final Policy policy = getPolicyJustCreditCard("justcreditcard");

        final PhileasFilterService service = new PhileasFilterService(phileasConfiguration, contextService, vectorService);
        final FilterResponse response = service.filter(policy, "context",  "documentid", "My cc is 4121742025464400", MimeType.TEXT_PLAIN);

        LOGGER.info(response.getFilteredText());

        Assertions.assertEquals("My cc is 4121742025464400", response.getFilteredText());

    }

    @Test
    public void endToEndWithFilterSpecificIgnoredTerms() throws Exception {

        final Properties properties = new Properties();
        final PhileasConfiguration phileasConfiguration = new PhileasConfiguration(properties);

        final Policy policy = getPolicyZipCodeWithIgnored("default");

        final PhileasFilterService service = new PhileasFilterService(phileasConfiguration, contextService, vectorService);
        final FilterResponse response = service.filter(policy, "context",  "documentid", "George Washington was president and his ssn was 123-45-6789 and he lived at 90210.", MimeType.TEXT_PLAIN);

        LOGGER.info(response.getFilteredText());

        Assertions.assertEquals("George Washington was president and his ssn was {{{REDACTED-ssn}}} and he lived at 90210.", response.getFilteredText());

    }

    @Test
    public void endToEndWithSSNAndZipCode() throws Exception {

        final Properties properties = new Properties();
        final PhileasConfiguration phileasConfiguration = new PhileasConfiguration(properties);

        final Policy policy = getPolicy("default");

        final PhileasFilterService service = new PhileasFilterService(phileasConfiguration, contextService, vectorService);
        final FilterResponse response = service.filter(policy, "context",  "documentid", "George Washington was president and his ssn was 123-45-6789 and he lived at 90210.", MimeType.TEXT_PLAIN);

        LOGGER.info(response.getFilteredText());

        Assertions.assertEquals("George Washington was president and his ssn was {{{REDACTED-ssn}}} and he lived at {{{REDACTED-zip-code}}}.", response.getFilteredText());

    }

    @Test
    public void endToEndNonexistentPolicy() throws Exception {

        final Properties properties = new Properties();
        final PhileasConfiguration phileasConfiguration = new PhileasConfiguration(properties);

        final Policy policy = getPolicy("custom1");

        Assertions.assertThrows(FileNotFoundException.class, () -> {

            final PhileasFilterService service = new PhileasFilterService(phileasConfiguration, contextService, vectorService);
            service.filter(policy, "context",  "documentid", "My email is test@something.com", MimeType.TEXT_PLAIN);

        });

    }

    private void showSpans(List<Span> spans) {

        for(Span span : spans) {
            LOGGER.info(span.toString());
        }

    }

}
