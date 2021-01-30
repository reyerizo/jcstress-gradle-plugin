package com.github.erizo.gradle;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import org.gradle.api.Project;

/**
 * @author jerzykrlk
 */
public class JcstressPluginExtension {

    private Project project;
    private String jcstressDependency = "org.openjdk.jcstress:jcstress-core:0.7";
    private String language = "en";
    private Boolean includeTests = false;
    private String concurrency;
    private String deoptRatio;
    private String forks;
    private String iterations;
    private String jvmArgs;
    private String mode;
    private String maxStride;
    private String minStride;
    private String reportDir;
    private String cpuCount;
    private String regexp;
    private String timeMillis;
    private String verbose;
    private String yield;

    public JcstressPluginExtension(final Project project) {
        this.project = project;
    }

    public List<String> buildArgs() {
        List<String> result = new ArrayList<>();
        addParameter(result, "-c", concurrency);
        addParameter(result, "-deoptRatio", deoptRatio);
        addParameter(result, "-f", forks);
        addParameter(result, "-iters", iterations);
        addParameter(result, "-jvmArgs", jvmArgs);
        addParameter(result, "-m", mode);
        addParameter(result, "-maxStride", maxStride);
        addParameter(result, "-minStride", minStride);
        addParameter(result, "-r", reportDir);
        addParameter(result, "-sc", cpuCount);
        addParameter(result, "-t", regexp);
        addParameter(result, "-time", timeMillis);
        addParameter(result, "-v", verbose);
        addParameter(result, "-yield", yield);
        return result;
    }

    public static void addParameter(List<String> result, String param, String value) {
        if ("-v".equals(param) && "true".equals(value)) {
            result.add(param);
        }
        else if (value != null) {
            result.add(param);
            result.add(value);
        }
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public String getJcstressDependency() {
        return jcstressDependency;
    }

    public void setJcstressDependency(String jcstressDependency) {
        this.jcstressDependency = jcstressDependency;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public Boolean getIncludeTests() {
        return includeTests;
    }

    public void setIncludeTests(Boolean includeTests) {
        this.includeTests = includeTests;
    }

    public String getConcurrency() {
        return concurrency;
    }

    public void setConcurrency(String concurrency) {
        this.concurrency = concurrency;
    }

    public String getDeoptRatio() {
        return deoptRatio;
    }

    public void setDeoptRatio(String deoptRatio) {
        this.deoptRatio = deoptRatio;
    }

    public String getForks() {
        return forks;
    }

    public void setForks(String forks) {
        this.forks = forks;
    }

    public String getIterations() {
        return iterations;
    }

    public void setIterations(String iterations) {
        this.iterations = iterations;
    }

    public String getJvmArgs() {
        return jvmArgs;
    }

    public void setJvmArgs(String jvmArgs) {
        this.jvmArgs = jvmArgs;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getMaxStride() {
        return maxStride;
    }

    public void setMaxStride(String maxStride) {
        this.maxStride = maxStride;
    }

    public String getMinStride() {
        return minStride;
    }

    public void setMinStride(String minStride) {
        this.minStride = minStride;
    }

    public String getReportDir() {
        return reportDir;
    }

    public void setReportDir(String reportDir) {
        this.reportDir = reportDir;
    }

    public String getCpuCount() {
        return cpuCount;
    }

    public void setCpuCount(String cpuCount) {
        this.cpuCount = cpuCount;
    }

    public String getRegexp() {
        return regexp;
    }

    public void setRegexp(String regexp) {
        this.regexp = regexp;
    }

    public String getTimeMillis() {
        return timeMillis;
    }

    public void setTimeMillis(String timeMillis) {
        this.timeMillis = timeMillis;
    }

    public String getVerbose() {
        return verbose;
    }

    public void setVerbose(String verbose) {
        this.verbose = verbose;
    }

    public String getYield() {
        return yield;
    }

    public void setYield(String yield) {
        this.yield = yield;
    }
}
