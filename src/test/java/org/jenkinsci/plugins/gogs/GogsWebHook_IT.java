package org.jenkinsci.plugins.gogs;

import static org.eclipse.jgit.lib.ConfigConstants.CONFIG_BRANCH_SECTION;
import static org.eclipse.jgit.lib.ConfigConstants.CONFIG_REMOTE_SECTION;
import static org.eclipse.jgit.lib.ConfigConstants.CONFIG_USER_SECTION;
import static org.jenkinsci.plugins.gogs.JenkinsHandler.waitUntilJenkinsHasBeenStartedUp;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.Writer;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeoutException;

import com.offbytwo.jenkins.JenkinsServer;
import com.offbytwo.jenkins.model.Artifact;
import com.offbytwo.jenkins.model.BuildWithDetails;
import com.offbytwo.jenkins.model.Job;
import com.offbytwo.jenkins.model.JobWithDetails;
import org.apache.commons.io.IOUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//FIXME: the test should run in sequence

public class GogsWebHook_IT {

    private static final String JENKINS_URL = "http://localhost:8080/";
    private static final String JENKINS_USER = "butler";
    private static final String JENKINS_PASSWORD = "butler";
    private static final String GOGS_URL = "http://localhost:3000";
    private static final String GOGS_USER = "butler";
    private static final String GOGS_PASSWORD = "butler";
    private static final String JSON_COMMANDFILE_PATH = "target/test-classes/Gogs-config-json/";
    private static final String JENKINS_CONFIGS_PATH = "target/test-classes/Jenkins-config/";
    private static final String JENKINS_JOB_NAME = "test project";
    final Logger log = LoggerFactory.getLogger(GogsWebHook_IT.class);

    @Rule
    public TestRule watcher = new TestWatcher() {
        protected void starting(Description description) {
            GogsWebHook_IT.this.log.info("\t **************  Start Test ({})  *******************", description
                    .getMethodName());
        }
    };

    @Test
    public void smokeTest_build_masterBranch() throws Exception {
        final String projectName = "testRep1";

        doItTest(JENKINS_JOB_NAME,
                 projectName,
                "test-project.xml",
                "webHookDefinition.json",
                null);
    }

    @Test
    public void smokeTest_build_testBranch() throws Exception {
        final String projectName = "testRep2";
        doItTest(JENKINS_JOB_NAME + "2",
                 projectName,
                "test-project-branch.xml",
                "webHookDefinition_branch.json",
                "test");
    }

