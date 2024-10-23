# Settings

Phileas has settings to control how it operates. The settings and how to configure each are described below.

> The configuration for the types of sensitive information that Phileas identifies are defined in [filter policies](filter_policies/filter_policies.md) outside of Phileas' configuration properties described on this page.

## Configuring Phileas

### The Phileas Settings File

Phileas looks for its settings in an `application.properties` file.

### Using Environment Variables

Properties set via environment variables take precedence over properties set in Phileas' settings file.

All following properties can also be set as environment variables by prepending `PHILTER_` to the property name and changing periods to underscores. For example, the property `filter.profiles.directory` can be set using the environment variable `PHILTER_FILTER_PROFILES_DIRECTORY` by:

```
export PHILTER_FILTER_PROFILES_DIRECTORY=/profiles/
```

Using environment variables to configure Phileas instead of using Phileas' settings file can allow for easier configuration management when deploying Phileas.

## Policies

| Setting                     | Description                                  | Allowed Values            | Default Value |
| --------------------------- | -------------------------------------------- | ------------------------- | ------------- |
| `filter.policies.directory` | The directory in which to look for policies. | Any valid directory path. | `./policies/` |

## Span Disambiguation

These values configure Phileas' span disambiguation feature to determine the most appropriate type of sensitive information when duplicate spans are identified. In a deployment of multiple Phileas instances, you must enable the [cache service](Settings#cache) for span disambiguation to work as expected.

|                               | Description                                   | Allowed Values  | Default Value |
| ----------------------------- | --------------------------------------------- | --------------- | ------------- |
| `span.disambiguation.enabled` | Whether or not to enable span disambiguation. | `true`, `false` | `false`       |

## Cache Service

The cache service is required to use [consistent anonymization](anonymization.md) and policies stored in Amazon S3. Phileas supports Redis as the backend cache. When Redis is not used, an in-memory cache is used instead. The in-memory cache is not recommended because all contents will be stored in memory on the local Phileas instance.

The cache will contain sensitive information. It is important that you take the necessary precautions to secure the cache itself and all communication between Phileas and the cache.

| Setting                  | Description                                                       | Allowed Values            | Default Value |
| ------------------------ | ----------------------------------------------------------------- | ------------------------- | ------------- |
| `cache.redis.enabled`    | Whether or not to use Redis as the cache.                         | `true`, `false`           | `false`       |
| `cache.redis.host`       | The hostname or IP address of the Redis cache.                    | Any valid Redis endpoint. | None          |
| `cache.redis.port`       | The Redis cache port.                                             | Any valid port.           | `6379`        |
| `cache.redis.auth.token` | The Redis auth token.                                             | Any valid token.          | None          |
| `cache.redis.ssl`        | Whether or not to use SSL for communication with the Redis cache. | `true`, `false`           | `false`       |

The following Redis settings are only required when using a self-signed SSL certificate.

| Setting                           | Description                  | Allowed Values       | Default Value |
| --------------------------------- | ---------------------------- | -------------------- | ------------- |
| `cache.redis.truststore`          | The path to the trust store. | Any valid file path. | None          |
| `cache.redis.truststore.password` | The trust store password.    | Any valid file path. | None          |
| `cache.redis.keystore`            | The path to the keystore.    | Any valid file path. | None          |
| `cache.redis.keystore.password`   | The keystore password.       | Any valid file path. | None          |

## Advanced Settings

> In most cases the settings below do not need changed. Contact us for more information on any of these settings.

| Setting                      | Description                                                                                                                                  | Allowed Values    | Default Value |
| ---------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------- | ----------------- | ------------- |
| `ner.timeout.sec`            | Controls the timeout in seconds when performing name entity recognition. Longer text may require longer processing times.                    | An integer value  | `600`         |
| `ner.max.idle.connections`   | The maximum number of idle connections to maintain for the named entity recognition. More connections may improve performance in some cases. | An integer value. | `30`          |
| `ner.keep.alive.duration.ms` | The amount of time in milliseconds to keep named entity recognition connections alive. Longer text may require longer processing times.      | An integer value. | `60`          |
