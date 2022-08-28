package com.github.erizo.gradle

import org.apache.commons.io.FileUtils
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import spock.lang.Specification
import spock.lang.TempDir

import java.nio.file.Path
import java.nio.file.Paths

class JcstressPluginIntegrationCasesSpec extends Specification {

    @TempDir
    File testProjectDir

    def pluginClasspath

    def setup() {
        pluginClasspath = getClass().classLoader.findResource('plugin-classpath.txt').readLines().collect {
            new File(it)
        }
    }

    def "should produce two separate jars"() {
        given:
        def jcstressProjectRoot = Paths.get(getClass().classLoader.getResource("simple-application-twojar").toURI()).toFile()
        def projectRoot = testProjectDir
        FileUtils.copyDirectory(jcstressProjectRoot, projectRoot, false)

        when:
        runGradleTask('jar', 'jcstressJar')
        def fileNames = Path.of(projectRoot.toString(), 'build', 'libs').toFile().list()

        then:
        fileNames.sort() == ['simple-application-twojar.jar', 'simple-application-twojar-jcstress.jar'].sort()
    }

    def "should not throw a null pointer on a new gradle and apisample"() {
        given:
        def jcstressProjectRoot = Paths.get(getClass().classLoader.getResource("simple-application-new-gradle-apisample").toURI()).toFile()
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

    def "should not throw a null pointer on a new gradle, apisample and Kotlin"() {
        given:
        def jcstressProjectRoot = Paths.get(getClass().classLoader.getResource("simple-application-new-gradle-apisample-kt").toURI()).toFile()
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

    private BuildResult runGradleTask(String... taskNames) {
        def arguments = new ArrayList()
        arguments.addAll(taskNames)
        arguments.addAll(['-i', '--stacktrace', '--refresh-dependencies'])

        GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withGradleVersion("6.7")
                .forwardStdOutput(System.out.newPrintWriter())
                .forwardStdError(System.err.newPrintWriter())
                .withArguments(arguments)
                .withPluginClasspath(pluginClasspath)
                .withDebug(true)
                .build()
    }


}
