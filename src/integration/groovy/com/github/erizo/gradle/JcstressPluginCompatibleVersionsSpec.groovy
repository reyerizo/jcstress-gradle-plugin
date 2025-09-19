package com.github.erizo.gradle

import org.apache.commons.io.FileUtils
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import spock.lang.Requires
import spock.lang.Specification
import spock.lang.TempDir

import java.nio.file.Paths

class JcstressPluginCompatibleVersionsSpec extends Specification {

    @TempDir
    File testProjectDir

    def pluginClasspath

    def setup() {
        pluginClasspath = getClass().classLoader.findResource('plugin-classpath.txt').readLines().collect {
            new File(it)
        }
    }

    @Requires({ !jvm.java17Compatible }) // Java17 support was added in Gradle 7.3
    def "should run with 7.0"() {
        given:
        def jcstressProjectRoot = Paths.get(getClass().classLoader.getResource("simple-application-sanity").toURI()).toFile()
        FileUtils.copyDirectory(jcstressProjectRoot, testProjectDir, false)

        when:
        def result = runGradleTask('7.0', 'jcstress')
        def errorMessage = result.output.find('FATAL: (.*)')
        def runResults = result.output.find('RUN RESULTS')

        then:
        verifyAll {
            result.task(":jcstress").outcome == TaskOutcome.SUCCESS
            errorMessage == null
            runResults == 'RUN RESULTS'
        }

    }

    @Requires({ !jvm.java16Compatible }) // Java16 support was added in Gradle 7
    def "should run with 6.6.1"() {
        given:
        def jcstressProjectRoot = Paths.get(getClass().classLoader.getResource("simple-application-sanity").toURI()).toFile()
        FileUtils.copyDirectory(jcstressProjectRoot, testProjectDir, false)

        when:
        def result = runGradleTask('6.6.1', 'jcstress')
        def errorMessage = result.output.find('FATAL: (.*)')
        def runResults = result.output.find('RUN RESULTS')

        then:
        verifyAll {
            result.task(":jcstress").outcome == TaskOutcome.SUCCESS
            errorMessage == null
            runResults == 'RUN RESULTS'
        }
    }

    @Requires({ !jvm.java19Compatible }) // Java19 support was added in Gradle 7.6
    def "should run with 7.5.1"() {
        given:
        def jcstressProjectRoot = Paths.get(getClass().classLoader.getResource("simple-application-sanity").toURI()).toFile()
        FileUtils.copyDirectory(jcstressProjectRoot, testProjectDir, false)

        when:
        def result = runGradleTask('7.5.1', 'jcstress')
        def errorMessage = result.output.find('FATAL: (.*)')
        def runResults = result.output.find('RUN RESULTS')

        then:
        verifyAll {
            result.task(":jcstress").outcome == TaskOutcome.SUCCESS
            errorMessage == null
            runResults == 'RUN RESULTS'
        }
    }

    @Requires({ !jvm.java20Compatible }) // Java20 support was added in Gradle 8.3
    def "should run with 8.0.1"() {
        given:
        def jcstressProjectRoot = Paths.get(getClass().classLoader.getResource("simple-application-sanity").toURI()).toFile()
        FileUtils.copyDirectory(jcstressProjectRoot, testProjectDir, false)

        when:
        def result = runGradleTask('8.0.1', 'jcstress')
        def errorMessage = result.output.find('FATAL: (.*)')
        def runResults = result.output.find('RUN RESULTS')

        then:
        verifyAll {
            result.task(":jcstress").outcome == TaskOutcome.SUCCESS
            errorMessage == null
            runResults == 'RUN RESULTS'
        }
    }

    @Requires({ jvm.java17Compatible })
    def "should run with 9.0.0"() {
        given:
        def jcstressProjectRoot = Paths.get(getClass().classLoader.getResource("simple-application-sanity").toURI()).toFile()
        FileUtils.copyDirectory(jcstressProjectRoot, testProjectDir, false)

        when:
        def result = runGradleTask('9.0.0', 'jcstress')
        def errorMessage = result.output.find('FATAL: (.*)')
        def runResults = result.output.find('RUN RESULTS')

        then:
        verifyAll {
            result.task(":jcstress").outcome == TaskOutcome.SUCCESS
            errorMessage == null
            runResults == 'RUN RESULTS'
        }
    }

    @Requires({ !jvm.java18Compatible }) // Java18 support was added in Gradle 7.5
    def "should run with 7.3.2"() {
        given:
        def jcstressProjectRoot = Paths.get(getClass().classLoader.getResource("simple-application-sanity").toURI()).toFile()
        FileUtils.copyDirectory(jcstressProjectRoot, testProjectDir, false)

        when:
        def result = runGradleTask('7.3.2', 'jcstress')
        def errorMessage = result.output.find('FATAL: (.*)')
        def runResults = result.output.find('RUN RESULTS')

        then:
        verifyAll {
            result.task(":jcstress").outcome == TaskOutcome.SUCCESS
            errorMessage == null
            runResults == 'RUN RESULTS'
        }
    }

    @Requires({ !jvm.java13Compatible }) // Java13 support was added in Gradle 6
    def "should run with 5.6.4"() {
        given:
        def jcstressProjectRoot = Paths.get(getClass().classLoader.getResource("simple-application-sanity").toURI()).toFile()
        FileUtils.copyDirectory(jcstressProjectRoot, testProjectDir, false)

        when:
        def result = runGradleTask('5.6.4', 'jcstress')
        def errorMessage = result.output.find('FATAL: (.*)')
        def runResults = result.output.find('RUN RESULTS')

        then:
        verifyAll {
            result.task(":jcstress").outcome == TaskOutcome.SUCCESS
            errorMessage == null
            runResults == 'RUN RESULTS'
        }
    }

    private BuildResult runGradleTask(String gradleVersion, String... taskNames) {
        def arguments = new ArrayList()
        arguments.addAll(taskNames)
        arguments.addAll(['-i', '--stacktrace', '--refresh-dependencies'])

        GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withGradleVersion(gradleVersion)
                .forwardStdOutput(System.out.newPrintWriter())
                .forwardStdError(System.err.newPrintWriter())
                .withArguments(arguments)
                .withPluginClasspath(pluginClasspath)
                .build()
    }

}
