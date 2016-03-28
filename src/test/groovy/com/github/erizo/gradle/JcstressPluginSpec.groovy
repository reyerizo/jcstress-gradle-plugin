package com.github.erizo.gradle

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.distribution.plugins.DistributionPlugin
import org.gradle.api.internal.artifacts.configurations.DefaultConfiguration
import org.gradle.api.internal.project.DefaultProject
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.Sync
import org.gradle.jvm.tasks.Jar
import org.gradle.plugins.ide.idea.IdeaPlugin
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification

import java.nio.file.Files

public class JcstressPluginSpec extends Specification {

    private final Project project = createRootProject()
    private final JcstressPlugin plugin = new JcstressPlugin()

    def "should apply JavaPlugin"() {
        when:
        plugin.apply(project)

        then:
        project.plugins.hasPlugin(JavaPlugin)
    }

    def "should apply DistributionPlugin"() {
        when:
        plugin.apply(project)

        then:
        project.plugins.hasPlugin(DistributionPlugin)
    }

    def "should add jcstress task"() {
        when:
        plugin.apply(project)

        then:
        project.tasks['jcstress'] instanceof JavaExec
    }

    def "should add jcstress configuration"() {
        when:
        plugin.apply(project)

        then:
        project.configurations['jcstress'] instanceof Configuration
    }

    def "should add jcstressJar task"() {
        when:
        plugin.apply(project)

        then:
        project.tasks['jcstressJar'] instanceof Jar
    }

    def "should add jcstressInstall task"() {
        when:
        plugin.apply(project)

        then:
        project.tasks['jcstressInstall'] instanceof Sync
    }

    def "should add default jvm args to jsctress task"() {
        when:
        plugin.apply(project)
        def jcstressTask = project.tasks['jcstress']

        then:
        jcstressTask.jvmArgs.containsAll(['-XX:+UnlockDiagnosticVMOptions', '-XX:+WhiteBoxAPI', '-XX:-RestrictContended'])
    }

    def "should add jcstress dependencies to jcstress configuration"() {
        given:
        def whiteboxApiDependency = project.dependencies.create(JcstressPlugin.WHITEBOX_API_DEPENDENCY)
        def jcstressDependency = project.dependencies.create('com.github.erizo.gradle:jcstress-core:1.0-20150729205107')

        when:
        plugin.apply(project)

        then:
        getConfiguration("jcstress").allDependencies.contains(whiteboxApiDependency)
        getConfiguration("jcstress").allDependencies.contains(jcstressDependency)
    }

    def "should not add jcstress dependencies to compile configuration"() {
        given:
        def whiteboxApiDependency = project.dependencies.create(JcstressPlugin.WHITEBOX_API_DEPENDENCY)
        def jcstressDependency = project.dependencies.create('com.github.erizo.gradle:jcstress-core:1.0-20150729205107')

        when:
        plugin.apply(project)

        then:
        !getConfiguration("compile").allDependencies.contains(whiteboxApiDependency)
        !getConfiguration("compile").allDependencies.contains(jcstressDependency)
    }

    def "should add jcstress configuration to test scope with Intellij plugin"() {
        given:
        project.apply(plugin: IdeaPlugin)

        when:
        plugin.apply(project)
        project.evaluate()

        then:
        project.idea.module.scopes.TEST.plus.contains(project.configurations.jcstress)
    }

    def "should add jcstress sources to test sources with Intellij plugin"() {
        given:
        project.apply(plugin: IdeaPlugin)

        when:
        plugin.apply(project)
        project.evaluate()

        then:
        project.idea.module.testSourceDirs.containsAll(project.sourceSets.jcstress.java.srcDirs)
    }

    static DefaultProject createRootProject() {
        return ProjectBuilder
                .builder()
                .withProjectDir(Files.createTempDirectory("myjcstressproject").toFile())
                .withName("myjcstressproject")
                .build()
    }

    private DefaultConfiguration getConfiguration(String configurationName) {
        DefaultConfiguration configuration = project.configurations[configurationName]
        configuration.getResolvedConfiguration()
        return configuration
    }

}
