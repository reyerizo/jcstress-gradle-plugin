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
        project.tasks.findByName('jcstress') instanceof JavaExec
    }

    def "should add jcstress configuration"() {
        when:
        plugin.apply(project)

        then:
        project.configurations.findByName('jcstress') instanceof Configuration
    }

    def "should add jcstressJar task"() {
        when:
        plugin.apply(project)

        then:
        project.tasks.findByName('jcstressJar') instanceof Jar
    }

    def "should add jcstressInstall task"() {
        when:
        plugin.apply(project)

        then:
        project.tasks.findByName('jcstressInstall') instanceof Sync
    }

    def "should add whitebox-api dependency to jcstress configuration only"() {
        given:
        project.apply(plugin: IdeaPlugin)
        plugin.apply(project)

        when:
        project.evaluate()

        DefaultConfiguration jcstressConfiguration = project.configurations.findByName("jcstress")
        jcstressConfiguration.getResolvedConfiguration()

        then:
        jcstressConfiguration.allDependencies.contains(project.dependencies.create(JcstressPlugin.WHITEBOX_API_DEPENDENCY))
    }


    def "should add jcstress configuration to test scope with Intellij plugin"() {
        given:
        project.apply(plugin: IdeaPlugin)
        plugin.apply(project)

        when:
        project.evaluate()

        then:
        project.idea.module.scopes.TEST.plus.contains(project.configurations.jcstress)
    }

    def "should add jcstress sources to test sources with Intellij plugin"() {
        given:
        project.apply(plugin: IdeaPlugin)
        plugin.apply(project)

        when:
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


}
