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

Create a `PlainTextFilterService`, using a `PhileasConfiguration`, and call `filter()` on the service:

```
Properties properties = new Properties();
PhileasConfiguration phileasConfiguration = new PhileasConfiguration(properties);

// A policy is deserialized from its JSON definition.
Policy policy = new Gson().fromJson(policyJson, Policy.class);

PlainTextFilterService filterService = new PlainTextFilterService(
        phileasConfiguration, new DefaultContextService(), new InMemoryVectorService(), null);

TextFilterResult result = filterService.filter(policy, context, body);
```

The `policy` is a `Policy` deserialized from JSON. (See below for more about policies.) The `context` is an arbitrary value you can use to uniquely identify the text being filtered. The `body` is the text you are filtering.

The `result` contains information about the identified sensitive information along with the filtered text, available from `result.getFilteredText()`.

A `PlainTextFilterService` is safe to call concurrently: `filter()` may be invoked from multiple threads on a single shared instance without external locking, so a per-row caller (such as a Spark, Kafka, or logging function) can share one instance across threads.

#### Usage Examples

The [PlainTextFilterServiceTest](https://github.com/philterd/phileas/blob/main/src/test/java/ai/philterd/phileas/services/PlainTextFilterServiceTest.java) and [EndToEndTests](https://github.com/philterd/phileas/blob/main/src/test/java/ai/philterd/phileas/services/EndToEndTests.java) test classes have examples of how to configure Phileas and filter text.

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

#### Reusing one warm instance across requests with different context and vector services

The constructor above binds a single `ContextService` and `VectorService` to the instance. When each request needs its own context or vector service (for example a server that scopes the context store per user, or per-request entity-type disambiguation), build the service once with the service-less constructor and pass the per-request `ContextService` and `VectorService` to `filter()`. One warm instance then serves every request, keeping its filter and post-filter caches populated rather than rebuilding them on each request. The instance is safe to call concurrently; supply a thread-safe `Random` (the default is `SecureRandom`) when sharing it across threads.

```
// Build once and share. No context or vector service is bound to the instance.
PlainTextFilterService service = new PlainTextFilterService(phileasConfiguration, httpClient);

// Each request supplies its own context and vector service.
TextFilterResult result = service.filter(policy, contextService, vectorService, context, text);

// prepare() works the same way: resolve the policy once, then pass per-request services.
PlainTextFilterService.PreparedPolicy prepared = service.prepare(policy);
TextFilterResult prepared1 = prepared.filter(contextService, vectorService, context, text);
```

`PdfFilterService` exposes the same service-less constructor and per-call `filter(...)` overload for PDF and image documents.

### Finding and Redacting Sensitive Information in a PDF Document

Create a `PdfFilterService`, using a `PhileasConfiguration`, and call `filter()` on the service:

```
PhileasConfiguration phileasConfiguration = new PhileasConfiguration(new Properties());

// A policy is deserialized from its JSON definition.
Policy policy = new Gson().fromJson(policyJson, Policy.class);

PdfFilterService filterService = new PdfFilterService(
        phileasConfiguration, new DefaultContextService(), new InMemoryVectorService(), null);

BinaryDocumentFilterResult result = filterService.filter(policy, context, body, MimeType.APPLICATION_PDF);
```

The `policy` is a `Policy` deserialized from JSON. (See below for more about policies.) The `context` is an arbitrary value you can use to uniquely identify the document being filtered. The `body` is the PDF document bytes. The last argument is the output `MimeType`.

The `result` contains the redacted document along with information about the identified sensitive information.