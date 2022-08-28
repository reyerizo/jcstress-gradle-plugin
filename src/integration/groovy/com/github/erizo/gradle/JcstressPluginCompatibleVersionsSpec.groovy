package com.github.erizo.gradle

import org.apache.commons.io.FileUtils
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
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
