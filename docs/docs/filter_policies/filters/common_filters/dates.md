# Dates

## Filter

This filter identifies dates such as `May 22, 2014` in text. The supported date formats are:

| Format        | Example                   |
|---------------|---------------------------|
| yyyy-MM-d     | 2020-05-10                |
| MM-dd-yyyy    | 05-10-2020                |
| M-d-y         | 5-10-2020                 |
| MMM dd        | May 5 or May 05           |
| MMMM dd, yyyy | May 5, 2020, May 22, 1965, or May 22 1965 |
| dd MMMM, yyyy | 5 May, 2020, 22nd May, 1965, or 22 May 1965 |
| dd-MMMM-yyyy  | 22-May-1965                |

A date written with a month name, a day of one or two digits, and a four-digit year, such as `March 27, 1965` or `Sep 10 1950`, is detected with its year for any four-digit year. Other month-name forms, such as `March 27th, 1965`, `Mar. 27, 1965`, or `March 27-1965`, are detected with their year when it is from 1900 to 2099 or has two digits. A month name and a four-digit year with no day, such as `March 1965`, is detected for any four-digit year. A number after a month name is not read as a two-digit year when a four-digit year follows it after one or two characters, as in `March 27, 1965`. A year set further apart, as in `March 27 - 1965` or with two spaces after the comma, is not detected with the date. A day-first date with a month name and a four-digit year, such as `27 March, 1965`, `3 Mar, 2020`, `27th March 1965`, or `27-Mar-1965`, is detected for a day from 1 to 31, with or without an ordinal (`st`, `nd`, `rd`, `th`), and for any four-digit year. The day and month are separated by a single space, `-`, `/`, or `.`, and the month and year by the same delimiter or, after a space, by `, `. Its span carries a day-first format, such as `dd MMMM, yyyy` or `dd-MMMM-yyyy`. Detection is pattern-based, so validate the output against your own data.

### Required Parameters

This filter has no required parameters.

### Optional Parameters

| Parameter              | Description                                                                                                                                                                | Default Value                                            |
|------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------|
| `dateFilterStrategies` | A list of filter strategies.                                                                                                                                               | None                                                     |
| `enabled`              | When set to false, the filter will be disabled and not applied                                                                                                             | `true`                                                   |
| `ignored`              | A list of terms to be ignored by the filter.                                                                                                                               | None                                                     |
| `onlyValidDates`       | When set to true, only valid dates will be filtered.                                                                                                                       | `false`                                                  |
| `windowSize`           | Sets the size of the window (in terms) surrounding a span to look for contextual terms. If set, this value overrides the value of `span.window.size` in the configuration. | The value of `span.window.size` which is by default `5`. |
| `priority`             | The priority (integer) of this filter. Valid values are any positive integer, where a higher value indicates a higher priority. Priority is used for tie-breaking when two spans may be otherwise identical. | `0`           |

### Filter Strategies

