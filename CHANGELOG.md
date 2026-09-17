# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Removed

- **Breaking:** the deprecated `concurrency`, `deoptRatio`, `maxStride` and `minStride` options.
  They have had no effect for several releases and the README has advertised their removal since.
  Use `cpuCount` in place of `concurrency`, and `strideSize` / `strideCount` in place of the stride
  options. `deoptRatio` was dropped by jcstress itself and has no replacement.

### Fixed

- `jcstressInstall` failed on every platform. The task set the executable bit with
  `PosixFilePermissions.fromString("ugo+x")`, which is not the format that method accepts, so it
  threw `IllegalArgumentException: Invalid mode` before it could do anything. Permissions now come
  from the distribution spec, which Gradle applies portably.
- `jcstressInstall` refused to run on a clean build. It rejected the destination directory as "neither
  empty nor an installation", because Gradle creates a task's output directory before the task runs.
- The installed distribution was missing jcstress itself. `lib/` was populated from the bare `jcstress`
  configuration, which holds no dependencies, while the start scripts were generated against
  `jcstressRuntimeClasspath`. The generated script pointed at a jar that was never copied.

### Changed

- The Java 8 target is enforced with `options.release` rather than `sourceCompatibility` alone, so
  using an API newer than Java 8 now fails at compile time instead of at runtime on Gradle 8.
- CI runs on Windows as well as Linux, caches Gradle, and cancels superseded runs.
- Coverage reporting includes the integration tests.

## [1.0.0] - 2026-09-17

### Added

- Gradle 9 support, verified up to Gradle 9.7.1.
- Configuration cache support for the `jcstress` task.

### Changed

- **Breaking:** Gradle 7 and older are no longer supported. The minimum is Gradle 8.0.
- The default jcstress version is 0.16.

### Fixed

- The plugin could not be applied on Gradle older than 8.3. A reference to `ConfigurableFilePermissions`
  leaked into a `JcstressPlugin` method signature, so plugin instantiation failed with
  `Could not generate a decorated class for type JcstressPlugin`.

[Unreleased]: https://github.com/reyerizo/jcstress-gradle-plugin/compare/1.0.0...HEAD
[1.0.0]: https://github.com/reyerizo/jcstress-gradle-plugin/releases/tag/1.0.0
