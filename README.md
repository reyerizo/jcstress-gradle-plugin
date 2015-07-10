# Jcstress Gradle Plugin
This plugin integrates [The Java Concurrency Stress tests](http://openjdk.java.net/projects/code-tools/jcstress) with Gradle.

### Usage

Add the following to your `build.gradle`:

_build.gradle:_
```java

buildscript {
    repositories {
        jcenter()
    }

    dependencies {
        classpath 'com.github.erizo.gradle:jcstress-gradle-plugin:0.1'
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

### Configuration

The plugin uses a separate location for `jcstress` files:

```
src/jcstress/java       // java sources
src/jcstress/resources  // resources
```

By default, the plugin uses a snapshot build of `jcstress` - this can be easily overridden with another custom build:

```java
jcstress {
    jcstressDependency 'org.openjdk.jcstress:jcstress-core:1.0-SNAPSHOT'
}
```

Once `jcstress` is available from Maven Central or JCenter, this plugin will switch to an official binary.

### Notes

This plugin is heavily based on [jmh-gradle-plugin](https://github.com/melix/jmh-gradle-plugin) and should behave in a similar way.