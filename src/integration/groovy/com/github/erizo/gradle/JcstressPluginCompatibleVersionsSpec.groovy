package com.github.erizo.gradle

import org.apache.commons.io.FileUtils
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import spock.lang.Requires
import spock.lang.Specification
import spock.lang.TempDir
import spock.lang.Unroll

import java.nio.file.Paths

/**
 * Covers the oldest supported Gradle release, and the newest release of every supported Gradle line.
 */
class JcstressPluginCompatibleVersionsSpec extends Specification {

    @TempDir
    File testProjectDir

    @Requires({ !jvm.java20Compatible }) // Java20 support was added in Gradle 8.3
    def "should run with 8.0.1"() {
        given:
        copySanityProject()

        when:
        def result = runGradleTask('8.0.1', 'jcstress')

        then:
        verifyJcstressRan(result)
    }

    @Unroll
    def "should run with #gradleVersion"() {
        given:
        copySanityProject()

        when:
        def result = runGradleTask(gradleVersion, 'jcstress')

        then:
        verifyJcstressRan(result)

        where:
        gradleVersion << ['8.14.5', '9.0.0', '9.7.1']
    }

    private static void verifyJcstressRan(BuildResult result) {
        assert result.task(":jcstress").outcome == TaskOutcome.SUCCESS
        assert result.output.find('FATAL: (.*)') == null
        assert result.output.find('RUN RESULTS') == 'RUN RESULTS'
    }

    private void copySanityProject() {
        def jcstressProjectRoot = Paths.get(getClass().classLoader.getResource("simple-application-sanity").toURI()).toFile()
        FileUtils.copyDirectory(jcstressProjectRoot, testProjectDir, false)
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
                .withPluginClasspath()
                .build()
    }

}
