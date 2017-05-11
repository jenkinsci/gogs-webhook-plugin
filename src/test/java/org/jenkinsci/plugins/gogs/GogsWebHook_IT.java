package org.jenkinsci.plugins.gogs;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

import static org.eclipse.jgit.lib.ConfigConstants.CONFIG_BRANCH_SECTION;
import static org.eclipse.jgit.lib.ConfigConstants.CONFIG_REMOTE_SECTION;
import static org.eclipse.jgit.lib.ConfigConstants.CONFIG_USER_SECTION;
import static org.junit.Assert.fail;

//TODO: Update and push local repository to Gogs

public class GogsWebHook_IT {
    public static final String JENKINS_URL = "http://localhost:8080/";
    public static final String GOGS_URL = "http://localhost:3000";
    public static final String GOGS_USER = "butler";
    public static final String GOGS_PASSWORD = "butler";
    public static final String WEBHOOK_URL = "http://localhost:8080/job/demoapp/build?delay=0";
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
        int hookId = gogsServer.createWebHook(jsonCommandFile, "demoapp");
        log.info("Created hook with ID " + hookId);

        gogsServer.removeHook("demoapp", hookId);

        try {
            gogsServer.createEmptyRepo("demoApp2");
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
        config.setString(CONFIG_USER_SECTION,null,"name","Automated Test");
        config.setString(CONFIG_USER_SECTION,null,"email","test@test.org");
        config.setString(CONFIG_REMOTE_SECTION, "origin", "url","http://localhost:3000/butler/demoApp2.git");
        config.setString(CONFIG_REMOTE_SECTION, "origin", "fetch", "ref-spec");
        config.setString(CONFIG_BRANCH_SECTION, "master", "remote", "origin");
        config.setString(CONFIG_BRANCH_SECTION, "master", "merge", "refs/heads/master");
        config.save();

        //add the files located there and commit them
        Status status = git.status().call();
        DirCache index = git.add().addFilepattern(".").call();
        RevCommit commit = git.commit().setMessage("Repos initialization").call();



        //push
//        RemoteConfig rc = new RemoteConfig(config, "origin");
//        assertFalse(rc.getPushURIs().isEmpty());
//        assertEquals("short:project.git", rc.getPushURIs().get(0).toASCIIString());

//      //Check if Jenkins server is there
//        int status = Request.Get(JENKINS_URL)
//                .execute().returnResponse().getStatusLine().getStatusCode();
//        assertEquals("Not the expected HTTP status", 200, status);

    }


}