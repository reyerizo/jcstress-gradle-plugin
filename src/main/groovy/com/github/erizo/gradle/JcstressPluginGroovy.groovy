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

import org.gradle.api.*
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.distribution.Distribution
import org.gradle.api.distribution.plugins.DistributionPlugin
import org.gradle.api.file.CopySpec
import org.gradle.api.file.FileCollection
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.application.CreateStartScripts
import org.gradle.api.tasks.bundling.Jar
import org.gradle.plugins.ide.idea.IdeaPlugin
import org.gradle.plugins.ide.idea.model.IdeaModel
import org.gradle.plugins.ide.idea.model.IdeaModule

import java.nio.file.Paths

/**
 * Configures the Jcstress Plugin.
 *
 * @author Cédric Champeau
 * @author jerzykrlk
 *
 */
class JcstressPluginGroovy {

    private static final String TASK_JCSTRESS_JAR_NAME = "jcstressJar";
    private static final String TASK_JCSTRESS_INSTALL_NAME = "jcstressInstall";
    private static final String TASK_JCSTRESS_SCRIPTS_NAME = "jcstressScripts";

    public Project project;

    public String jcstressApplicationName;

    public JcstressPluginExtension jcstressPluginExtension;

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

    Sync addInstallAppTask() {
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

    void addJcstressSourceSet(extension) {
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
    void addCreateScriptsTask(extension) {
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

}
