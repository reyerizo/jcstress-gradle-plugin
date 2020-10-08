package com.github.erizo.gradle

import org.apache.commons.io.FileUtils
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import spock.lang.Specification

import java.nio.file.Path
import java.nio.file.Paths

class JcstressPluginIntegrationCasesSpec extends Specification {

    @Rule
    MyTemporaryFolder testProjectDir = new MyTemporaryFolder()

    def pluginClasspath

    def setup() {
        pluginClasspath = getClass().classLoader.findResource('plugin-classpath.txt').readLines().collect {
            new File(it)
        }
    }

    def "should produce two separate jars"() {
        given:
        def jcstressProjectRoot = Paths.get(getClass().classLoader.getResource("simple-application-twojar").toURI()).toFile()
        def projectRoot = testProjectDir.root
        FileUtils.copyDirectory(jcstressProjectRoot, projectRoot, false)

        when:
        runGradleTask('jar', 'jcstressJar')
        def fileNames = Path.of(projectRoot.toString(), 'build', 'libs').toFile().list()

        then:
        fileNames.sort() == ['simple-application-twojar.jar', 'simple-application-twojar-jcstress.jar'].sort()
    }

    private BuildResult runGradleTask(String... taskNames) {
        def arguments = new ArrayList()
        arguments.addAll(taskNames)
        arguments.addAll(['-i', '--stacktrace', '--refresh-dependencies'])

        GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withGradleVersion("6.6.1")
                .forwardStdOutput(System.out.newPrintWriter())
                .forwardStdError(System.err.newPrintWriter())
                .withArguments(arguments)
                .withPluginClasspath(pluginClasspath)
                .build()
    }

}
