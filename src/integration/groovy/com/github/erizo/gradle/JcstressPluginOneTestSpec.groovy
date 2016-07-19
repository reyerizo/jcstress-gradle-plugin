package com.github.erizo.gradle

import org.apache.commons.io.FileUtils
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

import java.nio.file.Paths

class JcstressPluginOneTestSpec extends Specification {

    @Rule
    TemporaryFolder testProjectDir = new TemporaryFolder()

    def pluginClasspath

    def setup() {
        pluginClasspath = getClass().classLoader.findResource('plugin-classpath.txt').readLines().collect {
            new File(it)
        }
    }

    def "should complete a simple run"() {
        given:
        def jcstressProjectRoot = Paths.get(getClass().classLoader.getResource("simple-application").toURI()).toFile()
        FileUtils.copyDirectory(jcstressProjectRoot, testProjectDir.root, false)

        when:
        def result = runGradleTask('jcstress')

        then:
        result.task(":jcstress").outcome == TaskOutcome.SUCCESS
    }

    private BuildResult runGradleTask(String taskName) {
        GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments(taskName, '-i', '--stacktrace', '--refresh-dependencies')
                .withPluginClasspath(pluginClasspath)
                .build()
    }

}
