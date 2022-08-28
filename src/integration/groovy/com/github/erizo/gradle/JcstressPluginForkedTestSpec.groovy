package com.github.erizo.gradle

import org.apache.commons.io.FileUtils
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import spock.lang.Specification
import spock.lang.TempDir

import java.nio.file.Paths

class JcstressPluginForkedTestSpec extends Specification {

    @TempDir
    File testProjectDir

    def pluginClasspath

    def setup() {
        pluginClasspath = getClass().classLoader.findResource('plugin-classpath.txt').readLines().collect {
            new File(it)
        }
    }

    def "should complete a forked run"() {
        given:
        def jcstressProjectRoot = Paths.get(getClass().classLoader.getResource("simple-application-forked").toURI()).toFile()
        FileUtils.copyDirectory(jcstressProjectRoot, testProjectDir, false)

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

    private BuildResult runGradleTask(String taskName) {
        GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withArguments(taskName, '-i', '--stacktrace', '--refresh-dependencies')
                .forwardStdOutput(System.out.newPrintWriter())
                .forwardStdError(System.err.newPrintWriter())
                .withPluginClasspath(pluginClasspath)
                .build()
    }

}
