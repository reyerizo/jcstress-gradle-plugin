package com.github.erizo.gradle

import org.gradle.api.Project

/**
 * @author jerzykrlk
 */
class JcstressPluginExtension {
    def Project project;

    def jcstressDependency = 'com.github.erizo.gradle:org.openjdk.jcstress.jcstress-core:1.0-20150705182407'

    def include = ".*";
    def includeTests = false;

    public JcstressPluginExtension(final Project project) {
        this.project = project;
    }

    def buildArgs() {
        [include]
    }
}
