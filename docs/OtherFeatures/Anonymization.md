# Consistent Anonymization

Anonymization in the context of Phileas is the process of replacing certain values with random but similar values. For example, the identified name of “John Smith” may be replaced with “David Jones”, or an identified phone number of 123-555-9358 may be replaced by 842-436-2042. A [VIN](vins.md) number will be replaced by a 17 character randomly selected VIN number that adheres to the standard for VIN numbers.

Anonymization is useful in instances where you want to remove sensitive information from text without changing the meaning of the text. Anonymization can be enabled for each type of sensitive information in the policy by setting the filter strategy to `RANDOM_REPLACE`. (See [Policies](policies_README.md) for more information.)

## Consistent Anonymization

Consistent anonymization refers to the process of always anonymizing the same sensitive information with the same replacement values. For example, if the name "John Smith" is randomly replaced with "Pete Baker", all other occurrences of "John Smith" will also be replaced by "Pete Baker."

Consistent anonymization can be done on the document level or on the context level. When enabled on the document level, "John Smith" will only be replaced by "Pete Baker" in the same document. If "John Smith" occurs in a separate document it will be anonymized with a different random name. When enabled on the context level, "John Smith" will be replaced by "Pete Baker" whenever "John Smith" is found in all documents in the same context.

Enabling consistent anonymization on the context level requires a cache to store the sensitive information and the corresponding replacement values. If a single instance of Phileas is running, its internal cache service (enabled by default) is the best choice and no additional configuration is required.

If multiple instances of Phileas are deployed together, Phileas requires access to a Redis cache service as shown below. See Phileas' [Settings](settings.md) on how to configure the cache.

**When Phileas is deployed in a cluster, a Redis cache is required to enable consistent anonymization.**

The anonymization cache will contain PHI. It is important that you take the necessary precautions to secure the cache and all communication to and from the cache.
