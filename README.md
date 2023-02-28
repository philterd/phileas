# Phileas

*The PII and PHI redaction engine.*

For a turnkey text redaction engine, see [Philter](https://www.mtnfog.com/products/philter), which is built on top of Phileas.

Phileas is a framework to find, identify, and remove sensitive information from text. Given text or documents (PDF), Phileas analyzes the text searching for sensitive information such as persons' names, ages, addresses, and many other types of information.

When sensitive information is identified, Phileas can manipulate the sensitive information in a variety of ways. The information can be replaced, encrypted, anonymized, and more. The user chooses how to manipulate each type of sensitive information. We refer to each of these methods in whole as "redaction."

Information can be redacted based on the content of the information and other attributes. For example, only certain persons' names, only zip codes meeting some qualification, or IP addresses that match a given pattern.

Phileas is a highly-configurable library for managing sensitive information in text and documents.

## Building Phileas

After cloning, run `git lfs pull` to download models needed for unit tests. Phileas can then be built with `mvn clean install`.

## Using Phileas

Add the Phileas dependency to your project:

```
<dependency>
	<groupId>io.philterd</groupId>
	<artifactId>phileas-core</artifactId>
	<version>${phileas.version}</version>
</dependency>

```

Now create a `FilterService`, using a `PhileasConfiguration`, and call `filter()` on the service:

```

PhileasConfiguration phileasConfiguration = ConfigFactory.create(PhileasConfiguration.class);

FilterService filterService = new PhileasFilterService(phileasConfiguration);

FilterResponse response = filterService.filter(filterProfiles, context, documentId, body, MimeType.TEXT_PLAIN);
```

The `filterProfiles` is a list of `FilterProfile` classes. A "filter profile" tells Phileas the types of sensitive information you want to identify and what to do with it (redact, anonymize, etc.) when it is found. The `context` and `documentId` are arbitrary values you can use to uniquely identify the text being filtered. The `body` is the text you are filtering. Lastly, we specify that the data is plain text.

The `response` contains information about the identified sensitive information along with the filtered text.


## License

As of Phileas 2.2.1, Phileas is licensed under the Apache License, version 2.0. Previous versions were under a proprietary license.

Copyright 2018-2023 Mountain Fog, Inc.