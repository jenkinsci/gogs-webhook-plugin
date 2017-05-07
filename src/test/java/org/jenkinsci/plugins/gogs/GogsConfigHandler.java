package org.jenkinsci.plugins.gogs;

import org.apache.http.HttpHost;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ContentType;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * A class to handle the configuration of the Gogs server used during the integration tests.
 */
public class GogsConfigHandler {

    private String gogsServer_nodeName;
    private int gogsServer_port;
    private String gogsServer_user;
    private String gogsServer_password;


    /**
     * Instantiates an object used to handle operations on a Gogs server.
     * Operations can be verifying that the server up, create a web hook, etc.
     *
     * @param gogsUrl  the Url of the Gogs server (in the form of "http://localhost:3000" for example)
     * @param user     the Gogs user under which the manipulations are done (in general "butler")
     * @param password the password of the Gogs user (in general "butler")
     * @throws MalformedURLException
     */
    public GogsConfigHandler(String gogsUrl, String user, String password) throws MalformedURLException {
        URL aURL = new URL(gogsUrl);

        this.gogsServer_nodeName = aURL.getHost();
        this.gogsServer_port = aURL.getPort();
        this.gogsServer_user = user;
        this.gogsServer_password = password;
    }

    /**
     * A method to wait for the availability of the Gogs server
     *
     * @param retries    the number of times we should try to connect
     * @param retryDelay the number of seconds to wait between tentatives
     * @throws TimeoutException     thrown when number of retries was exhausted
     * @throws InterruptedException thrown when the wait has been interrupted
     */
    void waitForServer(int retries, int retryDelay) throws TimeoutException, InterruptedException {
        String testUrl = this.getGogsUrl() + "/";

        for (int i = 0; i < retries; i++) {
            int status = 0;
            try {
                status = Request.Get(testUrl)
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
        throw new TimeoutException("Timeout waiting for availability of " + testUrl);

    }

    /**
     * Rebuilds the complete Gogs URL from the stored properties
     *
     * @return the complete Gogs URL
     */
    private String getGogsUrl() {
        return "http://" + this.gogsServer_nodeName + ":" + this.gogsServer_port;
    }

    /**
     * Creates a webhook in Gogs
     *
     * @param jsonCommand A json buffer with the creation command of the web hook
     * @param projectName the project (owned by the user) where the webHook should be created
     * @throws IOException something went wrong
     */
    int createWebHook(String jsonCommand, String projectName) throws IOException {

        String gogsHooksConfigUrl = buildGogsHooksConfigUrl(this.getGogsUrl(), this.gogsServer_user, projectName);
        HttpHost httpHost = new HttpHost(this.gogsServer_nodeName, gogsServer_port);
        Executor executor = Executor.newInstance()
                .auth(httpHost, this.gogsServer_user, this.gogsServer_password)
                .authPreemptive(httpHost);


        String result = executor
                .execute(Request.Post(gogsHooksConfigUrl).bodyString(jsonCommand, ContentType.APPLICATION_JSON))
                .returnContent().asString();
        JSONObject obj = new JSONObject(result);
        int id = obj.getInt("id");

        return id;
    }

    int createWebHook(File jsonCommandFile, String projectName) throws IOException {
        byte[] encoded = Files.readAllBytes(jsonCommandFile.toPath());
        String jsonCommand = new String(encoded, Charset.defaultCharset());

        return createWebHook(jsonCommand,projectName);
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

    public String getGogsServer_nodeName() {
        return gogsServer_nodeName;
    }

    public void setGogsServer_nodeName(String gogsServer_nodeName) {
        this.gogsServer_nodeName = gogsServer_nodeName;
    }

    public int getGogsServer_port() {
        return gogsServer_port;
    }

    public void setGogsServer_port(int gogsServer_port) {
        this.gogsServer_port = gogsServer_port;
    }

    public String getGogsServer_user() {
        return gogsServer_user;
    }

    public void setGogsServer_user(String gogsServer_user) {
        this.gogsServer_user = gogsServer_user;
    }

    public String getGogsServer_password() {
        return gogsServer_password;
    }

    public void setGogsServer_password(String gogsServer_password) {
        this.gogsServer_password = gogsServer_password;
    }

}
