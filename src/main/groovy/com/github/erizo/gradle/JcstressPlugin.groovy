/**
 Copyright 2015 the original author or authors.

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */
package com.github.erizo.gradle

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.distribution.Distribution
import org.gradle.api.distribution.plugins.DistributionPlugin
import org.gradle.api.file.CopySpec
import org.gradle.api.file.FileCollection
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.application.CreateStartScripts
import org.gradle.api.tasks.bundling.Jar
import org.gradle.plugins.ide.idea.IdeaPlugin

import java.nio.file.Paths

/**
 * Configures the Jcstress Plugin.
 *
 * @author Cédric Champeau
 * @author jerzykrlk
 *
 */
class JcstressPlugin implements Plugin<Project> {

    static final String JCSTRESS_NAME = 'jcstress'
    static final String TASK_JCSTRESS_NAME = JCSTRESS_NAME
    static final String TASK_JCSTRESS_JAR_NAME = 'jcstressJar'
    static final String TASK_JCSTRESS_INSTALL_NAME = 'jcstressInstall'
    static final String TASK_JCSTRESS_SCRIPTS_NAME = "jcstressScripts"

    private Project project

    private String jcstressApplicationName

    void apply(Project project) {
        this.project = project
        this.jcstressApplicationName = project.name + "-jcstress"

        project.pluginManager.apply(JavaPlugin)
        project.pluginManager.apply(DistributionPlugin)

        final JcstressPluginExtension jcstressPluginExtension = project.extensions.create(JCSTRESS_NAME, JcstressPluginExtension, project)

        addJcstressConfiguration()

        addJcstressJarDependencies(jcstressPluginExtension)

        addJcstressSourceSet(jcstressPluginExtension)

        addJcstressJarTask(jcstressPluginExtension)

        addJcstressTask(jcstressPluginExtension)

        addCreateScriptsTask(jcstressPluginExtension)

        Sync installAppTask = addInstallAppTask()

        configureInstallTasks(installAppTask)

        updateIdeaPluginConfiguration()
    }

    private addJcstressJarDependencies(JcstressPluginExtension jcstressPluginExtension) {
        project.afterEvaluate {
            addDependency(project.configurations['jcstress'], jcstressPluginExtension.jcstressDependency)
            addDependency(project.configurations['testCompile'], jcstressPluginExtension.jcstressDependency)
        }
    }

    private addDependency(Configuration configuration, String dependency) {
        DependencyHandler dependencyHandler = project.getDependencies()
        configuration.getDependencies().add(dependencyHandler.create(dependency))
    }

    private Configuration addJcstressConfiguration() {
        project.configurations.create(JCSTRESS_NAME)
    }

    private updateIdeaPluginConfiguration() {
        project.afterEvaluate {
            def hasIdea = project.plugins.findPlugin(IdeaPlugin)
            if (hasIdea) {
                project.idea {
                    module {
                        scopes.TEST.plus += [project.configurations.jcstress]
                    }
                }
                project.idea {
                    module {
                        project.sourceSets.jcstress.java.srcDirs.each {
                            testSourceDirs += project.file(it)
                        }
                    }
                }
            }
        }
    }

    private void addJcstressJarTask(extension) {
        def jcstressExclusions = {
            exclude '**/META-INF/BenchmarkList'
            exclude '**/META-INF/CompilerHints'
        }

        project.tasks.create(name: TASK_JCSTRESS_JAR_NAME, type: Jar) {
            dependsOn 'jcstressClasses'
            inputs.dir project.sourceSets.jcstress.output
            project.afterEvaluate {
                from(project.sourceSets.jcstress.output)
                from(project.sourceSets.main.output, jcstressExclusions)
                if (extension.includeTests) {
                    from(project.sourceSets.test.output, jcstressExclusions)
                }
            }

            classifier = 'jcstress'
        }
    }

    private Task addJcstressTask(JcstressPluginExtension extension) {
        def task = project.tasks.create(name: TASK_JCSTRESS_NAME, type: JcstressTask) as JcstressTask

        task.dependsOn project.jcstressJar
        task.main = 'org.openjdk.jcstress.Main'
        task.group = 'Verification'
        task.description = 'Runs jcstress benchmarks.'
        task.jvmArgs = ['-XX:+UnlockDiagnosticVMOptions', '-XX:+WhiteBoxAPI', '-XX:-RestrictContended', "-Duser.language=${extension.language}"]
        task.classpath = project.configurations.jcstress + project.configurations.jcstressRuntime + project.configurations.runtime
        task.doFirst {
            getAndCreateDirectory(project.buildDir, "tmp", "jcstress")
        }

        project.afterEvaluate { project ->
            if (!extension.reportDir) {
                extension.reportDir = getAndCreateDirectory(project.buildDir, "reports", "jcstress")
            }
            task.args = [*task.args, *extension.buildArgs()]
            task.classpath += project.files(project.jcstressJar.archivePath)
            filterConfiguration(project.configurations.jcstress, 'jcstress-core')
            if (extension.includeTests) {
                task.classpath += project.configurations.testRuntime
            }
            File path = getAndCreateDirectory(project.buildDir, "tmp", "jcstress")
            task.workingDir = path
        }


        task.doFirst {
            args = [*args, *task.jcstressArgs()]
        }

    }

