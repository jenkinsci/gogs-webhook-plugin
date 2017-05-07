package org.jenkinsci.plugins.gogs;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

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


//        int status = Request.Get(JENKINS_URL)
//                .execute().returnResponse().getStatusLine().getStatusCode();
//        assertEquals("Not the expected HTTP status", 200, status);


    }


}