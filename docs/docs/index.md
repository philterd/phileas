# Phileas

Phileas is a Java library to deidentify text and redact PII, PHI, and other sensitive information from text. Given text or documents (PDF), Phileas analyzes the text searching for sensitive information such as persons' names, ages, addresses, and many other types of information. Phileas is highly configurable through its settings and policies.

When sensitive information is identified, Phileas can manipulate the sensitive information in a variety of ways. The information can be replaced, encrypted, anonymized, and more. The user chooses how to manipulate each type of sensitive information. We refer to each of these methods in whole as "redaction."

Information can be redacted based on the content of the information and other attributes. For example, only certain persons' names, only zip codes meeting some qualification, or IP addresses that match a given pattern.

## Using Phileas

Phileas releases are published to [Maven Central](https://central.sonatype.com/artifact/ai.philterd/phileas). Maven Central is configured by default, so for a release version you only need to add the dependency:

```
<dependency>
  <groupId>ai.philterd</groupId>
  <artifactId>phileas</artifactId>
  <version>4.1.0</version>
</dependency>
```

To track the latest development build, depend on the current `-SNAPSHOT` version and add the Maven Central snapshot repository to your build (snapshots are not served from the default Maven Central repository):

```
<repositories>
    <repository>
        <id>central-portal-snapshots</id>
        <url>https://central.sonatype.com/repository/maven-snapshots/</url>
        <releases>
            <enabled>false</enabled>
        </releases>
        <snapshots>
            <enabled>true</enabled>
        </snapshots>
    </repository>
</repositories>

<dependency>
  <groupId>ai.philterd</groupId>
  <artifactId>phileas</artifactId>
  <version>4.2.0-SNAPSHOT</version>
</dependency>
```

Snapshots are development builds: they are mutable and are periodically pruned, so pin a release version for anything you need to reproduce.

### Quick Start

Create a `FilterService`, using a `PhileasConfiguration`, and call `filter()` on the service:

```
Properties properties = new Properties();
PhileasConfiguration phileasConfiguration = new PhileasConfiguration(properties);

FilterService filterService = new PhileasFilterService(phileasConfiguration);

FilterResponse response = filterService.filter(policies, context, body, MimeType.TEXT_PLAIN);
```

The `policies` is a list of `Policy` classes. (See below for more about Policies.) The `context` is an arbitrary value you can use to uniquely identify the text being filtered. The `body` is the text you are filtering. Lastly, we specify that the data is plain text.

The `response` contains information about the identified sensitive information along with the filtered text.

A `FilterService` is safe to call concurrently: `filter()` may be invoked from multiple threads on a single shared instance without external locking, so a per-row caller (such as a Spark, Kafka, or logging function) can share one instance across threads.

#### Usage Examples

The [PhileasFilterServiceTest](https://github.com/philterd/phileas/blob/main/phileas-core/src/test/java/io/philterd/test/phileas/services/PhileasFilterServiceTest.java) and [EndToEndTests](https://github.com/philterd/phileas/blob/main/phileas-core/src/test/java/io/philterd/test/phileas/services/EndToEndTests.java) test classes have examples of how to configure Phileas and filter text.

#### Redacting many texts efficiently (prepared policy)

When you redact many texts with the same policy in a tight loop (for example a per-row Spark, Kafka, or logging function), prepare the policy once and reuse the handle. `prepare()` resolves the policy's filters and post-filters a single time, so each call avoids that per-call work. The handle is safe to reuse across calls and across threads.

```
PlainTextFilterService service = new PlainTextFilterService(
        phileasConfiguration, contextService, vectorService, null);

// Resolve the policy once.
PlainTextFilterService.PreparedPolicy prepared = service.prepare(policy);

// Reuse the handle for every text.
for (String text : texts) {
    TextFilterResult result = prepared.filter(context, text);
}
```

### Finding and Redacting Sensitive Information in a PDF Document

Create a `FilterService`, using a `PhileasConfiguration`, and call `filter()` on the service:

```
PhileasConfiguration phileasConfiguration = ConfigFactory.create(PhileasConfiguration.class);

FilterService filterService = new PhileasFilterService(phileasConfiguration);

BinaryDocumentFilterResponse response = filterService.filter(policies, context, body, MimeType.APPLICATION_PDF, MimeType.IMAGE_JPEG);
```

The `policies` is a list of `Policy` classes which are created by deserializing a policy from JSON. (See below for more about Policies.) The `context` is an arbitrary value you can use to uniquely identify the text being filtered. The `body` is the text you are filtering. Lastly, we specify that the data is plain text.

The `response` contains a zip file of the images generated by redacting the PDF document.