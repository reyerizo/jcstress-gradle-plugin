package com.github.erizo.gradle

import org.gradle.api.internal.tasks.options.Option
import org.gradle.api.tasks.JavaExec

class JcstressTask extends JavaExec {

    private String jcstressTestName

    @Option(option = "tests", description = "JCstress tests to execute.")
    public void setJcstressTestName(String jcstressTestName) {
        this.jcstressTestName = jcstressTestName
    }

    public String getJcstressTestName() {
        return jcstressTestName
    }

    public List<String> jcstressArgs() {
        def result = []
        if (jcstressTestName != null) {
            return ["-t", jcstressTestName]
        }
        return result
    }

}
