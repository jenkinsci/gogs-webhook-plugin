package org.jenkinsci.plugins.gogs;

import org.apache.http.HttpHost;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ContentType;
import org.json.JSONObject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.assertEquals;

//TODO: configure webhook in Gogs
//TODO: Update and push local repository to Gogs

public class GogsWebHook_IT {
    public static final String JENKINS_URL = "http://localhost:8080/";
    public static final String GOGS_URL = "http://localhost:3000";
    public static final String GOGS_USER = "butler";
    public static final String GOGS_PASSWORD = "butler";
    public static final String WEBHOOK_URL = "http://localhost:8080/job/demoapp/build?delay=0";
    final Logger log = LoggerFactory.getLogger(GogsWebHook_IT.class);

    @Rule
    public TestRule watcher = new TestWatcher() {
        protected void starting(Description description) {
            GogsWebHook_IT.this.log.info("\t **************  Start Test ({})  *******************", description
                    .getMethodName());
        }
    };

    @Test
    public void temp() throws Exception {
        waitForSite(JENKINS_URL, 5, 5);
        int status = Request.Get(JENKINS_URL)
                .execute().returnResponse().getStatusLine().getStatusCode();
        assertEquals("Not the expected HTTP status", 200, status);

        waitForSite(GOGS_URL, 5, 5);
        status = Request.Get(GOGS_URL)
                .execute().returnResponse().getStatusLine().getStatusCode();
        assertEquals("Not the expected HTTP status", 200, status);

        String jsonCommand = "{\"type\":\"gogs\",\"config\":{\"url\":\"" + WEBHOOK_URL + "\",\"content_type\":\"json\"},\"events\":[\"create\",\"push\",\"pull_request\"],\"active\":true}";
        String gogsHooksConfigUrl = buildGogsHooksConfigUrl(GOGS_URL, "butler", "demoapp");
        int hookId = createWebHook(gogsHooksConfigUrl, jsonCommand);
    }


    /**
     * A method to wait for the availability of a website
     *
     * @param url        a String representing the URL of the website to test
     * @param retries    the number of times we should try to connect
     * @param retryDelay the number of seconds to wait between tentatives
     * @throws TimeoutException     thrown when number of retries was exhausted
     * @throws InterruptedException thrown when the wait has been interrupted
     */
    void waitForSite(String url, int retries, int retryDelay) throws TimeoutException, InterruptedException {
        for (int i = 0; i < retries; i++) {
            int status = 0;
            try {
                status = Request.Get(url)
                        .execute().returnResponse().getStatusLine().getStatusCode();
            } catch (IOException e) {
                TimeUnit.SECONDS.sleep(retryDelay);
                continue;
            }
            if (status == 200) {
                return;
            } else {
                TimeUnit.SECONDS.sleep(retryDelay);
            }
        }
        throw new TimeoutException("Timeout waiting for availability of " + url);

    }

    /**
     * Creates a webhook in Gogs
     * @param urlString The command url to configure the web hook
     * @param jsonCommand A json buffer with the creation command of the web hook
     * @throws IOException something went wrong
     */
    int createWebHook(String urlString, String jsonCommand) throws IOException {

        URL aURL = new URL(urlString);
        String gogsHostName = aURL.getHost();
        int gogsPort = aURL.getPort();

        HttpHost httpHost = new HttpHost(gogsHostName, gogsPort);
        Executor executor = Executor.newInstance()
                .auth(httpHost, GOGS_USER, GOGS_PASSWORD)
                .authPreemptive(httpHost);


        String result = executor
                .execute(Request.Post(urlString).bodyString(jsonCommand, ContentType.APPLICATION_JSON))
                .returnContent().asString();
        JSONObject obj = new JSONObject(result);
        int id = obj.getInt("id");
        log.debug("ID = " + id);

        return id;
    }

    /**
     * Build the URL used to configure the web hook on the Gogs server
     *
     * @param gogsBaseUrl the protocol, host and port of the Gogs server
     * @param user        the user under which the repository is stored
     * @param projectName the project to add the web hook to
     * @return a properly formatted configuration url
     */
    private String buildGogsHooksConfigUrl(String gogsBaseUrl, String user, String projectName) {
        return gogsBaseUrl + "/api/v1/repos/" + user + "/" + projectName + "/hooks";
    }
}