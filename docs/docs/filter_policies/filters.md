# Filters

A "filter" corresponds to a type of sensitive information. Phileas has filters for sensitive information such as names, addresses, ages, and lots of others.

These are predefined filters that are ready to be used as well as custom filters that let you define your own Phileas to identify sensitive information outside of what the predefined filters can identify. An example of a custom filter is a filter to identify your patient account numbers, where the structure of an account number is specific to your organization.

Each filter is capable of identifying and redacting a specific type of sensitive information. For example, there is a filter for phone numbers, a filter for US social security numbers, and a filter for person's names. You can enable any combination of these filters based on the types of sensitive information you need to redact.

This section of the documentation describes the filters available in Phileas. The configuration options for each filter can vary due to the type of the sensitive information. For instance, only the zip code filter has a configuration to truncate the zip code.

A selection of filters and their configurations is called a [policy](policies_README.md). A policy describes how to de-identify a document.

## Predefined Filters

### Person's Names

Phileas uses several methods to identify person's names.

| Type                                                                    | Description                                                          |
|-------------------------------------------------------------------------|----------------------------------------------------------------------|
| [First Names](filters/persons_names/first-names.md)                     | Identifies common first names                                        |
| [Surnames](filters/persons_names/surnames.md)                           | Identifies common surnames                                           |
| [Person's Names (NER)](filters/persons_names/persons-names-ner.md)      | Identifies full names using natural language processing analysis     |
| [Physician's Names (NER)](filters/persons_names/physician-names-ner.md) | Identifies physican names using natural language processing analysis |

### Other Filters

| Type                                                                                                                             | Description                                                                     |
|----------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------|
| [Ages](filters/common_filters/ages.md)                                                                                           | Identifies ages such as `3.5 years old`                                         |
| [Bank Routing Numbers](filters/common_filters/bank-routing-numbers.md.md)                                                        | Identifies bank routing numbers                                                 |
| [Bitcoin Addresses](filters/common_filters/bitcoin-addresses.md)                                                                 | Identifies Bitcoin addresses such as `127NVqnjf8gB9BFAW2dnQeM6wqmy1gbGtv`       |
| [Cities](filters/common_filters/cities.md)                                                                                       | Identifies common cities                                                        |
| [Counties](filters/common_filters/counties.md)                                                                                   | Identifies common counties                                                      |
| [Credit Card Numbers](filters/common_filters/credit-cards.md)                                                                    | Identifies VISA, American Express, MasterCard, and Discover credit card numbers |
| [Dates](filters/common_filters/dates.md)                                                                                         | Identifies dates in many formats such as May 22, 1999                           |
| [Driver's License Numbers](filters/common_filters/drivers-license-numbers.md)                                                    | Identifies driver's license numbers for all 50 US states                        |
| [Email Addresses](filters/common_filters/email-addresses.md)                                                                     | Identifies email addresses                                                      |
| [Hospitals](filters/locations/hospitals.md)                                                                                      | Identifies common hospital names                                                |
| [Hospital Abreviations](filters/locations/hospital-abbreviations.md)                                                             | Identifies common hospitals by their name abbreviations                         |
| [IBAN Codes](filters/common_filters/iban-codes.md)                                                                               | Identifies international bank account numbers                                   |
| [IP Addresses](filters/common_filters/ip-addresses.md)                                                                           | Identifies IPv4 and IPv6 addresses                                              |
| [MAC Addresses](filters/common_filters/mac-addresses.md)                                                                         | Identifies network MAC addresses                                                |
| [Passport Numbers](filters/common_filters/passport-numbers.md)                                                                   | Identifies US passport numbers                                                  |
| [Phone Numbers](filters/common_filters/phone-numbers.md)                                                                         | Identifies phone numbers                                                        |
| [Phone Number Extensions](filters/common_filters/phone-number-extensions.md)                                                     | Identifies phone numbers                                                        |
| [Sections](filters/common_filters/sections.md)                                                                                   | Identifies sections in text denoted by                                          |
| [SSNs and TINs](filters/common_filters/ssns-and-tins.md)                                                                         | Identifies US SSNs and TINs                                                     |
| [States](filters/locations/states.md)                 | Identifies US state names                                    |
| [State Abbreviations](filters/locations/state-abbreviations.md) | Identifies US state names by their abbreviations                                |
| [Tracking Numbers](filters/common_filters/tracking-numbers.md)                                                                   | Identifies UPS, FedEx, and USPS tracking numbers                                |
| [URLs](filters/common_filters/urls.md)                                                                                           | Identifies URLs                                                                 |
| [VINs](filters/common_filters/vins.md)                                                                                           | Identifies vehicle identification numbers                                       |
| [Zip Codes](filters/common_filters/zip-codes.md)                                                                                 | Identifies US zip codes                                                         |

## Custom Filter Types of Sensitive Information

In addition to the predefined types of sensitive information listed in the table above, you can also define your own types of sensitive information. Through custom identifiers and dictionaries, Phileas can identify many other types of information that may be sensitive in your use-case. For example, if you have patient identifiers that follow a pattern of `AA-00000` you can define a custom identifier for this sensitive information.

Phileas can be configured to look identify sensitive information based on custom dictionaries. When a term in the dictionary is found in the text, Phileas will treat the term as sensitive information and apply the given filter strategy.

Custom dictionaries support fuzziness to accommodate for misspellings. The replacement strategy for a custom dictionary has a `sensitivityLevel` that controls the amount of allowed fuzziness.

| Type                                                        | Description                                                                                                                                                |
|-------------------------------------------------------------| ---------------------------------------------------------------------------------------------------------------------------------------------------------- |
| [Custom Dictionaries](filters/custom_filters/dictionary.md) | Identifies sensitive information based on dictionary values.                                                                                               |
| [Custom Identifiers](filters/custom_filters/identifier.md)                         | Identifies custom alphanumeric identifiers that may be used for medical record numbers, patient identifiers, account number, or other specific identifier. |
