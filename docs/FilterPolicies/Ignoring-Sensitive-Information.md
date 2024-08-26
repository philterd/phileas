# Ignoring Sensitive Information

Phileas can optionally ignore a list of terms and prevent those terms from being redacted. For example, if the name `John Smith` is being redacted and you do not want it to be redacted, you can add `John Smith` to an ignore list. Each time Phileas identifies sensitive information it will check the ignore lists to see if the sensitive information is to be ignored.

> Phileas can ignore terms and patterns per-policy, meaning each policy can have its own unique list of terms or patterns to ignore.


## Ignore Lists

Ignore lists can be specified at the policy level and/or for each filter in the policy. When set for the policy, the list of ignored terms will be applied to _all_ filter types. When set for a filter, the list of ignored terms will be applied _only_ to that filter.

### Ignore List for a Policy

In the policy shown below, an ignore list is set at the level of the policy. The terms specified in the list will be ignored for _all_ filter types enabled in the policy. Only the terms property is required. The `name` and `caseSensitive` properties are optional.

```
{
   "name": "example-policy",
   "ignored": [
     {
       "name": "names to ignore",
       "terms": ["john smith", "jane doe"],
       "caseSensitive": false
     }
   ],
   "identifiers": {
      "emailAddress": {
         "emailAddressFilterStrategies": [
            {
               "strategy": "REDACT",
               "redactionFormat": "{{{REDACTED-%t}}}"
            }
         ]
      }
   }
}
```

Terms to be ignored at the policy level can also be read from one or more files located on the local file system. The file must be formatted as one term per line.

```
{
   "name": "example-policy",
   "ignored": [
     {
       "name": "names to ignore",
       "terms": ["john smith", "jane doe"],
       "files": ["/tmp/names.txt"]
       "caseSensitive": false
     }
   ],   
   "identifiers": {
      "emailAddress": {
         "emailAddressFilterStrategies": [
            {
               "strategy": "REDACT",
               "redactionFormat": "{{{REDACTED-%t}}}"
            }
         ]
      }
   }
}
```

### Ignore List for a Filter

In the policy shown below, an ignore list is set at the level of a filter. The terms specified in the list will be ignored _only_ for that filter type. Each filter in a policy can have its own list of ignored terms. The terms listed will be ignored case-sensitive, meaning, "John" will be ignored if "John" is an ignored term but will not be ignored if "john" is an ignored term.

```
{
   "name": "example-filter-profile",
   "identifiers": {
      "emailAddress": {
         "ignored": ["john smith", "jane doe"],
         "emailAddressFilterStrategies": [
            {
               "strategy": "REDACT",
               "redactionFormat": "{{{REDACTED-%t}}}"
            }
         ]
      }
   }
}
```

## Ignoring Patterns

Phileas can ignore information based on a regular expression pattern. An example use of this feature is to ignore terms that are present in your text but are dynamic, such as logged timestamps. When using the date filter these timestamps may be identified as being sensitive but you do not want them redacted. With an ignore pattern we can ignore the logged timestamps.

## Ignore Patterns

Ignore patterns can be specified at the policy level and/or at the level of each type of filter. When set at the policy level, the list of ignored patterns will be applied to _all_ filter types. When set for an individual filter, the list of ignored patterns will be applied _only_ to that filter.

### Ignore Patterns for a Policy

In the policy shown below, ignore patterns are set at the level of the policy. The patterns specified in the list will be ignored for _all_ filter types enabled in the policy.

```
{
   "name": "example-policy",
   "ignoredPatterns": [
     {
       "name": "ignore-room-numbers",
       "pattern": "Room [A-Z0-4]{4}"
     }
   ],
   "identifiers": {
      "emailAddress": {
         "emailAddressFilterStrategies": [
            {
               "strategy": "REDACT",
               "redactionFormat": "{{{REDACTED-%t}}}"
            }
         ]
      }
   }
}
```

### Ignore Patterns for a Filter

In the policy shown below, ignore patterns are set at the level of a filter. The patterns specified in the list will be ignored _only_ for that filter type. Each filter in a policy can have its own list of ignored patterns.

```
{
   "name": "example-policy",
   "identifiers": {
      "emailAddress": {
         "ignoredPatterns": [
           {
             "name": "ignore-room-numbers",
             "pattern": "Room [A-Z0-4]{4}"
           }
         ],
         "emailAddressFilterStrategies": [
            {
               "strategy": "REDACT",
               "redactionFormat": "{{{REDACTED-%t}}}"
            }
         ]
      }
   }
}
```
