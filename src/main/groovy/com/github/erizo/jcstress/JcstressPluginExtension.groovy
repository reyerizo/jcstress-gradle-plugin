package com.github.erizo.jcstress

import org.gradle.api.Project

/**
 * Created by jerzy on 2015-07-05.
 */
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
