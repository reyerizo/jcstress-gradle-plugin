package com.github.erizo.gradle

import org.apache.commons.io.FileUtils
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import spock.lang.Requires
import spock.lang.Specification
import spock.lang.TempDir

import java.nio.file.Path
import java.nio.file.Paths

/**
 * Runs the sample projects against the oldest supported Gradle release, where breakage is most likely.
 */
@Requires({ !jvm.java20Compatible }) // Java20 support was added in Gradle 8.3
class JcstressPluginIntegrationCasesSpec extends Specification {

    private static final String GRADLE_VERSION = '8.0.1'

    @TempDir
    File testProjectDir

    def "should produce two separate jars"() {
        given:
        copySampleProject("simple-application-twojar")

        when:
        runGradleTask('jar', 'jcstressJar')
        def fileNames = Path.of(testProjectDir.toString(), 'build', 'libs').toFile().list()

        then:
        fileNames.sort() == ['simple-application-twojar.jar', 'simple-application-twojar-jcstress.jar'].sort()
    }

    def "should not throw a null pointer on a new gradle and apisample"() {
        given:
        copySampleProject("simple-application-new-gradle-apisample")

        when:
        def result = runGradleTask('jcstress')

        then:
        verifyJcstressRan(result)
    }

    def "should not throw a null pointer on a new gradle, apisample and Kotlin"() {
        given:
        copySampleProject("simple-application-new-gradle-apisample-kt")

        when:
        def result = runGradleTask('jcstress')

        then:
        verifyJcstressRan(result)
    }

    def "should run with a java 17 toolchain"() {
        given:
        copySampleProject("simple-application-sanity-java-17")

        when:
        def result = runGradleTask('jcstress')

        then:
        verifyJcstressRan(result)
    }

    private static void verifyJcstressRan(BuildResult result) {
        assert result.task(":jcstress").outcome == TaskOutcome.SUCCESS
        assert result.output.find('FATAL: (.*)') == null
        assert result.output.find('RUN RESULTS') == 'RUN RESULTS'
    }

    private void copySampleProject(String sampleName) {
        def jcstressProjectRoot = Paths.get(getClass().classLoader.getResource(sampleName).toURI()).toFile()
        FileUtils.copyDirectory(jcstressProjectRoot, testProjectDir, false)
    }

    private BuildResult runGradleTask(String... taskNames) {
        def arguments = new ArrayList()
        arguments.addAll(taskNames)
        arguments.addAll(['-i', '--stacktrace', '--refresh-dependencies'])

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
