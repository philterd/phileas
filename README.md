# Phileas

Phileas is a Java library to redact PII, PHI, and other sensitive information from text. Given text or documents (PDF), Phileas analyzes the text searching for sensitive information such as persons' names, ages, addresses, and many other types of information. Phileas is highly configurable through its settings and policies.s

When sensitive information is identified, Phileas can manipulate the sensitive information in a variety of ways. The information can be replaced, encrypted, anonymized, and more. The user chooses how to manipulate each type of sensitive information. We refer to each of these methods in whole as "redaction."

Information can be redacted based on the content of the information and other attributes. For example, only certain persons' names, only zip codes meeting some qualification, or IP addresses that match a given pattern.

Phileas is the underlying core of [Philter](https://philterd.ai/philter/), a turnkey text redaction engine which is built on top of Phileas and provides an API for redacting text. Philter runs entirely within your cloud and never transmits data outside of your cloud. Custom AI models are available for domains like healthcare, legal, and news.

* [Philter on the AWS Marketplace](https://aws.amazon.com/marketplace/pp/B07YVB8FFT?ref=_ptnr_philterd)
* [Philer on the Google Cloud Marketplace](https://console.cloud.google.com/marketplace/product/philterd-public/philter)
* [Philter on the Azure Marketplace](https://azuremarketplace.microsoft.com/en-us/marketplace/apps/philterdllc1687189098111.philter?tab=Overview)
* On-prem deployments by contacting us at [https://www.philterd.ai/](https://www.philterd.ai). 

## What Phileas Can Do

* Phileas can identify and redact over 30 types of sensitive information (see list below).
* Phileas can evaluate conditions when redating (only zip codes with population less than some value, only ages > 30, only when sentiment is a certain value, etc.).
* Phileas can perform sentiment and offensiveness classification.
* Phileas can redact, encrypt, and anonymize sensitive information.
* Phileas can replace persons names with random names, dates with similar but random dates, etc.
* Phileas can disambiguate types of sensitive information (i.e. SSN vs. phone number).
* Phileas can deidentify text consistently ("John Smith" is replaced consistently in certain documents).
* Phileas can shift dates or replace dates with approximate representations (i.e. "3 months ago").
* Phileas uses policies to define what sensitive information to find and how to redact it.

## Supported PII, PHI, and Other Sensitive Information

### Persons

* Person's Names - Multiple methods, e.g. NER, dictionary, census data
* Physician Names
* First Names
* Surnames

### Common

* Ages
* Bank Account Numbers
* Bitcoin Addresses
* Credit Cards
* Currency (USD)
* Dates
* (US) Driver's License Numbers
* Email Addresses
* IBAN Codes
* IP Addresses (IPv4 and IPv6)
* MAC Addresses
* (US) Passport Numbers
* Phone Numbers
* Phone Number Extensions
* Sections (of a document)
* SSNs and TINs
* Tracking Numbers (UPS / FedEx / USPS)
* URLs
* VINs
* Zip Codes

### (US) Locations

* Cities
* Counties
* Hospitals
* Hospital Abbreviations
* States
* State Abbreviations

### Custom Filters

* Dictionary
* Identifier

## Building Phileas

After cloning, run `git lfs pull` to download models needed for unit tests. Phileas can then be built with `mvn clean install`.

## Using Phileas


First, add the Phileas dependency to your project:

```
<dependency>
  <groupId>ai.philterd</groupId>
  <artifactId>phileas-core</artifactId>
  <version>${phileas.version}</version>
</dependency>

```

### Finding and Redacting Sensitive Information in Text

Create a `FilterService`, using a `PhileasConfiguration`, and call `filter()` on the service:

```
PhileasConfiguration phileasConfiguration = ConfigFactory.create(PhileasConfiguration.class);

FilterService filterService = new PhileasFilterService(phileasConfiguration);

FilterResponse response = filterService.filter(policies, context, documentId, body, MimeType.TEXT_PLAIN);
```

The `policies` is a list of `Policy` classes. (See below for more about Policies.) The `context` and `documentId` are arbitrary values you can use to uniquely identify the text being filtered. The `body` is the text you are filtering. Lastly, we specify that the data is plain text.

The `response` contains information about the identified sensitive information along with the filtered text.

#### Usage Examples

The [PhileasFilterServiceTest](https://github.com/philterd/phileas/blob/main/phileas-core/src/test/java/io/philterd/test/phileas/services/PhileasFilterServiceTest.java) and [EndToEndTests](https://github.com/philterd/phileas/blob/main/phileas-core/src/test/java/io/philterd/test/phileas/services/EndToEndTests.java) test classes have examples of how to configure Phileas and filter text.

### Finding and Redacting Sensitive Information in a PDF Document

Create a `FilterService`, using a `PhileasConfiguration`, and call `filter()` on the service:

```
PhileasConfiguration phileasConfiguration = ConfigFactory.create(PhileasConfiguration.class);

FilterService filterService = new PhileasFilterService(phileasConfiguration);

BinaryDocumentFilterResponse response = filterService.filter(policies, context, documentId, body, MimeType.APPLICATION_PDF, MimeType.IMAGE_JPEG);
```

The `policies` is a list of `Policy` classes which are created by deserializing a policy from JSON. (See below for more about Policies.) The `context` and `documentId` are arbitrary values you can use to uniquely identify the text being filtered. The `body` is the text you are filtering. Lastly, we specify that the data is plain text.

The `response` contains a zip file of the images generated by redacting the PDF document.

### Policies

A policy is an instance of a `Policy` class that tells Phileas the types of sensitive information to identify, and what to do with the sensitive information when found. A policy describes the entire filtering process, from what filters to apply, terms to ignore, to everything in between. Phileas can apply one or more policies when `filter()` is called. The list of policies will be applied in order as they were added to the list.

For examples on creating a policy, look at [EndToEndTestsHelper](https://github.com/philterd/phileas/blob/main/phileas-core/src/test/java/io/philterd/test/phileas/services/EndToEndTestsHelper.java). The [PhileasFilterServiceTest](https://github.com/philterd/phileas/blob/main/phileas-core/src/test/java/io/philterd/test/phileas/services/PhileasFilterServiceTest.java) and [EndToEndTests](https://github.com/philterd/phileas/blob/main/phileas-core/src/test/java/io/philterd/test/phileas/services/EndToEndTests.java) test classes have examples of how to configure Phileas and filter text.

Policies can be de/serialized to JSON. Here is a basic (but valid) policy that identifies and redacts ages:

```
{
  "name": "default",
  "ignored": [],
  "identifiers": {
    "age": {
      "ageFilterStrategies": [{
        "strategy": "REDACT",
        "redactionFormat": "{{{REDACTED-%t}}}"
      }]
    }
  }
}
```

There is a long list of `identifiers` that can be applied, and each identifier has several possible `strategy` values. In this case, when a age is found, it is redacted by being replaced with the text `{{{REDACTED-age}}}`. The `%t` is a placeholder for the type of filter. In this case, it is the literal text `age`.

## License

As of Phileas 2.2.1, Phileas is licensed under the Apache License, version 2.0. Previous versions were under a proprietary license.

Copyright 2023 Philterd, LLC.
Copyright 2018-2023 Mountain Fog, Inc.