package com.github.erizo.gradle

import org.apache.commons.io.FileUtils
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import spock.lang.Specification
import spock.lang.TempDir

import java.nio.file.Paths

class JcstressPluginJava17Spec extends Specification {

    @TempDir
    File testProjectDir

    def pluginClasspath

    def setup() {
        pluginClasspath = getClass().classLoader.findResource('plugin-classpath.txt').readLines().collect {
            new File(it)
        }
    }

    def "should run with 7.5.1 and java 17"() {
        given:
        def jcstressProjectRoot = Paths.get(getClass().classLoader.getResource("simple-application-unforked-java-17").toURI()).toFile()
        FileUtils.copyDirectory(jcstressProjectRoot, testProjectDir, false)

        when:
        def result = runGradleTask('7.5.1', 'jcstress')

        then:
        result.task(":jcstress").outcome == TaskOutcome.SUCCESS
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
                .withDebug(true)
                .build()
    }

}
