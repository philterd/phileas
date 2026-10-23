# Known Limitations

Phileas identifies sensitive information with patterns, word lists, and models, so detection is probabilistic: some
values are missed and some text that is not sensitive is redacted. How often depends on your documents and your
policy, so validate the output on a representative sample of your own data. See
[How to Evaluate Phileas' Performance](../evaluating-performance.md).

These limitations are known and reproducible. Each links to its filter's page, which describes it and how to work
around it.

| Filter | Known limitations |
|--------|-------------------|
| [Driver's License Numbers](filters/common_filters/drivers-license-numbers.md) | [A contextual term can belong to a different value, such as a house number near the word "license"](filters/common_filters/drivers-license-numbers.md#limitations). [A label farther away than `windowSize` words is not seen](filters/common_filters/drivers-license-numbers.md#limitations). |
| [Dates](filters/common_filters/dates.md) | [A number close to a month and day, or before a word that is not a listed unit, is taken as the year](filters/common_filters/dates.md#limitations). |
| [Cities](filters/locations/cities.md) | [A city that is also a common word, such as "Phoenix", is missed without a location context](filters/locations/cities.md#limitations). [A capitalized common word after "in" or "to", as in a title, is reported](filters/locations/cities.md#limitations). [With `fuzzy`, a capitalized word close to a city name can be reported](filters/locations/cities.md#limitations). |
