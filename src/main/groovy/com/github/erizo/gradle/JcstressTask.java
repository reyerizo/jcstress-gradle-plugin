package com.github.erizo.gradle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.gradle.api.tasks.JavaExec;
import org.gradle.api.tasks.options.Option;

public class JcstressTask extends JavaExec {

    private String jcstressTestName;

    @Option(option = "tests", description = "JCstress tests to execute.")
    public void setJcstressTestName(String jcstressTestName) {
        this.jcstressTestName = jcstressTestName;
    }

    public String getJcstressTestName() {
        return jcstressTestName;
    }

    public List<String> jcstressArgs() {
        List<String> result = new ArrayList<>();
        if (jcstressTestName != null) {
            return new ArrayList<>(Arrays.asList("-t", jcstressTestName));
        }

        return result;
    }

}
