# Phileas

[![CodeFactor Quality](https://img.shields.io/codefactor/grade/github/philterd/phileas)](https://www.codefactor.io/repository/github/philterd/phileas)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=philterd_phileas&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=philterd_phileas)

* For an open source API-based redaction app built on Phileas, please see [Philter](https://www.github.com/philterd/philter).

Phileas is a Java library to deidentify and redact PII, PHI, and other sensitive information from text. Given text or documents (PDF), Phileas analyzes the text searching for sensitive information such as persons' names, ages, addresses, and many other types of information. Phileas is highly configurable through its settings and policies.

When sensitive information is identified, Phileas can manipulate the sensitive information in a variety of ways. The information can be replaced, encrypted, anonymized, and more. The user chooses how to manipulate each type of sensitive information. We refer to each of these methods in whole as "redaction."

Information can be redacted based on the content of the information and other attributes. For example, only certain persons' names, only zip codes meeting some qualification, or IP addresses that match a given pattern.

For Phileas' documentation, please see https://philterd.github.io/phileas/.

## Ports

This is the *primary* Phileas repository, but it has been ported to a few other languages. Functionality between this Java version and the versions for other languages will differ. Some features available here are not (yet) available in the ports, and there may be features in the ports that are not available in the Java version. Refer to the documentation for each port for more information.

Development will likely focus on the Java version since it powers Philter and other applications.

* Phileas (Python) - https://github.com/philterd/phileas-python ([docs](https://philterd.github.io/phileas-python/))
* Phileas (.NET) - https://github.com/philterd/phileas-dotnet

## What Phileas Can Do

* Phileas can identify and redact over 30 types of sensitive information (see list below).
* Phileas can evaluate conditions when redacting (only zip codes with population less than some value, only ages > 30, only when sentiment is a certain value, etc.).
* Phileas can perform sentiment and offensiveness classification.
* Phileas can redact, encrypt, and anonymize sensitive information.
* Phileas can replace persons names with random names, dates with similar but random dates, etc.
* Phileas can disambiguate types of sensitive information (i.e. SSN vs. phone number).
* Phileas can deidentify text consistently ("John Smith" is replaced consistently in certain documents).
* Phileas can shift dates or replace dates with approximate representations (i.e. "3 months ago").
* Phileas uses policies to define what sensitive information to find and how to redact it.

## Supported PII, PHI, and Other Sensitive Information

This list might be outdated. Please check the individual filter classes for details.

### Persons

* Person's Names - Multiple methods, e.g. NER (via [Ph-Eye](https://www.github.com/philterd/ph-eye)), dictionary, census data
* Physician Names
* First Names
* Surnames

### Medical

* Medical Conditions

### Common

* Ages (numeric and spelled-out, e.g. "thirty-five years old")
* Bank Account Numbers
* Bitcoin Addresses
* Credit Cards
* Currency (USD)
* Dates (in addition to birthdates and deathdates)
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
* States
* State Abbreviations

### Custom Filters

* Dictionary
* Identifier

## Building Phileas

Phileas can be built with `mvn clean install`.

The benchmark tests (timed redaction workloads) are excluded from the normal build and are run separately with the `benchmark` profile, for example `mvn test -Pbenchmark` (optionally with `-Dbenchmark.redactor=mask_all -Dbenchmark.document=all -Dbenchmark.millis=2000`).

## Using Phileas

Phileas snapshots and releases as of version 2.12.1 are available in Maven Central. Previous versions were in the [Philterd repository](https://artifacts.philterd.ai).

Add the Phileas dependency to your project:

```
<dependency>
  <groupId>ai.philterd</groupId>
  <artifactId>phileas</artifactId>
  <version>4.0.0</version>
</dependency>

```

### Finding and Manipulating Sensitive Information in Text

Create a `FilterService`, using a `PhileasConfiguration`, and call `filter()` on the service:

```
Properties properties = new Properties();
PhileasConfiguration phileasConfiguration = new PhileasConfiguration(properties);

PlainTextFilterService filterService = new PlainTextFilterService(phileasConfiguration);

FilterResponse response = filterService.filter(policies, context, text);
```

The `policies` is a list of `Policy` classes. (See below for more about Policies.) Lastly, we specify that the data is plain text.

The `response` contains information about the identified sensitive information along with the filtered text.

#### Usage Examples

The [EndToEndTests](https://github.com/philterd/phileas/blob/main/src/test/java/ai/philterd/phileas/services/EndToEndTests.java) test class has examples of how to configure Phileas and filter text.

### Finding and Redacting Sensitive Information in a PDF Document

Create a `FilterService`, using a `PhileasConfiguration`, and call `filter()` on the service:

```
PhileasConfiguration phileasConfiguration = ConfigFactory.create(PhileasConfiguration.class);

PdfFilterService filterService = new PdfFilterService(phileasConfiguration);

BinaryDocumentFilterResponse response = filterService.filter(policies, context, body, MimeType.APPLICATION_PDF, MimeType.IMAGE_JPEG);
```

The `policies` is a list of `Policy` classes which are created by deserializing a policy from JSON. (See below for more about Policies.) The `body` is the text you are filtering. Lastly, we specify that the data is plain text.

The `response` contains a zip file of the images generated by redacting the PDF document.

### Policies

A policy is an instance of a `Policy` class that tells Phileas the types of sensitive information to identify, and what to do with the sensitive information when found. A policy describes the entire filtering process, from what filters to apply, terms to ignore, to everything in between. Phileas can apply one or more policies when `filter()` is called. The list of policies will be applied in order as they were added to the list.

For examples on creating a policy, look at [EndToEndTestsHelper](https://github.com/philterd/phileas/blob/main/src/test/java/ai/philterd/phileas/services/EndToEndTestsHelper.java) and [EndToEndTests](https://github.com/philterd/phileas/blob/main/src/test/java/ai/philterd/phileas/services/EndToEndTests.java).

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

## Policy Schema and PhiSQL

The [Phileas redaction policy JSON schema](https://philterd.ai/schemas/redaction-policy/1.0.0/schema.json) is the canonical execution contract for Phileas. It defines what a valid policy looks like and is the source of truth for every filter type, strategy, and configuration option Phileas understands. The schema is owned and versioned in the [philterd/phisql](https://github.com/philterd/phisql) repository.

[PhiSQL](https://github.com/philterd/phisql) is the declarative authoring language that compiles to that schema. Instead of writing JSON by hand, you write PhiSQL and the compiler produces a valid Phileas policy:

```
PhiSQL source  →  Compiler  →  Phileas JSON policy  →  Phileas runtime
```

Phileas accepts PhiSQL directly. `Policy.fromPhiSQL(...)` compiles a PhiSQL document to a JSON policy and loads it, so you do not have to run the compiler as a separate step:

```java
Policy policy = Policy.fromPhiSQL("""
        POLICY example DESCRIPTION 'redact emails and zip codes';
        REDACT EMAIL_ADDRESS WITH REDACT;
        REDACT ZIP_CODE WITH REDACT;
        """);
```

If the document cannot be parsed or compiled, `fromPhiSQL` throws a `PolicyCompilationException` carrying the underlying compiler diagnostics. The runtime engine is unchanged — PhiSQL is compiled to JSON and the same JSON policy is executed.

**Schema changes go through philterd/phisql first.** Adding a new entity type, strategy, or filter option requires a schema update and a matching PhiSQL grammar change in the same pull request in that repository. Once the schema version is released, Phileas is updated to target it. Existing JSON policies remain valid and continue to load unchanged — PhiSQL is purely additive.

Each Phileas release supports exactly one schema version:

| Phileas Version | Policy Schema Version |
|-----------------|-----------------------|
| 4.0.0           | 1.0.0                 |

## Local PhEye Inference

Phileas detects names by calling a [Ph-Eye](https://www.github.com/philterd/ph-eye) service over HTTP. To run a GLiNER model on-device instead, with no Ph-Eye server, add the optional [phileas-pheye-onnx](https://github.com/philterd/phileas-pheye-onnx) module, which performs local inference via ONNX Runtime. Configure a PhEye filter with a local model directory (`modelPath`) and detection runs in-process; without the module, Phileas uses the remote Ph-Eye service as before.

## Powered by Phileas

Phileas is the underlying core of [Philter](https://www.github.com/philterd/philter), a turnkey text redaction engine which is built on top of Phileas and provides an API for redacting text. Philter runs entirely within your cloud and never transmits data outside your cloud. Custom AI models are available for domains like healthcare, legal, and news.

* [Philter on the AWS Marketplace](https://aws.amazon.com/marketplace/pp/B07YVB8FFT?ref=_ptnr_philterd)
* [Philter on the Google Cloud Marketplace](https://console.cloud.google.com/marketplace/product/philterd-public/philter)
* [Philter on the Azure Marketplace](https://azuremarketplace.microsoft.com/en-us/marketplace/apps/philterdllc1687189098111.philter?tab=Overview)
* On-prem deployments by contacting us at [https://www.philterd.ai/](https://www.philterd.ai). 

## License

As of Phileas 2.2.1, Phileas is licensed under the Apache License, version 2.0. Previous versions were under a proprietary license.

Copyright 2024-2026 Philterd, LLC.

Copyright 2018-2023 Mountain Fog, Inc.
