package org.jenkinsci.plugins.gogs;

import org.apache.http.client.fluent.Request;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.assertEquals;

//TODO: configure webhook in Gogs
//TODO: Update and push local repository to Gogs

public class GogsWebHook_IT {
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
        waitForSite("http://localhost:8080/", 5, 5);
        int status = Request.Get("http://localhost:8080/")
                .execute().returnResponse().getStatusLine().getStatusCode();
        assertEquals("Not the expected HTTP status", 200, status);

//        Request.Post("http://targethost/login")
//                .bodyForm(Form.form().add("username",  "vip").add("password",  "secret").build())
//                .execute().returnContent();
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
}