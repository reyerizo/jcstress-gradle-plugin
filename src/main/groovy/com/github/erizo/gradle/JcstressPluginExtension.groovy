package com.github.erizo.gradle

import org.gradle.api.Project

/**
 * @author jerzykrlk
 */
class JcstressPluginExtension {
    def Project project

    def jcstressDependency = 'com.github.erizo.gradle:jcstress-core:1.0-20160519191500'

    def whiteboxApiDependency = null

    def includeTests = false

    def concurrency, deoptRatio, forks, iterations
    def jvmArgs, mode, maxStride, minStride, reportDir
    def cpuCount, regexp, timeMillis, verbose, yield

    public JcstressPluginExtension(final Project project) {
        this.project = project
    }

    def buildArgs() {
        def result = []
        addParameter(result, '-c', concurrency)
        addParameter(result, '-deoptRatio', deoptRatio)
        addParameter(result, '-f', forks)
        addParameter(result, '-iters', iterations)
        addParameter(result, '-jvmArgs', jvmArgs)
        addParameter(result, '-m', mode)
        addParameter(result, '-maxStride', maxStride)
        addParameter(result, '-minStride', minStride)
        addParameter(result, '-r', reportDir)
        addParameter(result, '-sc', cpuCount)
        addParameter(result, '-t', regexp)
        addParameter(result, '-time', timeMillis)
        addParameter(result, '-v', verbose)
        addParameter(result, '-yield', yield)
        result
    }

    static def addParameter(result, param, value) {
        if (param == '-v' && value == true) {
            result << param
        } else if (value != null) {
            result << param << value
        }
    }
}