The filter may have zero or more filter strategies. When no filter strategy is given the default strategy of `REDACT` is
used. When multiple filter strategies are given the filter strategies will be applied in order as they are listed.
See [Filter Strategies](#filter-strategies) for details.

| Strategy              | Description                                                   |
|-----------------------|---------------------------------------------------------------|
| `REDACT`              | Replace the sensitive text with a placeholder.                |
| `MASK`                | Replace each character of the sensitive text with a mask character (`*` by default).                                |
| `TRUNCATE`            | Replace all but a few characters of the sensitive text with a truncation character (`*` by default).                |
| `RANDOM_REPLACE`      | Replace the sensitive text with a similar, random value.      |
| `STATIC_REPLACE`      | Replace the sensitive text with a given value.                |
| `CRYPTO_REPLACE`      | Replace the sensitive text with its encrypted value.          |
| `HASH_SHA256_REPLACE` | Replace the sensitive text with its SHA256 hash value.        |
| `TRUNCATE_TO_YEAR`    | Replace the date with just its year.                          |
| `SHIFT`               | Shift the date by a number of days, months, and/or years, or by a random amount. |
| `RELATIVE`            | Replace the date with words relative to the date.            |

### Filter Strategy Options

The following filter strategy options are available for the `RELATIVE` filter strategy.

| Option        | Description                                                                                        | Default Value |
|---------------|----------------------------------------------------------------------------------------------------|---------------|
| `futureDates` | When `true`, future dates are replaced by relative words. When `false`, future dates are redacted. | `false`       |

The following filter strategy options are available for the `SHIFT` filter strategy.

| Option        | Description                                                                                                                                                                                                                | Default Value |
|---------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------|
| `shiftDays`   | The number of days to shift the date. Can be a negative or positive integer.                                                                                                                                              | `0`           |
| `shiftMonths` | The number of months to shift the date. Can be a negative or positive integer.                                                                                                                                            | `0`           |
| `shiftYears`  | The number of years to shift the date. Can be a negative or positive integer.                                                                                                                                             | `0`           |
| `shiftRandom` | When `true`, `shiftDays`, `shiftMonths`, and `shiftYears` are ignored and each date is instead shifted forward by 1 to 29 days and 1 to 11 months and back by 1 or 2 years, with each amount chosen at random for each date. | `false`       |

`SHIFT` and `TRUNCATE_TO_YEAR` redact a date they cannot parse in its detected format instead of transforming it, such as an invalid date (February 30) or a day-first date (`25/12/1980`).

### Conditions

Each filter strategy may have one condition. See [Conditions](#conditions) for details.

| Conditional  | Description                                                              | Operators                          |
|--------------|--------------------------------------------------------------------------|------------------------------------|
| `TOKEN`      | Compares the value of the sensitive text.                                | `==`, `!=`, `startswith`           |
| `TOKEN`      | Whether the date is a birth date or a death date. See below.             | `is`, `is not`                     |
| `CONTEXT`    | Compares the filtering context.                                          | `==` , `!=`                        |
| `CONFIDENCE` | Compares the confidence in the sensitive text against a threshold value. | `<` , `<=`, `>` , `>=`, `==`, `!=` |

#### Differentiating Between Dates and Birth Dates

In some cases it may be necessary to redact birth dates and dates differently. Using conditions it is possible to
determine if an identified date is a birth date or a death date. The condition `token is "birthdate"` is satisfied when
the words surrounding the identified date (within the filter's `windowSize`) suggest a birth date, such as `born`,
`birth`, or `dob`. `token is "deathdate"` does the same for a death date, with words such as `died`, `death`, or
`passed away`, and `token is "birthdate or deathdate"` is satisfied by either. `is not` is satisfied when the date is
not one of these. The value must be in double quotes.

The following policy shifts birth dates and redacts every other date:

```
{
   "identifiers": {
      "date": {
         "dateFilterStrategies": [
            {
               "condition": "token is \"birthdate\"",
               "strategy": "SHIFT",
               "shiftYears": 1
            },
            {
               "strategy": "REDACT"
            }
         ]
      }
   }
}
```

## Example Policy to Redact Dates

The following policy redacts dates.

```
{
   "identifiers": {
      "date": {
         "onlyValidDates": false,
         "dateFilterStrategies": [
            {
               "strategy": "REDACT",
               "redactionFormat": "{{{REDACTED-%t}}}"
            }
         ]
      }
   }
}
```

## Example Policy to Shift Dates

The following policy shifts dates forward by 2 days and 4 months.

```
{
   "identifiers": {
      "date": {
         "onlyValidDates": false,
         "dateFilterStrategies": [
            {
               "strategy": "SHIFT",
               "shiftDays": 2,
               "shiftMonths": 4,
               "shiftYears": 0
            }
         ]
      }
   }
}
```

## Example Policy to Shift Dates by a Random Amount

The following policy shifts each date by a random amount.

```
{
   "identifiers": {
      "date": {
         "dateFilterStrategies": [
            {
               "strategy": "SHIFT",
               "shiftRandom": true
            }
         ]
      }
   }
}
```
