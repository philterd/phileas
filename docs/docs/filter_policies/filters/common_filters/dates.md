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

A month-first date with a day of one or two digits, such as `March 27, 1965`, `Sep 10 1950`, `March 27th, 1865`, `Mar. 27, 2150`, or `March 27-1965`, is detected with its year for any four-digit year. Forms with an ordinal, a period, or a delimiter, such as `March 27th, 65`, also take a two-digit year. Between the day and a four-digit year there may be a comma, one or two hyphens, or an en or em dash, with up to two spaces on either side, or one to three spaces, as in `March 27 - 1965` or `March 27,  1965`. The day is then not read as a two-digit year. A year set further apart than these separators allow is not detected with the date. A month name and a four-digit year with no day, such as `March 1965`, is detected for any four-digit year.

A day-first date with a month name and a four-digit year, such as `27 March, 1965`, `3 Mar, 2020`, `27th March 1965`, or `27-Mar-1965`, is detected for a day from 1 to 31, with or without an ordinal (`st`, `nd`, `rd`, `th`), and for any four-digit year. The day and month are separated by a single space, `-`, `/`, or `.`, and the month and year by the same delimiter or, after a space, by `, `. Its span carries a day-first format, such as `dd MMMM, yyyy` or `dd-MMMM-yyyy`. Detection is pattern-based, so validate the output against your own data.

A month name may be spelled out or abbreviated to its first three letters, in any case and with or without a period after it: `Jan`, `Feb`, `Mar`, `Apr`, `May`, `Jun`, `Jul`, `Aug`, `Sep`, `Oct`, `Nov`, and `Dec`. September may also be abbreviated `Sept`, as in `Sept 3, 2020` or `3 Sept. 2020`. A word that only begins with a month's abbreviation, such as `Septic`, is not read as a month.

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

`SHIFT`, `TRUNCATE_TO_YEAR`, and `RELATIVE` transform these dates and redact any other date they cannot read:

| Date form | Examples | `TRUNCATE_TO_YEAR` | `SHIFT` | `RELATIVE` |
|---|---|---|---|---|
| Numeric, month first | `03/27/1965`, `1965-03-27` | Transformed | Transformed | Transformed |
| Month name with a day and a four-digit year, in either order | `March 27, 1965`, `Mar. 5th 2020`, `27 March, 1965`, `27-Mar-1965`, `March 27 - 1965` | Transformed | Transformed, in the same form | Transformed |
| Month name and a four-digit year, no day | `March 1965` | Transformed | Redacted | Transformed, as the first of the month |
| Month name with a two-digit year, or a month name and one number | `March 27th, 65`, `March 27` | Redacted | Redacted | Redacted |
| Numeric, day first | `25/12/1980` | Redacted | Redacted | Redacted |
| Not a real date | `February 30, 2020` | Redacted | Redacted | Redacted |

`SHIFT` writes a shifted month-name date in the form of the original: the month as it was written when it does not change, and otherwise spelled out or abbreviated to three letters as the original was (so `Sept 25, 2020` shifted into October becomes `Oct 5, 2020`), in the same case, with the same separators and any period after the month, with an ordinal (`st`, `nd`, `rd`, `th`) recomputed for the new day, and with a leading zero if the day had one. `March 27th, 1965` shifted by 10 days and 1 year becomes `April 6th, 1966`. A month and year with no day is redacted rather than shifted, since a shift of days alone would leave it unchanged. A two-digit year is not transformed because it is ambiguous (`65` could be 1965 or 2065). `May` is written as a full month name, since it cannot be told apart from the abbreviation, so in a document that abbreviates months, `May 5, 2020` shifted into June becomes `June 15, 2020` rather than `Jun 15, 2020`.

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

### Limitations

* **Day-first numeric dates are not transformed.** `SHIFT`, `TRUNCATE_TO_YEAR`, and `RELATIVE` redact a date such as
  `25/12/1980` rather than transforming it, whether or not `onlyValidDates` is set. The date is still redacted. Tracked
  in [philterd/phileas#467](https://github.com/philterd/phileas/issues/467).
* **A number after a month and day is taken as the year.** A month and day followed by a separator and any four-digit
  number is detected as one date, so in `Dose: Mar 3 - 2000 mg daily.` the dose is redacted with the date and the
  output is `Dose: {{{REDACTED-date}}} mg daily.` An ignored pattern broad enough to skip this would also skip real
  dates such as `March 27 - 1965`, so there is no workaround. Tracked in
  [philterd/phileas#468](https://github.com/philterd/phileas/issues/468).

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
