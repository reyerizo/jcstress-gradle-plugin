# Jcstress Gradle Plugin

[![Maven Status](https://maven-badges.herokuapp.com/maven-central/io.github.reyerizo.gradle/jcstress-gradle-plugin/badge.svg?style=flat)](https://mvnrepository.com/artifact/io.github.reyerizo.gradle/jcstress-gradle-plugin)
[![Build Status](https://github.com/reyerizo/jcstress-gradle-plugin/actions/workflows/gradle.yml/badge.svg)](https://github.com/reyerizo/jcstress-gradle-plugin/actions/workflows/gradle.yml)
[![Coverage Status](https://coveralls.io/repos/github/reyerizo/jcstress-gradle-plugin/badge.svg?branch=master)](https://coveralls.io/github/reyerizo/jcstress-gradle-plugin?branch=master)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

This plugin integrates [The Java Concurrency Stress tests](http://openjdk.java.net/projects/code-tools/jcstress) with Gradle.

### Usage

Add the following to your `build.gradle`:

_build.gradle:_
```groovy
plugins {
    id "io.github.reyerizo.gradle.jcstress" version "0.8.13"
}
```
### Tasks

Execute your tests with the following:

```
gradle jcstress
```

or a subset of your tests:

```
gradle jcstress --tests "MyFirstTest|MySecondTest"
```

The latter is an equivalent of `regexp` option below.

### Configuration

If you need to customize the configuration, add a block like the following to configure the plugin:

```
jcstress {
    verbose = true
    timeMillis = "200"
    spinStyle = "THREAD_YIELD"
}
```

These are all possible configuration options:

| Name               | Description                                                                                                                                                                                                              |
|--------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `affinityMode`   | Use the specific affinity mode, if available. `NONE` = No affinity whatsoever; `GLOBAL` = Affnity for the entire JVM; `LOCAL` = Affinity for the individual actors.                                                      |
| `cpuCount`       | Number of CPUs to use. Defaults to all CPUs in the system. Reducing the number of CPUs limits the amount of resources (including memory) the run is using.                                                               |
| `heapPerFork`    | Java heap size per fork, in megabytes. This affects the stride size: maximum footprint will never be exceeded, regardless of min/max stride sizes.                                                                       |
| `forkMultiplier` | "Fork multiplier for randomized/stress tests. This allows more efficient randomized testing, as each fork would use a different seed."                                                                                   |
| `forks`          | Should fork each test N times. `0` to run in the embedded mode with occasional forking, `-1` to never ever fork.                                                                                                         |
| `iterations`     | Iterations per test.                                                                                                                                                                                                     |
| `jvmArgs`        | Use given JVM arguments. This disables JVM flags auto-detection, and runs only the single JVM mode. Either a single space-separated option line, or multiple options are accepted. This option only affects forked runs. |
| `jvmArgsPrepend` | Prepend given JVM arguments to auto-detected configurations. This option only affects forked runs."                                                                                                                      |
| `mode`           | Test mode preset: `sanity`, `quick`, `default`, `tough`, `stress`.                                                                                                                                                       |
| `regexp`         | Regexp selector for tests.                                                                                                                                                                                               |
| `reportDir`      | Target destination to put the report into.                                                                                                                                                                               |
| `spinStyle`      | Busy loop wait style. `HARD` = hard busy loop; `THREAD_YIELD` = use `Thread.yield()`; `THREAD_SPIN_WAIT` = use `Thread.onSpinWait()`; `LOCKSUPPORT_PARK_NANOS` = use `LockSupport.parkNanos()`.                          |
| `splitPerActor`  | Use split per-actor compilation mode, if available.                                                                                                                                                                      |
| `strideSize`     | Internal stride size. Larger value decreases the synchronization overhead, but also reduces the number of collisions.                                                                                                    |
| `strideCount`    | Internal stride count per epoch. Larger value increases cache footprint.                                                                                                                                                 |
| `timeMillis`     | Time to spend in single test iteration. Larger value improves test reliability, since schedulers do better job in the long run.                                                                                          |
| `verbose`        | Be extra verbose.                                                                                                                                                                                                        |


More options are available, but you probably won't need them:

| Name | Description |
| --- | --- |
| `language` | format numbers according to the given locale, eg `en`, `fr`, etc. Will default to `en`. If unsure, just leave as is) |

Options deprecated - will be removed completely in the next version. In current version, they don't have any effect.

| Name          | Description                                                                                                                                                                |
|---------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `concurrency` | Number of CPUs to use. Defaults to all CPUs in the system. Reducing the number of CPUs limits the amount of resources (including memory) the run is using.                 |
| `deoptratio`  | Java heap size per fork, in megabytes. This affects the stride size: maximum footprint will never be exceeded, regardless of min/max stride sizes.                         |
| `maxStride`   | Should fork each test N times. `0` to run in the embedded mode with occasional forking, `-1` to never ever fork.                                                           |
| `minStride`   | Iterations per test.                                                                                                                                                       |

The plugin uses a separate location for `jcstress` files:

```
src/jcstress/java       // java sources
src/jcstress/resources  // resources
```

By default, the plugin uses `jcstress-core-0.15`. This can be easily changed with the following:

```groovy
jcstress {
    jcstressDependency 'org.openjdk.jcstress:jcstress-core:0.x'
}
```

### Notes

- This plugin is heavily based on [jmh-gradle-plugin](https://github.com/melix/jmh-gradle-plugin) and should behave in a similar way.
- At the moment, jcstress-0.9 and newer are not supported.
