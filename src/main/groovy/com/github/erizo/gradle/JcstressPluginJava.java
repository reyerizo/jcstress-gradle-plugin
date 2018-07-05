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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import groovy.lang.Closure;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.codehaus.groovy.runtime.StringGroovyMethods;
import org.gradle.api.Action;
import org.gradle.api.AntBuilder;
import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.distribution.Distribution;
import org.gradle.api.distribution.plugins.DistributionPlugin;
import org.gradle.api.file.CopyProcessingSpec;
import org.gradle.api.file.CopySpec;
import org.gradle.api.file.FileCollection;
import org.gradle.api.internal.AbstractTask;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.Sync;
import org.gradle.api.tasks.application.CreateStartScripts;
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
public class JcstressPluginJava implements Plugin<Project> {

    private static final String JCSTRESS_NAME = "jcstress";
    private static final String TASK_JCSTRESS_NAME = JCSTRESS_NAME;
    private static final String TASK_JCSTRESS_JAR_NAME = "jcstressJar";
    private static final String TASK_JCSTRESS_INSTALL_NAME = "jcstressInstall";
    private static final String TASK_JCSTRESS_SCRIPTS_NAME = "jcstressScripts";

    private Project project;

    private String jcstressApplicationName;

    public void apply(Project project) {
        this.project = project;
        this.jcstressApplicationName = project.getName() + "-jcstress";

        project.getPluginManager().apply(JavaPlugin.class);
        project.getPluginManager().apply(DistributionPlugin.class);

        final JcstressPluginExtension jcstressPluginExtension = project.getExtensions().create(JCSTRESS_NAME, JcstressPluginExtension.class, project);

        addJcstressConfiguration();

        addJcstressJarDependencies(jcstressPluginExtension);

        addJcstressSourceSet(jcstressPluginExtension);

        addJcstressJarTask(jcstressPluginExtension);

        addJcstressTask(jcstressPluginExtension);

        addCreateScriptsTask(jcstressPluginExtension);

        Sync installAppTask = addInstallAppTask();

        configureInstallTasks(installAppTask);

        updateIdeaPluginConfiguration();
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

    private Configuration addJcstressConfiguration() {
        return project.getConfigurations().create(JCSTRESS_NAME);
    }

    private void updateIdeaPluginConfiguration() {
        project.afterEvaluate(proj -> {
            IdeaPlugin ideaPlugin = project.getPlugins().findPlugin(IdeaPlugin.class);
            if (ideaPlugin != null) {
                IdeaModel ideaModel = project.getExtensions().getByType(IdeaModel.class);
                IdeaModule module = ideaModel.getModule();
                module.getScopes()
                        .get("TEST")
                        .get("plus")
                        .add(proj.getConfigurations().getByName("jcstress"));

                SourceSetContainer sourceSets = getSourceSets();
                sourceSets.getByName("jcstress").getAllJava().getSrcDirs().forEach(it -> module.getTestSourceDirs().add(proj.file(it)));

            }
        });
    }

    private SourceSetContainer getSourceSets() {
        JavaPluginConvention plugin = project.getConvention().getPlugin(JavaPluginConvention.class);
        return plugin.getSourceSets();
    }

    private void addJcstressJarTask(final JcstressPluginExtension extension) {

        Action<CopySpec> jcstressExclusions = copySpec -> copySpec.exclude("**/META-INF/BenchmarkList", "**/META-INF/CompilerHints");

        Jar jarTask = project.getTasks().create(TASK_JCSTRESS_JAR_NAME, Jar.class);

        jarTask.dependsOn(project.getTasks().getByName("jcstressclasses"));
        jarTask.getInputs().dir(getSourceSets().getByName("jcstress").getOutput());
        project.afterEvaluate(proj -> {
            jarTask.from(getSourceSets().getByName("jcstress").getOutput());
            jarTask.from(getSourceSets().getByName("main").getOutput(), jcstressExclusions);
            if (extension.getIncludeTests()) {
                jarTask.from(getSourceSets().getByName("test").getOutput(), jcstressExclusions);
            }
        });

    }

    private Task addJcstressTask(final JcstressPluginExtension extension) {
//        LinkedHashMap<String, Object> map = new LinkedHashMap<String, Object>(2);
//        map.put("name", getProperty("TASK_JCSTRESS_NAME"));
//        map.put("type", getProperty("JcstressTask"));
//        final JcstressTask task = DefaultGroovyMethods.asType(project.getTasks().create(map), JcstressTask.class);
//
//        task.dependsOn(project.jcstressJar);
//        task.setMain("org.openjdk.jcstress.Main");
//        task.setGroup("Verification");
//        task.setDescription("Runs jcstress benchmarks.");
//        task.setJvmArgs(new ArrayList<String>(Arrays.asList("-XX:+UnlockDiagnosticVMOptions", "-XX:+WhiteBoxAPI", "-XX:-RestrictContended", "-Duser.language=" + extension.getLanguage())));
//        task.setClasspath(project.getConfigurations().jcstress.plus(project.getConfigurations().jcstressRuntime).plus(project.getConfigurations().runtime));
//        task.doFirst(new Closure<File>(this, this) {
//            public File doCall(Task it) {
//                return getAndCreateDirectory(project.getBuildDir(), "tmp", "jcstress");
//            }
//
//            public File doCall() {
//                return doCall(null);
//            }
//
//        });
//
//        project.afterEvaluate(new Closure<File>(this, this) {
//            public File doCall(Object project) {
//                if (!StringGroovyMethods.asBoolean(extension.getReportDir())) {
//                    extension.setReportDir(getAndCreateDirectory(((Project) project).getBuildDir(), "reports", "jcstress").getAbsolutePath());
//                }
//
//                task.setArgs(new ArrayList(Arrays.asList(, )));
//                task.setProperty("classpath", task.getClasspath().plus(((Project) project).files(project.jcstressJar.archivePath)));
//                filterConfiguration(((Project) project).getConfigurations().jcstress, "jcstress-core");
//                if (extension.getIncludeTests()) {
//                    task.setProperty("classpath", task.getClasspath().plus(((Project) project).getConfigurations().testRuntime));
//                }
//
//                File path = getAndCreateDirectory(((Project) project).getBuildDir(), "tmp", "jcstress");
//                task.setWorkingDir(path);
//                return path;
//            }
//
//        });
//
//
//        return task.doFirst(new Closure<List>(this, this) {
//            public List doCall(Task it) {
//                return setProperty1(JcstressPluginJava.this, "args", new ArrayList(Arrays.asList(, )));
//            }
//
//            public List doCall() {
//                return doCall(null);
//            }
//
//        });
        return null;
    }

    private static File getAndCreateDirectory(File dir, String... subdirectory) {
        File path = Paths.get(dir.getPath(), subdirectory).toFile();
        path.mkdirs();
        return ((File) (path));
    }

    public void configureInstallTasks(Sync installTask) {
//        installTask.doFirst(new Closure<Void>(this, this) {
//            public void doCall(Task it) {
//                if (destinationDir.directory.asBoolean()) {
//                    if (!new File((File) destinationDir, "lib").isDirectory() || !new File((File) destinationDir, "bin").isDirectory()) {
//                        throw new GradleException("The specified installation directory \'" + String.valueOf(getProperty("destinationDir")) + "\' is neither empty nor does it contain an installation for this application.\n".plus("If you really want to install to this directory, delete it and run the install task again.\n").plus("Alternatively, choose a different installation directory."));
//                    }
//
//                }
//
//            }
//
//            public void doCall() {
//                doCall(null);
//            }
//
//        });
//        installTask.doLast(new Closure<Object>(this, this) {
//            public Object doCall(Task it) {
//                LinkedHashMap<String, String> map = new LinkedHashMap<String, String>(2);
//                map.put("file", String.valueOf(getProperty("destinationDir").absolutePath) + "/bin/" + String.valueOf(getProperty("jcstressApplicationName")));
//                map.put("perm", "ugo+x");
//                return ((AntBuilder) project.getAnt()).chmod(map);
//            }
//
//            public Object doCall() {
//                return doCall(null);
//            }
//
//        });
    }

    private CopySpec configureDistSpec(CopySpec distSpec) {
//        final Task jar = project.getTasks().getAt(TASK_JCSTRESS_JAR_NAME);
//        final Task startScripts = project.getTasks().getAt(TASK_JCSTRESS_SCRIPTS_NAME);
//
//        DefaultGroovyMethods.with(distSpec, new Closure<CopySpec>(this, this) {
//            public CopySpec doCall(CopySpec it) {
//                from(project.file("src/dist"));
//
//                into("lib", new Closure<CopySpec>(JcstressPluginJava.this, JcstressPluginJava.this) {
//                    public CopySpec doCall(CopySpec it) {
//                        from(jar);
//                        return from(project.getConfigurations().jcstress.plus(project.getConfigurations().runtime));
//                    }
//
//                    public CopySpec doCall() {
//                        return doCall(null);
//                    }
//
//                });
//                return into("bin", new Closure<Integer>(JcstressPluginJava.this, JcstressPluginJava.this) {
//                    public Integer doCall(CopySpec it) {
//                        from(startScripts);
//                        return setFileMode0(JcstressPluginJava.this, 0755);
//                    }
//
//                    public Integer doCall() {
//                        return doCall(null);
//                    }
//
//                });
//            }
//
//            public CopySpec doCall() {
//                return doCall(null);
//            }
//
//        });
////        distSpec.with(pluginConvention.applicationDistribution)
//
//        return distSpec;
        return null;
    }

    private Sync addInstallAppTask() {
//        Distribution distribution = (Distribution) project.distributions.invokeMethod("create", new Object[]{"jcstress"});
//        distribution.setBaseName(jcstressApplicationName);
//        configureDistSpec(distribution.getContents());
//
//        Sync installTask = project.getTasks().create(TASK_JCSTRESS_INSTALL_NAME, Sync.class);
//        installTask.setDescription("Installs the project as a JVM application along with libs and OS specific scripts.");
//        installTask.setGroup("Verification");
//        installTask.with(distribution.getContents());
//        installTask.into(new Closure<File>(this, this) {
//            public File doCall(Object it) {
//                return project.file(String.valueOf(project.getBuildDir()) + "/install/" + jcstressApplicationName);
//            }
//
//            public File doCall() {
//                return doCall(null);
//            }
//
//        });
//        return ((Sync) (installTask));
        return null;
    }

    private void addJcstressSourceSet(final JcstressPluginExtension extension) {
//        DefaultGroovyMethods.invokeMethod(project, "sourceSets", new Object[]{new Closure<Object>(this, this) {
//            public Object doCall(Object it) {
//                return invokeMethod("jcstress", new Object[]{new Closure<Object>(DUMMY__1234567890_DUMMYYYYYY___.this, DUMMY__1234567890_DUMMYYYYYY___.this) {
//                    public void doCall(Object it) {
//                        setProperty("compileClasspath", getProperty("compileClasspath") + project.getConfigurations().jcstress + project.getConfigurations().compile + ((FileCollection) getProperty("main").output));
//                        setProperty("runtimeClasspath", getProperty("runtimeClasspath") + project.getConfigurations().jcstress + project.getConfigurations().runtime + ((FileCollection) getProperty("main").output));
//
//                        project.afterEvaluate(new Closure<Object>(DUMMY__1234567890_DUMMYYYYYY___.this, DUMMY__1234567890_DUMMYYYYYY___.this) {
//                            public Object doCall(Project it) {
//                                if (extension.includeTests.asBoolean()) {
//                                    setProperty("compileClasspath", getProperty("compileClasspath") + project.getConfigurations().testCompile + ((FileCollection) getProperty("test").output));
//                                    return setProperty2(JcstressPluginJava.this, "runtimeClasspath", getProperty("runtimeClasspath") + project.getConfigurations().testRuntime + ((FileCollection) getProperty("test").output));
//                                }
//
//                            }
//
//                            public Object doCall() {
//                                return doCall(null);
//                            }
//
//                        });
//                    }
//
//                    public void doCall() {
//                        doCall(null);
//                    }
//
//                }});
//            }
//
//            public Object doCall() {
//                return doCall(null);
//            }
//
//        }});
    }

    private void addCreateScriptsTask(final JcstressPluginExtension extension) {
//        project.getTasks().create(TASK_JCSTRESS_SCRIPTS_NAME, CreateStartScripts.class, new Closure<Object>(this, this) {
//            public void doCall(CreateStartScripts it) {
//                description = new ArrayList<String>(Arrays.asList("Creates OS specific scripts to run the project as a jcstress test suite."));
//                classpath = project.getTasks().getAt(getTASK_JCSTRESS_JAR_NAME()).getOutputs().getFiles().plus(project.getConfigurations().jcstress).plus(project.getConfigurations().runtime);
//                mainClassName = "org.openjdk.jcstress.Main";
//                applicationName = jcstressApplicationName;
//                outputDir = new File(project.getBuildDir(), "scripts");
//                defaultJvmOpts = new ArrayList<String>(Arrays.asList("-XX:+UnlockDiagnosticVMOptions", "-XX:+WhiteBoxAPI", "-XX:-RestrictContended", "-Duser.language=" + String.valueOf(extension.language)));
//
//                project.afterEvaluate(new Closure<Object>(JcstressPluginJava.this, JcstressPluginJava.this) {
//                    public Object doCall(Project it) {
//                        if (extension.includeTests.asBoolean()) {
//                            return setProperty2(JcstressPluginJava.this, "classpath", getProperty("classpath") + project.getConfigurations().testRuntime);
//                        }
//
//                    }
//
//                    public Object doCall() {
//                        return doCall(null);
//                    }
//
//                });
//            }
//
//            public void doCall() {
//                doCall(null);
//            }
//
//        });
    }

    public static String getFileNameFromDependency(String gradleDependencyName) {
        String[] split = gradleDependencyName.split(":");
        return split[1] + "-" + split[2] + ".jar";
    }

    public static String getJCSTRESS_NAME() {
        return JCSTRESS_NAME;
    }

    public static String getTASK_JCSTRESS_NAME() {
        return TASK_JCSTRESS_NAME;
    }

    public static String getTASK_JCSTRESS_JAR_NAME() {
        return TASK_JCSTRESS_JAR_NAME;
    }

    public static String getTASK_JCSTRESS_INSTALL_NAME() {
        return TASK_JCSTRESS_INSTALL_NAME;
    }

    public static String getTASK_JCSTRESS_SCRIPTS_NAME() {
        return TASK_JCSTRESS_SCRIPTS_NAME;
    }

    private static <Value> Value setProperty1(AbstractTask propOwner, String name, Value value) {
        propOwner.setProperty(name, value);
        return value;
    }

    private static <Value extends Integer> Value setFileMode0(CopyProcessingSpec propOwner, Value var1) {
        propOwner.setFileMode(var1);
        return var1;
    }

    private static <Value> Value setProperty2(Project propOwner, String var1, Value var2) {
        propOwner.setProperty(var1, var2);
        return var2;
    }

//    private static <Value> Value setProperty0(DUMMY__1234567890_DUMMYYYYYY___ propOwner, String s, Value o) {
//        ((DUMMY__1234567890_DUMMYYYYYY___) propOwner).setProperty(s, o);
//        return o;
//    }
}
