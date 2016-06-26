package com.github.erizo.gradle

import org.gradle.api.artifacts.Configuration
import org.gradle.api.distribution.plugins.DistributionPlugin
import org.gradle.api.internal.project.AbstractProject
import org.gradle.api.plugins.GroovyPlugin
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.application.CreateStartScripts
import org.gradle.jvm.tasks.Jar
import org.gradle.plugins.ide.idea.IdeaPlugin
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification

import java.nio.file.Files
import java.nio.file.Paths

public class JcstressPluginSpec extends Specification {

    private final AbstractProject project = createRootProject()
    private final JcstressPlugin plugin = new JcstressPlugin()

    def setup() {
        project.repositories {
            mavenCentral()
            jcenter()
        }
    }

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

    def "should create jcstress source set"() {
        when:
        plugin.apply(project)

        then:
        project.sourceSets.jcstress.java.source == ['src/jcstress/java']
        project.sourceSets.jcstress.resources.source == ['src/jcstress/resources']
    }

    def "should add groovy source set if groovy plugin enabled"() {
        when:
        project.apply(plugin: GroovyPlugin)
        plugin.apply(project)

        then:
        project.sourceSets.jcstress.java.source == ['src/jcstress/java']
        project.sourceSets.jcstress.groovy.source == ['src/jcstress/groovy']
        project.sourceSets.jcstress.resources.source == ['src/jcstress/resources']
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

    def "should include a class from jcstress sourceSet in jcstress jar"() {
        given:
        def jcstressClassFile = createNewFile("build", "classes", "jcstress", "jcstress.class")

        when:
        plugin.apply(project)
        project.evaluate()

        then:
        Jar task = project.tasks['jcstressJar'] as Jar
        task.source.files.contains(jcstressClassFile)
    }

    def "should include a resource from jcstress sourceSet in jcstress jar"() {
        given:
        def jcstressClassFile = createNewFile("build", "resources", "jcstress", "jcstress.txt")

        when:
        plugin.apply(project)
        project.evaluate()

        then:
        Jar task = project.tasks['jcstressJar'] as Jar
        task.source.files.contains(jcstressClassFile)
    }

    def "should include a class from test sourceSet in jcstress jar when test enabled"() {
        given:
        def jcstressClassFile = createNewFile("build", "classes", "test", "jcstress.class")
        plugin.apply(project)
        project.jcstress {
            includeTests = true
        }

        when:
        project.evaluate()

        then:
        Jar task = project.tasks['jcstressJar'] as Jar
        task.source.files.contains(jcstressClassFile)
    }

    def "should not include a class from test sourceSet in jcstress jar when test disabled"() {
        given:
        def jcstressClassFile = createNewFile("build", "classes", "test", "jcstress.class")
        plugin.apply(project)

        when:
        project.evaluate()

        then:
        Jar task = project.tasks['jcstressJar'] as Jar
        !task.source.files.contains(jcstressClassFile)
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
        project.evaluate()
        def jcstressTask = project.tasks['jcstress']

        then:
        jcstressTask.jvmArgs.containsAll(['-XX:+UnlockDiagnosticVMOptions', '-XX:+WhiteBoxAPI', '-XX:-RestrictContended'])
    }

    def "should add jcstress configuration arguments to jsctress task"() {
        given:
        plugin.apply(project)
        project.jcstress {
            timeMillis = "200"
            forks = 30
        }

        when:
        project.tasks.jcstress.args = ["asdf"]
        project.evaluate()
        def jcstressTask = project.tasks['jcstress']

        then:
        jcstressTask.args.containsAll(['asdf', '-f', '30', '-time', '200'])
    }

    def "should include tests in jcstress tasks if includeTests is true"() {
        given:
        plugin.apply(project)

        project.jcstress {
            includeTests = true
        }

        project.dependencies {
            testCompile 'org.springframework:spring-core:4.0.0.RELEASE'
        }

        when:
        project.evaluate()
        def jcstressTask = project.tasks.jcstress

        then:
        jcstressTask.classpath.files.containsAll(project.configurations.testCompile.files)
    }

    def "should not include tests in jcstress tasks if includeTests is false"() {
        given:
        plugin.apply(project)

        project.jcstress {
            includeTests = false
        }

        project.dependencies {
            testCompile 'org.springframework:spring-core:4.0.0.RELEASE'
        }

        when:
        project.evaluate()
        def jcstressTask = project.tasks.jcstress

        then:
        jcstressTask.classpath.filter({ it.name.contains('spring-core') }).size() == 0
    }

    def "should include tests in jcstress sourceSet classpath if includeTests is true"() {
        given:
        plugin.apply(project)

        project.jcstress {
            includeTests = true
        }

        project.dependencies {
            testCompile 'org.springframework:spring-core:4.0.0.RELEASE'
        }

        when:
        project.evaluate()
        def compileClasspath = project.sourceSets.jcstress.compileClasspath
        def runtimeClasspath = project.sourceSets.jcstress.runtimeClasspath

        then:
        compileClasspath.files.containsAll(project.configurations.testCompile.files)
        runtimeClasspath.files.containsAll(project.configurations.testCompile.files)
    }

    def "should add whitebox-api to boot classpath"() {
        when:
        plugin.apply(project)
        project.evaluate()

        then:
        project.tasks['jcstress'].jvmArgs.findAll({ it.contains('whitebox') }).size() == 1
    }

    def "should add jcstress dependencies to jcstress configuration"() {
        given:
        def whiteboxApiDependency = project.dependencies.create(JcstressPlugin.WHITEBOX_API_DEPENDENCY)
        def jcstressDependency = project.dependencies.create('com.github.erizo.gradle:jcstress-core:1.0-20150729205107')

        when:
        plugin.apply(project)
        project.evaluate()

        then:
        getConfiguration("jcstress").allDependencies.contains(whiteboxApiDependency)
        getConfiguration("jcstress").allDependencies.contains(jcstressDependency)
    }

    def "should override jcstress dependency with gradle configuration"() {
        given:
        def whiteboxApiDependency = project.dependencies.create(JcstressPlugin.WHITEBOX_API_DEPENDENCY)
        def newJcstressDependency = project.dependencies.create('org.springframework:spring-core:4.0.0.RELEASE')
        def defaultJcstressDependency = project.dependencies.create('com.github.erizo.gradle:jcstress-core:1.0-20150729205107')

        when:
        plugin.apply(project)

        project.jcstress {
            jcstressDependency = 'org.springframework:spring-core:4.0.0.RELEASE'
        }

        project.evaluate()

        then:
        getConfiguration("jcstress").allDependencies.contains(whiteboxApiDependency)
        getConfiguration("jcstress").allDependencies.contains(newJcstressDependency)
        !getConfiguration("jcstress").allDependencies.contains(defaultJcstressDependency)
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

    def "should add jsctress scripts task"() {
        given:
        plugin.apply(project)

        when:
        def scriptTask = project.tasks.jcstressScripts

        then:
        scriptTask instanceof CreateStartScripts
        scriptTask.mainClassName == 'org.openjdk.jcstress.Main'
        scriptTask.applicationName == 'myjcstressproject-jcstress'
    }

    def "should include compile, runtime and jcstress dependencies in script task classpath"() {
        given:
        plugin.apply(project)

        project.dependencies {
            compile 'org.springframework:spring-core:4.0.0.RELEASE'
            runtime 'org.springframework:spring-webmvc:4.0.0.RELEASE'
        }

        when:
        project.evaluate()
        def classpathFiles = project.tasks.jcstressScripts.classpath.files

        then:
        classpathFiles.containsAll(project.configurations.runtime.files)
        classpathFiles.containsAll(project.configurations.compile.files)
    }

    def "should not include test in script task classpath if includeTests is not set"() {
        given:
        plugin.apply(project)

        project.dependencies {
            testCompile 'org.springframework:spring-test:4.0.0.RELEASE'
        }

        when:
        project.evaluate()
        def classpathFiles = project.tasks.jcstressScripts.classpath.files

        then:
        !classpathFiles.containsAll(project.configurations.testCompile.files)
    }

    def "should include test in script task classpath if includeTests is true"() {
        given:
        plugin.apply(project)

        project.dependencies {
            testCompile 'org.springframework:spring-test:4.0.0.RELEASE'
        }

        project.jcstress {
            includeTests = true
        }

        when:
        project.evaluate()
        def classpathFiles = project.tasks.jcstressScripts.classpath.files

        then:
        classpathFiles.containsAll(project.configurations.testCompile.files)
    }

    static AbstractProject createRootProject() {
        return ProjectBuilder
                .builder()
                .withProjectDir(Files.createTempDirectory("myjcstressproject").toFile())
                .withName("myjcstressproject")
                .build() as AbstractProject
    }

    private Configuration getConfiguration(String configurationName) {
        return project.configurations[configurationName]
    }

    private File createNewFile(String... filePath) {
        def result = Paths.get(project.rootDir.toString(), filePath).toFile()
        result.parentFile.mkdirs()
        result.createNewFile()
        return result
    }

//    private static Collection<String> getFileNames(FileCollection fileCollection) {
//        fileCollection.files.collect { it.getName() }
//    }

}
