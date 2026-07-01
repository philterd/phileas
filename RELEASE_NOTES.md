# Phileas Release Notes

Notable changes to Phileas, most recent first.

Full changelogs for each release are available in the [GitHub releases](https://github.com/philterd/phileas/releases). Issues whose identifiers start with `PHL-` were previously tracked in Jira before the project's issues were managed in GitHub.

## Version 4.3.0 - Unreleased

* The street-address filter (opt-in, off by default) now matches many more formats: leading and trailing directionals (`123 N Main St`, `123 Main St NW`), ordinal street names (`123 5th Avenue`), house-number ranges and letter suffixes (`123-125 Main St`, `123A Main Street`), saint/abbreviated names (`100 St. Charles Avenue`), a wider set of street types (turnpike, expressway, crossing, plaza, loop, mews, and more), a trailing unit folded into the span (`Apt 4B`, `Suite 200`, `#5`), and PO boxes (`PO Box 1234`, `P.O. Box 56`, `Post Office Box 789`). Everyday phrases such as "Chapter 4 summary" are still not matched.
* The passport filter now detects a bare all-numeric 9-digit US passport book number. Its confidence (0.55) is set above the driver's-license 9-digit shape (0.50), so when both filters are enabled a bare 9-digit number resolves to the passport type. A number that also matches one of the specific passport prefixes keeps that higher-confidence match. (13-character driver's-license numbers of the form one letter followed by 12 digits were already detected.)
* The date filter now detects day-first numeric dates (for example `25/12/1980`). When a numeric date is not a valid date in month-first order, the filter re-validates it in day-first order, so real day-first dates are redacted rather than left in the clear when `onlyValidDates` is enabled. Ambiguous dates such as `03/04/1981` keep their month-first interpretation. Numeric dates using a `.` delimiter (for example `25.12.1980`) are now detected as well; the `.` delimiter is not applied to the month-and-year pattern, so a decimal such as `3.14` is not treated as a date. Day-first dates are removed by redaction; the `SHIFT` and `TRUNCATE_TO_YEAR` strategies fall back to redaction for a day-first date rather than transforming it.

## Version 4.2.0 - June 24, 2026

