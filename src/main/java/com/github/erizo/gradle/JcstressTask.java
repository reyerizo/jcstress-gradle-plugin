package com.github.erizo.gradle;

import java.util.ArrayList;
import java.util.List;

import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.JavaExec;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.options.Option;

public class JcstressTask extends JavaExec {

    private String jcstressTestName;

    @Option(option = "tests", description = "JCstress tests to execute.")
    public void setJcstressTestName(String jcstressTestName) {
        this.jcstressTestName = jcstressTestName;
    }

    @Input
    @Optional
    public String getJcstressTestName() {
        return jcstressTestName;
    }

    public List<String> jcstressArgs() {
        List<String> result = new ArrayList<>();
        if (jcstressTestName != null) {
            result.add("-t");
            result.add(jcstressTestName);
        }
        return result;
    }

}
