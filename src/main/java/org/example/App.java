package org.example;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.yarn.api.ApplicationConstants;
import org.apache.hadoop.yarn.api.protocolrecords.GetNewApplicationResponse;
import org.apache.hadoop.yarn.api.records.*;
import org.apache.hadoop.yarn.client.api.YarnClient;
import org.apache.hadoop.yarn.client.api.YarnClientApplication;
import org.apache.hadoop.yarn.conf.YarnConfiguration;
import org.apache.hadoop.yarn.util.Apps;
import org.apache.hadoop.yarn.util.ConverterUtils;
import org.apache.hadoop.yarn.util.Records;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class App {

    static Configuration conf = new YarnConfiguration();

    public static void main(String[] args) throws Exception {

        conf.set("fs.file.impl",
                org.apache.hadoop.fs.LocalFileSystem.class.getName()
        );

        // Create yarnClient
        YarnConfiguration conf = new YarnConfiguration();
        YarnClient yarnClient = YarnClient.createYarnClient();
        yarnClient.init(conf);
        yarnClient.start();

        YarnClientApplication app = yarnClient.createApplication();
        GetNewApplicationResponse appResponse = app.getNewApplicationResponse();

        ApplicationSubmissionContext appContext = app.getApplicationSubmissionContext();
        ApplicationId appId = appContext.getApplicationId();

        appContext.setKeepContainersAcrossApplicationAttempts(true);
        appContext.setApplicationName("harish_app");

        ContainerLaunchContext amContainer = Records.newRecord(ContainerLaunchContext.class);
        amContainer.setCommands(
                Collections.singletonList(
                        "$JAVA_HOME/bin/java" +
                                " org.example.App" +
                                // " -jar /Users/harishbohara/workspace/personal/yarn_application/target/yarn_application-1.0-SNAPSHOT-jar-with-dependencies.jar " +
                                // " " + String.valueOf(n) +
                                " 1>" + ApplicationConstants.LOG_DIR_EXPANSION_VAR + "/stdout" +
                                " 2>" + ApplicationConstants.LOG_DIR_EXPANSION_VAR + "/stderr"
                )
        );

        // Setup CLASSPATH for ApplicationMaster
        Map<String, String> appMasterEnv = new HashMap<>();
        setupAppMasterEnv(appMasterEnv);
        amContainer.setEnvironment(appMasterEnv);

        LocalResource appMasterJar = Records.newRecord(LocalResource.class);
        Path jarPath = new Path("file:///Users/harishbohara/workspace/personal/yarn_application/target/yarn_application-1.0-SNAPSHOT-jar-with-dependencies.jar");
        setupAppMasterJar(jarPath, appMasterJar);
        amContainer.setLocalResources(Collections.singletonMap("simpleapp.jar", appMasterJar));

        Resource capability = Records.newRecord(Resource.class);
        capability.setMemory(256);
        capability.setVirtualCores(1);

        appContext.setApplicationName("simple-yarn-app"); // application name
        appContext.setAMContainerSpec(amContainer);
        appContext.setResource(capability);
        appContext.setQueue("default"); // queue

        appId = appContext.getApplicationId();
        System.out.println("Submitting application " + appId);
        yarnClient.submitApplication(appContext);

        ApplicationReport appReport = yarnClient.getApplicationReport(appId);
        YarnApplicationState appState = appReport.getYarnApplicationState();
        while (appState != YarnApplicationState.FINISHED &&
                appState != YarnApplicationState.KILLED &&
                appState != YarnApplicationState.FAILED) {
            Thread.sleep(100);
            appReport = yarnClient.getApplicationReport(appId);
            appState = appReport.getYarnApplicationState();
            System.out.println(">>> " + appReport + " " + appState);
        }

        System.out.println(appResponse);
    }

    private static void setupAppMasterJar(Path jarPath, LocalResource appMasterJar) throws IOException {
        FileStatus jarStat = FileSystem.get(conf).getFileStatus(jarPath);
        appMasterJar.setResource(ConverterUtils.getYarnUrlFromPath(jarPath));
        appMasterJar.setSize(jarStat.getLen());
        appMasterJar.setTimestamp(jarStat.getModificationTime());
        appMasterJar.setType(LocalResourceType.FILE);
        appMasterJar.setVisibility(LocalResourceVisibility.PUBLIC);
    }

    private static void setupAppMasterEnv(Map<String, String> appMasterEnv) {
//        String classPathEnv = "$CLASSPATH:./*";
        String classPathEnv = "./*";
        appMasterEnv.put("CLASSPATH", classPathEnv);

        String[] defaultYarnAppClasspath = conf.getStrings(YarnConfiguration.YARN_APPLICATION_CLASSPATH);
        System.out.println("*** YARN_APPLICATION_CLASSPATH: " +
                Arrays.asList(defaultYarnAppClasspath != null ? defaultYarnAppClasspath : new String[]{}));

        for (String c : conf.getStrings(
                YarnConfiguration.YARN_APPLICATION_CLASSPATH,
                YarnConfiguration.DEFAULT_YARN_APPLICATION_CLASSPATH)) {
            System.out.println("--> " + c);
            Apps.addToEnvironment(appMasterEnv, ApplicationConstants.Environment.CLASSPATH.name(),
                    c.trim(), File.pathSeparator);
        }

//      Apps.addToEnvironment(appMasterEnv,
//          Environment.CLASSPATH.name(),
//          Environment.PWD.$() + File.separator + "*");

        System.out.println("*** APP MASTER ENV: " + appMasterEnv);
    }
}
