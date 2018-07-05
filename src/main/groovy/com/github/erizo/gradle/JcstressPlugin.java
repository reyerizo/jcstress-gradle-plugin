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
package com.github.erizo.gradle;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import groovy.lang.Closure;
import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.distribution.plugins.DistributionPlugin;
import org.gradle.api.file.CopySpec;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.Sync;
import org.gradle.api.tasks.bundling.Jar;
import org.gradle.plugins.ide.idea.IdeaPlugin;
import org.gradle.plugins.ide.idea.model.IdeaModel;
import org.gradle.plugins.ide.idea.model.IdeaModule;

/**
 * Configures the Jcstress Plugin.
 *
 * @author Cédric Champeau
 * @author jerzykrlk
 *
 */
public class JcstressPlugin implements Plugin<Project> {

    private static final String JCSTRESS_NAME = "jcstress";
    private static final String TASK_JCSTRESS_NAME = JCSTRESS_NAME;
    private static final String TASK_JCSTRESS_JAR_NAME = "jcstressJar";
    private static final String TASK_JCSTRESS_INSTALL_NAME = "jcstressInstall";
    private static final String TASK_JCSTRESS_SCRIPTS_NAME = "jcstressScripts";

    private Project project;

    private String jcstressApplicationName;

    private JcstressPluginGroovy jcstressPluginGroovy = new JcstressPluginGroovy();

    @Override
    public void apply(Project project) {
        this.project = project;
        this.jcstressApplicationName = project.getName() + "-jcstress";

        project.getPluginManager().apply(JavaPlugin.class);
        project.getPluginManager().apply(DistributionPlugin.class);

        final JcstressPluginExtension jcstressPluginExtension = project.getExtensions().create(JCSTRESS_NAME, JcstressPluginExtension.class, project);

        jcstressPluginGroovy.project = project;
        jcstressPluginGroovy.jcstressPluginExtension = jcstressPluginExtension;
        jcstressPluginGroovy.jcstressApplicationName = jcstressApplicationName;

        addJcstressConfiguration();

        addJcstressJarDependencies(jcstressPluginExtension);

        jcstressPluginGroovy.addJcstressSourceSet(jcstressPluginExtension);

        addJcstressJarTask(jcstressPluginExtension);

        addJcstressTask(jcstressPluginExtension);

        jcstressPluginGroovy.addCreateScriptsTask(jcstressPluginExtension);

        Sync installAppTask = jcstressPluginGroovy.addInstallAppTask();

        jcstressPluginGroovy.configureInstallTasks(installAppTask);

        updateIdeaPluginConfiguration();

    }

    private void addJcstressTask(final JcstressPluginExtension extension) {
        final JcstressTask task = project.getTasks().create(TASK_JCSTRESS_NAME, JcstressTask.class);

        task.dependsOn(project.getTasks().getByName("jcstressJar"));
        task.setMain("org.openjdk.jcstress.Main");
        task.setGroup("Verification");
        task.setDescription("Runs jcstress benchmarks.");
        task.setJvmArgs(new ArrayList<>(Arrays.asList("-XX:+UnlockDiagnosticVMOptions", "-XX:+WhiteBoxAPI", "-XX:-RestrictContended", "-Duser.language=" + extension.getLanguage())));
        task.setClasspath(project.getConfigurations().getByName("jcstress").plus(project.getConfigurations().getByName("jcstressRuntime").plus(project.getConfigurations().getByName("runtime"))));
        task.doFirst(task1 -> getAndCreateDirectory(project.getBuildDir(), "tmp", "jcstress"));

        project.afterEvaluate(project -> {
            if (extension.getReportDir() == null) {
                extension.setReportDir(getAndCreateDirectory(project.getBuildDir(), "reports", "jcstress").getAbsolutePath());
            }

            task.args(extension.buildArgs());
            Jar jcstressJarTask = (Jar) project.getTasks().getByName("jcstressJar");
            task.setProperty("classpath", task.getClasspath().plus(project.files(jcstressJarTask.getArchivePath())));
            filterConfiguration(project.getConfigurations().getByName("jcstress"), "jcstress-core");
            if (extension.getIncludeTests()) {
                task.setProperty("classpath", task.getClasspath().plus(project.getConfigurations().getByName("testRuntime")));
            }

            File path = getAndCreateDirectory(project.getBuildDir(), "tmp", "jcstress");
            task.setWorkingDir(path);
        });

        task.doFirst(task1 -> {
            JcstressTask jcstressTask = (JcstressTask) task1;
            jcstressTask.args(jcstressTask.jcstressArgs());
        });
    }

