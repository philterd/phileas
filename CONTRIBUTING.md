# Contributing to Phileas

## Code of Conduct 

In the interest of fostering an open and welcoming environment, we as contributors and maintainers pledge to making participation in our project and our community a harassment-free experience for everyone, regardless of age, body size, disability, ethnicity, gender identity and expression, level of experience, nationality, personal appearance, race, religion, or sexual identity and orientation.

Please read and understand the [Code of Conduct](https://github.com/philterd/phileas/blob/main/CODE_OF_CONDUCT.md).

## GitHub Workflow

We prefer to take contributions as GitHub pull requests. This workflow allows you to create your own copy of Phileas, try out some changes, and then share your changes back to the community, with proper review and feedback from other Phileas contributors.

1. Create a fork of philterd/phileas
2. Create a feature branch
3. Build and test local changes
4. Commit changes to your feature branch
5. Open a pull request
6. Participate in code review
7. Celebrate your accomplishment

## Building and Testing Changes

### Required Tools

* Java 17+
* maven
* redis-server

### Building on Linux

Ubuntu is our daily driver, but any Linux distribution should work.

### Building on MacOS

In System Settings | Privacy & Security | Developer Tools, enable `Terminal` and `IntelliJ IDEA` (and other IDEs where you run tests).
Otherwise you'll get errors where `redis-server` fails to start when running tests.