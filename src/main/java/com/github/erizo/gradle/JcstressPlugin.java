/**
 * Copyright 2015 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.erizo.gradle;

import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.distribution.Distribution;
import org.gradle.api.distribution.DistributionContainer;
import org.gradle.api.distribution.plugins.DistributionPlugin;
import org.gradle.api.file.CopySpec;
import org.gradle.api.file.FileCollection;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.Sync;
import org.gradle.api.tasks.application.CreateStartScripts;
import org.gradle.api.tasks.bundling.Jar;
import org.gradle.plugins.ide.idea.IdeaPlugin;
import org.gradle.plugins.ide.idea.model.IdeaModel;
import org.gradle.plugins.ide.idea.model.IdeaModule;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;

/**
 * Configures the Jcstress Plugin.
 *
 * @author Cédric Champeau
 * @author jerzykrlk
 */
public class JcstressPlugin implements Plugin<Project> {

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

	private void addCreateScriptsTask(final JcstressPluginExtension extension) {
		CreateStartScripts createStartScripts = project.getTasks().create(TASK_JCSTRESS_SCRIPTS_NAME, CreateStartScripts.class);
		createStartScripts.setDescription("Creates OS specific scripts to run the project as a jcstress test suite.");
		createStartScripts.setClasspath(
				project.getTasks().getByName(TASK_JCSTRESS_JAR_NAME).getOutputs().getFiles()
						.plus(project.getConfigurations().getByName("jcstress"))
						.plus(project.getConfigurations().getByName("runtime"))
		);

		createStartScripts.setMainClassName("org.openjdk.jcstress.Main");
		createStartScripts.setApplicationName(jcstressApplicationName);
		createStartScripts.setOutputDir(new File(project.getBuildDir(), "scripts"));
		createStartScripts.setDefaultJvmOpts(new ArrayList<>(Arrays.asList(
				"-XX:+UnlockDiagnosticVMOptions",
				"-XX:+WhiteBoxAPI",
				"-XX:-RestrictContended",
				"-Duser.language=" + extension.getLanguage())));

		project.afterEvaluate(it -> {
			if (extension.getIncludeTests()) {
				FileCollection classpath = createStartScripts.getClasspath();
				createStartScripts.setClasspath(classpath.plus(project.getConfigurations().getByName("testRuntime")));
			}
		});
	}

	public void configureInstallTasks(Sync installTask) {
		installTask.doFirst(task -> {
			File destinationDir = installTask.getDestinationDir();
			if (destinationDir.isDirectory()) {
				if (!new File(destinationDir, "lib").isDirectory() || !new File(destinationDir, "bin").isDirectory()) {
					throw new GradleException("The specified installation directory \'" + destinationDir
							+ "\' is neither empty nor does it contain an installation for this application.\n"
							+ "If you really want to install to this directory, delete it and run the install task again.\n"
							+ "Alternatively, choose a different installation directory.");
				}
			}
		});

		installTask.doLast(task ->
		{
			Path bin = Paths.get(installTask.getDestinationDir().getAbsolutePath(), "bin", jcstressApplicationName);
			try {
				Set<PosixFilePermission> posixFilePermissions = PosixFilePermissions.fromString("ugo+x");
				Files.setPosixFilePermissions(bin, posixFilePermissions);
			} catch (IOException e) {
				throw new UncheckedIOException("Failed to update attributes of [" + bin + "]", e);
			}
		});
	}

	private Sync addInstallAppTask() {
		DistributionContainer distributions = (DistributionContainer) project.getExtensions().getByName("distributions");
		Distribution distribution = distributions.create("jcstress");
		distribution.setBaseName(jcstressApplicationName);
		configureDistSpec(distribution.getContents());

		Sync installTask = project.getTasks().create(TASK_JCSTRESS_INSTALL_NAME, Sync.class);
		installTask.setDescription("Installs the project as a JVM application along with libs and OS specific scripts.");
		installTask.setGroup("Verification");
		installTask.with(distribution.getContents());
		installTask.into(project.file(project.getBuildDir() + "/install/" + jcstressApplicationName));

		return installTask;
	}

	private void configureDistSpec(CopySpec distSpec) {
		final Task jar = project.getTasks().getByName(TASK_JCSTRESS_JAR_NAME);
		final Task startScripts = project.getTasks().getByName(TASK_JCSTRESS_SCRIPTS_NAME);

		CopySpec copy = project.copySpec();
		copy.from(project.file("src/dist"));
		copy.into("lib", cs -> {
			cs.from(jar);
			cs.from(project.getConfigurations().getByName("jcstress").plus(project.getConfigurations().getByName("runtime")));
		});

		copy.into("bin", cs -> {
			cs.from(startScripts);
			cs.setFileMode(0755);
		});

		distSpec.with(copy);
	}

	private void addJcstressSourceSet(final JcstressPluginExtension extension) {

		SourceSet jcstress = getProjectSourceSets().create("jcstress");

		FileCollection compileClasspath = jcstress.getCompileClasspath()
				.plus(project.getConfigurations().getByName("jcstress"))
				.plus(project.getConfigurations().getByName("compile"))
				.plus(getProjectSourceSets().getByName("main").getOutput());
		jcstress.setCompileClasspath(compileClasspath);

		FileCollection runtimeClasspath = jcstress.getRuntimeClasspath()
				.plus(project.getConfigurations().getByName("jcstress"))
				.plus(project.getConfigurations().getByName("runtime"))
				.plus(getProjectSourceSets().getByName("main").getOutput());
		jcstress.setRuntimeClasspath(runtimeClasspath);

		project.afterEvaluate(proj -> {
			if (extension.getIncludeTests()) {

				// TODO - back to java, check the mutability
				FileCollection ccp = jcstress.getCompileClasspath();
				ccp = ccp
						.plus(proj.getConfigurations().getByName("testCompile"))
						.plus(getProjectSourceSets().getByName("test").getOutput());
				jcstress.setCompileClasspath(ccp);

				FileCollection rcp = jcstress.getRuntimeClasspath();
				rcp = rcp
						.plus(proj.getConfigurations().getByName("testRuntime"))
						.plus(getProjectSourceSets().getByName("test").getOutput());
				jcstress.setRuntimeClasspath(rcp);
			}
		});
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
	 *
	 * @param configuration configuration
	 * @param jarFileName   jar file name
	 * @return ignored
	 */
	private static Set<File> filterConfiguration(Configuration configuration, final String jarFileName) {
		return configuration.filter(it -> it.getName().contains(jarFileName)).getFiles();
	}

}
