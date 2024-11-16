# phileas-benchmark
Benchmark tests for Phileas PII engine

This command-line utility runs a series of single-threaded workloads using [Phileas](https://github.com/philterd/phileas)
to redact PII tokens in strings of varying sizes. Workloads can be run multiple times to warm up the JVM or test long-term use.
Workloads run for a fixed amount of time rather than a fixed number of iterations.

[![CodeFactor](https://www.codefactor.io/repository/github/philterd/phileas-benchmark/badge)](https://www.codefactor.io/repository/github/resurfaceio/phileas-benchmark)

## Dependencies

* Java 21
* Maven 3.9.x
* [philterd/phileas](https://github.com/philterd/phileas) 

## Running Locally

```
mvn clean package

# run workloads across all documents
java -server -Xmx512M -XX:+AlwaysPreTouch -XX:PerBytecodeRecompilationCutoff=10000 -XX:PerMethodRecompilationCutoff=10000 -jar target/phileas-benchmark-cmd.jar all mask_all 1 15000

# run workloads for specific document
java -server -Xmx512M -XX:+AlwaysPreTouch -XX:PerBytecodeRecompilationCutoff=10000 -XX:PerMethodRecompilationCutoff=10000 -jar target/phileas-benchmark-cmd.jar gettysberg_address mask_credit_cards 1 1000
```

To get the results back as a JSON object, append a `json` argument to the command:

```
java -server -Xmx512M -XX:+AlwaysPreTouch -XX:PerBytecodeRecompilationCutoff=10000 -XX:PerMethodRecompilationCutoff=10000 -jar target/phileas-benchmark-cmd.jar all mask_all 1 15000 json
```

### Available documents

* hello_world (11 chars)
* gettysberg_address (1474 chars)
* i_have_a_dream (7727 chars)

### Available redactors

For testing single identifiers:
* mask_bank_routing_numbers
* mask_bitcoin_addresses
* mask_credit_cards
* mask_drivers_licenses
* mask_email_addresses
* mask_iban_codes
* mask_ip_addresses
* mask_passport_numbers
* mask_phone_numbers
* mask_ssns
* mask_tracking_numbers
* mask_vehicle_numbers

For testing multiple identifiers:
* mask_all (the identifiers listed above 👆)
* mask_fastest (bank routing numbers, bitcoin addresses, credit cards, email addresses, IBAN codes, phone numbers, ssns)
* mask_none

---
Copyright 2024 Philterd, LLC @ https://www.philterd.ai
