package com.mtnfog.test.phileas.services;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mtnfog.phileas.configuration.PhileasConfiguration;
import com.mtnfog.phileas.model.enums.MimeType;
import com.mtnfog.phileas.model.objects.Span;
import com.mtnfog.phileas.model.profile.FilterProfile;
import com.mtnfog.phileas.model.profile.Identifiers;
import com.mtnfog.phileas.model.profile.Ignored;
import com.mtnfog.phileas.model.profile.filters.*;
import com.mtnfog.phileas.model.profile.filters.Date;
import com.mtnfog.phileas.model.profile.filters.strategies.AbstractFilterStrategy;
import com.mtnfog.phileas.model.profile.filters.strategies.ai.PersonsFilterStrategy;
import com.mtnfog.phileas.model.profile.filters.strategies.custom.CustomDictionaryFilterStrategy;
import com.mtnfog.phileas.model.profile.filters.strategies.dynamic.*;
import com.mtnfog.phileas.model.profile.filters.strategies.rules.*;
import com.mtnfog.phileas.model.responses.BinaryDocumentFilterResponse;
import com.mtnfog.phileas.model.responses.FilterResponse;
import com.mtnfog.phileas.model.serializers.PlaceholderDeserializer;
import com.mtnfog.phileas.services.PhileasFilterService;
import org.aeonbits.owner.ConfigFactory;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class PhileasFilterServiceTest {

    private static final Logger LOGGER = LogManager.getLogger(PhileasFilterServiceTest.class);

    private String INDEXES_DIRECTORY = "/mtnfog/code/philter/philter/distribution/indexes/";
    private Gson gson;

    @BeforeEach
    public void before() {
        INDEXES_DIRECTORY = System.getProperty( "os.name" ).contains( "indow" ) ? INDEXES_DIRECTORY.substring(1) : INDEXES_DIRECTORY;

        final GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(String.class, new PlaceholderDeserializer());
        gson = gsonBuilder.create();
    }

    @Test
    public void filterProfile() throws IOException, URISyntaxException {

        final FilterProfile filterProfile = getFilterProfile("default");
        final String json = gson.toJson(filterProfile);
        LOGGER.info(json);

        final FilterProfile deserialized = gson.fromJson(json, FilterProfile.class);

        Assertions.assertEquals("default", deserialized.getName());

    }

    @Test
    public void filterProfileWithPlaceholder() throws IOException, URISyntaxException {

        final Ignored ignored = new Ignored();
        ignored.setTerms(Arrays.asList("john", "jeff", "${USER}"));

        final FilterProfile filterProfile = getFilterProfile("placeholder");
        filterProfile.setIgnored(Arrays.asList(ignored));
        final String json = gson.toJson(filterProfile);
        LOGGER.info(json);

        final FilterProfile deserialized = gson.fromJson(json, FilterProfile.class);

        Assertions.assertEquals("placeholder", deserialized.getName());
        Assertions.assertEquals(3, filterProfile.getIgnored().get(0).getTerms().size());
        Assertions.assertTrue(CollectionUtils.isNotEmpty(deserialized.getIgnored().get(0).getTerms()));

    }

    @Test
    public void endToEnd1() throws Exception {

        final Path temp = Files.createTempDirectory("philter");
        final File file = Paths.get(temp.toFile().getAbsolutePath(), "default.json").toFile();
        LOGGER.info("Writing profile to {}", file.getAbsolutePath());
        FileUtils.writeStringToFile(file, gson.toJson(getFilterProfile("default")), Charset.defaultCharset());

        Properties properties = new Properties();
        properties.setProperty("indexes.directory", INDEXES_DIRECTORY);
        properties.setProperty("store.enabled", "false");
        properties.setProperty("filter.profiles.directory", temp.toFile().getAbsolutePath());

        final PhileasConfiguration phileasConfiguration = ConfigFactory.create(PhileasConfiguration.class, properties);

        final PhileasFilterService service = new PhileasFilterService(phileasConfiguration);
        final FilterResponse response = service.filter(Arrays.asList("default"), "context", "documentid", "George Washington was president and his ssn was 123-45-6789 and he lived at 90210.", MimeType.TEXT_PLAIN);

        LOGGER.info(response.getFilteredText());

        Assertions.assertEquals("{{{REDACTED-person}}} was president and his ssn was {{{REDACTED-ssn}}} and he lived at {{{REDACTED-zip-code}}}.", response.getFilteredText());
        Assertions.assertEquals("documentid", response.getDocumentId());

    }

    @Test
    public void endToEnd2() throws Exception {

        final Path temp = Files.createTempDirectory("philter");
        final File file = Paths.get(temp.toFile().getAbsolutePath(), "default.json").toFile();
        LOGGER.info("Writing profile to {}", file.getAbsolutePath());
        FileUtils.writeStringToFile(file, gson.toJson(getFilterProfile("default")), Charset.defaultCharset());

        Properties properties = new Properties();
        properties.setProperty("indexes.directory", INDEXES_DIRECTORY);
        properties.setProperty("store.enabled", "false");
        properties.setProperty("filter.profiles.directory", temp.toFile().getAbsolutePath());

        final PhileasConfiguration phileasConfiguration = ConfigFactory.create(PhileasConfiguration.class, properties);

        PhileasFilterService service = new PhileasFilterService(phileasConfiguration);
        final FilterResponse response = service.filter(Arrays.asList("default"), "context", "documentid", "My email is test@something.com and cc is 4121742025464465", MimeType.TEXT_PLAIN);

        LOGGER.info(response.getFilteredText());

        Assertions.assertEquals("My email is {{{REDACTED-email-address}}} and cc is {{{REDACTED-credit-card}}}", response.getFilteredText());
        Assertions.assertEquals("documentid", response.getDocumentId());

    }

    @Test
    public void endToEnd3() throws Exception {

        final Path temp = Files.createTempDirectory("philter");
        final File file = Paths.get(temp.toFile().getAbsolutePath(), "default.json").toFile();
        LOGGER.info("Writing profile to {}", file.getAbsolutePath());
        FileUtils.writeStringToFile(file, gson.toJson(getFilterProfile("default")), Charset.defaultCharset());

        Properties properties = new Properties();
        properties.setProperty("indexes.directory", INDEXES_DIRECTORY);
        properties.setProperty("store.enabled", "false");
        properties.setProperty("filter.profiles.directory", temp.toFile().getAbsolutePath());

        final PhileasConfiguration phileasConfiguration = ConfigFactory.create(PhileasConfiguration.class, properties);

        PhileasFilterService service = new PhileasFilterService(phileasConfiguration);
        final FilterResponse response = service.filter(Arrays.asList("default"), "context", "documentid", "test@something.com is email and cc is 4121742025464465", MimeType.TEXT_PLAIN);

        LOGGER.info(response.getFilteredText());

        Assertions.assertEquals("{{{REDACTED-email-address}}} is email and cc is {{{REDACTED-credit-card}}}", response.getFilteredText());
        Assertions.assertEquals("documentid", response.getDocumentId());

    }

    @Test
    public void endToEnd4() throws Exception {

        final Path temp = Files.createTempDirectory("philter");
        final File file = Paths.get(temp.toFile().getAbsolutePath(), "default.json").toFile();
        LOGGER.info("Writing profile to {}", file.getAbsolutePath());
        FileUtils.writeStringToFile(file, gson.toJson(getFilterProfile("default")), Charset.defaultCharset());

        Properties properties = new Properties();
        properties.setProperty("indexes.directory", INDEXES_DIRECTORY);
        properties.setProperty("store.enabled", "false");
        properties.setProperty("filter.profiles.directory", temp.toFile().getAbsolutePath());

        final PhileasConfiguration phileasConfiguration = ConfigFactory.create(PhileasConfiguration.class, properties);

        PhileasFilterService service = new PhileasFilterService(phileasConfiguration);
        final FilterResponse response = service.filter(Arrays.asList("default"), "context", "documentid", "test@something.com", MimeType.TEXT_PLAIN);

        LOGGER.info(response.getFilteredText());

        Assertions.assertEquals("{{{REDACTED-email-address}}}", response.getFilteredText());
        Assertions.assertEquals("documentid", response.getDocumentId());

    }

    @Test
    public void endToEnd5() throws Exception {

        final Path temp = Files.createTempDirectory("philter");
        final File file = Paths.get(temp.toFile().getAbsolutePath(), "default.json").toFile();
        LOGGER.info("Writing profile to {}", file.getAbsolutePath());
        FileUtils.writeStringToFile(file, gson.toJson(getFilterProfile("default")), Charset.defaultCharset());

        Properties properties = new Properties();
        properties.setProperty("indexes.directory", INDEXES_DIRECTORY);
        properties.setProperty("store.enabled", "false");
        properties.setProperty("filter.profiles.directory", temp.toFile().getAbsolutePath());

        final PhileasConfiguration phileasConfiguration = ConfigFactory.create(PhileasConfiguration.class, properties);

        PhileasFilterService service = new PhileasFilterService(phileasConfiguration);
        final FilterResponse response = service.filter(Arrays.asList("default"), "context", "documentid", "90210", MimeType.TEXT_PLAIN);

        LOGGER.info(response.getFilteredText());

        Assertions.assertEquals("{{{REDACTED-zip-code}}}", response.getFilteredText());
        Assertions.assertEquals("documentid", response.getDocumentId());

    }

    @Test
    public void endToEnd6() throws Exception {

        final Path temp = Files.createTempDirectory("philter");
        final File file = Paths.get(temp.toFile().getAbsolutePath(), "default.json").toFile();
        LOGGER.info("Writing profile to {}", file.getAbsolutePath());
        FileUtils.writeStringToFile(file, gson.toJson(getFilterProfile("default")), Charset.defaultCharset());

        Properties properties = new Properties();
        properties.setProperty("indexes.directory", INDEXES_DIRECTORY);
        properties.setProperty("store.enabled", "false");
        properties.setProperty("filter.profiles.directory", temp.toFile().getAbsolutePath());

        final PhileasConfiguration phileasConfiguration = ConfigFactory.create(PhileasConfiguration.class, properties);

        PhileasFilterService service = new PhileasFilterService(phileasConfiguration);
        final FilterResponse response = service.filter(Arrays.asList("default"), "context", "documentid", "his name was JEFF.", MimeType.TEXT_PLAIN);

        LOGGER.info(response.getFilteredText());

        Assertions.assertEquals("his name was {{{REDACTED-person}}}.", response.getFilteredText());
        Assertions.assertEquals("documentid", response.getDocumentId());

    }

    @Test
    public void endToEnd7() throws Exception {

        final Path temp = Files.createTempDirectory("philter");
        final File file = Paths.get(temp.toFile().getAbsolutePath(), "default.json").toFile();
        LOGGER.info("Writing profile to {}", file.getAbsolutePath());
        FileUtils.writeStringToFile(file, gson.toJson(getFilterProfile("default")), Charset.defaultCharset());

        Properties properties = new Properties();
        properties.setProperty("indexes.directory", INDEXES_DIRECTORY);
        properties.setProperty("store.enabled", "false");
        properties.setProperty("filter.profiles.directory", temp.toFile().getAbsolutePath());

        final PhileasConfiguration phileasConfiguration = ConfigFactory.create(PhileasConfiguration.class, properties);

        PhileasFilterService service = new PhileasFilterService(phileasConfiguration);
        final FilterResponse response = service.filter(Arrays.asList("default"), "context", "documentid", "he was seen on 10-19-2020.", MimeType.TEXT_PLAIN);

        LOGGER.info(response.getFilteredText());

        Assertions.assertEquals("he was seen on 12-20-2023.", response.getFilteredText());
        Assertions.assertEquals("documentid", response.getDocumentId());

    }

    @Test
    public void endToEnd8() throws Exception {

        final Path temp = Files.createTempDirectory("philter");
        final File file = Paths.get(temp.toFile().getAbsolutePath(), "default.json").toFile();
        LOGGER.info("Writing profile to {}", file.getAbsolutePath());
        FileUtils.writeStringToFile(file, gson.toJson(getFilterProfile("default")), Charset.defaultCharset());

        Properties properties = new Properties();
        properties.setProperty("indexes.directory", INDEXES_DIRECTORY);
        properties.setProperty("store.enabled", "false");
        properties.setProperty("filter.profiles.directory", temp.toFile().getAbsolutePath());

        final PhileasConfiguration phileasConfiguration = ConfigFactory.create(PhileasConfiguration.class, properties);

        PhileasFilterService service = new PhileasFilterService(phileasConfiguration);
        final FilterResponse response = service.filter(Arrays.asList("default"), "context", "documentid",
                "George Washington was president." + System.lineSeparator() + "Abraham Lincoln was president.", MimeType.TEXT_PLAIN);

        LOGGER.info(response.getFilteredText());

        // Ensure that the line separator in the input text was not removed.
        Assertions.assertTrue(response.getFilteredText().contains(System.lineSeparator()));

    }

    @Test
    public void endToEnd9() throws Exception {

        final Path temp = Files.createTempDirectory("philter");
        final File file = Paths.get(temp.toFile().getAbsolutePath(), "default.json").toFile();
        LOGGER.info("Writing profile to {}", file.getAbsolutePath());
        FileUtils.writeStringToFile(file, gson.toJson(getFilterProfile("default")), Charset.defaultCharset());

        Properties properties = new Properties();
        properties.setProperty("indexes.directory", INDEXES_DIRECTORY);
        properties.setProperty("store.enabled", "false");
        properties.setProperty("filter.profiles.directory", temp.toFile().getAbsolutePath());

        final PhileasConfiguration phileasConfiguration = ConfigFactory.create(PhileasConfiguration.class, properties);

        final PhileasFilterService service = new PhileasFilterService(phileasConfiguration);
        final FilterResponse response = service.filter(Arrays.asList("default"), "context", "documentid", "George Washington was president and his ssn was 123-45-6789 and he lived at 90210. The name 456 should be filtered. Jeff Smith should be ignored.", MimeType.TEXT_PLAIN);

        LOGGER.info(response.getFilteredText());

        Assertions.assertEquals("{{{REDACTED-person}}} was president and his ssn was {{{REDACTED-ssn}}} and he lived at {{{REDACTED-zip-code}}}. The name 456 should be filtered. {{{REDACTED-person}}} should be ignored.", response.getFilteredText());
        Assertions.assertEquals("documentid", response.getDocumentId());

    }

    @Test
    public void endToEnd10() throws Exception {

        // The purpose of this test is to test an input that was throwing a Span error in OpenNLP.
        // The test should run without error.

        /*
        Nov 15 23:38:34 ip-10-0-2-209 bash[585]: java.lang.IllegalArgumentException: start index must be zero or greater: -1
        Nov 15 23:38:34 ip-10-0-2-209 bash[585]:         at opennlp.tools.util.Span.<init>(Span.java:56) ~[opennlp-tools-2.1.0.jar!/:2.1.0]
         */

        final Path temp = Files.createTempDirectory("philter");
        final File file = Paths.get(temp.toFile().getAbsolutePath(), "default.json").toFile();
        LOGGER.info("Writing profile to {}", file.getAbsolutePath());
        FileUtils.writeStringToFile(file, gson.toJson(getFilterProfile("default")), Charset.defaultCharset());

        Properties properties = new Properties();
        properties.setProperty("indexes.directory", INDEXES_DIRECTORY);
        properties.setProperty("store.enabled", "false");
        properties.setProperty("filter.profiles.directory", temp.toFile().getAbsolutePath());

        final PhileasConfiguration phileasConfiguration = ConfigFactory.create(PhileasConfiguration.class, properties);

        /*
        spanText: Doug Reddy
        text: All Dollar Figures In Us Dollars, Unless Otherwise Indicated Vancouver, May 15, 2020 /prnewswire/ - Equinox Gold Corp. (tsx: Eqx, Nyse American: Eqx) (equinox Gold Or The Company) Is Pleased To Report Its First Quarter 2020 Summary Financial And Operating Results. The Company's Unaudited Condensed Consolidated Interim Financial Statements And Related Management's Discussion And Analysis For The Three Months Ended March 31, 2020 Will Be Available For Download Shortly On Sedar, On Edgar And On The Company's Website. The Company Will Host A Conference Call And Webcast Today Commencing At 2:00 Pm Vancouver Time To Discuss The Company's Business Strategy And Objectives, First Quarter Results And Activities Underway At The Company's Projects. Further Details Are Provided At The End Of This News Release. Equinox Gold Had A Strong First Quarter Bolstered By Production From Our Newly Acquired Assets And Achieved Record Gold Production And Record Earnings From Mine Operations, Despite Navigating Challenges Related To The Covid-19 Pandemic, Said Christian Milau, Chief Executive Officer. With More Than $350 Million Of Cash On Hand, Equinox Gold Is In A Strong Financial Position And Fully Funded For Its Organic Growth Plans. Castle Mountain Phase 1 Construction Is 75% Complete, The Los Filos Expansion And Santa Luz Restart Projects Are Expected To Significantly Increase Production Over The Course Of 2021 And 2022 And We Recently Completed A Positive Preliminary Economic Assessment For Development Of An Underground Mine At Aurizona. While The Temporary Suspension Of Mining Activities At Our Los Filos Mine In Mexico Will Affect Q2 Production, Our Other Mines Have Experienced Only Minimal Covid-19 Disruption To Date. We Remain Focused On Achieving Our Growth Objectives While Continuing To Maintain Appropriate Operational And Safety Procedures To Help Protect The Health And Economic Wellbeing Of Our Workforce And Local Communities. Highlights For The Three Months Ended March 31, 2020(1)n• Completed 3.5 Million Work Hours With Three Lost-time Injuries Across All Sitesn• Produced 88,951 Ounces (oz) Of Gold And Sold 82,629 Oz Of Gold (excluding Leagold Production Prior To The Merger Close On )n• Mine Cash Costs Of /oz(2) And All-in-sustaining Costs (aisc) Of /oz(2,3)n• Ebitda Of (2) And Adjusted Ebitda Of (2,4)n• Net Income Of Or Per Sharen• Adjusted Net Income Of (2,4) Or Per Sharen• Cash Flow From Operations Before Changes In Working Capital Ofn• Cash And Cash Equivalents (unrestricted) Of (more Than At )n• Completed At-market Merger With Leagold And Concurrent Financing Package That Included:n• Added To The Gdxj (vaneck Vectors Junior Gold Miners Etf) Onn• Drew The Remaining From The Revolving Credit Facility As A Covid-19 Proactive Measuren• Production Of 540,000 To 600,000 Ounces Of Gold(5)n• Aisc Of To Per Oz Of Gold Sold(2)n• Sustaining Capital Of And Expansion Capital Of (5)n• Guidance To Be Updated When Practicaln• Phase 1 Construction 75% Complete With First Gold Pour Expected In Q3 2020n• Guadalupe Open Pit And Bermejal Underground Development Activities Continued During The Quarter But Were Suspended At The Beginning Of April In Compliance With Mexican Covid-19 Restrictions; First Ore Anticipated In Early 2021n• Updating Study On New Carbon-in-leach Plant To Incorporate Several Optimization Opportunities To Prepare For A Project Construction Decision Later This Yearn• Commenced An Update And Review Of Costs And Engineering For The Resin-in-leach Plant To Prepare For A Project Construction Decision Later This Yearn• Added To The Gdx (vaneck Vectors Gold Miners Etf) Onn• Received Approximately From The Issuance Of Convertible Notes And Common Shares Related To A Shareholder's Anti-dilution Rightn• Completed A Positive Preliminary Economic Assessment (pea) For Potential Development Of An Underground Mine At Aurizona That Could Be Operated Concurrently With The Existing Open-pit Mine, Demonstrating Potential For Both Mine Life Extension And Increased Annual Gold Production(6)n• Completed Updated Reserve And Resource Estimates For Aurizona And Mesquiten• Los Filos Preparing To Safely Resume Operations Following The Mexico Federal Government Declaration On That Mining Is An Essential Activity Equinox Gold Took Early Precautionary Measures At Its Mine Sites And Offices To Proactively Manage Issues Related To The Covid-19 Pandemic. Each Of The Company's Operations Has Implemented Preventive Measures In Collaboration With The Company's Employees, Contractors, Local Communities And Governments To Help Ensure The Health, Safety And Economic Wellbeing Of The Company's Workforce And Local Communities. The Company Has Also Evaluated Supply Chain And Other Risks At Each Operation And Implemented Business Continuity Protocols So The Mines Can Operate As Effectively As Possible. The Company's Operations Have Experienced Limited Disruption To Date, With The Exception Of The Company's Los Filos Mine In Mexico At Which Mining And Development Activities Were Suspended At The Beginning Of April In Compliance With A Mexico Federal Government Order Requiring The Temporary Suspension Of All Non-essential Businesses. On May 14th The Mexico Federal Government Declared Mining An Essential Activity And Allowed For The Restart Of Operations On June 1st. The Company Is Preparing To Safely Resume Operations. Additional Information Regarding Equinox Gold's Covid-19 Response Plan, Preventive Measures Taken To Date And The Potential Impact On Operations Is Available In The Q1 2020 Management's Discussion And Analysis And On The Company's Website At Www.equinoxgold.com. The Company Drew The Remaining $180 Million From Its $400 Million Revolving Credit Facility In Late March As A Proactive Measure Given The Uncertainty Of The Potential Effects Of The Covid-19 Pandemic On The Company's Operations. There Are No Current Plans To Spend These Funds And Equinox Gold Remains In A Strong Financial Position With More Than $350 Million In Cash And Cash Equivalents (unrestricted) At The Date Of This News Release. Operating And Financial Results For The Three Months Ended March 31, 2020 Selected Financial Results For The Three Months Ended March 31, 2020 Additional Information Regarding The Company's Financial Results And Activities Underway At The Company's Projects Are Available In The Company's Q1 2020 Financial Statements And Accompanying Management's Discussion And Analysis For The Three Months Ended March 31, 2020, Which Will Be Available For Download Shortly On The Company's Website At Www.equinoxgold.com, On Sedar At Www.sedar.com And On Edgar At Www.sec.gov/edgar. Equinox Gold Will Host A Conference Call And Webcast On May 15 Commencing At 2:00 Pm Vancouver Time To Discuss The Company's Business Strategy And Objectives, First Quarter Results And Activities Underway At The Company's Projects. All Participants Will Have The Opportunity To Ask Questions Of Equinox Gold's Chairman, Ceo And Executive Team. The Webcast Will Be Archived On Equinox Gold's Website Until August 15, 2020. Equinox Gold Is A Canadian Mining Company With Six Producing Gold Mines, A Multi-million-ounce Gold Reserve Base And A Strong Growth Profile From Two Development Projects And Two Expansion Projects. Equinox Gold Operates Entirely In The Americas, With Two Properties In The United States, One In Mexico And Five In Brazil. Equinox Gold's Common Shares Are Listed On The Tsx And The Nyse American Under The Trading Symbol Eqx. Further Information About Equinox Gold's Portfolio Of Assets And Long-term Growth Strategy Is Available At Www.equinoxgold.com Or By Email At [email Protected]. This News Release Refers To Cash Costs, Cash Costs Per Ounce Sold, All-in Sustaining Costs (aisc), Aisc Per Ounce Sold, Adjusted Ebitda And Sustaining And Non-sustaining Capital Expenditures That Are Measures With No Standardized Meaning Under International Financial Reporting Standards (ifrs), I.e. They Are Non-ifrs Measures, And May Not Be Comparable To Similar Measures Presented By Other Companies. Their Measurement And Presentation Is Intended To Provide Additional Information And Should Not Be Considered In Isolation Or As A Substitute For Measures Of Performance Prepared In Accordance With Ifrs. Aisc Per Gold Oz Sold Is A Non-ifrs Measure Based On Guidance Announced By The World Gold Council (wgc) In September 2013 And Updated In November 2018. The Wgc Is A Non-profit Association Of The World's Leading Gold Mining Companies Established In 1987 To Promote The Use Of Gold To Industry, Consumers And Investors. The Wgc Is Not A Regulatory Body And Does Not Have The Authority To Develop Accounting Standards Or Disclosure Requirements. The Wgc Has Worked With Its Member Companies To Develop A Measure That Expands On Ifrs Measures Such As Operating Expenses And Non-ifrs Measures To Provide Visibility Into The Economics Of A Gold Mining Company. Current Ifrs Measures Used In The Gold Industry, Such As Operating Expenses, Do Not Capture All Of The Expenditures Incurred To Discover, Develop And Sustain Gold Production. The Company Believes The Aisc Measure Provides Further Transparency Into Costs Associated With Producing Gold And Will Assist Analysts, Investors And Other Stakeholders Of The Company In Assessing Its Operating Performance, Its Ability To Generate Free Cash Flow From Current Operations And Its Overall Value. Combined Aisc Does Not Include Corporate G&a. Adriaan (attie) Roux, Pr.sci.nat., Equinox Gold's Coo, Doug Reddy, Equinox Gold's Evp Technical Services And Scott Heffernan, Msc, P.geo. Equinox Gold's Evp Exploration, Are The Qualified Persons Under National Instrument 43-101 For Equinox Gold And Have Reviewed, Approved And Verified The Technical Content Of This Document. This News Release Contains Certain Forward-looking Information And Forward-looking Statements Within The Meaning Of Applicable Securities Legislation And May Include Future-oriented Financial Information. Forward-looking Statements And Forward-looking Information In This News Release Relate To, Among Other Things: The Duration, Extent And Other Implications Of The Novel Coronavirus (covid-19) And Any Related Restrictions And Suspensions With Respect To The Company's Operations, The Strategic Vision For The Company And Expectations Regarding Expanding Production Capabilities And Future Financial Or Operating Performance, Equinox Gold's Production And Cost Guidance, And Conversion Of Mineral Resources To Mineral Reserves. Forward-looking Statements Or Information Generally Identified By The Use Of The Words Believe, Will, Advancing, Strategy, Plans, Budget, Anticipated, Expected, Estimated, Target, Objective And Similar Expressions And Phrases Or Statements That Certain Actions, Events Or Results May, Could, Should, Will Be Taken Or Be Achieved, Or The Negative Connotation Of Such Terms, Are Intended To Identify Forward-looking Statements And Information. Although The Company Believes That The Expectations Reflected In Such Forward-looking Statements And Information Are Reasonable, Undue Reliance Should Not Be Placed On Forward-looking Statements Since The Company Can Give No Assurance That Such Expectations Will Prove To Be Correct. The Company Has Based These Forward-looking Statements And Information On The Company's Current Expectations And Projections About Future Events And These Assumptions Include: Tonnage Of Ore To Be Mined And Processed; Ore Grades And Recoveries; Prices For Gold Remaining As Estimated; Development At Los Filos, Castle Mountain, Santa Luz And Aurizona Being Completed And Performed In Accordance With Current Expectations; Currency Exchange Rates Remaining As Estimated; Availability Of Funds For The Company's Projects And Future Cash Requirements; Capital, Decommissioning And Reclamation Estimates; The Company's Mineral Reserve And Resource Estimates And The Assumptions On Which They Are Based; Prices For Energy Inputs, Labour, Materials, Supplies And Services; No Labour-related Disruptions And No Unplanned Delays Or Interruptions In Scheduled Development And Production; All Necessary Permits, Licenses And Regulatory Approvals Are Received In A Timely Manner; And The Company's Ability To Comply With Environmental, Health And Safety Laws. The Company's Previously Announced Guidance Is Included In This News Release And Does Not Account For Any Possible Adverse Effects Of Covid-19 To The Company's Business And Results Of Operations. While The Company Considers These Assumptions To Be Reasonable Based On Information Currently Available, They May Prove To Be Incorrect. Accordingly, Readers Are Cautioned Not To Put Undue Reliance On The Forward-looking Statements Or Information Contained In This News Release. The Company Cautions That Forward-looking Statements And Information Involve Known And Unknown Risks, Uncertainties And Other Factors That May Cause Actual Results And Developments To Differ News Release Materially From Those Expressed Or Implied By Such Forward-looking Statements And Information Contained In This And The Company Has Made Assumptions And Estimates Based On Or Related To Many Of These Factors. Such Factors Include, Without Limitation: Fluctuations In Gold Prices; Fluctuations In Prices For Energy Inputs, Labour, Materials, Supplies And Services; Fluctuations In Currency Markets; Operational Risks And Hazards Inherent With The Business Of Mining (including Environmental Accidents And Hazards, Industrial Accidents, Equipment Breakdown, Unusual Or Unexpected Geological Or Structural Formations, Cave-ins, Flooding And Severe Weather); Inadequate Insurance, Or Inability To Obtain Insurance To Cover These Risks And Hazards; Employee Relations; Relationships With, And Claims By, Local Communities And Indigenous Populations; The Company's Ability To Obtain All Necessary Permits, Licenses And Regulatory Approvals In A Timely Manner Or At All; Changes In Laws, Regulations And Government Practices, Including Environmental, Export And Import Laws And Regulations; Legal Restrictions Relating To Mining Including Those Imposed In Connection With Covid-19; Risks Relating To Expropriation; Increased Competition In The Mining Industry; And Those Factors Identified In The Company's Md&a Dated February 28, 2020 For The Year-ended December 31, 2019 And Its Annual Information Form Dated May 13, 2020, Which Are Available On Sedar At Www.sedar.com And On Edgar At Www.sec.gov/edgar. Forward-looking Statements And Information Are Designed To Help Readers Understand Management's Views As Of That Time With Respect To Future Events And Speak Only As Of The Date They Are Made. Except As Required By Applicable Law, The Company Assumes No Obligation And Does Not Intend To Update Or To Publicly Announce The Results Of Any Change To Any Forward-looking Statement Or Information Contained Or Incorporated By Reference To Reflect Actual Results, Future Events Or Developments, Changes In Assumptions Or Changes In Other Factors Affecting The Forward-looking Statements And Information. If The Company Updates Any One Or More Forward-looking Statements, No Inference Should Be Drawn That The Company Will Make Additional Updates With Respect To Those Or Other Forward-looking Statements. All Forward-looking Statements And Information Contained In This News Release Are Expressly Qualified In Their Entirety By This Cautionary Statement.
        characterStart: 9498
        actual: 9502
        spanText: Scott Heffernan
         */

        final String input = IOUtils.toString(this.getClass().getResourceAsStream("/inputs/1.txt"), Charset.defaultCharset());

        final PhileasFilterService service = new PhileasFilterService(phileasConfiguration);
        final FilterResponse response = service.filter(Arrays.asList("default"), "context", "documentid", input, MimeType.TEXT_PLAIN);

        LOGGER.info(response.getFilteredText());

        Assertions.assertEquals("documentid", response.getDocumentId());

    }

    @Test
    public void endToEnd11() throws Exception {

        final Path temp = Files.createTempDirectory("philter");
        final File file = Paths.get(temp.toFile().getAbsolutePath(), "default.json").toFile();
        LOGGER.info("Writing profile to {}", file.getAbsolutePath());
        FileUtils.writeStringToFile(file, gson.toJson(getFilterProfile("default")), Charset.defaultCharset());

        Properties properties = new Properties();
        properties.setProperty("indexes.directory", INDEXES_DIRECTORY);
        properties.setProperty("store.enabled", "false");
        properties.setProperty("filter.profiles.directory", temp.toFile().getAbsolutePath());

        final PhileasConfiguration phileasConfiguration = ConfigFactory.create(PhileasConfiguration.class, properties);

        final String input = "IN THE UNITED STATES DISTRICT COURT \nEASTERN DISTRICT OF ARKANSAS \nWESTERN DIVISION \nJAMES EDWARD SMITH, \nafk/a James Edward Bridges, \nADC#103093 \nv. No. 4:14-cv-455-DPM \nPLAINTIFF \nCHARLES A. SMITH; \nMARY ANN CONLEY, \nafk/a Mary Ann Smith; and \nROBERT CASTILLOW DEFENDANTS \nORDER \nJames Smith's prose complaint must be dismissed without prejudice. \nHe hasn't paid the filing fee, moved to proceed in forma pauperis, or provided \nproof of service on any defendant. FED. R. CIV. P. 4(I); Local Rule 5.5(c)(2). \nSo Ordered. \nD.P. Marshall Jr. \nUnited States District Judge \nCase 4:14-cv-00455-DPM   Document 2   Filed 12/09/14   Page 1 of 1\n";

        final PhileasFilterService service = new PhileasFilterService(phileasConfiguration);
        final FilterResponse response = service.filter(Arrays.asList("default"), "context", "documentid", input, MimeType.TEXT_PLAIN);

        LOGGER.info(response.getFilteredText());

        //Assertions.assertEquals("George Washington was president and his ssn was {{{REDACTED-ssn}}} and he lived at {{{REDACTED-zip-code}}}. The name 456 should be filtered. Jeff Smith should be ignored.", response.getFilteredText());
        Assertions.assertEquals("documentid", response.getDocumentId());

    }

    @Test
    public void endToEndUsingCustomDictionary() throws Exception {

        final CustomDictionary customDictionary = new CustomDictionary();
        customDictionary.setCustomDictionaryFilterStrategies(Arrays.asList(new CustomDictionaryFilterStrategy()));
        customDictionary.setTerms(Arrays.asList("george", "samuel"));
        customDictionary.setFuzzy(false);

        final FilterProfile filterProfile = new FilterProfile();
        filterProfile.setName("custom-dictionary");
        filterProfile.getIdentifiers().setCustomDictionaries(Arrays.asList(customDictionary));

        final Path temp = Files.createTempDirectory("philter");
        final File file = Paths.get(temp.toFile().getAbsolutePath(), "default.json").toFile();
        FileUtils.writeStringToFile(file, gson.toJson(filterProfile), Charset.defaultCharset());
        LOGGER.info("Filter profile written to {}", file.getAbsolutePath());

        final Properties properties = new Properties();
        properties.setProperty("indexes.directory", INDEXES_DIRECTORY);
        properties.setProperty("store.enabled", "false");
        properties.setProperty("filter.profiles.directory", temp.toFile().getAbsolutePath());

        final PhileasConfiguration phileasConfiguration = ConfigFactory.create(PhileasConfiguration.class, properties);

        final PhileasFilterService service = new PhileasFilterService(phileasConfiguration);
        final FilterResponse response = service.filter(Arrays.asList("default"), "context", "documentid", "his name was samuel and george.", MimeType.TEXT_PLAIN);

        LOGGER.info(response.getFilteredText());

        Assertions.assertEquals("his name was {{{REDACTED-custom-dictionary}}} and {{{REDACTED-custom-dictionary}}}.", response.getFilteredText());
        Assertions.assertEquals("documentid", response.getDocumentId());

    }

    @Test
    public void endToEndUsingCustomDictionaryFileLuceneFilter() throws Exception {

        final Path temp = Files.createTempDirectory("philter");

        final String terms = IOUtils.toString(this.getClass().getResourceAsStream("/customdictionaries/terms1.txt"), Charset.defaultCharset());
        final File termsFile = Paths.get(temp.toFile().getAbsolutePath(), "terms1.txt").toFile();
        FileUtils.writeStringToFile(termsFile, terms, Charset.defaultCharset());
        LOGGER.info("Terms file written to {}", termsFile.getAbsolutePath());

        final CustomDictionary customDictionary = new CustomDictionary();
        customDictionary.setFiles(Arrays.asList(termsFile.getAbsolutePath()));
        customDictionary.setCustomDictionaryFilterStrategies(Arrays.asList(new CustomDictionaryFilterStrategy()));
        customDictionary.setClassification("names");
        customDictionary.setTerms(Arrays.asList("george"));
        customDictionary.setFuzzy(false);
        customDictionary.setSensitivity("low");

        final FilterProfile filterProfile = new FilterProfile();
        filterProfile.setName("custom-dictionary");
        filterProfile.getIdentifiers().setCustomDictionaries(Arrays.asList(customDictionary));

        final File file = Paths.get(temp.toFile().getAbsolutePath(), "default.json").toFile();
        FileUtils.writeStringToFile(file, gson.toJson(filterProfile), Charset.defaultCharset());
        LOGGER.info("Filter profile written to {}", file.getAbsolutePath());

        final Properties properties = new Properties();
        properties.setProperty("indexes.directory", INDEXES_DIRECTORY);
        properties.setProperty("store.enabled", "false");
        properties.setProperty("filter.profiles.directory", temp.toFile().getAbsolutePath());

        final PhileasConfiguration phileasConfiguration = ConfigFactory.create(PhileasConfiguration.class, properties);

        final PhileasFilterService service = new PhileasFilterService(phileasConfiguration);
        final FilterResponse response = service.filter(Arrays.asList("default"), "context", "documentid", "his name was samuel and george.", MimeType.TEXT_PLAIN);

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
        customDictionary.setFiles(Arrays.asList(termsFile.getAbsolutePath()));
        customDictionary.setCustomDictionaryFilterStrategies(Arrays.asList(new CustomDictionaryFilterStrategy()));
        customDictionary.setClassification("names");
        customDictionary.setFuzzy(false);

        final FilterProfile filterProfile = new FilterProfile();
        filterProfile.setName("custom-dictionary-bloom");
        filterProfile.getIdentifiers().setCustomDictionaries(Arrays.asList(customDictionary));

        final File file = Paths.get(temp.toFile().getAbsolutePath(), "default.json").toFile();
        FileUtils.writeStringToFile(file, gson.toJson(filterProfile), Charset.defaultCharset());
        LOGGER.info("Filter profile written to {}", file.getAbsolutePath());

        final Properties properties = new Properties();
        properties.setProperty("indexes.directory", INDEXES_DIRECTORY);
        properties.setProperty("store.enabled", "false");
        properties.setProperty("filter.profiles.directory", temp.toFile().getAbsolutePath());

        final PhileasConfiguration phileasConfiguration = ConfigFactory.create(PhileasConfiguration.class, properties);

        final PhileasFilterService service = new PhileasFilterService(phileasConfiguration);
        final FilterResponse response = service.filter(Arrays.asList("default"), "context", "documentid", "his name was samuel.", MimeType.TEXT_PLAIN);

        LOGGER.info(response.getFilteredText());

        Assertions.assertEquals("his name was {{{REDACTED-custom-dictionary}}}.", response.getFilteredText());
        Assertions.assertEquals("documentid", response.getDocumentId());

    }

    @Test
    public void endToEndWithoutDocumentId() throws Exception {

        final Path temp = Files.createTempDirectory("philter");
        final File file = Paths.get(temp.toFile().getAbsolutePath(), "default.json").toFile();
        LOGGER.info("Writing profile to {}", file.getAbsolutePath());
        FileUtils.writeStringToFile(file, gson.toJson(getFilterProfile("default")), Charset.defaultCharset());

        Properties properties = new Properties();
        properties.setProperty("indexes.directory", INDEXES_DIRECTORY);
        properties.setProperty("store.enabled", "false");
        properties.setProperty("filter.profiles.directory", temp.toFile().getAbsolutePath());

        final PhileasConfiguration phileasConfiguration = ConfigFactory.create(PhileasConfiguration.class, properties);

        final PhileasFilterService service = new PhileasFilterService(phileasConfiguration);
        final FilterResponse response = service.filter(Arrays.asList("default"), "context", null, "his name was JEFF.", MimeType.TEXT_PLAIN);

        LOGGER.info("Generated document ID: " + response.getDocumentId());
        LOGGER.info(response.getFilteredText());

        Assertions.assertEquals("his name was {{{REDACTED-person}}}.", response.getFilteredText());
        Assertions.assertNotNull(response.getDocumentId());

    }

    @Test
    public void endToEndMultipleFilterProfiles() throws Exception {

        final Path temp = Files.createTempDirectory("philter");

        final File file1 = Paths.get(temp.toFile().getAbsolutePath(), "default.json").toFile();
        LOGGER.info("Writing profile to {}", file1.getAbsolutePath());
        FileUtils.writeStringToFile(file1, gson.toJson(getFilterProfile("default")), Charset.defaultCharset());

        final File file2 = Paths.get(temp.toFile().getAbsolutePath(), "justcreditcard.json").toFile();
        LOGGER.info("Writing profile to {}", file2.getAbsolutePath());
        FileUtils.writeStringToFile(file2, gson.toJson(getFilterProfileJustCreditCard("justcreditcard")), Charset.defaultCharset());

        Properties properties = new Properties();
        properties.setProperty("indexes.directory", INDEXES_DIRECTORY);
        properties.setProperty("store.enabled", "false");
        properties.setProperty("filter.profiles.directory", temp.toFile().getAbsolutePath());

        final PhileasConfiguration phileasConfiguration = ConfigFactory.create(PhileasConfiguration.class, properties);

        final PhileasFilterService service = new PhileasFilterService(phileasConfiguration);
        final FilterResponse response = service.filter(Arrays.asList("justcreditcard"), "context", "documentid", "My email is test@something.com", MimeType.TEXT_PLAIN);

        LOGGER.info(response.getFilteredText());

        Assertions.assertEquals("My email is test@something.com", response.getFilteredText());

    }

    @Test
    public void endToEndJustCreditCard() throws Exception {

        final Path temp = Files.createTempDirectory("philter");

        final File file2 = Paths.get(temp.toFile().getAbsolutePath(), "justcreditcard.json").toFile();
        LOGGER.info("Writing profile to {}", file2.getAbsolutePath());
        FileUtils.writeStringToFile(file2, gson.toJson(getFilterProfileJustCreditCard("justcreditcard")), Charset.defaultCharset());

        Properties properties = new Properties();
        properties.setProperty("indexes.directory", INDEXES_DIRECTORY);
        properties.setProperty("store.enabled", "false");
        properties.setProperty("filter.profiles.directory", temp.toFile().getAbsolutePath());

        final PhileasConfiguration phileasConfiguration = ConfigFactory.create(PhileasConfiguration.class, properties);

        final PhileasFilterService service = new PhileasFilterService(phileasConfiguration);
        final FilterResponse response = service.filter(Arrays.asList("justcreditcard"), "context", "documentid", "My cc is 4121742025464465", MimeType.TEXT_PLAIN);

        LOGGER.info(response.getFilteredText());

        Assertions.assertEquals("My cc is {{{REDACTED-credit-card}}}", response.getFilteredText());

    }

    @Test
    public void endToEndJustCreditCardWithIgnoredTerms() throws Exception {

        final Path temp = Files.createTempDirectory("philter");

        final File file2 = Paths.get(temp.toFile().getAbsolutePath(), "justcreditcard.json").toFile();
        LOGGER.info("Writing profile to {}", file2.getAbsolutePath());
        FileUtils.writeStringToFile(file2, gson.toJson(getFilterProfileJustCreditCard("justcreditcard")), Charset.defaultCharset());

        Properties properties = new Properties();
        properties.setProperty("indexes.directory", INDEXES_DIRECTORY);
        properties.setProperty("store.enabled", "false");
        properties.setProperty("filter.profiles.directory", temp.toFile().getAbsolutePath());

        final PhileasConfiguration phileasConfiguration = ConfigFactory.create(PhileasConfiguration.class, properties);

        final PhileasFilterService service = new PhileasFilterService(phileasConfiguration);
        final FilterResponse response = service.filter(Arrays.asList("justcreditcard"), "context", "documentid", "My cc is 4121742025464400", MimeType.TEXT_PLAIN);

        LOGGER.info(response.getFilteredText());

        Assertions.assertEquals("My cc is 4121742025464400", response.getFilteredText());

    }

    @Test
    public void endToEndWithFilterSpecificIgnoredTerms() throws Exception {

        final Path temp = Files.createTempDirectory("philter");
        final File file = Paths.get(temp.toFile().getAbsolutePath(), "default.json").toFile();
        LOGGER.info("Writing profile to {}", file.getAbsolutePath());
        FileUtils.writeStringToFile(file, gson.toJson(getFilterProfileZipCodeWithIgnored("default")), Charset.defaultCharset());

        Properties properties = new Properties();
        properties.setProperty("indexes.directory", INDEXES_DIRECTORY);
        properties.setProperty("store.enabled", "false");
        properties.setProperty("filter.profiles.directory", temp.toFile().getAbsolutePath());

        final PhileasConfiguration phileasConfiguration = ConfigFactory.create(PhileasConfiguration.class, properties);

        final PhileasFilterService service = new PhileasFilterService(phileasConfiguration);
        final FilterResponse response = service.filter(Arrays.asList("default"), "context", "documentid", "George Washington was president and his ssn was 123-45-6789 and he lived at 90210.", MimeType.TEXT_PLAIN);

        LOGGER.info(response.getFilteredText());

        Assertions.assertEquals("George Washington was president and his ssn was {{{REDACTED-ssn}}} and he lived at 90210.", response.getFilteredText());

    }

    @Test
    public void endToEndWithFilterIgnoredTermsFromFile() throws Exception {

        final Path temp = Files.createTempDirectory("philter");
        final File file = Paths.get(temp.toFile().getAbsolutePath(), "default.json").toFile();
        LOGGER.info("Writing profile to {}", file.getAbsolutePath());
        FileUtils.writeStringToFile(file, gson.toJson(getFilterProfileZipCodeWithIgnoredFromFile("default")), Charset.defaultCharset());

        Properties properties = new Properties();
        properties.setProperty("indexes.directory", INDEXES_DIRECTORY);
        properties.setProperty("store.enabled", "false");
        properties.setProperty("filter.profiles.directory", temp.toFile().getAbsolutePath());

        final PhileasConfiguration phileasConfiguration = ConfigFactory.create(PhileasConfiguration.class, properties);

        final PhileasFilterService service = new PhileasFilterService(phileasConfiguration);
        final FilterResponse response = service.filter(Arrays.asList("default"), "context", "documentid", "George Washington was president and his ssn was 123-45-6789 and he lived at 90210.", MimeType.TEXT_PLAIN);

        LOGGER.info(response.getFilteredText());

        Assertions.assertEquals("George Washington was president and his ssn was {{{REDACTED-ssn}}} and he lived at 90210.", response.getFilteredText());

    }

    @Test
    public void endToEndNonexistentFilterProfile() throws Exception {

        final Path temp = Files.createTempDirectory("philter");

        final File file1 = Paths.get(temp.toFile().getAbsolutePath(), "default.json").toFile();
        LOGGER.info("Writing profile to {}", file1.getAbsolutePath());
        FileUtils.writeStringToFile(file1, gson.toJson(getFilterProfile("default")), Charset.defaultCharset());

        Properties properties = new Properties();
        properties.setProperty("indexes.directory", INDEXES_DIRECTORY);
        properties.setProperty("store.enabled", "false");
        properties.setProperty("filter.profiles.directory", temp.toFile().getAbsolutePath());

        final PhileasConfiguration phileasConfiguration = ConfigFactory.create(PhileasConfiguration.class, properties);

        Assertions.assertThrows(FileNotFoundException.class, () -> {

            PhileasFilterService service = new PhileasFilterService(phileasConfiguration);
            final FilterResponse response = service.filter(Arrays.asList("custom1"), "context", "documentid", "My email is test@something.com", MimeType.TEXT_PLAIN);

        });

    }

    // PHL-223: Face recognition
    /*@Test
    public void imageFaces() throws Exception {

        final String filename = "05-29-Small-crowd.jpg";

        final InputStream is = this.getClass().getResourceAsStream("/images/" + filename);
        final byte[] document = IOUtils.toByteArray(is);
        is.close();

        final Path temp = Files.createTempDirectory("philter");

        final File file1 = Paths.get(temp.toFile().getAbsolutePath(), "pdf.json").toFile();
        LOGGER.info("Writing profile to {}", file1.getAbsolutePath());
        FileUtils.writeStringToFile(file1, gson.toJson(getPdfFilterProfile("pdf")), Charset.defaultCharset());

        Properties properties = new Properties();
        properties.setProperty("indexes.directory", INDEXES_DIRECTORY);
        properties.setProperty("store.enabled", "false");
        properties.setProperty("filter.profiles.directory", temp.toFile().getAbsolutePath());

        final PhileasConfiguration phileasConfiguration = ConfigFactory.create(PhileasConfiguration.class, properties);

        final PhileasFilterService service = new PhileasFilterService(phileasConfiguration);
        final BinaryDocumentFilterResponse response = service.filter("pdf", "context", "documentid", document, MimeType.IMAGE_JPEG, MimeType.IMAGE_JPEG);

        // Write the byte array to a file.
        final File outputFile = File.createTempFile("redact", ".jpg");
        //outputFile.deleteOnExit();
        final String output = outputFile.getAbsolutePath();
        LOGGER.info("Writing redacted PDF to {}", output);
        FileUtils.writeByteArrayToFile(new File(output), response.getDocument());

        LOGGER.info("Spans: {}", response.getExplanation().getAppliedSpans().size());
        showSpans(response.getExplanation().getAppliedSpans());

        // TODO: How to assert? MD5 gives a different value each time.

    }*/

    @Test
    public void pdf1() throws Exception {

        final String filename = "12-12110 K.pdf";

        final InputStream is = this.getClass().getResourceAsStream("/pdfs/" + filename);
        final byte[] document = IOUtils.toByteArray(is);
        is.close();

        final Path temp = Files.createTempDirectory("philter");

        final File file1 = Paths.get(temp.toFile().getAbsolutePath(), "pdf.json").toFile();
        LOGGER.info("Writing profile to {}", file1.getAbsolutePath());
        FileUtils.writeStringToFile(file1, gson.toJson(getPdfFilterProfile("pdf")), Charset.defaultCharset());

        Properties properties = new Properties();
        properties.setProperty("indexes.directory", INDEXES_DIRECTORY);
        properties.setProperty("store.enabled", "false");
        properties.setProperty("filter.profiles.directory", temp.toFile().getAbsolutePath());

        final PhileasConfiguration phileasConfiguration = ConfigFactory.create(PhileasConfiguration.class, properties);

        PhileasFilterService service = new PhileasFilterService(phileasConfiguration);
        final BinaryDocumentFilterResponse response = service.filter(Arrays.asList("pdf"), "context", "documentid", document, MimeType.APPLICATION_PDF, MimeType.APPLICATION_PDF);

        // Write the byte array to a file.
        final File outputFile = File.createTempFile("redact", ".pdf");
        //outputFile.deleteOnExit();
        final String output = outputFile.getAbsolutePath();
        LOGGER.info("Writing redacted PDF to {}", output);
        FileUtils.writeByteArrayToFile(new File(output), response.getDocument());

        LOGGER.info("Spans: {}", response.getExplanation().getAppliedSpans().size());
        showSpans(response.getExplanation().getAppliedSpans());

        // TODO: How to assert? MD5 gives a different value each time.

    }

    @Test
    public void pdf2() throws Exception {

        final InputStream is = this.getClass().getResourceAsStream("/pdfs/new-lines.pdf");
        final byte[] document = IOUtils.toByteArray(is);
        is.close();

        final Path temp = Files.createTempDirectory("philter");

        final File file1 = Paths.get(temp.toFile().getAbsolutePath(), "pdf.json").toFile();
        LOGGER.info("Writing profile to {}", file1.getAbsolutePath());
        FileUtils.writeStringToFile(file1, gson.toJson(getPdfFilterProfile("pdf")), Charset.defaultCharset());

        Properties properties = new Properties();
        properties.setProperty("indexes.directory", INDEXES_DIRECTORY);
        properties.setProperty("store.enabled", "false");
        properties.setProperty("filter.profiles.directory", temp.toFile().getAbsolutePath());

        final PhileasConfiguration phileasConfiguration = ConfigFactory.create(PhileasConfiguration.class, properties);

        PhileasFilterService service = new PhileasFilterService(phileasConfiguration);
        final BinaryDocumentFilterResponse response = service.filter(Arrays.asList("pdf"), "context", "documentid", document, MimeType.APPLICATION_PDF, MimeType.APPLICATION_PDF);

        // Write the byte array to a file.
        final File outputFile = File.createTempFile("redact", ".pdf");
        outputFile.deleteOnExit();
        final String output = outputFile.getAbsolutePath();
        LOGGER.info("Writing redacted PDF to {}", output);
        FileUtils.writeByteArrayToFile(new File(output), response.getDocument());

        LOGGER.info("Spans: {}", response.getExplanation().getAppliedSpans().size());
        showSpans(response.getExplanation().getAppliedSpans());

        // output:
        // characterStart: 35;  characterEnd: 40;  filterType: zip-code;  context: context;  documentId: documentid;  confidence: 0.9;  text: 90210;  replacement: {{{REDACTED-zip-code}}};  salt: ;  ignored: false;  classification: null;

        // TODO: How to assert? MD5 gives a different value each time.

    }

    @Test
    public void pdf3() throws Exception {

        final String filename = "12-12110 K.pdf";

        final InputStream is = this.getClass().getResourceAsStream("/pdfs/" + filename);
        final byte[] document = IOUtils.toByteArray(is);
        is.close();

        final Path temp = Files.createTempDirectory("philter");

        final File file1 = Paths.get(temp.toFile().getAbsolutePath(), "pdf.json").toFile();
        LOGGER.info("Writing profile to {}", file1.getAbsolutePath());
        FileUtils.writeStringToFile(file1, gson.toJson(getPdfFilterWithPersonProfile("pdf")), Charset.defaultCharset());

        Properties properties = new Properties();
        properties.setProperty("indexes.directory", INDEXES_DIRECTORY);
        properties.setProperty("store.enabled", "false");
        properties.setProperty("filter.profiles.directory", temp.toFile().getAbsolutePath());

        final PhileasConfiguration phileasConfiguration = ConfigFactory.create(PhileasConfiguration.class, properties);

        PhileasFilterService service = new PhileasFilterService(phileasConfiguration);
        final BinaryDocumentFilterResponse response = service.filter(Arrays.asList("pdf"), "context", "documentid", document, MimeType.APPLICATION_PDF, MimeType.APPLICATION_PDF);

        // Write the byte array to a file.
        final File outputFile = File.createTempFile("redact", ".pdf");
        outputFile.deleteOnExit();
        final String output = outputFile.getAbsolutePath();
        LOGGER.info("Writing redacted PDF to {}", output);
        FileUtils.writeByteArrayToFile(new File(output), response.getDocument());

        LOGGER.info("Spans: {}", response.getExplanation().getAppliedSpans().size());
        showSpans(response.getExplanation().getAppliedSpans());

        final String md5 = DigestUtils.md5Hex(new FileInputStream(outputFile));
        Assertions.assertEquals("c89cb09e91a3bb84ddbd1837fc0ad44e", md5);

    }

    private FilterProfile getFilterProfileZipCodeWithIgnored(String filterProfileName) throws IOException {

        Set<String> ignored = new HashSet<>();
        ignored.add("90210");

        SsnFilterStrategy ssnFilterStrategy = new SsnFilterStrategy();

        Ssn ssn = new Ssn();
        ssn.setSsnFilterStrategies(Arrays.asList(ssnFilterStrategy));

        ZipCodeFilterStrategy zipCodeFilterStrategy = new ZipCodeFilterStrategy();
        zipCodeFilterStrategy.setTruncateDigits(2);

        ZipCode zipCode = new ZipCode();
        zipCode.setZipCodeFilterStrategies(Arrays.asList(zipCodeFilterStrategy));
        zipCode.setIgnored(ignored);

        Identifiers identifiers = new Identifiers();
        identifiers.setSsn(ssn);
        identifiers.setZipCode(zipCode);

        FilterProfile filterProfile = new FilterProfile();
        filterProfile.setName(filterProfileName);
        filterProfile.setIdentifiers(identifiers);

        return filterProfile;

    }

    private FilterProfile getFilterProfileZipCodeWithIgnoredFromFile(String filterProfileName) throws IOException {

        // Copy file to temp directory.
        final File file = File.createTempFile("philter", "ignore");
        FileUtils.writeLines(file, Arrays.asList("90210", "John Smith"));

        Set<String> ignoredFiles = new HashSet<>();
        ignoredFiles.add(file.getAbsolutePath());

        SsnFilterStrategy ssnFilterStrategy = new SsnFilterStrategy();

        Ssn ssn = new Ssn();
        ssn.setSsnFilterStrategies(Arrays.asList(ssnFilterStrategy));

        ZipCodeFilterStrategy zipCodeFilterStrategy = new ZipCodeFilterStrategy();
        zipCodeFilterStrategy.setTruncateDigits(2);

        ZipCode zipCode = new ZipCode();
        zipCode.setZipCodeFilterStrategies(Arrays.asList(zipCodeFilterStrategy));
        zipCode.setIgnoredFiles(ignoredFiles);

        Identifiers identifiers = new Identifiers();
        identifiers.setSsn(ssn);
        identifiers.setZipCode(zipCode);

        FilterProfile filterProfile = new FilterProfile();
        filterProfile.setName(filterProfileName);
        filterProfile.setIdentifiers(identifiers);

        return filterProfile;

    }

    private FilterProfile getFilterProfile(String filterProfileName) throws IOException, URISyntaxException {

        AgeFilterStrategy ageFilterStrategy = new AgeFilterStrategy();

        Age age = new Age();
        age.setAgeFilterStrategies(Arrays.asList(ageFilterStrategy));

        CreditCardFilterStrategy creditCardFilterStrategy = new CreditCardFilterStrategy();

        CreditCard creditCard = new CreditCard();
        creditCard.setCreditCardFilterStrategies(Arrays.asList(creditCardFilterStrategy));

        DateFilterStrategy dateFilterStrategy = new DateFilterStrategy();
        dateFilterStrategy.setStrategy(AbstractFilterStrategy.SHIFT);
        dateFilterStrategy.setShiftYears(3);
        dateFilterStrategy.setShiftMonths(2);
        dateFilterStrategy.setShiftDays(1);

        Date date = new Date();
        date.setDateFilterStrategies(Arrays.asList(dateFilterStrategy));

        EmailAddressFilterStrategy emailAddressFilterStrategy = new EmailAddressFilterStrategy();

        EmailAddress emailAddress = new EmailAddress();
        emailAddress.setEmailAddressFilterStrategies(Arrays.asList(emailAddressFilterStrategy));

        Identifier identifier1 = new Identifier();
        identifier1.setIdentifierFilterStrategies(Arrays.asList(new IdentifierFilterStrategy()));
        identifier1.setPattern("asdfasdfasdf");
        identifier1.setCaseSensitive(true);

        IdentifierFilterStrategy identifier2FilterStrategy = new IdentifierFilterStrategy();
        identifier2FilterStrategy.setStrategy(AbstractFilterStrategy.STATIC_REPLACE);
        identifier2FilterStrategy.setStaticReplacement("STATIC-REPLACEMENT");
        Identifier identifier2 = new Identifier();
        identifier2.setPattern("JEFF");
        identifier2.setCaseSensitive(true);
        identifier2.setIdentifierFilterStrategies(Arrays.asList(identifier2FilterStrategy));

        IpAddressFilterStrategy ipAddressFilterStrategy = new IpAddressFilterStrategy();

        IpAddress ipAddress = new IpAddress();
        ipAddress.setIpAddressFilterStrategies(Arrays.asList(ipAddressFilterStrategy));

        PhoneNumberFilterStrategy phoneNumberFilterStrategy = new PhoneNumberFilterStrategy();

        PhoneNumber phoneNumber = new PhoneNumber();
        phoneNumber.setPhoneNumberFilterStrategies(Arrays.asList(phoneNumberFilterStrategy));

        SsnFilterStrategy ssnFilterStrategy = new SsnFilterStrategy();

        Ssn ssn = new Ssn();
        ssn.setSsnFilterStrategies(Arrays.asList(ssnFilterStrategy));

        StateAbbreviationFilterStrategy stateAbbreviationFilterStrategy = new StateAbbreviationFilterStrategy();

        StateAbbreviation stateAbbreviation = new StateAbbreviation();
        stateAbbreviation.setStateAbbreviationsFilterStrategies(Arrays.asList(stateAbbreviationFilterStrategy));

        UrlFilterStrategy urlFilterStrategy = new UrlFilterStrategy();

        Url url = new Url();
        url.setUrlFilterStrategies(Arrays.asList(urlFilterStrategy));

        VinFilterStrategy vinFilterStrategy = new VinFilterStrategy();

        Vin vin = new Vin();
        vin.setVinFilterStrategies(Arrays.asList(vinFilterStrategy));

        ZipCodeFilterStrategy zipCodeFilterStrategy = new ZipCodeFilterStrategy();
        zipCodeFilterStrategy.setTruncateDigits(2);

        ZipCode zipCode = new ZipCode();
        zipCode.setZipCodeFilterStrategies(Arrays.asList(zipCodeFilterStrategy));

        PersonsFilterStrategy personsFilterStrategy = new PersonsFilterStrategy();

        final File model = new File(getClass().getClassLoader().getResource("ner/model.onnx").toURI());
        final File vocab = new File(getClass().getClassLoader().getResource("ner/vocab.txt").toURI());

        Person person = new Person();
        person.setModel(model.getAbsolutePath());
        person.setVocab(vocab.getAbsolutePath());
        person.setPersonFilterStrategies(Arrays.asList(personsFilterStrategy));

        // ----------------------------------------------------------------------------------

        CityFilterStrategy cityFilterStrategy = new CityFilterStrategy();

        City city = new City();
        city.setCityFilterStrategies(Arrays.asList(cityFilterStrategy));

        CountyFilterStrategy countyFilterStrategy = new CountyFilterStrategy();

        County county = new County();
        county.setCountyFilterStrategies(Arrays.asList(countyFilterStrategy));

        FirstNameFilterStrategy firstNameFilterStrategy = new FirstNameFilterStrategy();

        FirstName firstName = new FirstName();
        firstName.setFirstNameFilterStrategies(Arrays.asList(firstNameFilterStrategy));

        HospitalAbbreviationFilterStrategy hospitalAbbreviationFilterStrategy = new HospitalAbbreviationFilterStrategy();

        HospitalAbbreviation hospitalAbbreviation = new HospitalAbbreviation();
        hospitalAbbreviation.setHospitalAbbreviationFilterStrategies(Arrays.asList(hospitalAbbreviationFilterStrategy));

        HospitalFilterStrategy hospitalFilterStrategy = new HospitalFilterStrategy();

        Hospital hospital = new Hospital();
        hospital.setHospitalFilterStrategies(Arrays.asList(hospitalFilterStrategy));

        StateFilterStrategy stateFilterStrategy = new StateFilterStrategy();

        State state = new State();
        state.setStateFilterStrategies(Arrays.asList(stateFilterStrategy));

        SurnameFilterStrategy surnameFilterStrategy = new SurnameFilterStrategy();

        Surname surname = new Surname();
        surname.setSurnameFilterStrategies(Arrays.asList(surnameFilterStrategy));

        // ----------------------------------------------------------------------------------

        Identifiers identifiers = new Identifiers();

        identifiers.setAge(age);
        identifiers.setCreditCard(creditCard);
        identifiers.setDate(date);
        identifiers.setEmailAddress(emailAddress);
        identifiers.setIdentifiers(Arrays.asList(identifier1, identifier2));
        identifiers.setIpAddress(ipAddress);
        identifiers.setPerson(person);
        identifiers.setPhoneNumber(phoneNumber);
        identifiers.setSsn(ssn);
        identifiers.setStateAbbreviation(stateAbbreviation);
        identifiers.setUrl(url);
        identifiers.setVin(vin);
        identifiers.setZipCode(zipCode);

        /*identifiers.setCity(city);
        identifiers.setCounty(county);
        identifiers.setFirstName(firstName);
        identifiers.setHospital(hospital);
        identifiers.setHospitalAbbreviation(hospitalAbbreviation);
        identifiers.setState(state);
        identifiers.setSurname(surname);*/

        FilterProfile filterProfile = new FilterProfile();
        filterProfile.setName(filterProfileName);
        filterProfile.setIdentifiers(identifiers);

        return filterProfile;

    }

    private FilterProfile getPdfFilterProfile(String filterProfileName) throws IOException {

        ZipCodeFilterStrategy zipCodeFilterStrategy = new ZipCodeFilterStrategy();
        zipCodeFilterStrategy.setTruncateDigits(2);

        ZipCode zipCode = new ZipCode();
        zipCode.setZipCodeFilterStrategies(Arrays.asList(zipCodeFilterStrategy));

        CustomDictionaryFilterStrategy customDictionaryFilterStrategy = new CustomDictionaryFilterStrategy();
        customDictionaryFilterStrategy.setStrategy("REDACT");

        CustomDictionary customDictionary = new CustomDictionary();
        customDictionary.setCustomDictionaryFilterStrategies(Arrays.asList(customDictionaryFilterStrategy));
        customDictionary.setTerms(Arrays.asList("Wendy"));

        Identifiers identifiers = new Identifiers();

        identifiers.setCustomDictionaries(Arrays.asList(customDictionary));
        identifiers.setZipCode(zipCode);

        FilterProfile filterProfile = new FilterProfile();
        filterProfile.setName(filterProfileName);
        filterProfile.setIdentifiers(identifiers);

        return filterProfile;

    }

    private FilterProfile getPdfFilterWithPersonProfile(String filterProfileName) throws URISyntaxException {

        final File model = new File(getClass().getClassLoader().getResource("ner/model.onnx").toURI());
        final File vocab = new File(getClass().getClassLoader().getResource("ner/vocab.txt").toURI());

        final Person person = new Person();
        person.setModel(model.getAbsolutePath());
        person.setVocab(vocab.getAbsolutePath());

        Identifiers identifiers = new Identifiers();
        identifiers.setPerson(person);

        FilterProfile filterProfile = new FilterProfile();
        filterProfile.setName(filterProfileName);
        filterProfile.setIdentifiers(identifiers);

        return filterProfile;

    }

    private FilterProfile getFilterProfileJustCreditCard(String filterProfileName) throws IOException {

        CreditCardFilterStrategy creditCardFilterStrategy = new CreditCardFilterStrategy();

        CreditCard creditCard = new CreditCard();
        creditCard.setCreditCardFilterStrategies(Arrays.asList(creditCardFilterStrategy));

        Identifiers identifiers = new Identifiers();
        identifiers.setCreditCard(creditCard);

        Ignored ignored = new Ignored();
        ignored.setTerms(Arrays.asList("4121742025464400", "12341341234", "2423543545"));

        FilterProfile filterProfile = new FilterProfile();
        filterProfile.setName(filterProfileName);
        filterProfile.setIdentifiers(identifiers);
        filterProfile.setIgnored(Arrays.asList(ignored));

        return filterProfile;

    }

    private void showSpans(List<Span> spans) {

        for(Span span : spans) {
            LOGGER.info(span.toString());
        }

    }

}