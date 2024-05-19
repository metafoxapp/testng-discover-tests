package com.metafox.qatools;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.testng.xml.XmlClass;
import org.testng.xml.XmlSuite;
import org.testng.xml.XmlTest;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Mojo(name = "testng-discover-tests", defaultPhase = LifecyclePhase.COMPILE)
public class TestngDiscoverTestsMojo extends AbstractMojo {
    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    MavenProject project;

    private String path;

    /**
     * package prefix of discover, etc: com.metafox.app
     */
    @Parameter(property = "glue", required = true)
    private String glue;

    @Parameter(property = "sourceDir", required = true)
    private String sourceDir;

    /**
     * Class name pattern: etc: glob:*CucumberTests.{java}
     */
    private String pattern;

    final private XmlSuite xmlSuite = new XmlSuite();

    final private ArrayList<XmlClass> xmlClasses = new ArrayList<>();
    final private XmlTest test = new XmlTest();

    private String testName;

    public void execute() {
        List<?> dependencies = project.getDependencies();
        File baseDir = project.getBasedir();
        getLog().info("Project name " + project.getName());
        getLog().info("Project baseDir " + baseDir);
        String filter = System.getProperty("filter");
        this.path = baseDir + "/" + sourceDir;
        this.glue = "com.metafox.app";
        this.pattern = "glob:*" + filter + ".java";
        this.testName = filter.replace("Tests", "").replace("*","");
        String name = project.getProperties().getProperty("suiteFile", "src/test/resource/local.testng.xml");
        File suiteFile = Paths.get(baseDir.toString(), name).toFile();
        discoverTests();
        writeSuiteToFile(suiteFile);
        getLog().info("Filter by " + this.pattern);
        getLog().info("Using dynamic suite file " + suiteFile.getPath());
    }

    public void writeSuiteToFile(File suiteFile) {
        try {
            if (suiteFile.exists()) {
                if (!suiteFile.delete()) {
                    throw new RuntimeException("Could not remove test file " + suiteFile.toString());
                }
            }
            if (!suiteFile.getParentFile().exists()) {
                if (!suiteFile.getParentFile().mkdirs()) {
                    throw new RuntimeException("Could not make directory " + suiteFile.getParentFile().toString());
                }
            }
            PrintWriter out = new PrintWriter(suiteFile.toString());
            out.write(xmlSuite.toXml());
            out.close();

        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }


    public String[] getAvailableAppNames(File dirName) {
        return dirName.list((current, name) -> new File(current, name).isDirectory());
    }

    public void addDynamicTestXml(String appName) {
        List<Path> files = PathReader.listFiles(Paths.get(path, appName), pattern);

        if (files.isEmpty()) {
            return;
        }
        for (Path file : files) {
            String className = String.format("%s.%s.%s", glue, appName, file.getFileName().toString().replace(".java", ""));
            xmlClasses.add(new XmlClass(className, false));
        }
    }

    public void discoverTests() {
        test.setSuite(xmlSuite);
        test.setName(testName);
        test.setParallel(XmlSuite.ParallelMode.CLASSES);
        ArrayList<String> apps = getApps(path);
        getLog().info(apps.toString());
        apps.forEach(this::addDynamicTestXml);
        test.setClasses(xmlClasses);
        xmlSuite.addTest(test);
        xmlSuite.setParallel(XmlSuite.ParallelMode.TESTS);
        xmlSuite.setName(testName);
        xmlSuite.setThreadCount(getThreadCount());
    }

    public int getThreadCount() {
        return Integer.parseInt(System.getProperty("threads", "4"));
    }

    private ArrayList<String> getApps(String path) {
        File dirName = new File(path);

        ArrayList<String> apps = new ArrayList<>(Arrays.asList(getAvailableAppNames(dirName)));
        String filter = System.getProperty("app");

        if (Objects.isNull(filter) || filter.trim().isEmpty()) {
            return apps;
        }

        if (filter.startsWith("~")) {
            Arrays.stream(filter.substring(1).split(","))
                    .map(x -> x.trim().toLowerCase())
                    .forEach(x -> {
                        apps.removeIf(r -> r.equals(x));
                    });
            return apps;
        }

        ArrayList<String> enableApps = new ArrayList<>();
        Arrays.stream(filter.split(","))
                .map(x -> x.trim().toLowerCase())
                .filter(x -> !x.isEmpty())
                .forEach(enableApps::add);

        if (!enableApps.isEmpty()) {
            apps.removeIf(r -> !enableApps.contains(r));
        }

        return apps;
    }
}