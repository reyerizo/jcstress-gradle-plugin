package com.github.erizo.gradle

import org.apache.commons.io.FileUtils
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import spock.lang.Specification
import spock.lang.TempDir

import java.nio.file.Paths

class JcstressPluginUnforkedTestSpec extends Specification {

    @TempDir
    File testProjectDir

    def pluginClasspath

    def setup() {
        pluginClasspath = getClass().classLoader.findResource('plugin-classpath.txt').readLines().collect {
            new File(it)
        }
    }

    def "should complete an unforked run"() {
        given:
        def jcstressProjectRoot = Paths.get(getClass().classLoader.getResource("simple-application-unforked").toURI()).toFile()
        FileUtils.copyDirectory(jcstressProjectRoot, testProjectDir, false)

        when:
        def result = runGradleTask('jcstress')

        def errorMessage = result.output.find('FATAL: (.*)')

        then:
        verifyAll {
            result.task(":jcstress").outcome == TaskOutcome.SUCCESS
            errorMessage == null
        }
    }

    private BuildResult runGradleTask(String taskName) {
        GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withArguments(taskName, '-i', '--stacktrace', '--refresh-dependencies')
                .forwardStdOutput(System.out.newPrintWriter())
                .forwardStdError(System.err.newPrintWriter())
                .withPluginClasspath(pluginClasspath)
                .withDebug(true)
                .build()
    }

}
