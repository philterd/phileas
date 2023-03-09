# `owner`

This module is a fork of the [owner](https://github.com/matteobaccan/owner) project that provides an easy to use API for Java property files.

The project was forked here and not used as a dependency because it adds the `PhileasSystemLoader` class. This class allows for specifying `system:properties` and `system:env` with the `@Sources` annotation. 
It looks for environment variables whose names start with `philter_`. Note that underscores are replaced for periods because periods are not allowed in environment variable names.

This allows properties to be set either via a properties file or via environment variables. Note that properties set by environment variables take precedence over properties set in a properties file.

For more context, see this [comment and thread](https://github.com/matteobaccan/owner/issues/267#issuecomment-642194768) on GitHub.