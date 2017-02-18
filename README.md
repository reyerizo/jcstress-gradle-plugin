# Jcstress Gradle Plugin
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
        classpath 'com.github.erizo.gradle:jcstress-gradle-plugin:0.7.5'
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


The plugin uses a separate location for `jcstress` files:

```
src/jcstress/java       // java sources
src/jcstress/resources  // resources
```

By default, the plugin uses `jcstress-core-0.2`. This can be easily overridden with the following:

```groovy
jcstress {
    jcstressDependency 'org.openjdk.jcstress:jcstress-core:0.3'
}
```

### Notes

This plugin is heavily based on [jmh-gradle-plugin](https://github.com/melix/jmh-gradle-plugin) and should behave in a similar way.