package com.github.erizo.gradle

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Ignore
import spock.lang.Specification

@Ignore("Move to functional sourceset")
class JcstressPluginFunctionalSpec extends Specification {

    @Rule
    TemporaryFolder testProjectDir = new TemporaryFolder()

    def buildFile
    def pluginClasspath

    def setup() {
        buildFile = testProjectDir.newFile('build.gradle')
        pluginClasspath = getClass().classLoader.findResource('plugin-classpath.txt').readLines().collect {
            new File(it)
        }
    }

    def "should create a Linux script"() {
        given:
        buildFile << buildFileContents

        when:
        def result = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments('jcstressJar', '--stacktrace', '--refresh-dependencies')
                .withPluginClasspath(pluginClasspath)
                .build()

        then:
        result.task(":jcstressJar").outcome == TaskOutcome.SUCCESS
    }

    def buildFileContents =
            """
                plugins {
                    id 'jcstress'
                }

                repositories {
                    jcenter()
                }
            """
}
