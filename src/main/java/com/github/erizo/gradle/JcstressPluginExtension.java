package com.github.erizo.gradle;

import org.gradle.api.Project;
import org.gradle.api.model.ReplacedBy;

import java.util.ArrayList;
import java.util.List;

/**
 * @author jerzykrlk
 */
public class JcstressPluginExtension {

    public static final String JCSTRESS_DEFAULT_VERSION = "0.15";

    private Project project;
    private String jcstressDependency = "org.openjdk.jcstress:jcstress-core:" + JCSTRESS_DEFAULT_VERSION;
    private String language = "en";
    private Boolean includeTests = false;
    private String concurrency;
    private String deoptRatio;
    private String forks;
    private String forkMultiplier;
    private String iterations;
    private String jvmArgs;
    private String jvmArgsPrepend;
    private boolean list;
    private String mode;
    private String strideSize;
    private String strideCount;
    private String maxStride;
    private String minStride;
    private String reportDir;
    private boolean parse;
    private String cpuCount;
    private String regexp;
    private String timeMillis;
    private String verbose;
    private String spinStyle;
    private String heapPerFork;
    private boolean splitPerActor;
    private String affinityMode;

    public JcstressPluginExtension(final Project project) {
        this.project = project;
    }

    public List<String> buildArgs() {
        List<String> result = new ArrayList<>();
        addParameter(result, "-c", cpuCount);
        addParameter(result, "-deoptRatio", deoptRatio);
        addParameter(result, "-f", forks);
        addParameter(result, "-fsm", forkMultiplier);
        addParameter(result, "-iters", iterations);
        addParameter(result, "-jvmArgs", jvmArgs);
        addParameter(result, "-jvmArgsPrepend", jvmArgsPrepend);
        if (this.list) {
            addParameter(result, "-l", Boolean.toString(list));
        }
        addParameter(result, "-m", mode);
        addParameter(result, "-strideSize", strideSize);
        addParameter(result, "-strideCount", strideCount);
        addParameter(result, "-r", reportDir);
        if (this.parse) {
            addParameter(result, "-p", "true");
        }
        addParameter(result, "-t", regexp);
        addParameter(result, "-time", timeMillis);
        addParameter(result, "-v", verbose);
        addParameter(result, "-spinStyle", spinStyle);
        addParameter(result, "-hs", heapPerFork);
        addParameter(result, "-sc", Boolean.toString(splitPerActor));
        addParameter(result, "-af", affinityMode);
        return result;
    }

    public static void addParameter(List<String> result, String param, String value) {
        if ("-v".equals(param) && "true".equals(value)) {
            result.add(param);
        } else if (value != null) {
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

    @Deprecated
    @ReplacedBy("cpuCount")
    public String getConcurrency() {
        return concurrency;
    }

    @Deprecated
    public void setConcurrency(String concurrency) {
        this.concurrency = concurrency;
    }

    public boolean isList() {
        return list;
    }

    public void setList(boolean list) {
        this.list = list;
    }

    /**
     * Completely removed from jcstress. No direct replacement.
     */
    @Deprecated
    public String getDeoptRatio() {
        return deoptRatio;
    }

    @Deprecated
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

    @Deprecated
    @ReplacedBy("strideSize,strideCount")
    public String getMaxStride() {
        return maxStride;
    }

    @Deprecated
    public void setMaxStride(String maxStride) {
        this.maxStride = maxStride;
    }

    @Deprecated
    @ReplacedBy("strideSize,strideCount")
    public String getMinStride() {
        return minStride;
    }

    @Deprecated
    public void setMinStride(String minStride) {
        this.minStride = minStride;
    }

    public String getStrideSize() {
        return strideSize;
    }

    public void setStrideSize(String strideSize) {
        this.strideSize = strideSize;
    }

    public String getStrideCount() {
        return strideCount;
    }

    public void setStrideCount(String strideCount) {
        this.strideCount = strideCount;
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

    public String getSpinStyle() {
        return spinStyle;
    }

    public void setSpinStyle(String spinStyle) {
        this.spinStyle = spinStyle;
    }

    public String getHeapPerFork() {
        return heapPerFork;
    }

    public void setHeapPerFork(String heapPerFork) {
        this.heapPerFork = heapPerFork;
    }

    public String getForkMultiplier() {
        return forkMultiplier;
    }

    public void setForkMultiplier(String forkMultiplier) {
        this.forkMultiplier = forkMultiplier;
    }

    public String getJvmArgsPrepend() {
        return jvmArgsPrepend;
    }

    public void setJvmArgsPrepend(String jvmArgsPrepend) {
        this.jvmArgsPrepend = jvmArgsPrepend;
    }

    public Boolean getSplitPerActor() {
        return splitPerActor;
    }

    public void setSplitPerActor(Boolean splitPerActor) {
        this.splitPerActor = splitPerActor;
    }

    public String getAffinityMode() {
        return affinityMode;
    }

    public void setAffinityMode(String affinityMode) {
        this.affinityMode = affinityMode;
    }

    public boolean getParse() {
        return parse;
    }

    public void setParse(boolean parse) {
        this.parse = parse;
    }
}
