package org.jenkinsci.plugins.gogs;

import com.offbytwo.jenkins.JenkinsServer;
import com.offbytwo.jenkins.model.Job;
import com.offbytwo.jenkins.model.QueueReference;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URI;

import static org.eclipse.jgit.lib.ConfigConstants.*;
import static org.jenkinsci.plugins.gogs.JenkinsHandler.waitUntilJenkinsHasBeenStartedUp;
import static org.junit.Assert.fail;

//TODO: Update and push local repository to Gogs

public class GogsWebHook_IT {
    public static final String JENKINS_URL = "http://localhost:8080/";
    public static final String JENKINS_USER = "butler";
    public static final String JENKINS_PASSWORD = "butler";
    public static final String GOGS_URL = "http://localhost:3000";
    public static final String GOGS_USER = "butler";
    public static final String GOGS_PASSWORD = "butler";
    public static final String WEBHOOK_URL = "http://localhost:8080/job/testRep1/build?delay=0";
    public static final String JSON_COMMANDFILE_PATH = "target/test-classes/Gogs-config-json/";
    final Logger log = LoggerFactory.getLogger(GogsWebHook_IT.class);

    @Rule
    public TestRule watcher = new TestWatcher() {
        protected void starting(Description description) {
            GogsWebHook_IT.this.log.info("\t **************  Start Test ({})  *******************", description
                    .getMethodName());
        }
    };

    @Test
    public void smokeTest() throws Exception {
        GogsConfigHandler gogsServer = new GogsConfigHandler(GOGS_URL, GOGS_USER, GOGS_PASSWORD);

        gogsServer.waitForServer(5, 5);

        File jsonCommandFile = new File(JSON_COMMANDFILE_PATH + "webHookDefinition_1.json");
        int hookId = gogsServer.createWebHook(jsonCommandFile, "demoApp");
        log.info("Created hook with ID " + hookId);

        gogsServer.removeHook("demoApp", hookId);

        //Create the test repository on the server
        try {
            gogsServer.createEmptyRepo("testRep1");
        } catch (IOException e) {
            //check for the exist message;
            if (e.getMessage().contains("422")) {
                log.warn("GOGS Repo already exists. Trying to continue.");
            } else {
                fail("Unexpected error creating GOGS repo: " + e.getMessage());
            }
        }

        //initialize local Git repository used for tests
        File testRepoDir = new File("target/test-repos/demo-app");
        Git git = Git.init().setDirectory(testRepoDir).call();

        //Configure user, email, remote and tracking branch
        StoredConfig config = git.getRepository().getConfig();
        config.setString(CONFIG_USER_SECTION, null, "name", "Automated Test");
        config.setString(CONFIG_USER_SECTION, null, "email", "test@test.org");
        config.setString(CONFIG_REMOTE_SECTION, "origin", "url", "http://localhost:3000/butler/testRep1.git");
        config.setString(CONFIG_REMOTE_SECTION, "origin", "fetch", "+refs/heads/*:refs/remotes/origin/*");
        config.setString(CONFIG_BRANCH_SECTION, "master", "remote", "origin");
        config.setString(CONFIG_BRANCH_SECTION, "master", "merge", "refs/heads/master");
        config.save();


        //add the files located there and commit them
        Status status = git.status().call();
        DirCache index = git.add().addFilepattern(".").call();
        RevCommit commit = git.commit().setMessage("Repos initialization").call();


        //push
        UsernamePasswordCredentialsProvider user2 = new UsernamePasswordCredentialsProvider("butler", "butler");

        RefSpec spec = new RefSpec("refs/heads/master:refs/heads/master");
        Iterable<PushResult> resultIterable = git.push()
                .setRemote("origin")
                .setCredentialsProvider(user2)
                .setRefSpecs(spec)
                .call();

        JenkinsServer jenkins = new JenkinsServer(new URI(JENKINS_URL), JENKINS_USER, JENKINS_PASSWORD);

        waitUntilJenkinsHasBeenStartedUp(jenkins);

        Job job = jenkins.getJob("test project");
        if (job == null) {
            //create job
            fail("Job is missing");
//            jenkins.createJob("test",configXml);
        }
        QueueReference ref = job.build();

        String configXml = jenkins.getJobXml("test project");
        log.info("Job config: " + configXml);
        String marker = job.getFileFromWorkspace("artifact/marker.txt");

//        downloadArtifact


//<?xml version='1.0' encoding='UTF-8'?>
//<flow-definition plugin="workflow-job@2.11">
//  <actions/>
//  <description></description>
//  <keepDependencies>false</keepDependencies>
//  <properties>
//    <org.jenkinsci.plugins.workflow.job.properties.PipelineTriggersJobProperty>
//      <triggers/>
//    </org.jenkinsci.plugins.workflow.job.properties.PipelineTriggersJobProperty>
//  </properties>
//  <definition class="org.jenkinsci.plugins.workflow.cps.CpsScmFlowDefinition" plugin="workflow-cps@2.30">
//    <scm class="hudson.plugins.git.GitSCM" plugin="git@3.3.0">
//      <configVersion>2</configVersion>
//      <userRemoteConfigs>
//        <hudson.plugins.git.UserRemoteConfig>
//          <url>http://gitserver:3000/butler/testRep1</url>
//          <credentialsId>GOGS-USER</credentialsId>
//        </hudson.plugins.git.UserRemoteConfig>
//      </userRemoteConfigs>
//      <branches>
//        <hudson.plugins.git.BranchSpec>
//          <name>*/master</name>
//        </hudson.plugins.git.BranchSpec>
//      </branches>
//      <doGenerateSubmoduleConfigurations>false</doGenerateSubmoduleConfigurations>
//      <submoduleCfg class="list"/>
//      <extensions/>
//    </scm>
//    <scriptPath>Jenkinsfile</scriptPath>
//    <lightweight>false</lightweight>
//  </definition>
//  <triggers/>
//  <disabled>false</disabled>
//</flow-definition>
//
    }


}