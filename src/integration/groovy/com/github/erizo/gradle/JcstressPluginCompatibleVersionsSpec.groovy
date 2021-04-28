package com.github.erizo.gradle

import org.apache.commons.io.FileUtils
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import spock.lang.Specification

import java.nio.file.Path
import java.nio.file.Paths

class JcstressPluginCompatibleVersionsSpec extends Specification {

    @Rule
    MyTemporaryFolder testProjectDir = new MyTemporaryFolder()

    def pluginClasspath

    def setup() {
        pluginClasspath = getClass().classLoader.findResource('plugin-classpath.txt').readLines().collect {
            new File(it)
        }
    }

    def "should run with 6.6.1"() {
        given:
        def jcstressProjectRoot = Paths.get(getClass().classLoader.getResource("simple-application-unforked").toURI()).toFile()
        FileUtils.copyDirectory(jcstressProjectRoot, testProjectDir.root, false)

        when:
        def result = runGradleTask('6.6.1', 'jcstress')

        then:
        result.task(":jcstress").outcome == TaskOutcome.SUCCESS
    }

    def "should run with 5.6.4"() {
        given:
        def jcstressProjectRoot = Paths.get(getClass().classLoader.getResource("simple-application-unforked").toURI()).toFile()
        FileUtils.copyDirectory(jcstressProjectRoot, testProjectDir.root, false)

        when:
        def result = runGradleTask('5.6.4', 'jcstress')

        then:
        result.task(":jcstress").outcome == TaskOutcome.SUCCESS
    }

    private BuildResult runGradleTask(String gradleVersion, String... taskNames) {
        def arguments = new ArrayList()
        arguments.addAll(taskNames)
        arguments.addAll(['-i', '--stacktrace', '--refresh-dependencies'])

        GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withGradleVersion(gradleVersion)
                .forwardStdOutput(System.out.newPrintWriter())
                .forwardStdError(System.err.newPrintWriter())
                .withArguments(arguments)
                .withPluginClasspath(pluginClasspath)
                .build()
    }

}
