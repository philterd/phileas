# Phileas

[![CodeFactor Quality](https://img.shields.io/codefactor/grade/github/philterd/phileas)](https://www.codefactor.io/repository/github/philterd/phileas)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=philterd_phileas&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=philterd_phileas)

Phileas is a Java library that finds and redacts PII, PHI, and other sensitive information in text and PDF documents. Give it text and a policy, and it locates sensitive values such as names, ages, addresses, account numbers, and dates, then transforms each one the way the policy says: redact it, replace it, encrypt it, anonymize it, and more.

Phileas is the redaction engine underneath [Philter](https://www.github.com/philterd/philter), the turnkey redaction API. Most of its work is driven by two companion projects:

* [PhiSQL](https://github.com/philterd/phisql) is the declarative language for authoring policies. It compiles to the Phileas redaction policy schema, and Phileas can load it directly.
* [Ph-Eye](https://www.github.com/philterd/ph-eye) hosts the GLiNER NER models that detect names and other free-text entities. Phileas calls it over HTTP, or runs the model on-device with the optional local inference module.

Full documentation is at https://philterd.github.io/phileas/.

## Quickstart

Add the dependency (see [Using Phileas](#using-phileas) for the current version), then redact some text. Policies are written in PhiSQL and loaded with `Policy.fromPhiSQL(...)`:

```java
import ai.philterd.phileas.PhileasConfiguration;
import ai.philterd.phileas.policy.Policy;
import ai.philterd.phileas.model.filtering.TextFilterResult;
import ai.philterd.phileas.services.context.DefaultContextService;
import ai.philterd.phileas.services.disambiguation.vector.InMemoryVectorService;
import ai.philterd.phileas.services.filters.filtering.PlainTextFilterService;

import java.util.Properties;

final PhileasConfiguration configuration = new PhileasConfiguration(new Properties());

final PlainTextFilterService filterService = new PlainTextFilterService(
        configuration,
        new DefaultContextService(),
        new InMemoryVectorService(),
        null); // optional HttpClient for Ph-Eye; null uses the default

final Policy policy = Policy.fromPhiSQL("""
        REDACT EMAIL_ADDRESS WITH REDACT;
        REDACT ZIP_CODE WITH REDACT;
        """);

final TextFilterResult result = filterService.filter(policy, "context", "Email test@example.com, zip 90210.");

System.out.println(result.getFilteredText());
```

The `context` is an identifier that scopes consistent replacements: the same value is redacted the same way within one context (so "John Smith" maps to the same replacement throughout a document). The result carries the filtered text and details about every span that was found and how it was redacted.

## What Phileas Can Do

* Identify and redact over 30 types of sensitive information (see the [supported information list](#supported-pii-phi-and-other-sensitive-information)).
* Apply conditions when redacting, for example only zip codes with a population below some value, or only ages greater than 30.
* Redact, encrypt, anonymize, mask, truncate, or replace each type of information, chosen per type in the policy.
* Replace values with realistic substitutes: random names, similar but random dates, and so on.
* Disambiguate the type of an ambiguous value (for example an SSN versus a phone number) using context.
* Redact consistently within a context, so a given value is always replaced the same way.
* Shift dates or replace them with approximate representations such as "3 months ago".
* Validate matched identifiers against checksum and structural rules so only valid values are redacted.

## Policies and PhiSQL

A `Policy` tells Phileas which types of sensitive information to find and what to do with each one when found. It describes the whole filtering process: which filters to apply, terms to ignore, replacement strategies, and conditions. One policy is passed to each `filter()` call.

### Authoring with PhiSQL

[PhiSQL](https://github.com/philterd/phisql) is the declarative authoring language for policies. Rather than writing JSON by hand, you write PhiSQL and it compiles to a valid Phileas policy:

```
PhiSQL source  ->  Compiler  ->  Phileas JSON policy  ->  Phileas runtime
```

`Policy.fromPhiSQL(...)` compiles a PhiSQL document to a JSON policy and loads it in one step, so you do not run the compiler separately:

```java
Policy policy = Policy.fromPhiSQL("""
        POLICY example DESCRIPTION 'redact emails and zip codes';
        REDACT EMAIL_ADDRESS WITH REDACT;
        REDACT ZIP_CODE WITH REDACT;
        """);
```

If the document cannot be parsed or compiled, `fromPhiSQL` throws a `PolicyCompilationException` carrying the compiler diagnostics. PhiSQL is purely additive: it compiles to JSON and the same JSON policy is executed, so existing JSON policies keep working unchanged.

### Authoring with JSON

Policies can also be written as JSON and deserialized into a `Policy`. Here is a small but valid policy that redacts ages:

```json
{
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

Each entry under `identifiers` configures one filter, and each filter has one or more strategies. Here, a detected age is replaced with `{{{REDACTED-age}}}`. The `%t` placeholder is filled with the filter type, which is the literal text `age` in this case.

### Policy schema and versioning

The [Phileas redaction policy JSON schema](https://philterd.ai/schemas/redaction-policy/1.1.0/schema.json) is the canonical execution contract: it defines what a valid policy looks like and is the source of truth for every filter type, strategy, and option Phileas understands. The schema is owned and versioned in the [philterd/phisql](https://github.com/philterd/phisql) repository.

Schema changes go through philterd/phisql first. Adding an entity type, strategy, or filter option requires a schema update and a matching PhiSQL grammar change in the same pull request there. Once a schema version is released, Phileas is updated to target it.

Each Phileas release supports exactly one schema version:

| Phileas Version | Policy Schema Version |
|-----------------|-----------------------|
| 4.0.0           | 1.0.0                 |
| 4.1.0           | 1.1.0                 |

## Names and Free-Text Entities with Ph-Eye

Phileas detects names and other free-text entities with NER models served by [Ph-Eye](https://www.github.com/philterd/ph-eye), which Phileas calls over HTTP. A policy can reference one or more Ph-Eye services.

To run a GLiNER model on-device instead, with no Ph-Eye server, add the optional [phileas-pheye-onnx](https://github.com/philterd/phileas-pheye-onnx) module, which performs local inference via ONNX Runtime. Configure a PhEye filter with a local model directory (`modelPath`) and detection runs in-process. Without the module, Phileas uses the remote Ph-Eye service as before.

## Identifier Validation

Custom identifier filters can carry a `validator` so that only structurally valid identifiers are redacted. This avoids false positives on numbers that match a pattern but fail a checksum. The validator is written in compact form (`"validator": "luhn"`) or object form with parameters (`"validator": { "name": "luhn", "params": { ... } }`).

Built-in validators include `luhn`, `mod11`, `mod23-letter`, `mod97`, `bic-structural`, and several national identifier formats (`de-personalausweis`, `de-steuerid`, `es-cif`).

## Supported PII, PHI, and Other Sensitive Information

### Persons

* Person's Names, detected by NER via [Ph-Eye](https://www.github.com/philterd/ph-eye), dictionary, and census data
* Physician Names
* First Names
* Surnames

### Medical

* Medical Conditions (via Ph-Eye)

### Common

* Ages (numeric and spelled-out, e.g. "thirty-five years old")
* Bank Routing Numbers
* Bitcoin Addresses
* Credit Cards
* Currency (USD)
* Dates (including birthdates and deathdates)
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
* Street Addresses
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

## Redacting PDF Documents

`PdfFilterService` redacts PDF documents. It is constructed like the plain text service and returns a `BinaryDocumentFilterResult` containing the redacted output:

```java
import ai.philterd.phileas.model.filtering.BinaryDocumentFilterResult;
import ai.philterd.phileas.model.filtering.MimeType;
import ai.philterd.phileas.services.filters.filtering.PdfFilterService;

final PdfFilterService filterService = new PdfFilterService(
        configuration,
        new DefaultContextService(),
        new InMemoryVectorService(),
        null);

final BinaryDocumentFilterResult result = filterService.filter(
        policy, "context", document, MimeType.APPLICATION_PDF);
```

## Usage Examples

The [EndToEndTests](https://github.com/philterd/phileas/blob/main/src/test/java/ai/philterd/phileas/services/EndToEndTests.java) and [EndToEndTestsHelper](https://github.com/philterd/phileas/blob/main/src/test/java/ai/philterd/phileas/services/EndToEndTestsHelper.java) test classes show how to configure Phileas, build policies, and filter text and PDFs.

## Building Phileas

Build with `mvn clean install`.

The benchmark tests (timed redaction workloads) are excluded from the normal build and run separately with the `benchmark` profile, for example `mvn test -Pbenchmark` (optionally with `-Dbenchmark.redactor=mask_all -Dbenchmark.document=all -Dbenchmark.millis=2000`).

## Using Phileas

Phileas snapshots and releases as of version 2.12.1 are published to Maven Central. Earlier versions were in the [Philterd repository](https://artifacts.philterd.ai).

```xml
<dependency>
  <groupId>ai.philterd</groupId>
  <artifactId>phileas</artifactId>
  <version>4.1.0</version>
</dependency>
```

## Ports

This is the primary Phileas repository. It has been ported to other languages, and functionality differs between the Java version and the ports: some features here are not yet in the ports, and the ports may have features not yet here. Refer to each port's documentation for details. Development focuses on the Java version since it powers Philter and other applications.

* Phileas (Python) - https://github.com/philterd/phileas-python ([docs](https://philterd.github.io/phileas-python/))
* Phileas (.NET) - https://github.com/philterd/phileas-dotnet

## Powered by Phileas

Phileas is the core of [Philter](https://www.github.com/philterd/philter), a turnkey text redaction engine that provides an API for redacting text. Philter runs entirely within your cloud and never transmits data outside it. Custom AI models are available for domains such as healthcare, legal, and news.

* [Philter on the AWS Marketplace](https://aws.amazon.com/marketplace/pp/B07YVB8FFT?ref=_ptnr_philterd)
* [Philter on the Google Cloud Marketplace](https://console.cloud.google.com/marketplace/product/philterd-public/philter)
* [Philter on the Azure Marketplace](https://azuremarketplace.microsoft.com/en-us/marketplace/apps/philterdllc1687189098111.philter?tab=Overview)
* On-prem deployments by contacting us at [https://www.philterd.ai/](https://www.philterd.ai).

## License

As of Phileas 2.2.1, Phileas is licensed under the Apache License, version 2.0. Previous versions were under a proprietary license.

Copyright 2024-2026 Philterd, LLC.

Copyright 2018-2023 Mountain Fog, Inc.
