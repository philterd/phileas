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
| [Dates](filters/common_filters/dates.md) | [A number after a month and day, such as the dose in `Mar 3 - 2000 mg`, is redacted as the year](filters/common_filters/dates.md#limitations). |
| [Cities](filters/locations/cities.md) | [The bundled list contains common words, such as "Call" and "Oak", which are redacted as cities](filters/locations/cities.md#limitations). |