    private static File getAndCreateDirectory(File dir, String... subdirectory) {
        def path = Paths.get(dir.path, subdirectory).toFile()
        path.mkdirs()
        path
    }

    void configureInstallTasks(Sync installTask) {
        installTask.doFirst {
            if (destinationDir.directory) {
                if (!new File(destinationDir as File, 'lib').directory || !new File(destinationDir as File, 'bin').directory) {
                    throw new GradleException("The specified installation directory '${destinationDir}' is neither empty nor does it contain an installation for this application.\n" +
                            "If you really want to install to this directory, delete it and run the install task again.\n" +
                            "Alternatively, choose a different installation directory."
                    )
                }
            }
        }
        installTask.doLast {
            project.ant.chmod(file: "${destinationDir.absolutePath}/bin/${jcstressApplicationName}", perm: 'ugo+x')
        }
    }

    private CopySpec configureDistSpec(CopySpec distSpec) {
        def jar = project.tasks[TASK_JCSTRESS_JAR_NAME]
        def startScripts = project.tasks[TASK_JCSTRESS_SCRIPTS_NAME]

        distSpec.with {
            from(project.file("src/dist"))

            into("lib") {
                from(jar)
                from(project.configurations.jcstress + project.configurations.runtime)
            }
            into("bin") {
                from(startScripts)
                fileMode = 0755
            }
        }
//        distSpec.with(pluginConvention.applicationDistribution)

        distSpec
    }

    private Sync addInstallAppTask() {
        Distribution distribution = project.distributions.create("jcstress") as Distribution
        distribution.baseName = jcstressApplicationName
        configureDistSpec(distribution.contents)

        def installTask = project.tasks.create(TASK_JCSTRESS_INSTALL_NAME, Sync)
        installTask.description = "Installs the project as a JVM application along with libs and OS specific scripts."
        installTask.group = 'Verification'
        installTask.with distribution.contents
        installTask.into { project.file("${project.buildDir}/install/${jcstressApplicationName}") }
        installTask
    }

    private void addJcstressSourceSet(extension) {
        project.sourceSets {
            jcstress {
                compileClasspath += project.configurations.jcstress + project.configurations.compile + (main.output as FileCollection)
                runtimeClasspath += project.configurations.jcstress + project.configurations.runtime + (main.output as FileCollection)

                project.afterEvaluate {
                    if (extension.includeTests) {
                        compileClasspath += project.configurations.testCompile + (test.output as FileCollection)
                        runtimeClasspath += project.configurations.testRuntime + (test.output as FileCollection)
                    }
                }
            }
        }
    }

    // @Todo: refactor this task configuration to extend a copy task and use replace tokens
    private void addCreateScriptsTask(extension) {
        project.tasks.create(TASK_JCSTRESS_SCRIPTS_NAME, CreateStartScripts) {
            description = ["Creates OS specific scripts to run the project as a jcstress test suite."]
            classpath = project.tasks[TASK_JCSTRESS_JAR_NAME].outputs.files + project.configurations.jcstress + project.configurations.runtime
            mainClassName = 'org.openjdk.jcstress.Main'
            applicationName = jcstressApplicationName
            outputDir = new File(project.buildDir, 'scripts')
            defaultJvmOpts = ['-XX:+UnlockDiagnosticVMOptions', '-XX:+WhiteBoxAPI', '-XX:-RestrictContended', "-Duser.language=${extension.language}"]

            project.afterEvaluate {
                if (extension.includeTests) {
                    classpath += project.configurations.testRuntime
                }
            }
        }
    }

    /**
     * Dummy method, loads configuration dependencies.
     * @param configuration configuration
     * @param jarFileName jar file name
     * @return ignored
     */
    private static filterConfiguration(Configuration configuration, String jarFileName) {
        configuration.filter({ it.name.contains(jarFileName) }).files
    }

    static getFileNameFromDependency(String gradleDependencyName) {
        def split = gradleDependencyName.split(":")
        split[1] + "-" + split[2] + ".jar"
    }


}