* Lowered the Java baseline from 25 to 17. Phileas now compiles to Java 17 bytecode, so it can be embedded in Java 17 and Java 21 runtimes (for example OpenSearch and Elasticsearch plugins, and Spark or Databricks jobs) that could not load the previous Java 25 build. There are no API changes, and consumers already on Java 21 or newer are unaffected.
* Added a warm, reusable filtering path so one filter-service instance can serve requests that each bring their own context and vector service (issue #413). `PlainTextFilterService` and `PdfFilterService` now offer service-less constructors plus `filter(...)` overloads (and a `PreparedPolicy.filter(...)` overload) that take a per-call `ContextService` and `VectorService`. A single shared instance keeps its filter and post-filter caches populated across requests instead of rebuilding them per request, while context and vector state stay per request. The existing constructors and `filter(policy, context, input)` methods are unchanged, so current callers need no changes. The instance is safe to share across threads.
* Standardized anonymization and synthetic-data randomness on `SecureRandom`, replacing all use of `java.util.Random`. The `SecureRandom` is supplied per instance at construction and defaults to a fresh `SecureRandom`; `PlainTextFilterService` and `PdfFilterService` add optional constructor overloads that accept a specific `SecureRandom` (for example one from a particular JCE or FIPS provider). It must be thread-safe when an instance is shared across threads, which `SecureRandom` is. Redaction output is unchanged.
* Using PhiSQL 1.2.0.

## Version 4.1.0 - June 21, 2026

* The custom `identifier` filter now supports an optional `validator`. The validator runs a named, built-in check on each regex match and keeps the match only if the check passes, so a generic identifier can reject format-valid but checksum-invalid values without a dedicated filter and without embedding any executable code in the policy. The first validator is `luhn` (standard mod-10 Luhn over the digits of the match, ignoring separators), which validates identifiers such as the Canadian SIN. The validator may be written as a string (`"validator": "luhn"`) or as an object with a `name` and optional `params`. An unknown or not-yet-implemented validator name is a policy error rather than being silently ignored. This requires redaction policy schema 1.1.0. See the documentation for details.
* Added the `bic-structural` identifier validator, which checks that a match is a structurally valid SWIFT/BIC code (ISO 9362: 8 or 11 characters, with a valid ISO 3166-1 country segment). SWIFT/BIC has no checksum, so this is a structural check rather than a checksum.
* Added the `de-personalausweis` identifier validator, which validates the ICAO 9303 (7-3-1 weighted) check digit of a German Personalausweis (national ID card) number.
* Added the `de-steuerid` identifier validator, which validates a German tax identification number (Steuer-ID) using the structural digit-repetition rule on the first ten digits and the ISO/IEC 7064 MOD 11,10 check digit.
* Added the `mod11` identifier validator (weighted-sum mod-11 check digits), with `cpf` and `cnpj` variants for the Brazilian CPF and CNPJ.
* Added the `mod97` identifier validator (control derived from the value mod 97), with an `iban` variant (ISO 13616 MOD-97-10) and a `nir` variant for the French INSEE/NIR, including Corsica department substitutions.
* Added the `mod23-letter` identifier validator (control letter from a 23-entry table), for the Spanish DNI and NIE.
* Added the `es-cif` identifier validator for the Spanish CIF (organization tax ID).
* PhEye detection can now be backed by a pluggable detector. In addition to calling a remote Ph-Eye service over HTTP, a PhEye filter can run a GLiNER model on-device by setting `modelPath` on its configuration; local inference is provided by the optional [`phileas-pheye-onnx`](https://github.com/philterd/phileas-pheye-onnx) module (ONNX Runtime). Without that module on the classpath, setting `modelPath` fails fast while the policy's filters are built (for example at `prepare(policy)` time, or on the first `filter()` call) with a dedicated `MissingPhEyeProviderException` and a logged error pointing at the missing dependency, rather than silently falling back to the remote service; remote Ph-Eye behavior is otherwise unchanged.
* `filter()` is now documented and tested as safe to call concurrently on a single shared `FilterService` instance. The per-policy filter cache is populated with `computeIfAbsent`, so concurrent first-calls for the same policy build the filter set once instead of each racing thread rebuilding it. Per-row callers (such as a Spark, Kafka, or logging UDF) can share one instance across threads without locking around `filter()`.
* Large resource-backed dictionaries are now loaded once per process and shared, instead of being read and held separately by each `FilterService` instance and data generator. This covers both the predefined detection dictionaries (cities, counties, states, hospitals, first names, and surnames) and the name lists used for replacement and anonymization (first names, surnames, cities, counties, hospitals, and similar). These lists are identical everywhere and large (names especially), so sharing them cuts memory when many instances run on one host (for example a Spark executor with many cores). The shared structures are immutable and read-only, so detection and replacement behavior are unchanged. Custom (policy-supplied) dictionaries are unaffected.
* Post-filters are now cached per policy, the same way compiled filters are, so the post-filter list (including ignore-term and ignore-pattern filters, the latter recompiling regexes) is built once per policy and reused instead of being rebuilt on every `filter()` call. This removes that per-call overhead for the non-prepared `filter(policy, context, text)` path; defaults and behavior are unchanged.
* Added a prepared-policy API to `PlainTextFilterService` for high-throughput, per-row callers (such as a Spark, Kafka, or logging function). `service.prepare(policy)` resolves the policy's filters and post-filters once and returns a `PreparedPolicy` handle whose `filter(context, text)` reuses them, so the per-call policy resolution (including rebuilding the post-filter list) is not repeated on every call. The existing `filter(policy, context, text)` is unchanged, and the handle is safe to reuse across calls and threads. See the documentation for a hot-loop example.
* The per-policy cache key is now memoized on the `Policy` rather than recomputed on every `filter()` call. Previously each call re-serialized the whole policy to JSON and hashed it, even on a cache hit; the key is now hashed once and reused, which removes that per-row overhead for high-throughput callers that reuse a policy. A `Policy` is treated as immutable once it has been used for filtering: its key is computed on first use and not recomputed, so to change a policy you build a new one (the usual path, since an edited policy is loaded as a new object).
* The default Ph-Eye detection label changed from `Person` to `name`, aligning with the new default `ph-eye-pii-en-*` models, which are trained on the `name` label. Both `name` and `Person` now map to `FilterType.PERSON`, so existing deployments stay compatible: the older `ph-eye-pii-base` model still detects names when asked for `name`, and policies that set the Ph-Eye `labels` explicitly are unaffected.
* Using PhiSQL 1.1.0.

## Version 4.0.0 - June 10, 2026

* Now uses Java 25.
* `CRYPTO_REPLACE` now uses authenticated AES-GCM encryption with a fresh random nonce per value, replacing AES-CBC with a static IV. Values encrypted by earlier versions are not compatible, and the `iv` policy field is no longer used (it is still accepted in policies for backward compatibility but is ignored). Encryption keys and FPE tweaks are referenced from environment variables with the `env:` prefix rather than stored in policies. See the documentation for details.
    * **Upgrading from 3.x:** values that were redacted with `CRYPTO_REPLACE` under 3.x (AES-CBC) cannot be decrypted by 4.0.0, and there is no in-place migration. Decrypt anything you need to recover with a 3.x build before upgrading. See the documentation for details.
* The redaction policy JSON schema is now published at [philterd.ai](https://philterd.ai/schemas/redaction-policy/1.0.0/schema.json) and authored in the [philterd/phisql](https://github.com/philterd/phisql) repository. The Phileas-to-schema version mapping is in the README. See the documentation for details.
* Phileas 4.0.0 supports [PhiSQL](https://github.com/philterd/phisql) 1.0.0 as a policy authoring format. A `Policy` can now be created directly from a PhiSQL document with `Policy.fromPhiSQL(...)`, which compiles the PhiSQL to a JSON policy and loads it. Existing JSON policies continue to load unchanged — PhiSQL is purely additive.
* Format-preserving encryption that cannot encrypt a token now redacts that token instead of failing the document.
* Added a configurable regular-expression timeout to guard against catastrophic backtracking. See the documentation for details.
* The age filter now also detects ages written as spelled-out numbers, for example "thirty-five years old", "thirty-five-year-old", "aged forty-two", and "one hundred years old", in addition to numeric ages. (Other digit-based filters, such as SSN and credit card, continue to match numeric values only.)
* Dictionary-based filters now match terms with a direct, case-insensitive set lookup instead of a bloom filter. This applies to custom dictionaries and to the predefined location and name dictionaries (cities, counties, states, hospitals, first names, and surnames) when fuzzy matching is not enabled. The bloom filter was only a pre-filter in front of an in-memory set that already performs the actual lookup, so it added work (and memory) without speeding anything up at any dictionary size. The now-unused `BloomFilterDictionaryFilter` was removed.
* The non-fuzzy (exact) dictionary filters now match a term even when it is immediately adjacent to punctuation — for example, "Boston" is now redacted in "He visited Boston." Previously the trailing punctuation was treated as part of the token, so the term was not matched. The redaction covers only the term, not the surrounding punctuation.
* Custom dictionary, identifier, and ph-eye filters are now cached per policy like the other filters, rather than being rebuilt on every request. The complete filter list for a policy is built once (keyed by a hash of the policy) and reused — so, for example, custom dictionary term files are no longer re-read from disk on each request. This improves throughput for policies that use these filters.
* Fixed the fuzzy dictionary filter sensitivity levels (used by the city, county, hospital, first name, surname, and fuzzy custom dictionary filters). Previously the `MEDIUM` and `LOW` levels both allowed a Levenshtein distance of up to 2 and so behaved identically. The levels are now distinct: `HIGH` matches only exact tokens, `MEDIUM` allows a distance of up to 1, and `LOW` allows a distance of up to 2.
* Fixed a referential integrity bug in context-scoped anonymization (`RANDOM_REPLACE` with a `CONTEXT` replacement scope). When a detected token happened to equal a replacement value already generated for a different token in the same context, the anonymizer returned a null replacement and the token was left unredacted (it appeared in output as the literal text `null`). The token is now always redacted and mapped consistently.
* Overhauled span disambiguation (the feature that uses context vectors to decide which type an ambiguously-typed span is, such as an SSN vs. a phone number) so it works as intended, is safe under concurrency, and can persist its learning:
    * Correctness: it now learns only from genuinely unambiguous spans over time (a span at a location two filters disagreed about is no longer mistakenly recorded as training data), uses correct cosine-similarity scoring with deterministic cold-start handling, hashes context tokens case-insensitively and over their UTF-8 bytes (so a token maps to the same vector index on every platform), and honors the configured stop words and hash algorithm.
    * Thread safety: the in-memory vector store no longer loses accumulated counts or discards a context's training data when documents are processed concurrently.
    * Persistence: added `FileBasedVectorService`, which loads on startup and saves on demand (or on `close()`), so the "learns over time" behavior survives process restarts. The persisted file records the vector size and hash algorithm its vectors were built with; if either no longer matches the configuration, the stale vectors are discarded with a warning (a cold start) rather than reused as invalid data.
    * Wiring: the service is now selected by `SpanDisambiguationServiceFactory`, which returns the vector-based implementation when the feature is enabled and a no-op implementation when it is disabled, so the pipeline no longer checks whether the feature is enabled before calling it. The `SpanDisambiguationService.isEnabled()` method was removed as part of this change.
    * Context windows no longer include empty tokens produced when a token was only punctuation (or from an empty split), removing correlated noise from the vectors and cleaning up the window used by all filters.
* The tracking number filter now also detects USPS tracking numbers written in the space-separated grouped form (for example, `9400 1000 0000 0000 0000`). Previously only tracking numbers written as a continuous string of digits were detected.
* Fixed an `IllegalStateException` in the credit card filter that could occur when a credit-card-like number appeared inside a Unix timestamp and both the "ignore when in a Unix timestamp" and "only valid credit card numbers" options were enabled. The number is now correctly ignored instead of throwing.

## Version 3.3.0 - April 2, 2026

* Added a UUID anonymization method ([#297](https://github.com/philterd/phileas/pull/297)) and optional custom anonymization lists for each filter ([#293](https://github.com/philterd/phileas/pull/293)).
* Moved the anonymization service to the filter strategy ([#298](https://github.com/philterd/phileas/pull/298)) and simplified how the anonymized token is retrieved ([#296](https://github.com/philterd/phileas/pull/296)).
* Allowed multiple ph-eye services in a policy ([#304](https://github.com/philterd/phileas/pull/304)) and added the filter type to the context service ([#295](https://github.com/philterd/phileas/pull/295)).
* Added the `mmmm dd yyyy` (no comma) date format ([#301](https://github.com/philterd/phileas/pull/301)).
* Replaced the faker library ([#299](https://github.com/philterd/phileas/pull/299)) and removed the hospital abbreviations filter ([#300](https://github.com/philterd/phileas/pull/300)) and the policy name ([#302](https://github.com/philterd/phileas/pull/302)).
* Fixed applying existing spans to text ([#292](https://github.com/philterd/phileas/pull/292)), fixed the `firstName.setCapitalized(true)` check only being applied to fuzzy matches ([#305](https://github.com/philterd/phileas/pull/305)), and adjusted the libphonenumber max tries ([#294](https://github.com/philterd/phileas/pull/294)).

## Version 3.2.0 - January 20, 2026

* Added support for the ph-eye medical conditions filter ([#287](https://github.com/philterd/phileas/pull/287), [#288](https://github.com/philterd/phileas/pull/288)).
* Anonymization services can now use a custom `Random` implementation ([#282](https://github.com/philterd/phileas/pull/282)), and the HTTP client can be passed in for more control ([#283](https://github.com/philterd/phileas/pull/283)).
* Fixed persons being detected as ages ([#290](https://github.com/philterd/phileas/pull/290)).
* Updated log4j to 2.25.3 ([#285](https://github.com/philterd/phileas/pull/285)).

## Version 3.1.0 - December 29, 2025

* Format-preserving encryption is now available on more filters ([#270](https://github.com/philterd/phileas/pull/270)).
* Added line and paragraph numbers to spans during PDF processing ([#273](https://github.com/philterd/phileas/pull/273), [#276](https://github.com/philterd/phileas/pull/276)) and the ability to redact previously identified spans in a PDF ([#275](https://github.com/philterd/phileas/pull/275)).
* Added a `fuzzy` option to dictionaries that enables a bloom filter ([#262](https://github.com/philterd/phileas/pull/262)).
* Switched random value generation to `SecureRandom` ([#278](https://github.com/philterd/phileas/pull/278), [#280](https://github.com/philterd/phileas/pull/280)).
* Internal refactoring: separated `PhileasFilterService` into subclasses ([#274](https://github.com/philterd/phileas/pull/274)), created an abstract filter result ([#266](https://github.com/philterd/phileas/pull/266)), removed `apply()` ([#272](https://github.com/philterd/phileas/pull/272)), and removed the alert on filter strategies ([#264](https://github.com/philterd/phileas/pull/264)).

## Version 3.0.0 - October 31, 2025

This version restructured the project for simplification — Phileas is now a single Maven module instead of multiple modules, and the `context` concept was reimagined ([#249](https://github.com/philterd/phileas/pull/249)).

* Simplified policy handling: policies are now passed as `Policy` objects and the `PolicyService` abstraction was removed ([#214](https://github.com/philterd/phileas/pull/214), [#216](https://github.com/philterd/phileas/pull/216), [#227](https://github.com/philterd/phileas/pull/227), [#236](https://github.com/philterd/phileas/pull/236)).
* Added "incremental redaction" that hashes the text after each span is applied, for both text and PDFs ([#238](https://github.com/philterd/phileas/pull/238), [#251](https://github.com/philterd/phileas/pull/251)).
* Added token counts to responses ([#244](https://github.com/philterd/phileas/pull/244)), a split service based on character count ([#241](https://github.com/philterd/phileas/pull/241)), a `skip` property on post filters ([#245](https://github.com/philterd/phileas/pull/245)), and the ability to apply previously identified spans to text ([#254](https://github.com/philterd/phileas/pull/254)).
* Changed the condition operator `=>` to the more common `>=` ([#248](https://github.com/philterd/phileas/pull/248)).
* Fixed invalid IBAN codes being returned ([#250](https://github.com/philterd/phileas/pull/250)) and a hardcoded ph-eye filter type ([#247](https://github.com/philterd/phileas/pull/247)).
* Removed the document ID ([#253](https://github.com/philterd/phileas/pull/253)), sentiment detection ([#231](https://github.com/philterd/phileas/pull/231)), built-in metrics ([#225](https://github.com/philterd/phileas/pull/225)), and various unused code and dependencies.
* Renamed the Response classes to Result and reorganized packages ([#257](https://github.com/philterd/phileas/pull/257)).

## Version 2.12.3 - May 29, 2025

* Fixed the HTTP connection to ph-eye being closed after each request ([#212](https://github.com/philterd/phileas/pull/212)).

## Version 2.12.2 - May 27, 2025

* Artifacts are now published to Maven Central ([#209](https://github.com/philterd/phileas/pull/209)).
* Consolidated the caches ([#202](https://github.com/philterd/phileas/pull/202)) and replaced retrofit with Apache HttpComponents ([#205](https://github.com/philterd/phileas/pull/205)).
* Removed the `domain` property from policies ([#207](https://github.com/philterd/phileas/pull/207)) and fixed the `SAME` value ([#201](https://github.com/philterd/phileas/pull/201)).

## Version 2.12.1 - March 24, 2025

* Fixed the duplicate span check for filter priority ([#195](https://github.com/philterd/phileas/pull/195)).

## Version 2.12.0 - March 20, 2025

* Added a filter `priority` ([#195](https://github.com/philterd/phileas/pull/195)) and a per-filter `windowSize` ([#194](https://github.com/philterd/phileas/pull/194)).
* Added an option to validate zip codes ([#193](https://github.com/philterd/phileas/pull/193)) and updated the zip code population data to the 2020 census ([#192](https://github.com/philterd/phileas/pull/192)).

## Version 2.11.0 - March 3, 2025

* PDF redaction improvements ([#186](https://github.com/philterd/phileas/pull/186)).
* Changed the behavior for zip codes that don't exist ([#189](https://github.com/philterd/phileas/pull/189)).
* Ignored words are now checked with a bloom filter ([#191](https://github.com/philterd/phileas/pull/191)).

## Version 2.10.0 - January 6, 2025

* Replaced the Lucene-based dictionary filter with a fuzzy dictionary filter ([#185](https://github.com/philterd/phileas/pull/185)).
* Added a truncation filter strategy for all filters ([#180](https://github.com/philterd/phileas/pull/180)) and the ability to output the replacement value on PDFs ([#179](https://github.com/philterd/phileas/pull/179)).
* Updated PDFBox to 3.0 ([#177](https://github.com/philterd/phileas/pull/177)) and fixed the policy service being hardcoded to "local" ([#178](https://github.com/philterd/phileas/pull/178)).
* Removed the guava ([#172](https://github.com/philterd/phileas/pull/172)) and commons-csv ([#174](https://github.com/philterd/phileas/pull/174)) dependencies, and `FilterResponse` is no longer a final record class ([#166](https://github.com/philterd/phileas/pull/166)).

## Version 2.9.1 - November 20, 2024

* Fixed `LineWidthSplitService` using a new line separator instead of a space ([#163](https://github.com/philterd/phileas/issues/163)).
* An empty list of spans from ph-eye no longer indicates failure ([#162](https://github.com/philterd/phileas/issues/162)).
* Added a default `PhEyeConfiguration` value so filters do not have to provide one ([#161](https://github.com/philterd/phileas/issues/161)).

## Version 2.9.0 - November 19, 2024

* More in-depth regex checking on credit cards based on BIN ([#152](https://github.com/philterd/phileas/pull/152)).
* Fixed concurrent modification issues ([#148](https://github.com/philterd/phileas/pull/148)) and failing tests on Windows ([#160](https://github.com/philterd/phileas/pull/160)).
* Upgraded ANTLR to 4.13.2 ([#150](https://github.com/philterd/phileas/pull/150)) and cleaned up dependencies ([#156](https://github.com/philterd/phileas/pull/156), [#158](https://github.com/philterd/phileas/pull/158)).

## Version 2.8.0 - September 25, 2024

* Incorporated ph-eye as a replacement for the PersonsV1 filter ([#144](https://github.com/philterd/phileas/issues/144)).
* Changed the default `ner.endpoint` value to `ph-eye:18080` ([#143](https://github.com/philterd/phileas/issues/143)).

## Version 2.7.1 - September 25, 2024

* Fixed span start positions not being adjusted based on previous replacements, a regression introduced in 2.7.0 ([#145](https://github.com/philterd/phileas/issues/145)).

## Version 2.7.0 - September 4, 2024

* Added the MASK filter strategy ([#105](https://github.com/philterd/phileas/pull/105)).
* Improved performance of email address detection ([#132](https://github.com/philterd/phileas/pull/132)) and added an optional check that email addresses have a valid TLD ([#135](https://github.com/philterd/phileas/pull/135)).
* Credit card improvements: a flag to ignore credit-card-like numbers in Unix timestamps ([#137](https://github.com/philterd/phileas/pull/137)) and reduced confidence when a credit card span is bordered by dashes ([#142](https://github.com/philterd/phileas/pull/142)).
* Spans are now placed into the applied and identified lists appropriately ([#127](https://github.com/philterd/phileas/pull/127)).
* Removed the metrics service implementation ([#133](https://github.com/philterd/phileas/pull/133)) and fixed the EOL handling in the ANTLR grammar for conditions ([#136](https://github.com/philterd/phileas/pull/136)).

## Version 2.6.0 - July 2, 2024

* PHL-313 - Remove OWNER project fork for managing application properties
* PHL-312 - Add death date detection similar to the birthdate detection
* PHL-312 - Add a new condition comparator for “IS NOT” so you can say date filter strategy "IS NOT birthdate" or "IS NOT deathdate"
* PHL-311 - Return the probabilities of sentiment and offensiveness in addition to the predicted categories
* [#101](https://github.com/philterd/phileas/issues/101) - Dependency version updates.

## Version 2.5.0 - November 19, 2023

* PHL-309 - Load OpenNLP models from the classpath by default
* PHL-308 - Add sentiment as a condition

## Version 2.4.0 - October 17, 2023

* PHL-306 - Rename "filter profile" to "policy"
* PHL-305 - Allow dates to be shifted by a random value
* PHL-304 - Skip onnx runtime tests on OSX

## Version 2.3.0 - September 17, 2023

* PHL-277 - Combine PDFs image into a new PDF
* PHL-264 - Bring back Flair as PersonsV1 filter

## Version 2.2.0 - August 14, 2023

* PHL-277 - Combine PDFs image into a new PDF - **05/Sep/23 12:27** - Today 1:37
* PHL-264 - Bring back Flair as PersonsV1 filter

## Version 2.1.0 - May 16, 2023

* PHL-261 - Incorporate a sentence detector into the NER
* PHL-260 - Switch to Apache OpenNLP 2.0 instead of using ONNX Runtime directly
* PHL-259 - Allow for reading encryption values from environment variables
* PHL-258 - Implement format-preserving encryption

## Version 2.0.0 - February 5, 2022

* PHL-254 - Remove splitting capability
* PHL-251 - Replace phileas-ner with Java ONNX service

## Version 1.12.2 - December 20, 2021

* PHL-249 - Upgrade log4j to 2.16.0

## Version 1.12.1 - December 14, 2021

* PHL-249 - Upgrade log4j to 2.16.0

## Version 1.12.0 - December 14, 2021

* PHL-248 - Upgrade log4j to 2.15.0
* PHL-246 - Remove store from FilterService
* PHL-245 - Add S3 key to S3FilterProfileService
* PHL-244 - Allow user to set bounding boxes for PDF redaction
* PHL-243 - Allow for combining filter profiles per request
* PHL-242 - A date filter test is failing due to mid-month
* PHL-239 - Support dates like Aug. 31, 2020
* PHL-238 - Support ages like: 61 y/o
* PHL-233 - Filter profiles reference environment variable values
* PHL-227 - Support currency such as $.50
* PHL-173 - 9 digit zip codes without a delimiter are not found - Improvement

## Version 1.11.0 - June 7, 2021

* PHL-226 - Add currency filter
* PHL-222 - Add bank routing number filter
* PHL-218 - Add document analysis prior to filtering
* PHL-211 - Encapsulate the constructor arguments to the filters

## Version 1.10.0 - March 23, 2021

* PHL-217 - Change redisson delete to delete by index
* PHL-216 - Update project dependencies
* PHL-208 - Redact multiple occurrences of same span in line in PDF - Bug
* PHL-207 - Improve date year validation
* PHL-204 - Date "July 3, 2012" is not being identified - Bug
* PHL-201 - Add domain property to filter profile
* PHL-200 - Add properties to the filter profile to enable/disable post filters
* PHL-198 - Add property to NER filter to set a confidence threshold
* PHL-197 - Add function to return Span as a CSV
* PHL-196 - Create spans from LAPPS JSON
* PHL-195 - Include JPEG processing libraries for PDF extraction
* PHL-187 - PostFilter classes with empty constructors should be singletons
* PHL-183 - Adjust phone number confidence based on pattern
* PHL-162 - Fix RELATIVE date filter strategy for "October 2009" date

## Version 1.9.0 - January 19, 2021

* PHL-188 - Add street address filter
* PHL-184 - Remove new line characters from spans
* PHL-179 - Lowercase names in first name index
* PHL-178 - Surname filter tests aren't finding anything
* PHL-177 - Identify physician names
* PHL-176 - IDENTIFIER filter is finding just capitalized words
* PHL-175 - Add support for age format "64-year-old"
* PHL-174 - Ages should not identify "10 years"
* PHL-171 - Add redaction color for PDF
* PHL-169 - Add PDF document processing
* PHL-166 - Add redaction method to leave the last four digits
* PHL-165 - Redact dates to just the 4 digit year
* PHL-163 - Handle dates like 09-2021
* PHL-161 - Fix logger class names in date filter and identifier filter
* PHL-160 - Add optional period in ages
* PHL-159 - Increase ner.timeout.sec to a large value
* PHL-158 - Set -1 to split threshold to set no limit
* PHL-157 - Resolve NPEs with dictionary filter
* PHL-4 - Shift dates by some time period

## Version 1.8.0 - November 1, 2020

* PHL-155 - Maintain metrics on individual filter performance times
* PHL-148 - Read list of filter ignored terms from a file

## Version 1.7.0 - September 21, 2020

* PHL-154 - Add Prometheus monitoring endpoint
* PHL-152 - Add detect() method to filters
* PHL-151 - Make filter ignore lists be not case-sensitive
* PHL-150 - BloomDictionaryFilter needs to support phrases and not just words
* PHL-149 - Fix IBAN code validation - Bug
* PHL-147 - Allow for ignoring based on patterns
* PHL-146 - Allow timeout between Philter and Philter NER to be customizable
* PHL-145 - Split large text into multiple pieces
* PHL-141 - Read list of globally ignored words from a file
* PHL-140 - Add classification condition to IdentifierFilterStrategy
* PHL-139 - Allow spaces in IBAN codes

## Version 1.6.0 - June 8, 2020

* PHL-143 - Fix spans not going to the store - Bug
* PHL-142 - Add PhileasSystemLoader to OWNER
* PHL-137 - Create filter for mailing tracking numbers
* PHL-136 - Replace DropWizard metrics with micrometer
* PHL-135 - Change date parsing to use year instead of year of era - Improvement
* PHL-134 - Fix filter type for Mac address - Bug
* PHL-133 - Fix case sensitivity check for ignored terms - Bug
* PHL-132 - Fix credit card validation - Bug
* PHL-131 - Upgrade to JUnit 5
* PHL-129 - Change "label" to "classification" - Improvement
* PHL-128 - Fix ignore check for LuceneDictionaryFilter - Bug
* PHL-127 - Add fuzzy property to custom dictionary filter to trigger a bloom filter
* PHL-126 - Allow custom dictionaries to read terms from a file
* PHL-125 - Add tests for all FilterStrategy classes
* PHL-124 - Add condition for checking classification of a span
* PHL-123 - Expand US passport regex to only include valid first two digits
* PHL-122 - Fix mac address filter strategy assignment
* PHL-121 - Add option to disable metrics printed to the console
* PHL-120 - Set contextual words for each regex filter
* PHL-118 - Wrap regular expression patterns into an object

## Version 1.5.0 - April 30, 2020

* PHL-98 - Make cache generic and use cache for filter profiles
* PHL-97 - Incorporate the S3FilterProfileService into Phileas
* PHL-69 - Create "Section" filter that removes between start and end tags

## Version 1.4.0 - April 9, 2020

* PHL-96 - Upgrade libphonenumber to 8.12.1
* PHL-95 - Add token condition to NerFilterStrategy
* PHL-93 - Presence of a confidence condition on a phone number filter strategy causes the text to not be redacted
* PHL-92 - The token condition specifies quotes twice
* PHL-89 - Add detection framework
* PHL-87 - Drop ignored spans before overlapping spans
* PHL-86 - Use "input" and "token" consistently
* PHL-85 - StateAbbreviationFilter is making a span with the input and not the state
* PHL-84 - Capture a span's window when creating a span

## Version 1.3.1 - February 23, 2020

* PHL-82 - Allow client to set document ID
* PHL-78 - Switch to Java 11
* PHL-76 - Support credit card numbers with dashes
* PHL-75 - Add support for TIN to SsnFilter
* PHL-74 - Add filter for MAC addresses
* PHL-73 - Add a redaction option for a crypto value
* PHL-71 - Add replacement template value for the token
* PHL-70 - Add filter condition based on context

## Version 1.3.0 - January 27, 2020

* PHL-68 - When there are no strategies just redact
* PHL-67 - Don't identify "may" as a date
* PHL-66 - Check filter profile delete status
* PHL-65 - Span text should not end with a period or a space
* PHL-64 - Add regex for URLs containing IP addresses instead of domain name
* PHL-62 - Add option to URL filter to require http(s) protocol or www
* PHL-59 - Improve performance by tuning PyTorch filter client
* PHL-58 - Generate document ID based on input hash
* PHL-56 - Don't throw NPE when Philter-NER throws an error

## Version 1.2.0 - January 16, 2020

* PHL-55 - Fix issue where NER conditions are not being applied to NER spans
* PHL-54 - Add filter profile option to remove punctuation
* PHL-53 - Release 1.2.0
* PHL-52 - Move Status class from Phileas to Philter
* PHL-50 - Offer encrypted connections to Redis
* PHL-47 - Add enabled property to filter types in filter profile
* PHL-46 - Add option to require dates to be valid dates
* PHL-42 - Add option to find invalid credit cards
* PHL-35 - Add ignore lists specific to individual filters
* PHL-33 - Test multiple identifiers in a filter profile

## Version 1.1.0 - December 14, 2019

* PHL-41 - Fix replacement checks in strategies
* PHL-39 - Allow for the filters to be reloaded at runtime
* PHL-38 - Don't use spring boot for dependency management
* PHL-36 - Integrate Philter Profile Registry into Phileas
* PHL-34 - Make store implementation for Elasticsearch
* PHL-32 - In IdentifierFilter fix the name/label assignment
* PHL-31 - Automatically set Lucene distance based on the string length
* PHL-30 - Add ignore lists to filter profile schema
* PHL-29 - Include item text in Span
* PHL-24 - Allow for custom dictionary lookup in filters
* PHL-23 - Get philter-ner endpoint from Philter
* PHL-21 - Add filter profile schema
* PHL-19 - Provide an enhanced response to describe how the spans were found/removed
* PHL-18 - Set default values for filter profile fields
* PHL-16 - Report metrics per individual filter type
* PHL-15 - Add metrics prefix property
* PHL-13 - Do not require filter profile name for StaticFilterProfileService
* PHL-3 - Apply sensitivity level to NER entities

## Version 1.0.1 - October 17, 2019

* PHL-11 - Fix issue where spans aren't getting applied - Bug
* PHL-10 - Remove unneeded guava dependency
* PHL-9 - Add license information to poms

## Version 1.0.0 - October 06, 2019

Initial release.

* PHL-7 - RedisAnonymizationCacheService needs containsValue implemented
* PHL-5 - Allow Identifier filters to specify regex
