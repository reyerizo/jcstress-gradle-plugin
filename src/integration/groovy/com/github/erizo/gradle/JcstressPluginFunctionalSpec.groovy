package com.github.erizo.gradle

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import spock.lang.Specification
import spock.lang.TempDir

import java.nio.file.Paths

class JcstressPluginFunctionalSpec extends Specification {

    @TempDir
    File testProjectDir

    def buildFile
    def settingsFile
    def pluginClasspath

    def setup() {
        buildFile = new File(testProjectDir, 'build.gradle')
        settingsFile = new File(testProjectDir, 'settings.gradle')
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
        fileText.contains("jcstress-core-${JcstressPluginExtension.JCSTRESS_DEFAULT_VERSION}.jar")
    }

    def "should create a Windows script"() {
        when:
        def result = runGradleTask('jcstressScripts')

        then:
        result.task(":jcstressScripts").outcome == TaskOutcome.SUCCESS

        def fileText = getFileContents("build", "scripts", "myTestProject-jcstress.bat")
        fileText.contains("jcstress-core-${JcstressPluginExtension.JCSTRESS_DEFAULT_VERSION}.jar")
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

    private String getFileContents(String... pathElements) {
        Paths.get(testProjectDir.toString(), pathElements).text
    }

    def buildFileContents =
            """
                plugins {
                    id 'jcstress'
                }

                repositories {
                    mavenCentral()
                }

                jcstress {
                }
            """

    def settingsFileContents = "rootProject.name = 'myTestProject'"
}
