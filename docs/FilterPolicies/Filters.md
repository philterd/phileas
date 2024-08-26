# Filters

A "filter" corresponds to a type of sensitive information. Philter has filters for sensitive information such as names, addresses, ages, and lots of others.

These are predefined filters that are ready to be used as well as custom filters that let you define your own Philter to identify sensitive information outside of what the predefined filters can identify. An example of a custom filter is a filter to identify your patient account numbers, where the structure of an account number is specific to your organization.

Each filter is capable of identifying and redacting a specific type of sensitive information. For example, there is a filter for phone numbers, a filter for US social security numbers, and a filter for person's names. You can enable any combination of these filters based on the types of sensitive information you need to redact.

This section of the documentation describes the filters available in Philter. The configuration options for each filter can vary due to the type of the sensitive information. For instance, only the zip code filter has a configuration to truncate the zip code.

A selection of filters and their configurations is called a [policy](policies_README.md). A policy describes how to de-identify a document.

## Predefined Filters

### Person's Names

Philter uses several methods to identify person's names.

| Type                                                         | Description                                                      |
| ------------------------------------------------------------ | ---------------------------------------------------------------- |
| [First Names](first-names.md)                | Identifies common first names                                    |
| [Surnames](surnames.md)                      | Identifies common surnames                                       |
| [Person's Names (NER)](persons-names-ner.md) | Identifies full names using natural language processing analysis |

### Other Filters

| Type                                                                                                  | Description                                                                      |
| ----------------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------- |
| [Ages](ages.md)                                                                      | Identifies ages such as `3.5 years old`                                          |
| [Bitcoin Addresses](bitcoin-addresses.md)                                            | Identifies Bitcoin addresses such as `127NVqnjf8gB9BFAW2dnQeM6wqmy1gbGtv`        |
| [Cities](cities.md)                                                                         | Identifies common cities                                                         |
| [Counties](counties.md)                                                                     | Identifies common counties                                                       |
| [Credit Card Numbers](credit-cards.md)                                               | Identifies VISA, American Express, MasterCard, and Discover credit card numbers. |
| [Dates](dates.md)                                                                    | Identifies dates in many formats such as May 22, 1999                            |
| [Driver's License Numbers](drivers-license-numbers.md)                               | Identifies driver's license numbers for all 50 US states                         |
| [Email Addresses](email-addresses.md)                                                | Identifies email addresses                                                       |
| [Hospitals](hospitals.md) and [Hospital Abbreviations](hospital-abbreviations.md) | Identifies common hospital names and their abbreviations                         |
| [IBAN Codes](iban-codes.md)                                                          | Identifies international bank account numbers                                    |
| [IP Addresses](ip-addresses.md)                                                      | Identifies IPv4 and IPv6 addresses                                               |
| [MAC Addresses](mac-addresses.md)                                                    | Identifies network MAC addresses                                                 |
| [Passport Numbers](passport-numbers.md)                                              | Identifies US passport numbers                                                   |
| [Phone Numbers](phone-numbers.md)                                                    | Identifies phone numbers and phone number extensions                             |
| [Sections](sections.md)                                                              | Identifies sections in text denoted by                                           |
| [SSNs and TINs](ssns-and-tins.md)                                                    | Identifies US SSNs and TINs                                                      |
| [States](states.md) and [State Abbreviations](state-abbreviations.md)             | Identifies US state names and abbreviations                                      |
| [Tracking Numbers](tracking-numbers.md)                                              | Identifies UPS, FedEx, and USPS tracking numbers                                 |
| [URLs](urls.md)                                                                      | Identifies URLs                                                                  |
| [VINs](vins.md)                                                                      | Identifies vehicle identification numbers                                        |
| [Zip Codes](zip-codes.md)                                                            | Identifies US zip codes                                                          |

## Custom Filter Types of Sensitive Information

In addition to the predefined types of sensitive information listed in the table above, you can also define your own types of sensitive information. Through custom identifiers and dictionaries, Philter can identify many other types of information that may be sensitive in your use-case. For example, if you have patient identifiers that follow a pattern of `AA-00000` you can define a custom identifier for this sensitive information.

Philter can be configured to look identify sensitive information based on custom dictionaries. When a term in the dictionary is found in the text, Philter will treat the term as sensitive information and apply the given filter strategy.

Custom dictionaries support fuzziness to accommodate for misspellings. The replacement strategy for a custom dictionary has a `sensitivityLevel` that controls the amount of allowed fuzziness.

| Type                                                | Description                                                                                                                                                |
| --------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------- |
| [Custom Dictionaries](dictionary.md) | Identifies sensitive information based on dictionary values.                                                                                               |
| [Custom Identifiers](identifier.md)  | Identifies custom alphanumeric identifiers that may be used for medical record numbers, patient identifiers, account number, or other specific identifier. |
