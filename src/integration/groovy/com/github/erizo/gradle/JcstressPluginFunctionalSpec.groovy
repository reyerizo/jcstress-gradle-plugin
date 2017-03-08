package com.github.erizo.gradle

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

import java.nio.file.Paths

class JcstressPluginFunctionalSpec extends Specification {

    @Rule
    TemporaryFolder testProjectDir = new TemporaryFolder()

    def buildFile
    def settingsFile
    def pluginClasspath

    def setup() {
        buildFile = testProjectDir.newFile('build.gradle')
        settingsFile = testProjectDir.newFile('settings.gradle')
        pluginClasspath = getClass().classLoader.findResource('plugin-classpath.txt').readLines().collect {
            new File(it)
        }
        buildFile << buildFileContents
        settingsFile << settingsFileContents
    }

    def "should create a Linux script"() {
        when:
        def result = runGradleTask('jcstressScripts')

        then:
        result.task(":jcstressScripts").outcome == TaskOutcome.SUCCESS

        def fileText = getFileContents("build", "scripts", "myTestProject-jcstress")
        fileText.contains("jcstress-core-0.2.jar")
    }

    def "should create a Windows script"() {
        when:
        def result = runGradleTask('jcstressScripts')

        then:
        result.task(":jcstressScripts").outcome == TaskOutcome.SUCCESS

        def fileText = getFileContents("build", "scripts", "myTestProject-jcstress.bat")
        fileText.contains("jcstress-core-0.2.jar")
    }

    private BuildResult runGradleTask(String taskName) {
        GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments(taskName, '-i', '--stacktrace', '--refresh-dependencies')
                .withPluginClasspath(pluginClasspath)
                .build()
    }

    private String getFileContents(String... pathElements) {
        Paths.get(testProjectDir.root.toString(), pathElements).text
    }

    def buildFileContents =
            """
                plugins {
                    id 'jcstress'
                }

                repositories {
                    jcenter()
                    mavenLocal()
                }

                jcstress {
                }
            """

    def settingsFileContents = "rootProject.name = 'myTestProject'"
}