    /**
     * Do integration tests
     *
     * @param jenkinsJobName Jenkins jobname
     * @param projectName Gogs project name
     * @param jenkinsXML XML file name with Jenkins job definition
     * @param webhookDefinition JSON file with webhook definition
     * @param branch Branch name to do test on (null means master)
     * @throws Exception Something unexpected went wrong
     */
    private void doItTest(final String jenkinsJobName,
                          final String projectName,
                          final String jenkinsXML,
                          final String webhookDefinition,
                          final String branch) throws Exception {
        // Default refspec is master
        StringBuilder refSpec = new StringBuilder("refs/heads/master:refs/heads/");

        //Instantiate the Gogs Handler object and wait for the server to be available
        GogsConfigHandler gogsServer = new GogsConfigHandler(GOGS_URL, GOGS_USER, GOGS_PASSWORD);
        gogsServer.waitForServer(12, 5);

        //Create the test repository on the server
        try {
            gogsServer.createEmptyRepo(projectName);
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
        config.setString(CONFIG_REMOTE_SECTION, "origin", "url", "http://localhost:3000/butler/"+projectName+".git");
        config.setString(CONFIG_REMOTE_SECTION, "origin", "fetch", "+refs/heads/*:refs/remotes/origin/*");
        config.setString(CONFIG_BRANCH_SECTION, "master", "remote", "origin");
        config.setString(CONFIG_BRANCH_SECTION, "master", "merge", "refs/heads/master");
        config.save();

        //add the files located there and commit them
        Status status = git.status().call();
        DirCache index = git.add().addFilepattern(".").call();
        RevCommit commit = git.commit().setMessage("Repos initialization").call();
        log.info("Commit" + commit.getName());

        //push
        UsernamePasswordCredentialsProvider user2 = new UsernamePasswordCredentialsProvider("butler", "butler");

        if (branch == null) {
            refSpec.append("master");
        } else {
            refSpec.append(branch);
        }

        RefSpec spec = new RefSpec(refSpec.toString());
        git.push()
           .setRemote("origin")
           .setCredentialsProvider(user2)
           .setRefSpecs(spec)
           .setPushAll()
           .call();

        //Setup the Jenkins job
        JenkinsServer jenkins = new JenkinsServer(new URI(JENKINS_URL), JENKINS_USER, JENKINS_PASSWORD);
        waitUntilJenkinsHasBeenStartedUp(jenkins);

        //Check if the job exist. If not create it.
        Job job = jenkins.getJob(jenkinsJobName);
        if (job == null) {
            //Read the job configuration into a string
            File jenkinsConfigFile = new File(JENKINS_CONFIGS_PATH + jenkinsXML);
            byte[] encoded = Files.readAllBytes(jenkinsConfigFile.toPath());
            String configXml = new String(encoded, Charset.defaultCharset());

            jenkins.createJob(jenkinsJobName, configXml);
        }

        //Get the expected build number
        JobWithDetails jobAtIntitalState = jenkins.getJob(jenkinsJobName);
        int expectedBuildNbr = jobAtIntitalState.getNextBuildNumber();
        log.info("Next build number: " + expectedBuildNbr);

        //Build the job
        jobAtIntitalState.build(true);

        //Wait for the job to complete
        long timeOut = 60000L;
        waitForBuildToComplete(jenkins, expectedBuildNbr, timeOut, jenkinsJobName);

        //Get the data we stored in the marker file and check it
        Properties markerAsProperty = loadMarkerArtifactAsProperty(jenkins, jenkinsJobName);
        String buildedCommit = markerAsProperty.getProperty("GIT_COMMIT");

        assertEquals("Not the expected GIT commit", commit.getName(), buildedCommit);

        //add the trigger to Gogs
        File jsonCommandFile = new File(JSON_COMMANDFILE_PATH + webhookDefinition);
        int hookId = gogsServer.createWebHook(jsonCommandFile, projectName);
        log.info("Created hook with ID " + hookId);

        //Get what is the next build number of the test jenkins job
        jobAtIntitalState = jenkins.getJob(jenkinsJobName);
        expectedBuildNbr = jobAtIntitalState.getNextBuildNumber();

        //change the source file
        changeTheSourceFile("target/test-repos/demo-app/README.md");

        //commit and push the changed file
        git.add().addFilepattern(".").call();
        RevCommit commitForHook = git.commit().setMessage("Small test modification").call();
        log.info("Commit" + commitForHook.getName());
        git.push()
           .setRemote("origin")
           .setCredentialsProvider(user2)
           .setRefSpecs(spec)
           .setPushAll()
           .call();

        try {
            //wait for the build
            waitForBuildToComplete(jenkins, expectedBuildNbr, timeOut, jenkinsJobName);

            //Get the data we stored in the marker file and check it
            Properties hookMarkerAsProperty = loadMarkerArtifactAsProperty(jenkins, jenkinsJobName);
            String hookBuildedCommit = hookMarkerAsProperty.getProperty("GIT_COMMIT");
            assertEquals("Not the expected GIT commit", commitForHook.getName(), hookBuildedCommit);
        } finally {
            // Cleanup the mess we made
            gogsServer.removeHook(projectName, hookId);
            gogsServer.removeRepo(projectName);
        }
    }

    /**
     * Loads the marker file of the last build (archived during the build)
     *
     * @param jenkins the jenkins instance we want to load from
     * @return the marker file loaded as a property file (so that it can be easily queried)
     * @throws IOException        Something unexpected went wrong when querying the Jenkins server
     * @throws URISyntaxException Something unexpected went wrong loading the marker as a property
     */
    private Properties loadMarkerArtifactAsProperty(JenkinsServer jenkins, String jobName) throws IOException, URISyntaxException {
        JobWithDetails detailedJob = jenkins.getJob(jobName);
        BuildWithDetails lastBuild = detailedJob.getLastBuild().details();
        int buildNbr = lastBuild.getNumber();
        boolean isBuilding = lastBuild.isBuilding();
        log.info("BuildNbr we are examining: " + buildNbr);

        List<Artifact> artifactList = lastBuild.getArtifacts();
        assertEquals("Not the expected number of artifacts", 1, artifactList.size());

        Artifact markerArtifact = artifactList.get(0);
        String markerArtifactFileName = markerArtifact.getFileName();
        assertEquals("The artifact is not the expected one", "marker.txt", markerArtifactFileName);
        InputStream markerArtifactInputStream = lastBuild.details().downloadArtifact(markerArtifact);
        String markerAsText = IOUtils.toString(markerArtifactInputStream, Charset.defaultCharset());
        log.info("\n" + markerAsText);
        StringReader reader = new StringReader(markerAsText);
        Properties markerAsProperty = new Properties();
        markerAsProperty.load(reader);

        //check if the marker matches the build number we expect.
        String buildNbrFromMarker = markerAsProperty.getProperty("BUILD_NUMBER");
        String buildNbrFromQery = String.valueOf(buildNbr);
        assertEquals("The build number from the marker does not match the last build number", buildNbrFromMarker, buildNbrFromQery);
        return markerAsProperty;
    }

    /**
     * Introduces a change in the source file
     *
     * @param fileName the source file to modify
     * @throws IOException something went wrong when updating the file
     */
    private void changeTheSourceFile(@SuppressWarnings("SameParameterValue") String fileName) throws IOException {
        Writer output;
        output = new BufferedWriter(new FileWriter(fileName, true));
        Date dNow = new Date();
        SimpleDateFormat ft = new SimpleDateFormat("E yyyy.MM.dd 'at' hh:mm:ss a zzz");
        String outString = "\nThe file was modified by the test application : " + ft.format(dNow) + " \n";
        output.append(outString);
        output.close();
    }

    /**
     * Wait for the build to be queued and complete
     *
     * @param jenkins          Jenkins server instance
     * @param expectedBuildNbr the build number we're are expecting
     * @param timeOut          the maximum time in millisecond we are waiting
     * @throws InterruptedException the build was interrupted
     * @throws TimeoutException     we exeeded the timeout period.
     * @throws IOException          an unexpected error occurred while communicating with Jenkins
     */
    private void waitForBuildToComplete(JenkinsServer jenkins, int expectedBuildNbr, long timeOut, String jobname) throws InterruptedException, TimeoutException, IOException {
        boolean buildCompleted = false;
        long timeoutCounter = 0L;
        while (!buildCompleted) {
            Thread.sleep(2000);
            timeoutCounter = timeoutCounter + 2000L;
            if (timeoutCounter > timeOut) {
                throw new TimeoutException("The job did not complete in the expected time");
            }
            //When the build is in the queue, the nextbuild number didn't change.
            //When it changed, It might still be running.
            JobWithDetails wrkJobData = jenkins.getJob(jobname);
            int newNextNbr = wrkJobData.getNextBuildNumber();
            log.info("New Next Nbr:" + newNextNbr);
            if (expectedBuildNbr != newNextNbr) {
                log.info("The expected build is there");
                boolean isBuilding = wrkJobData.getLastBuild().details().isBuilding();
                if (!isBuilding) {
                    buildCompleted = true;
                }
            }
        }
    }

}
