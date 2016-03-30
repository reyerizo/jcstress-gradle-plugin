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
import org.gradle.api.distribution.Distribution
import org.gradle.api.distribution.plugins.DistributionPlugin
import org.gradle.api.file.CopySpec
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.application.CreateStartScripts
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.testing.Test
import org.gradle.plugins.ide.idea.IdeaPlugin

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
    static final String WHITEBOX_API_DEPENDENCY = "com.github.erizo.gradle:sun.hotspot.whitebox-api:1.0"

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

        addJcstressSourceSet(project, jcstressPluginExtension.includeTests)

        addJcstressJarTask(project, jcstressPluginExtension)

        addJcstressTask(project, jcstressPluginExtension)

        addCreateScriptsTask(project, jcstressPluginExtension.includeTests)

        addJcstressToTestScope(project)

        Task installAppTask = addInstallAppTask(project)

        configureInstallTasks(installAppTask, project.tasks[DistributionPlugin.TASK_INSTALL_NAME])

        updateIdeaPluginConfiguration(project)
    }

    private addJcstressJarDependencies(jcstressPluginExtension) {
        project.dependencies {
            jcstress "${jcstressPluginExtension.jcstressDependency}"
            jcstress WHITEBOX_API_DEPENDENCY
            testCompile "${jcstressPluginExtension.jcstressDependency}"
            testCompile WHITEBOX_API_DEPENDENCY
        }
    }

    private Configuration addJcstressConfiguration() {
        project.configurations.create(JCSTRESS_NAME)
    }

    private updateIdeaPluginConfiguration(Project project) {
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

    private Task addJcstressToTestScope(Project project) {
        /** Intellij scope hack */
        project.tasks.create(name: 'addJcstressToTestScope', type: Test) {
            description = 'Adds jcstress to IDE test scope.'
            testClassesDir = project.sourceSets.jcstress.output.classesDir
            classpath = project.sourceSets.jcstress.runtimeClasspath
        }
    }

    private void addJcstressJarTask(Project project, extension) {
        def jcstressExclusions = {
            exclude '**/META-INF/BenchmarkList'
            exclude '**/META-INF/CompilerHints'
        }

        project.tasks.create(name: TASK_JCSTRESS_JAR_NAME, type: Jar) {
            dependsOn 'jcstressClasses'
            inputs.dir project.sourceSets.jcstress.output
            doFirst {
                from(project.sourceSets.jcstress.output)
                from(project.sourceSets.main.output, jcstressExclusions)
                if (extension.includeTests) {
                    from(project.sourceSets.test.output, jcstressExclusions)
                }
            }

            classifier = 'jcstress'
        }
    }

    private Task addJcstressTask(Project project, JcstressPluginExtension extension) {
        project.tasks.create(name: TASK_JCSTRESS_NAME, type: JavaExec) {
            dependsOn project.jcstressJar
            main = 'org.openjdk.jcstress.Main'
            group = 'Verification'
            description = 'Runs jcstress benchmarks.'
            jvmArgs = ['-XX:+UnlockDiagnosticVMOptions', '-XX:+WhiteBoxAPI', '-XX:-RestrictContended']

            // TODO: flatten somehow, so that we get rid of doFirst here
            project.afterEvaluate {
                // TODO move to above
                args = [*args, *extension.buildArgs()]

                //TODO: move to bootclasspath above
                jvmArgs += '-Xbootclasspath/a:' + getWhiteboxJarFromConfiguration(project.configurations.jcstress, "whitebox")

                classpath += project.configurations.jcstress + project.configurations.jcstressRuntime + project.configurations.runtime + project.jcstressJar.archivePath
                if (extension.includeTests) {
                    classpath += project.configurations.testRuntime
                }
            }
        }
    }

    void configureInstallTasks(Task... installTasks) {
        installTasks.each { installTask ->
            installTask.doFirst {
                if (destinationDir.directory) {
                    if (!new File(destinationDir, 'lib').directory || !new File(destinationDir, 'bin').directory) {
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
    }

    private CopySpec configureDistSpec(Project project, CopySpec distSpec) {
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

    private Task addInstallAppTask(Project project) {
        Distribution distribution = project.distributions[DistributionPlugin.MAIN_DISTRIBUTION_NAME]
        distribution.conventionMapping.baseName = { jcstressApplicationName }
        configureDistSpec(project, distribution.contents)

        def installTask = project.tasks.create(TASK_JCSTRESS_INSTALL_NAME, Sync)
        installTask.description = "Installs the project as a JVM application along with libs and OS specific scripts."
        installTask.group = 'Verification'
        installTask.with distribution.contents
        installTask.into { project.file("${project.buildDir}/install/${jcstressApplicationName}") }
        installTask
    }


    private void addJcstressSourceSet(Project project, includeTests) {
        project.sourceSets {
            jcstress {
                java.srcDir 'src/jcstress/java'
                if (project.plugins.hasPlugin('groovy')) {
                    groovy.srcDir 'src/jcstress/groovy'
                }
                resources.srcDir 'src/jcstress/resources'

                compileClasspath += project.configurations.jcstress + project.configurations.compile + main.output
                runtimeClasspath += project.configurations.jcstress + project.configurations.runtime + main.output

                if (includeTests) {
                    compileClasspath += project.configurations.testCompile
                    runtimeClasspath += project.configurations.testRuntime
                }
            }
        }
    }

    // @Todo: refactor this task configuration to extend a copy task and use replace tokens
    // @Todo: whitebox-api lib dependency should go into new scripts (win / unix)
    private void addCreateScriptsTask(Project project, boolean includeTests) {
        project.tasks.create(TASK_JCSTRESS_SCRIPTS_NAME, CreateStartScripts) {
            description = "Creates OS specific scripts to run the project as a jcstress test suite."
            classpath = project.tasks[TASK_JCSTRESS_JAR_NAME].outputs.files + project.configurations.jcstress + project.configurations.runtime
            conventionMapping.mainClassName = { 'org.openjdk.jcstress.Main' }
            conventionMapping.applicationName = { jcstressApplicationName }
            conventionMapping.outputDir = { new File(project.buildDir, 'scripts') }
            conventionMapping.defaultJvmOpts = {
                ['-XX:+UnlockDiagnosticVMOptions', '-XX:+WhiteBoxAPI', '-XX:-RestrictContended', '-Xbootclasspath/a:../lib/sun.hotspot.whitebox-api-1.0.jar']
            }

            doFirst {
                if (includeTests) {
                    classpath += project.configurations.testRuntime
                }
            }
        }
    }

    private static def getWhiteboxJarFromConfiguration(
            def configuration, String jarFileName) {
        def files = []
        configuration.files.each({
            if (it.name.contains(jarFileName)) {
                files.add(it)
            }
        })
        files[0]
    }

}
