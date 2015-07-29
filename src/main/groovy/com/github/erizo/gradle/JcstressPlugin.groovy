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

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ResolvableDependencies
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.testing.Test

/**
 * Configures the Jcstress Plugin.
 *
 * @author Cédric Champeau
 * @author jerzykrlk
 *
 */
class JcstressPlugin implements Plugin<Project> {

    void apply(Project project) {
        project.plugins.apply(JavaPlugin)
        final JcstressPluginExtension extension = project.extensions.create('jcstress', JcstressPluginExtension, project)
        final Configuration configuration = project.configurations.create('jcstress')

        final Configuration testConfiguration = project.configurations.testCompile
        testConfiguration.incoming.beforeResolve { ResolvableDependencies resolvableDependencies ->
            DependencyHandler dependencyHandler = project.getDependencies();
            def dependencies = testConfiguration.getDependencies()
            dependencies.add(dependencyHandler.create("${extension.jcstressDependency}"))
        }

        project.sourceSets {
            jcstress {
                java.srcDir 'src/jcstress/java'
                if (project.plugins.hasPlugin('groovy')) {
                    groovy.srcDir 'src/jcstress/groovy'
                }
                resources.srcDir 'src/jcstress/resources'
                compileClasspath += project.configurations.jcstress + project.configurations.compile + main.output
                runtimeClasspath += project.configurations.jcstress + project.configurations.runtime + main.output

                if (extension.includeTests) {
                    compileClasspath += project.configurations.testCompile
                    runtimeClasspath += project.configurations.testRuntime
                }
            }
        }

        configuration.incoming.beforeResolve { ResolvableDependencies resolvableDependencies ->
            DependencyHandler dependencyHandler = project.getDependencies();
            def dependencies = configuration.getDependencies()
            dependencies.add(dependencyHandler.create("${extension.jcstressDependency}"))
        }

        def jcstressExclusions = {
            exclude '**/META-INF/BenchmarkList'
            exclude '**/META-INF/CompilerHints'
        }

        project.tasks.create(name: 'jcstressJar', type: Jar) {
            dependsOn 'jcstressClasses'
            inputs.dir project.sourceSets.jcstress.output
            doFirst {
                def filter = { it.isDirectory() ? it : project.zipTree(it) }
                def exclusions = {
                    exclude '**/META-INF/services/**'
                    exclude '**/META-INF/*.SF'
                    exclude '**/META-INF/*.DSA'
                    exclude '**/META-INF/*.RSA'
                }
                from(project.configurations.jcstress.collect(filter), exclusions)
                from(project.configurations.compile.collect(filter), exclusions)
                from(project.sourceSets.jcstress.output)
                from(project.sourceSets.main.output, jcstressExclusions)
                if (extension.includeTests) {
                    from(project.sourceSets.test.output, jcstressExclusions)
                }
            }

            manifest {
                attributes 'Main-Class': 'org.openjdk.jcstress.Main'
            }

            classifier = 'jcstress'
        }

        project.tasks.create(name: 'jcstress', type: JavaExec) {
            dependsOn project.jcstressJar
            main = 'org.openjdk.jcstress.Main'
            classpath = project.files { project.jcstressJar.archivePath }
            group = 'Verification'
            description = 'Runs jcstress benchmarks.'
            jvmArgs = ['-XX:+UnlockDiagnosticVMOptions', '-XX:+WhiteBoxAPI', '-XX:-RestrictContended', '-Xbootclasspath/a:' + project.jcstressJar.archivePath]

            doFirst {
                args = [*args, *extension.buildArgs()]
            }
        }

        /** Intellij scope hack */
        project.tasks.create(name: 'addJcstressToTestScope', type: Test) {
            description = 'Adds jcstress to IDE test scope.'
            testClassesDir = project.sourceSets.jcstress.output.classesDir
            classpath = project.sourceSets.jcstress.runtimeClasspath
        }

        project.afterEvaluate {
            def hasIdea = project.plugins.findPlugin(org.gradle.plugins.ide.idea.IdeaPlugin)
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

}
