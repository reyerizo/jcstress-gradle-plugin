package com.github.erizo.gradle

import org.apache.commons.io.FileUtils
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import spock.lang.Specification
import spock.lang.TempDir

import java.nio.file.Paths

class JcstressPluginIncludeTestsSpec extends Specification {

    @TempDir
    File testProjectDir

    def pluginClasspath

    def setup() {
        pluginClasspath = getClass().classLoader.findResource('plugin-classpath.txt').readLines().collect {
            new File(it)
        }
    }

    def "should include a test class a simple run"() {
        given:
        def jcstressProjectRoot = Paths.get(getClass().classLoader.getResource("simple-application-include-tests").toURI()).toFile()
        FileUtils.copyDirectory(jcstressProjectRoot, testProjectDir, false)

        when:
        def result = runGradleTask('jcstress')

        then:
        result.task(":jcstress").outcome == TaskOutcome.SUCCESS
    }

    private BuildResult runGradleTask(String taskName) {
        GradleRunner.create()
                .withProjectDir(testProjectDir)
                .forwardStdOutput(System.out.newPrintWriter())
                .forwardStdError(System.err.newPrintWriter())
                .withArguments(taskName, '-i', '--stacktrace', '--refresh-dependencies')
                .withPluginClasspath(pluginClasspath)
                .build()
    }

}
