package com.github.erizo.gradle

import org.gradle.api.Project

class JcstressPluginExtension {
    def Project project;

    def jcstressDependency = 'org.openjdk.jcstress:jcstress-core:1.0-SNAPSHOT'

    def include = ".*";
    def includeTests = false;

    public JcstressPluginExtension(final Project project) {
        this.project = project;
    }

    def buildArgs() {
        [include]
    }
}
