package com.github.erizo.gradle

import org.gradle.api.Project
import org.gradle.api.internal.project.DefaultProject
import org.gradle.api.plugins.JavaPlugin
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
        project.plugins.hasPlugin(JavaPlugin.class)
    }

    static DefaultProject createRootProject() {
        return ProjectBuilder
                .builder()
                .withProjectDir(Files.createTempDirectory("myjcstressproject").toFile())
                .withName("myjcstressproject")
                .build()
    }


}