    private static File getAndCreateDirectory(File dir, String... subdirectory) {
        File path = Paths.get(dir.getPath(), subdirectory).toFile();
        path.mkdirs();
        return path;
    }

    private void addJcstressJarTask(final JcstressPluginExtension extension) {

        Action<CopySpec> jcstressExclusions = copySpec -> copySpec.exclude("**/META-INF/BenchmarkList", "**/META-INF/CompilerHints");

        Jar jarTask = project.getTasks().create(TASK_JCSTRESS_JAR_NAME, Jar.class);

        jarTask.dependsOn(project.getTasks().getByName("jcstressClasses"));
        jarTask.getInputs().dir(getProjectSourceSets().getByName("jcstress").getOutput());
        project.afterEvaluate(proj -> {
            jarTask.from(getProjectSourceSets().getByName("jcstress").getOutput());
            jarTask.from(getProjectSourceSets().getByName("main").getOutput(), jcstressExclusions);
            if (extension.getIncludeTests()) {
                jarTask.from(getProjectSourceSets().getByName("test").getOutput(), jcstressExclusions);
            }
        });

    }

    private void updateIdeaPluginConfiguration() {
        project.afterEvaluate(project -> {
            IdeaPlugin ideaPlugin = project.getPlugins().findPlugin(IdeaPlugin.class);
            if (ideaPlugin != null) {
                IdeaModel ideaModel = project.getExtensions().getByType(IdeaModel.class);
                IdeaModule module = ideaModel.getModule();
                module.getScopes()
                        .get("TEST")
                        .get("plus")
                        .add(project.getConfigurations().getByName("jcstress"));

                SourceSetContainer sourceSets = getProjectSourceSets();

                Set<File> jcstressSourceDirs = sourceSets.getByName("jcstress").getJava().getSrcDirs();
                Set<File> dirs = module.getTestSourceDirs();
                dirs.addAll(jcstressSourceDirs);
                module.setTestSourceDirs(dirs);
            }
        });
    }

    private SourceSetContainer getProjectSourceSets() {
        JavaPluginConvention plugin = project.getConvention().getPlugin(JavaPluginConvention.class);
        return plugin.getSourceSets();
    }

    private void addJcstressConfiguration() {
        project.getConfigurations().create(JCSTRESS_NAME);
    }

    private void addJcstressJarDependencies(final JcstressPluginExtension jcstressPluginExtension) {
        project.afterEvaluate(proj -> {
            addDependency(proj, "jcstress", jcstressPluginExtension.getJcstressDependency());
            addDependency(proj, "testCompile", jcstressPluginExtension.getJcstressDependency());
        });
    }

    private void addDependency(Project project, String configurationName, String dependencyName) {
        DependencyHandler dependencyHandler = this.project.getDependencies();
        Dependency dependency = dependencyHandler.create(dependencyName);

        project.getConfigurations().getByName(configurationName).getDependencies().add(dependency);
    }


    public static String getFileNameFromDependency(String gradleDependencyName) {
        String[] split = gradleDependencyName.split(":");
        return split[1] + "-" + split[2] + ".jar";
    }

    /**
     * Dummy method, loads configuration dependencies.
     * @param configuration configuration
     * @param jarFileName jar file name
     * @return ignored
     */
    private static Set<File> filterConfiguration(Configuration configuration, final String jarFileName) {
        return configuration.filter(it -> it.getName().contains(jarFileName)).getFiles();
    }

}
