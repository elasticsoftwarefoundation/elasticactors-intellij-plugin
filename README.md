# Elastic Actors IntelliJ IDEA Plugin

Elastic Actors framework support for IntelliJ IDEA.

[![CI](https://github.com/elasticsoftwarefoundation/elasticactors-intellij-plugin/actions/workflows/gradle.yml/badge.svg)](https://github.com/elasticsoftwarefoundation/elasticactors-intellij-plugin/actions/workflows/gradle.yml)
[![License: Apache 2](https://img.shields.io/badge/LICENSE-Apache2-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0.txt)

### Features

* Method signature verfication for Message Handler methods
* Detect suspicious types for messages being sent by Actors
* Detect potential issues with mutable and immutable Message classes
* Find usages of classes in the context of Actor Message handling
  * Usages of a class in Message Handler methods
  * Usages of a class inside the Actor's `onReceive` method

### Release process

This project uses the Gradle Release Plugin and GitHub Actions to create releases.\
Just run `./gradlew release` to select the version to be released and create a
VCS tag.

GitHub Actions will start [the build process](https://github.com/elasticsoftwarefoundation/elasticactors-intellij-plugin/actions/workflows/gradle-publish.yml).

If successful, the build will be automatically published to [Jetbrains Marketplace](https://plugins.jetbrains.com/plugin/13814-elastic-actors).
