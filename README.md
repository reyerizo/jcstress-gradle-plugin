# Jcstress Gradle Plugin

<nobr>[![Build Status](https://travis-ci.com/jerzykrlk/jcstress-gradle-plugin.svg?branch=master)](https://travis-ci.com/github/jerzykrlk/jcstress-gradle-plugin)
[![Coverage Status](https://coveralls.io/repos/github/jerzykrlk/jcstress-gradle-plugin/badge.svg?branch=master)](https://coveralls.io/github/jerzykrlk/jcstress-gradle-plugin?branch=master)
[![Download](https://api.bintray.com/packages/jerzykrlk/maven/jcstress-gradle-plugin/images/download.svg) ](https://bintray.com/jerzykrlk/maven/jcstress-gradle-plugin/_latestVersion)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)</nobr>

This plugin integrates [The Java Concurrency Stress tests](http://openjdk.java.net/projects/code-tools/jcstress) with Gradle.

### Usage

Add the following to your `build.gradle`:

_build.gradle:_
```groovy

buildscript {
    repositories {
        jcenter()
    }

    dependencies {
        classpath 'com.github.erizo.gradle:jcstress-gradle-plugin:0.8.8'
    }
}

apply plugin: 'jcstress'

repositories {
    jcenter()
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
    yield = true
}
```

These are all possible configuration options:

| Name | Description |
| --- | --- |
| `concurrency` | Concurrency level for tests. This value can be greater than number of CPUs available. |
| `deoptRatio` | De-optimize (roughly) every N-th iteration. Larger value improves test performance, but decreases the chance we hit unlucky compilation. |
| `forks` | Should fork each test N times. `0` to run in the embedded mode with occasional forking, `-1` to never ever fork. |
| `iterations`   | Iterations per test. |
| `jvmArgs`   | Append these JVM arguments for the forked runs. |
| `mode`   | Test mode preset: `sanity`, `quick`, `default`, `tough`, `stress`. |
| `maxStride`   | Maximum internal stride size. Larger value decreases the synchronization overhead, but also reduces accuracy. |
| `minStride`   | Minimum internal stride size. Larger value decreases the synchronization overhead, but also reduces accuracy. |
| `reportDir`   | Target destination to put the report into. |
| `cpuCount`   | Number of CPUs in the system. Setting this value overrides the autodetection. |
| `regexp`   | Regexp selector for tests. |
| `timeMillis`   | Time to spend in single test iteration. Larger value improves test reliability, since schedulers do better job in the long run. |
| `verbose`   | Be extra verbose. |
| `yield`   | Call `Thread.yield()` in busy loops. |

More options are available, but you probably won't need them:

| Name | Description |
| --- | --- |
| `language` | format numbers according to the given locale, eg `en`, `fr`, etc. Will default to `en`. If unsure, just leave as is) |


The plugin uses a separate location for `jcstress` files:

```
src/jcstress/java       // java sources
src/jcstress/resources  // resources
```

By default, the plugin uses `jcstress-core-0.7`. This can be easily changed with the following:

```groovy
jcstress {
    jcstressDependency 'org.openjdk.jcstress:jcstress-core:0.x'
}
```

### Notes

This plugin is heavily based on [jmh-gradle-plugin](https://github.com/melix/jmh-gradle-plugin) and should behave in a similar way.
