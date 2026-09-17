package com.github.erizo.gradle

import org.apache.commons.io.FileUtils
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import spock.lang.Requires
import spock.lang.Specification
import spock.lang.TempDir

import java.nio.file.Paths

@Requires({ jvm.java17Compatible }) // Gradle 9 requires Java 17
class JcstressPluginGradle9Spec extends Specification {

    private static final String GRADLE_VERSION = '9.7.1'

    @TempDir
    File testProjectDir

    def "should complete a forked run"() {
        given:
        copySampleProject("simple-application-forked")

        when:
        def result = runGradleTask('jcstress')
        def errorMessage = result.output.find('FATAL: (.*)')
        def runResults = result.output.find('RUN RESULTS')

        then:
        verifyAll {
            result.task(":jcstress").outcome == TaskOutcome.SUCCESS
            errorMessage == null
            runResults == 'RUN RESULTS'
        }
    }

    def "should include a test class in a simple run"() {
        given:
        copySampleProject("simple-application-include-tests")

        when:
        def result = runGradleTask('jcstress')
        def errorMessage = result.output.find('FATAL: (.*)')
        def runResults = result.output.find('RUN RESULTS')

        then:
        verifyAll {
            result.task(":jcstress").outcome == TaskOutcome.SUCCESS
            errorMessage == null
            runResults == 'RUN RESULTS'
        }
    }

    def "should produce two separate jars"() {
        given:
        copySampleProject("simple-application-twojar")

        when:
        def result = runGradleTask('jar', 'jcstressJar')

        then:
        verifyAll {
            result.task(":jar").outcome == TaskOutcome.SUCCESS
            result.task(":jcstressJar").outcome == TaskOutcome.SUCCESS
            new File(testProjectDir, "build/libs/simple-application-twojar.jar").exists()
            new File(testProjectDir, "build/libs/simple-application-twojar-jcstress.jar").exists()
        }
    }

    def "should create start scripts"() {
        given:
        copySampleProject("simple-application-sanity")

        when:
        def result = runGradleTask('jcstressScripts')

        then:
        verifyAll {
            result.task(":jcstressScripts").outcome == TaskOutcome.SUCCESS
            getFileContents("build", "scripts", "jcstress-test-simple-jcstress").contains("jcstress-core-${JcstressPluginExtension.JCSTRESS_DEFAULT_VERSION}.jar")
            getFileContents("build", "scripts", "jcstress-test-simple-jcstress.bat").contains("jcstress-core-${JcstressPluginExtension.JCSTRESS_DEFAULT_VERSION}.jar")
        }
    }

    def "should run with the configuration cache enabled"() {
        given:
        copySampleProject("simple-application-sanity")

        when:
        // --refresh-dependencies would invalidate the configuration cache, so it is left out here
        def firstRun = runGradle('jcstress', '--configuration-cache', '-i', '--stacktrace')
        def secondRun = runGradle('jcstress', '--configuration-cache', '-i', '--stacktrace')

        then:
        verifyAll {
            firstRun.task(":jcstress").outcome == TaskOutcome.SUCCESS
            firstRun.output.contains('Configuration cache entry stored')
            !firstRun.output.contains('configuration cache problem')
            secondRun.output.contains('Reusing configuration cache')
            secondRun.task(":jcstress").outcome in [TaskOutcome.SUCCESS, TaskOutcome.UP_TO_DATE]
        }
    }

    private void copySampleProject(String sampleName) {
        def jcstressProjectRoot = Paths.get(getClass().classLoader.getResource(sampleName).toURI()).toFile()
        FileUtils.copyDirectory(jcstressProjectRoot, testProjectDir, false)
    }

    private String getFileContents(String... pathElements) {
        Paths.get(testProjectDir.toString(), pathElements).text
    }

    private BuildResult runGradleTask(String... taskNames) {
        def arguments = new ArrayList()
        arguments.addAll(taskNames)
        arguments.addAll(['-i', '--stacktrace', '--refresh-dependencies'])

        return runGradle(arguments as String[])
    }

    private BuildResult runGradle(String... arguments) {
        GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withGradleVersion(GRADLE_VERSION)
                .forwardStdOutput(System.out.newPrintWriter())
                .forwardStdError(System.err.newPrintWriter())
                .withArguments(arguments)
                .withPluginClasspath()
                .build()
    }

}
